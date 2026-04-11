package com.project.nagarSetu.service.authenication;

import com.project.nagarSetu.util.dto.authentication.TokenInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String subject , String email , String roles){
        Map<String , String> map = new HashMap<>();
        map.put("EMAIL" , email);
        map.put("ROLE" , roles);

        log.trace("Jwts Token is created with subject Id : {} ({})", subject , email);
        return Jwts
                .builder()
                .claims(map)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 259_200_000))
                .signWith(getKey())
                .compact();
    }

    public Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public TokenInfo extractClaim(String token){
        final Claims claims = extractAllClaims(token);
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setId(claims.getSubject());
        tokenInfo.setRoles(claims.get("ROLE", String.class));
        tokenInfo.setExpirationAt(claims.getExpiration());

        return tokenInfo;

    }
}
