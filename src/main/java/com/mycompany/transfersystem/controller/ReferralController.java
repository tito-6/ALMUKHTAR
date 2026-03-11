package com.mycompany.transfersystem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/referral")
@CrossOrigin(origins = "*")
public class ReferralController {

    @GetMapping("/my-code")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> myCode() {
        return ResponseEntity.ok(Map.of("code", "", "totalReferrals", "0"));
    }
}
