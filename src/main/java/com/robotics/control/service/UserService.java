package com.robotics.control.service;

import com.robotics.control.model.Role;
import com.robotics.control.model.User;
import com.robotics.control.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean registerUser(String username, String password, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return false; // Username is already taken
        }
        User user = new User(username, passwordEncoder.encode(password), role);
        userRepository.save(user);
        return true;
    }
}
