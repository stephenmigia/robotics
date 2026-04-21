package com.robotics.control.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDateTime timestamp;
    private String username;
    private String commandType;
    private String payload;
    private String statusDetails;

    public AuditLog() {}

    public AuditLog(String username, String commandType, String payload, String statusDetails) {
        this.username = username;
        this.commandType = commandType;
        this.payload = payload;
        this.statusDetails = statusDetails;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getStatusDetails() { return statusDetails; }
    public void setStatusDetails(String statusDetails) { this.statusDetails = statusDetails; }
}
