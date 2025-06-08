package com.auth_service.service;
import com.auth_service.entity.Users;
import com.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Users user = repo.findByUsername(username).orElseThrow();
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("USER") // default authority, not based on DB role
                .build();
    }


}
