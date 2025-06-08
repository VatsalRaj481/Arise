//package com.auth_service.controller;
//
//
//import com.auth_service.entity.Users;
//import com.auth_service.repository.UserRepository;
//import com.auth_service.security.JwtUtil;
//import com.auth_service.service.CustomUserDetailsService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.*;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.web.bind.annotation.*;
//
//import java.security.Principal;
//
//@RestController
//@RequestMapping("/auth")
//@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:3000")
//public class AuthController {
//
//    private final UserRepository repo;
//    private final AuthenticationManager authManager;
//    private final CustomUserDetailsService userDetailsService;
//    private final JwtUtil jwtUtil;
//    private final PasswordEncoder encoder;
//
//    @PostMapping("/register")
//    public ResponseEntity<String> register(@RequestBody Users user) {
//        if (repo.findByUsername(user.getUsername()).isPresent())
//            return ResponseEntity.badRequest().body("User already exists");
//
//        user.setPassword(encoder.encode(user.getPassword()));
//        repo.save(user);
//        return ResponseEntity.ok("User registered");
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<String> login(@RequestBody Users loginRequest) {
//        authManager.authenticate(
//                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
//        );
//
//        UserDetails user = userDetailsService.loadUserByUsername(loginRequest.getUsername());
//        String token = jwtUtil.generateToken(user);
//        return ResponseEntity.ok(token);
//    }
////*****//
//    @GetMapping("/me")
//    public ResponseEntity<Users> getAuthenticatedUser(Principal principal) { // Using Principal for authenticated user
//        if (principal == null || principal.getName() == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        // The 'principal.getName()' gives you the username of the currently authenticated user
//        String username = principal.getName();
//
//        return repo.findByUsername(username)
//                .map(user -> {
//                    // Create a DTO for user response if you want to explicitly control fields.
//                    // For now, we'll null it out for direct entity return (less ideal but quick fix)
//                    user.setPassword(null); // Nullify password before sending
//                    return ResponseEntity.ok(user);
//                })
//                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
//    }
//}
//
//

package com.auth_service.controller;

import com.auth_service.dto.UserProfileUpdateDto;
import com.auth_service.entity.Users;
import com.auth_service.repository.UserRepository;
import com.auth_service.security.JwtUtil;
import com.auth_service.service.CustomUserDetailsService;
import com.auth_service.dto.LoginResponseDto; // Import the new DTO
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final UserRepository repo;
    private final AuthenticationManager authManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder encoder;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Users user) {
        if (repo.findByUsername(user.getUsername()).isPresent())
            return ResponseEntity.badRequest().body("User already exists");

        user.setPassword(encoder.encode(user.getPassword()));
        repo.save(user);
        return ResponseEntity.ok("User registered");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody Users loginRequest) { // Changed return type to LoginResponseDto
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        // Fetch the full Users entity to get email (if not already in UserDetails)
        // Ensure your Users entity has 'username' and 'email' fields
        Users user = repo.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found after authentication"));

        // Construct the DTO with token and user details
        LoginResponseDto responseDto = new LoginResponseDto(token, user.getUsername(), user.getEmail());

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/me")
    public ResponseEntity<Users> getAuthenticatedUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = principal.getName();

        return repo.findByUsername(username)
                .map(user -> {
                    user.setPassword(null); // Nullify password before sending sensitive data
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@RequestBody UserProfileUpdateDto updateDto, Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
        }

        String currentUsername = principal.getName();
        Users user = repo.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 1. Validate Current Password (if new password is provided)
        if (updateDto.getNewPassword() != null && !updateDto.getNewPassword().isEmpty()) {
            if (updateDto.getCurrentPassword() == null || updateDto.getCurrentPassword().isEmpty()) {
                return ResponseEntity.badRequest().body("Current password is required to change password.");
            }
            try {
                // Authenticate with current username and provided current password
                authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(currentUsername, updateDto.getCurrentPassword())
                );
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid current password.");
            }
            // If authentication successful, encode and set new password
            user.setPassword(encoder.encode(updateDto.getNewPassword()));
        }


        // 2. Update Username and Email (if provided and changed)
        if (updateDto.getUsername() != null && !updateDto.getUsername().isEmpty() && !updateDto.getUsername().equals(user.getUsername())) {
            if (repo.findByUsername(updateDto.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("New username already taken.");
            }
            user.setUsername(updateDto.getUsername());
        }
        if (updateDto.getEmail() != null && !updateDto.getEmail().isEmpty()) {
            user.setEmail(updateDto.getEmail());
        }

        repo.save(user); // Save the updated user

        return ResponseEntity.ok("Profile updated successfully!");
    }
}