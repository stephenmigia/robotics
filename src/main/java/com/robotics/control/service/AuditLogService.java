package com.robotics.control.service;

import com.robotics.control.model.AuditLog;
import com.robotics.control.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {
    private final AuditLogRepository auditRepo;

    public AuditLogService(AuditLogRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    @Transactional
    public void logCommand(String username, String commandType, String payload, String statusDetails) {
        AuditLog log = new AuditLog(username, commandType, payload, statusDetails);
        auditRepo.save(log);
    }

    public List<AuditLog> getAllLogs() {
        return auditRepo.findAll();
    }
}
