package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.UserRequest;
import com.mycompany.transfersystem.dto.UserResponse;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToResponse(user);
    }

    public List<UserResponse> getUsersByFundId(Long fundId) {
        List<User> users = userRepository.findByFundId(fundId);
        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse createUser(UserRequest request) {
        // Validate fund exists if fundId is provided
        if (request.getFundId() != null) {
            fundRepository.findById(request.getFundId())
                    .orElseThrow(() -> new RuntimeException("Fund not found with id: " + request.getFundId()));
        }

        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setFundId(request.getFundId());

        User savedUser = userRepository.save(user);

        // Log the action
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        auditService.log("CREATE_USER", currentUser, "User", savedUser.getId());

        return convertToResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Validate fund exists if fundId is provided
        if (request.getFundId() != null) {
            fundRepository.findById(request.getFundId())
                    .orElseThrow(() -> new RuntimeException("Fund not found with id: " + request.getFundId()));
        }

        user.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setRole(request.getRole());
        user.setFundId(request.getFundId());

        User updatedUser = userRepository.save(user);

        // Log the action
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        auditService.log("UPDATE_USER", currentUser, "User", updatedUser.getId());

        return convertToResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Log the action before deletion
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        auditService.log("DELETE_USER", currentUser, "User", user.getId());

        userRepository.delete(user);
    }

    private UserResponse convertToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setFundId(user.getFundId());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        // Get fund name if fundId exists
        if (user.getFundId() != null) {
            try {
                fundRepository.findById(user.getFundId())
                        .ifPresent(fund -> response.setFundName(fund.getName()));
            } catch (Exception e) {
                // If fund lookup fails, just leave fundName as null
                response.setFundName(null);
            }
        }

        return response;
    }
}