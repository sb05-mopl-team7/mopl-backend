package com.mopl.security;

import com.mopl.domain.auth.dto.JwtInformation;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequiredArgsConstructor
public class InMemoryJwtRegistry<T> implements JwtRegistry<T> {

    // <userId, Queue<JwtInformation>>
    private final Map<T, Queue<JwtInformation>> origin = new ConcurrentHashMap<>();
    private final Set<String> accessTokenIndexes = ConcurrentHashMap.newKeySet();
    private final Set<String> refreshTokenIndexes = ConcurrentHashMap.newKeySet();

    private final int maxActiveJwtCount;
    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationEventPublisher eventPublisher;

    @CacheEvict(value = "users", key = "'all'")
    @Override
    public void registerJwtInformation(JwtInformation jwtInformation) {
        origin.compute((T) jwtInformation.getUserDto().id(), (key, queue) -> {
            if (queue == null) {
                queue = new ConcurrentLinkedQueue<>();
            }
            // If the queue exceeds the max size, remove the oldest token
            if (queue.size() >= maxActiveJwtCount) {
                JwtInformation deprecatedJwtInformation = queue.poll();// Remove the oldest token
                if (deprecatedJwtInformation != null) {
                    removeTokenIndex(
                            deprecatedJwtInformation.getAccessToken(),
                            deprecatedJwtInformation.getRefreshToken()
                    );
                }
            }
            queue.add(jwtInformation); // Add the new token
            addTokenIndex(
                    jwtInformation.getAccessToken(),
                    jwtInformation.getRefreshToken()
            );
            return queue;
        });
//    eventPublisher.publishEvent(
//        new UserLogInOutEvent<Long>(jwtInformation.getUserDto().id(), true)
//    );
    }

    @CacheEvict(value = "users", key = "'all'")
    @Override
    public void invalidateJwtInformationByUserId(T userId) {
        origin.computeIfPresent(userId, (key, queue) -> {
            queue.forEach(jwtInformation -> {
                removeTokenIndex(
                        jwtInformation.getAccessToken(),
                        jwtInformation.getRefreshToken()
                );
            });
            queue.clear(); // Clear the queue for this user
            return null; // Remove the user from the registry
        });
//    eventPublisher.publishEvent(
//        new UserLogInOutEvent<>(userId, false)
//    );
    }

    @Override
    public boolean hasActiveJwtInformationByUserId(T userId) {
        return origin.containsKey(userId);
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        return accessTokenIndexes.contains(accessToken);
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        return refreshTokenIndexes.contains(refreshToken);
    }

    @Override
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        origin.computeIfPresent((T) newJwtInformation.getUserDto().id(), (key, queue) -> {
            queue.stream().filter(jwtInformation -> jwtInformation.getRefreshToken().equals(refreshToken))
                    .findFirst()
                    .ifPresent(jwtInformation -> {
                        removeTokenIndex(jwtInformation.getAccessToken(), jwtInformation.getRefreshToken());
                        jwtInformation.rotate(
                                newJwtInformation.getAccessToken(),
                                newJwtInformation.getRefreshToken()
                        );
                        addTokenIndex(
                                newJwtInformation.getAccessToken(),
                                newJwtInformation.getRefreshToken()
                        );
                    });
            return queue;
        });
    }

    @Scheduled(fixedDelay = 1000 * 60 * 5)
    @Override
    public void clearExpiredJwtInformation() {
        origin.entrySet().removeIf(entry -> {
            Queue<JwtInformation> queue = entry.getValue();
            queue.removeIf(jwtInformation -> {
                boolean isExpired =
                        !jwtTokenProvider.validateAccessToken(jwtInformation.getAccessToken()) ||
                                !jwtTokenProvider.validateRefreshToken(jwtInformation.getRefreshToken());
                if (isExpired) {
                    removeTokenIndex(
                            jwtInformation.getAccessToken(),
                            jwtInformation.getRefreshToken()
                    );
                }
                return isExpired;
            });
            return queue.isEmpty(); // Remove the entry if the queue is empty
        });
    }

    private void addTokenIndex(String accessToken, String refreshToken) {
        accessTokenIndexes.add(accessToken);
        refreshTokenIndexes.add(refreshToken);
    }

    private void removeTokenIndex(String accessToken, String refreshToken) {
        accessTokenIndexes.remove(accessToken);
        refreshTokenIndexes.remove(refreshToken);
    }
}
