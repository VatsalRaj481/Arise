package com.project.api_gateway.config;


import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final String SECRET = "b116vjAnwd1/DAffQ0QJVuhpMgmlHSp7IgPUNaliFG2k7eraXqirnJHJVYj8QRx2";

    public void validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT Token");
        }
    }
}
