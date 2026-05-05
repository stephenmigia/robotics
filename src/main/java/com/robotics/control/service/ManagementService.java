package com.robotics.control.service;

import com.robotics.control.model.Position;
import com.robotics.control.model.Status;
import com.robotics.control.model.VirtualEnvironment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ManagementService {

    private final UnitService unitService;
    private final VirtualEnvironment virtualEnvironment;
    private final AuditLogService auditLogService;
    private final ResilientCommandDispatcher commandDispatcher;

    public ManagementService(UnitService unitService,
                             VirtualEnvironment virtualEnvironment,
                             AuditLogService auditLogService,
                             ResilientCommandDispatcher commandDispatcher) {
        this.unitService = unitService;
        this.virtualEnvironment = virtualEnvironment;
        this.auditLogService = auditLogService;
        this.commandDispatcher = commandDispatcher;
    }

    public Status getCurrentStatus() {
        return unitService.getCurrentStatus();
    }

    public com.robotics.control.client.ApiClient.MapResponse getMap() {
        return unitService.getMap();
    }

    @PreAuthorize("hasRole('COMMANDER')")
    public String executeMove(int x, int y, String username) {
        Position targetPos = new Position(x, y);

        auditLogService.logCommand(username, "MOVE", targetPos.toString(), "PENDING");

        if (!virtualEnvironment.isWithinBounds(targetPos)) {
            String details = "FAILED: Target coordinate " + targetPos + " is out of bounds!";
            auditLogService.logCommand(username, "MOVE", targetPos.toString(), details);
            throw new IllegalArgumentException(details);
        }

        if (virtualEnvironment.isObstacleAt(targetPos)) {
            String details = "FAILED: Target coordinate " + targetPos + " hits a known obstacle!";
            auditLogService.logCommand(username, "MOVE", targetPos.toString(), details);
            throw new IllegalArgumentException(details);
        }

        if (commandDispatcher.isBusy()) {
            String details = "FAILED: Robot is busy processing previous command!";
            auditLogService.logCommand(username, "MOVE", targetPos.toString(), details);
            commandDispatcher.broadcastStatusUpdate("BUSY", details);
            throw new IllegalStateException(details);
        }

        try {
            commandDispatcher.dispatch(ResilientCommandDispatcher.CommandType.MOVE, targetPos, username);
            return "SUCCESS: Move command accepted and queued in resilient dispatcher.";
        } catch (Exception e) {
            String details = "FAILED: Command dispatcher exception: " + e.getMessage();
            auditLogService.logCommand(username, "MOVE", targetPos.toString(), details);
            throw e;
        }
    }

    @PreAuthorize("hasRole('COMMANDER')")
    public String executeReset(String username) {
        if (commandDispatcher.isBusy()) {
            String details = "FAILED: Robot is busy processing previous command!";
            commandDispatcher.broadcastStatusUpdate("BUSY", details);
            throw new IllegalStateException(details);
        }

        try {
            commandDispatcher.dispatch(ResilientCommandDispatcher.CommandType.RESET, null, username);
            return "SUCCESS: Reset command accepted and queued in resilient dispatcher.";
        } catch (Exception e) {
            throw e;
        }
    }
}
