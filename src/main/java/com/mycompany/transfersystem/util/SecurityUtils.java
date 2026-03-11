package com.mycompany.transfersystem.util;

import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static User resolveUser(UserDetails userDetails, UserRepository userRepository) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userDetails.getUsername()));
    }

    public static Long resolveUserId(UserDetails userDetails, UserRepository userRepository) {
        return resolveUser(userDetails, userRepository).getId();
    }
}
