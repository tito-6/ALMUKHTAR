package com.mycompany.transfersystem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
@CrossOrigin(origins = "*")
public class DisputeController {

    @GetMapping("/my-disputes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<?>> myDisputes() {
        return ResponseEntity.ok(List.of());
    }
}
