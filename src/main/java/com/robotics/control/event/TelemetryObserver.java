package com.robotics.control.event;

import com.robotics.control.model.Position;
import com.robotics.control.model.Status;
import com.robotics.control.model.VirtualEnvironment;
import com.robotics.control.service.UnitService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Listens to live telemetry events from the simulator WebSocket (position, battery, status),
 * updates the in-memory Unit state, and broadcasts to frontend STOMP clients.
 * 
 * CRITICAL ARCHITECTURAL RULE: To prevent flickering and race conditions, sensor data (lidar/proximity)
 * and obstacle grid mapping are NOT processed here. They remain strictly REST-only via UnitService.syncState().
 */
@Component
public class TelemetryObserver {

    private final UnitService unitService;
    private final VirtualEnvironment virtualEnvironment;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public TelemetryObserver(UnitService unitService,
                             VirtualEnvironment virtualEnvironment,
                             SimpMessagingTemplate messagingTemplate) {
        this.unitService = unitService;
        this.virtualEnvironment = virtualEnvironment;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @EventListener
    public void onTelemetryUpdate(TelemetryUpdateEvent event) {
        try {
            String rawJson = event.getPayload();
            SimulatorTelemetryDto payload = objectMapper.readValue(rawJson, SimulatorTelemetryDto.class);

            if (payload != null && payload.position != null && payload.status != null) {
                // 1. Update liveUnit position, battery, and status
                Position pos = new Position(payload.position.x, payload.position.y);
                Status status = Status.valueOf(payload.status);
                unitService.getUnit().updateState(pos, payload.battery, status);

                // 2. Gather latest known sensor readings and obstacles (managed by REST syncState)
                Map<String, Integer> proximityMap = unitService.getUnit().getProximitySensor().getCardinalReadings();
                double[] doubleLidar = java.util.Arrays.stream(unitService.getUnit().getLidarSensor().getDegreeReadings())
                        .asDoubleStream().toArray();

                // 3. Broadcast snapshot to frontend STOMP clients
                LocalBroadcastDto broadcastData = new LocalBroadcastDto(
                        pos.getX(),
                        pos.getY(),
                        payload.battery,
                        payload.status,
                        proximityMap,
                        doubleLidar,
                        virtualEnvironment.getObstacleLocations()
                );
                messagingTemplate.convertAndSend("/topic/telemetry", broadcastData);
            }
        } catch (Exception e) {
            // Silently ignore malformed telemetry frames
        }
    }

    private static class SimulatorTelemetryDto {
        public PositionDto position;
        public double battery;
        public String status;

        public static class PositionDto {
            public int x;
            public int y;
        }
    }

    public static class LocalBroadcastDto {
        public int x;
        public int y;
        public double battery;
        public String status;
        public Map<String, Integer> proximity;
        public double[] lidar;
        public List<Position> obstacles;

        public LocalBroadcastDto(int x, int y, double battery, String status, Map<String, Integer> proximity, double[] lidar, List<Position> obstacles) {
            this.x = x;
            this.y = y;
            this.battery = battery;
            this.status = status;
            this.proximity = proximity;
            this.lidar = lidar;
            this.obstacles = obstacles;
        }
    }
}
