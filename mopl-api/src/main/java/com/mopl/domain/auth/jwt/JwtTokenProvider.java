package com.mopl.domain.auth.jwt;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@Getter
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.access-secret}")
    private String accessKey;
    private SecretKey accessSecretKey;

    @Value("${jwt.refresh-secret}")
    private String refreshKey;
    private SecretKey refreshSecretKey;

    @PostConstruct
    private void initKey() {
        accessSecretKey = Keys.hmacShaKeyFor(accessKey.getBytes(StandardCharsets.UTF_8));
        refreshSecretKey = Keys.hmacShaKeyFor(refreshKey.getBytes(StandardCharsets.UTF_8));
    }

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;
    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    public String createAccessToken(User user) {
        return createToken(user, accessTokenValidity, accessSecretKey);
    }

    public String createRefreshToken(User user) {
        return createToken(user, refreshTokenValidity, refreshSecretKey);
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, accessSecretKey);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshSecretKey);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token, accessSecretKey);

        Long userId = claims.get("userId", Long.class);
        String email = claims.getSubject();
        Role role = Role.valueOf(claims.get("role", String.class));

        UserPrincipal principal = new UserPrincipal(userId, email, role);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    private String createToken(User user, long validity, Key key) {

        Instant now = Instant.now();
        Instant expiry = now.plusMillis(validity);

        return Jwts.builder()
                .setIssuer("mopl-app")
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    private boolean validateToken(String token, Key key) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private Claims getClaims(String token, Key key) {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
    }

    public Long getUserId(String accessToken) {
        return getClaims(accessToken, accessSecretKey).get("userId", Long.class);
    }

    public Long getUserIdFromRefreshToken(String refreshToken) {
        return getClaims(refreshToken, refreshSecretKey).get("userId", Long.class);
    }
}