package com.mopl.domain.auth.jwt;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.user.enums.Role;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@Getter
public class ChatJwtTokenProvider {

    @Value("${jwt.access-secret}")
    private String accessKey;
    private SecretKey accessSecretKey;

    @PostConstruct
    private void initKey() {
        // 채팅 서버는 AccessToken 검증만 하면 되므로 Refresh Key는 필요 없음
        accessSecretKey = Keys.hmacShaKeyFor(accessKey.getBytes(StandardCharsets.UTF_8));
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(accessSecretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("토큰 검증 실패", e);
            return false;
        }
    }

    // 토큰에서 인증 객체 생성
    public Authentication getAuthentication(String token) {
        Long userId = getUserId(token);
        Role role = getRole(token);

        UserPrincipal userPrincipal = new UserPrincipal(userId, role);
        return new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    public Long getUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    public Role getRole(String token) {
        String role = getClaims(token).get("role", String.class);
        return Role.valueOf(role);
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(accessSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("만료된 AccessToken입니다.");
            throw new MoplException(ErrorCode.EXPIRED_TOKEN);
        } catch (Exception e) {
            log.warn("토큰 검증 실패", e);
            throw new MoplException(ErrorCode.INVALID_TOKEN);
        }
    }
}
