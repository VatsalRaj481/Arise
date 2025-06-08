package com.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Example of a DTO for updating user profile (in your backend auth_service.dto package)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDto {
    private String username;
    private String email;
    private String currentPassword; // To verify the user's identity before changing password
    private String newPassword;
}