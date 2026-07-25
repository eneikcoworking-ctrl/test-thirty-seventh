package com.eneik.generated.controller;

import com.eneik.generated.domain.User;
import com.eneik.generated.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        log.info("Received login attempt for user: {}", username);

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("Login failed: Username or password missing");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "errorCode", "UNAUTHORIZED",
                            "message", "Username and password are required.",
                            "timestamp", LocalDateTime.now().toString()
                    ));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            log.warn("Login failed: User '{}' not found", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "errorCode", "UNAUTHORIZED",
                            "message", "Invalid username or password.",
                            "timestamp", LocalDateTime.now().toString()
                    ));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login failed: Password mismatch for user '{}'", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "errorCode", "UNAUTHORIZED",
                            "message", "Invalid username or password.",
                            "timestamp", LocalDateTime.now().toString()
                    ));
        }

        // Establish session
        HttpSession session = request.getSession(true);
        session.setAttribute("admin_user", user.getUsername());
        session.setMaxInactiveInterval(1800); // 30 minutes

        log.info("Login successful. Session established for user '{}'", username);

        return ResponseEntity.ok(Map.of(
                "message", "Successfully authenticated",
                "username", user.getUsername()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("admin_user") == null) {
            log.debug("Status check failed: No active session");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "errorCode", "UNAUTHORIZED",
                            "message", "Not authenticated",
                            "timestamp", LocalDateTime.now().toString()
                    ));
        }

        String username = (String) session.getAttribute("admin_user");
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", username
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("Invalidating session for user: {}", session.getAttribute("admin_user"));
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Successfully logged out"));
    }
}
