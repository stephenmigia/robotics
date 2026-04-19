package com.robotics.control;

import com.robotics.control.model.Role;
import com.robotics.control.model.User;
import com.robotics.control.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableRetry
public class ControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(ControlApplication.class, args);
    }

    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                if (userRepository.count() == 0) {
                    User viewer = new User("viewer", passwordEncoder.encode("viewer123"), Role.VIEWER);
                    User commander = new User("commander", passwordEncoder.encode("commander123"), Role.COMMANDER);
                    userRepository.save(viewer);
                    userRepository.save(commander);
                    System.out.println("--- Bootstrapped Default Security Roles ---");
                    System.out.println("Viewer User: 'viewer' / 'viewer123'");
                    System.out.println("Commander User: 'commander' / 'commander123'");
                    System.out.println("-------------------------------------------");
                }
            } catch (Exception e) {
                System.out.println("--- Skipping User Seeding: Database not ready or mocked (" + e.getMessage() + ") ---");
            }
        };
    }
}
