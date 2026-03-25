package com.mopl.domain.contentchat.service;

import com.mopl.domain.contentchat.dto.ContentChatDto;
import com.mopl.domain.contentchat.dto.ContentChatSendRequest;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.s3.S3Manager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContentChatServiceConcurrencyTest {

    private static final int THREAD_COUNT = 120;
    private static final int HOT_USER_COUNT = 3;
    private static final int WARM_UP_ITERATIONS = 5_000;
    private static final int EXTERNAL_IO_PARALLELISM = 8;
    private static final long USER_LOOKUP_DELAY_MS = 15L;
    private static final long PRESIGNED_URL_DELAY_MS = 15L;
    private static final long TOTAL_DURATION_LOWER_BOUND_MS = 250L;
    private static final long P95_LATENCY_LOWER_BOUND_MS = 250L;
    private static final long READY_TIMEOUT_SECONDS = 5L;
    private static final long DONE_TIMEOUT_SECONDS = 15L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Manager s3Manager;

    @Mock
    private RedisManager redisManager;

    @InjectMocks
    private ContentChatService contentChatService;

    private Map<Long, User> usersById;

    @BeforeEach
    void setUp() {
        usersById = createUsers(THREAD_COUNT);
        configureFastDependencies();
    }

    @Test
    @DisplayName("120개 동시 요청이 소수 사용자 정보에 집중되면 외부 조회 병목이 wall-clock latency로 드러난다")
    void createMessage_shouldExposeExternalLookupBottleneck_under120ConcurrentRequests() throws Exception {
        warmUpCreateMessage();
        configureBottleneckingDependencies();
        clearInvocations(userRepository, s3Manager, redisManager);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ConcurrentLinkedQueue<Long> wallClockLatenciesNanos = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<ContentChatDto> results = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Long> requestedUserIds = new ConcurrentLinkedQueue<>();

        for (int requestIndex = 0; requestIndex < THREAD_COUNT; requestIndex++) {
            final long currentUserId = (requestIndex % HOT_USER_COUNT) + 1L;
            executorService.execute(() -> {
                readyLatch.countDown();

                try {
                    startLatch.await();

                    long requestStartNanos = System.nanoTime();
                    ContentChatDto result = contentChatService.createMessage(
                            currentUserId,
                            new ContentChatSendRequest("message-from-" + currentUserId)
                    );
                    long requestLatencyNanos = System.nanoTime() - requestStartNanos;

                    results.add(result);
                    wallClockLatenciesNanos.add(requestLatencyNanos);
                    requestedUserIds.add(currentUserId);
                } catch (Throwable throwable) {
                    failures.add(throwable);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertThat(readyLatch.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("모든 스레드가 동시에 시작할 준비를 마쳐야 한다")
                .isTrue();

        StopWatch stopWatch = new StopWatch("content-chat-concurrency");
        stopWatch.start("120-concurrent-create-message");
        startLatch.countDown();

        assertThat(doneLatch.await(DONE_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .as("모든 요청이 제한 시간 안에 끝나야 한다")
                .isTrue();
        stopWatch.stop();

        executorService.shutdown();
        assertThat(executorService.awaitTermination(3, TimeUnit.SECONDS)).isTrue();

        ConcurrencyMetrics metrics = ConcurrencyMetrics.from(
                wallClockLatenciesNanos,
                stopWatch.getTotalTimeNanos()
        );
        printMetrics(metrics);

        assertThat(failures).as("동시 실행 중 예외가 없어야 한다").isEmpty();
        assertThat(results).hasSize(THREAD_COUNT);
        assertThat(requestedUserIds.stream().distinct())
                .as("부하는 소수의 사용자에게 집중되어야 한다")
                .hasSize(HOT_USER_COUNT);
        assertThat(metrics.totalDurationMs())
                .as("외부 조회 지연이 누적되면 전체 wall-clock 시간은 %dms 이상이어야 한다", TOTAL_DURATION_LOWER_BOUND_MS)
                .isGreaterThanOrEqualTo(TOTAL_DURATION_LOWER_BOUND_MS);
        assertThat(metrics.p95WallClockLatencyMs())
                .as("P95 wall-clock latency는 외부 의존성 병목을 드러낼 만큼 커야 한다")
                .isGreaterThanOrEqualTo(P95_LATENCY_LOWER_BOUND_MS);

        verify(userRepository, times(THREAD_COUNT)).findById(anyLong());
        verify(s3Manager, times(THREAD_COUNT)).generatePresignedUrl(anyString());
    }

    private void configureFastDependencies() {
        org.mockito.BDDMockito.given(userRepository.findById(anyLong()))
                .willAnswer(invocation -> Optional.ofNullable(usersById.get(invocation.getArgument(0))));

        org.mockito.BDDMockito.given(s3Manager.generatePresignedUrl(anyString()))
                .willAnswer(invocation -> "https://cdn.mopl.test/" + invocation.getArgument(0));
    }

    private void configureBottleneckingDependencies() {
        Semaphore userLookupSlots = new Semaphore(EXTERNAL_IO_PARALLELISM, true);
        Semaphore presignedUrlSlots = new Semaphore(EXTERNAL_IO_PARALLELISM, true);

        org.mockito.BDDMockito.given(userRepository.findById(anyLong()))
                .willAnswer(invocation -> withArtificialLatency(
                        userLookupSlots,
                        USER_LOOKUP_DELAY_MS,
                        () -> Optional.ofNullable(usersById.get(invocation.getArgument(0)))
                ));

        org.mockito.BDDMockito.given(s3Manager.generatePresignedUrl(anyString()))
                .willAnswer(invocation -> withArtificialLatency(
                        presignedUrlSlots,
                        PRESIGNED_URL_DELAY_MS,
                        () -> "https://cdn.mopl.test/" + invocation.getArgument(0)
                ));
    }

    private Map<Long, User> createUsers(int count) {
        Map<Long, User> users = new java.util.LinkedHashMap<>(count);

        for (long userId = 1L; userId <= count; userId++) {
            User user = new User("tester-" + userId, "tester-" + userId + "@mopl.com", "password");
            ReflectionTestUtils.setField(user, "id", userId);
            user.updateProfileImageUrl("profile/test-user-" + userId + ".png");
            users.put(userId, user);
        }

        return users;
    }

    private void warmUpCreateMessage() {
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            long userId = (i % THREAD_COUNT) + 1L;
            contentChatService.createMessage(userId, new ContentChatSendRequest("warm-up-" + userId));
        }
    }

    private void printMetrics(ConcurrencyMetrics metrics) {
        System.out.printf(
                """

                [ContentChatService concurrency test]
                threads=%d
                hotUsers=%d
                externalIoParallelism=%d
                totalWallClockDuration=%.3f ms
                avgWallClockLatency=%.3f ms
                p95WallClockLatency=%d ms
                maxWallClockLatency=%d ms

                """,
                THREAD_COUNT,
                HOT_USER_COUNT,
                EXTERNAL_IO_PARALLELISM,
                metrics.totalDurationMs(),
                metrics.avgWallClockLatencyMs(),
                metrics.p95WallClockLatencyMs(),
                metrics.maxWallClockLatencyMs()
        );
    }

    private <T> T withArtificialLatency(Semaphore slots, long delayMs, ThrowingSupplier<T> supplier) throws Exception {
        slots.acquire();
        try {
            TimeUnit.MILLISECONDS.sleep(delayMs);
            return supplier.get();
        } finally {
            slots.release();
        }
    }

    private record ConcurrencyMetrics(
            double totalDurationMs,
            double avgWallClockLatencyMs,
            long p95WallClockLatencyMs,
            long maxWallClockLatencyMs
    ) {
        private static ConcurrencyMetrics from(
                ConcurrentLinkedQueue<Long> wallClockLatenciesNanos,
                long totalDurationNanos
        ) {
            List<Long> sortedWallClockLatencies = new ArrayList<>(wallClockLatenciesNanos);
            sortedWallClockLatencies.sort(Comparator.naturalOrder());

            return new ConcurrencyMetrics(
                    totalDurationNanos / 1_000_000.0,
                    toAverageMillis(sortedWallClockLatencies),
                    toPercentileMillis(sortedWallClockLatencies, 0.95),
                    toMaxMillis(sortedWallClockLatencies)
            );
        }

        private static double toAverageMillis(List<Long> latenciesNanos) {
            return latenciesNanos.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0) / 1_000_000.0;
        }

        private static long toPercentileMillis(List<Long> latenciesNanos, double percentile) {
            if (latenciesNanos.isEmpty()) {
                return 0L;
            }

            int index = Math.min(latenciesNanos.size() - 1, (int) Math.ceil(latenciesNanos.size() * percentile) - 1);
            return TimeUnit.NANOSECONDS.toMillis(latenciesNanos.get(index));
        }

        private static long toMaxMillis(List<Long> latenciesNanos) {
            if (latenciesNanos.isEmpty()) {
                return 0L;
            }

            return TimeUnit.NANOSECONDS.toMillis(latenciesNanos.get(latenciesNanos.size() - 1));
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
