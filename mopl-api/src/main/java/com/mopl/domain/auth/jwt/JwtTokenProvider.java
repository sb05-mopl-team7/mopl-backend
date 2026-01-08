package com.mopl.domain.auth.jwt;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

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

    // TODO: application.yml과 .env로 키 분리
    @Value("${JWT_ACCESS_SECRET}")
    private String accessKey;
    private SecretKey accessSecretKey;

    @Value("${JWT_REFRESH_SECRET}")
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
        Long userId = getUserId(token);
        String email = getEmail(token);
        Role role = getRole(token);

        UserPrincipal principal = new UserPrincipal(userId, email, role);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    public String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if ("ACCESS_TOKEN".equals(cookie.getName())) {
                return cookie.getValue();
            }
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

    private Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(accessSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("AccessToken 서명 검증 실패 → RefreshToken key 재시도", e);
            try {
                return Jwts.parserBuilder()
                        .setSigningKey(refreshSecretKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (Exception ex) {
                log.error("RefreshToken 서명 검증도 실패했습니다.", ex);
                throw new MoplException(ErrorCode.FAILED_JWT_TOKEN_PARSE);
            }
        }
    }

    private String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    public Role getRole(String token) {
        String role = getClaims(token).get("role", String.class);
        return Role.valueOf(role);
    }
    public Long getUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }
}