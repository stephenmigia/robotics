package com.robotics.control.controller;

import com.robotics.control.model.Status;
import com.robotics.control.service.ManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/control")
public class Controller {

    private final ManagementService managementService;

    public Controller(ManagementService managementService) {
        this.managementService = managementService;
    }

    @GetMapping("/status")
    public ResponseEntity<Status> getStatus() {
        return ResponseEntity.ok(managementService.getCurrentStatus());
    }

    @GetMapping("/map")
    public ResponseEntity<int[][]> getMap() {
        try {
            com.robotics.control.client.ApiClient.MapResponse mapRes = managementService.getMap();
            if (mapRes != null && mapRes.grid != null) {
                return ResponseEntity.ok(mapRes.grid);
            }
            return ResponseEntity.ok(new int[21][21]);
        } catch (Exception e) {
            return ResponseEntity.status(503).body(null);
        }
    }

    @PostMapping("/move")
    public ResponseEntity<String> move(@RequestParam int x, @RequestParam int y, Principal principal) {
        String username = principal != null ? principal.getName() : "anonymous";
        try {
            String response = managementService.executeMove(x, y, username);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(503).body("API 503: Simulator connection lost. Outage or high latency detected.");
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<String> reset(Principal principal) {
        String username = principal != null ? principal.getName() : "anonymous";
        try {
            String response = managementService.executeReset(username);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(503).body("API 503: Simulator connection lost. Outage or high latency detected.");
        }
    }
}
