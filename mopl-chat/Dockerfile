# 1단계: 빌드 스테이지
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

# Gradle 빌드에 필요한 파일들을 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

COPY mopl-core/build.gradle mopl-core/
COPY mopl-api/build.gradle mopl-api/
COPY mopl-batch/build.gradle mopl-batch/
COPY mopl-chat/build.gradle mopl-chat/

# 의존성 라이브러리 미리 다운로드
RUN chmod +x gradlew
RUN ./gradlew :mopl-chat:dependencies --no-daemon

# 실제 소스 코드 복사
COPY mopl-core mopl-core
COPY mopl-chat mopl-chat

# 빌드 실행 (테스트 제외)
RUN ./gradlew :mopl-chat:clean :mopl-chat:bootJar --no-daemon -x test


# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/mopl-chat/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]