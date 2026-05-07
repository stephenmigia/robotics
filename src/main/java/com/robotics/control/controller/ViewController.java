package com.robotics.control.controller;

import com.robotics.control.model.Role;
import com.robotics.control.model.VirtualEnvironment;
import com.robotics.control.service.AuditLogService;
import com.robotics.control.service.UnitService;
import com.robotics.control.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ViewController {

    private final UnitService unitService;
    private final VirtualEnvironment virtualEnvironment;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public ViewController(UnitService unitService,
                          VirtualEnvironment virtualEnvironment,
                          AuditLogService auditLogService,
                          UserService userService) {
        this.unitService = unitService;
        this.virtualEnvironment = virtualEnvironment;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam Role role,
                               Model model) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Username and password are required.");
            return "signup";
        }
        boolean success = userService.registerUser(username.trim(), password, role);
        if (!success) {
            model.addAttribute("error", "Username already taken.");
            return "signup";
        }
        return "redirect:/login?registered=true";
    }

    @GetMapping("/")
    public String index(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";
        boolean isCommander = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMMANDER"));

        unitService.syncState();

        model.addAttribute("username", username);
        model.addAttribute("role", isCommander ? "COMMANDER" : "VIEWER");
        model.addAttribute("isCommander", isCommander);
        model.addAttribute("unit", unitService.getUnit());
        model.addAttribute("environment", virtualEnvironment);
        model.addAttribute("obstacles", virtualEnvironment.getObstacleLocations());
        model.addAttribute("auditLogs", auditLogService.getAllLogs());

        return "index";
    }
}
