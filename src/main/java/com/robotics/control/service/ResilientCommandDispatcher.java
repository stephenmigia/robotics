package com.robotics.control.service;

import com.robotics.control.client.ApiClient;
import com.robotics.control.model.Position;
import com.robotics.control.model.VirtualEnvironment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ResilientCommandDispatcher {

    public enum CommandType {
        MOVE,
        RESET,
        SYNC_SENSORS
    }

    public static class Command {
        public final CommandType type;
        public final Position targetPosition;
        public final String username;

        public Command(CommandType type, Position targetPosition, String username) {
            this.type = type;
            this.targetPosition = targetPosition;
            this.username = username;
        }
    }

    private final ApiClient apiClient;
    private final UnitService unitService;
    private final VirtualEnvironment virtualEnvironment;
    private final AuditLogService auditLogService;
    private final SimpMessagingTemplate messagingTemplate;
    
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile Command currentPendingCommand = null;

    public ResilientCommandDispatcher(ApiClient apiClient,
                                      UnitService unitService,
                                      VirtualEnvironment virtualEnvironment,
                                      AuditLogService auditLogService,
                                      SimpMessagingTemplate messagingTemplate) {
        this.apiClient = apiClient;
        this.unitService = unitService;
        this.virtualEnvironment = virtualEnvironment;
        this.auditLogService = auditLogService;
        this.messagingTemplate = messagingTemplate;
    }

    public boolean isBusy() {
        return isProcessing.get();
    }

    public Command getCurrentPendingCommand() {
        return currentPendingCommand;
    }

    public void dispatch(CommandType type, Position target, String username) {
        if (isProcessing.compareAndSet(false, true)) {
            currentPendingCommand = new Command(type, target, username);
            String initialMsg = "Robot processing command: " + type + (target != null ? " to " + target : "");
            
            // Broadcast initial status
            broadcastStatusUpdate("PROCESSING", initialMsg);
            
            executor.submit(() -> {
                boolean success = false;
                int attempt = 0;
                while (!success) {
                    attempt++;
                    try {
                        if (type == CommandType.MOVE) {
                            apiClient.postMove(target);
                        } else if (type == CommandType.RESET) {
                            virtualEnvironment.clearObstacles();
                            apiClient.postReset();
                        } else if (type == CommandType.SYNC_SENSORS) {
                            unitService.syncState();
                        }
                        success = true;
                    } catch (Exception e) {
                        // SMART SUCCESS DETECTION:
                        // Check if the simulator is actually reachable now and if the command already succeeded!
                        try {
                            ApiClient.StatusResponse statusRes = apiClient.fetchStatus();
                            if (type == CommandType.MOVE) {
                                if ("MOVING".equalsIgnoreCase(statusRes.status) || 
                                    (statusRes.position.x == target.getX() && statusRes.position.y == target.getY())) {
                                    success = true;
                                    break;
                                }
                            } else if (type == CommandType.RESET) {
                                if (statusRes.position.x == 0 && statusRes.position.y == 0 && statusRes.battery == 100.0) {
                                    success = true;
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            // Simulator is still completely unreachable (e.g. 503 outage). Keep retrying!
                        }

                        if (!success) {
                            String errMsg = "Command " + type + " failed (Attempt " + attempt + "). Retrying... Error: " + e.getMessage();
                            System.err.println(errMsg);
                            
                            broadcastStatusUpdate("RETRYING", errMsg);
                            auditLogService.logCommand(username, type.name(), target != null ? target.toString() : "", "RETRYING: Attempt " + attempt);
                            
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
                
                try {
                    if (type != CommandType.SYNC_SENSORS) {
                        unitService.syncState();
                    }
                    auditLogService.logCommand(username, type.name(), target != null ? target.toString() : "", "SUCCESS");
                } catch (Exception e) {
                    // Ignore sync errors
                }
                
                currentPendingCommand = null;
                isProcessing.set(false);
                broadcastStatusUpdate("ONLINE", "Robot is ready for commands.");
            });
        } else {
            if (type == CommandType.SYNC_SENSORS) {
                return;
            }
            String busyMsg = "Robot is busy processing previous command (" + 
                    (currentPendingCommand != null ? currentPendingCommand.type : "UNKNOWN") + ")!";
            broadcastStatusUpdate("BUSY", busyMsg);
            throw new IllegalStateException(busyMsg);
        }
    }

    public void broadcastStatusUpdate(String state, String message) {
        Map<String, Object> payload = Map.of(
            "type", "COMMAND_STATUS",
            "state", state,
            "message", message,
            "pending", currentPendingCommand != null
        );
        messagingTemplate.convertAndSend("/topic/telemetry", (Object) payload);
    }
}
