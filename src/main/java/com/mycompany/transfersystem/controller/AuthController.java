package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.LoginRequest;
import com.mycompany.transfersystem.dto.LoginResponse;
import com.mycompany.transfersystem.ratelimit.RateLimitService;
import com.mycompany.transfersystem.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    public AuthController(AuthService authService, RateLimitService rateLimitService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!rateLimitService.tryConsume("login:" + ip, 10)) {
            return ResponseEntity.status(429).body("Too many requests");
        }
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
}
