package com.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String emailId;
    private String username;
    private String password;

    public String getEmail() {
        return this.emailId;
    }

    public void setEmail(String email) {
        this.emailId=email;
    }
}


