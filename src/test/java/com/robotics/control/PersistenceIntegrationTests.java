package com.robotics.control;

import com.robotics.control.model.AuditLog;
import com.robotics.control.model.Role;
import com.robotics.control.model.State;
import com.robotics.control.model.User;
import com.robotics.control.repository.AuditLogRepository;
import com.robotics.control.repository.StateRepository;
import com.robotics.control.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PersistenceIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private StateRepository stateRepository;

    @Test
    void testUserRepositoryPersistence() {
        User user = new User("persisted_user", "secure_pass", Role.VIEWER);
        User saved = userRepository.save(user);

        assertNotNull(saved.getId(), "User ID should be auto-generated upon saving");

        Optional<User> retrieved = userRepository.findByUsername("persisted_user");
        assertTrue(retrieved.isPresent(), "User should be retrievable by username");
        assertEquals("persisted_user", retrieved.get().getUsername());
        assertEquals("secure_pass", retrieved.get().getPassword());
        assertEquals(Role.VIEWER, retrieved.get().getRole());
    }

    @Test
    void testAuditLogRepositoryPersistence() {
        AuditLog auditLog = new AuditLog("commander_user", "MOVE", "{\"x\":5,\"y\":5}", "SUCCESS");
        AuditLog saved = auditLogRepository.save(auditLog);

        assertNotNull(saved.getId(), "AuditLog ID should be auto-generated upon saving");
        assertNotNull(saved.getTimestamp(), "AuditLog timestamp should be populated upon creation");

        Optional<AuditLog> retrieved = auditLogRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent(), "AuditLog should be retrievable by ID");
        assertEquals("commander_user", retrieved.get().getUsername());
        assertEquals("MOVE", retrieved.get().getCommandType());
        assertEquals("{\"x\":5,\"y\":5}", retrieved.get().getPayload());
        assertEquals("SUCCESS", retrieved.get().getStatusDetails());
    }

    @Test
    void testStateRepositoryPersistence() {
        State state = new State(10.0, 15.0, "MOVING");
        State saved = stateRepository.save(state);

        assertNotNull(saved.getId(), "State ID should be auto-generated upon saving");
        assertNotNull(saved.getTimestamp(), "State timestamp should be populated upon creation");

        Optional<State> retrieved = stateRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent(), "State should be retrievable by ID");
        assertEquals(10.0, retrieved.get().getX());
        assertEquals(15.0, retrieved.get().getY());
        assertEquals("MOVING", retrieved.get().getStatus());
    }
}
