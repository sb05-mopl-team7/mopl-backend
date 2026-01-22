# 1단계: 빌드 스테이지
FROM eclipse-temurin:17-jdk-jammy AS builder

# 작업 디렉토리 설정
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
RUN ./gradlew :mopl-api:dependencies --no-daemon

# 실제 소스 코드 복사 (이후 수정사항은 여기서부터 반영됨)
COPY mopl-core mopl-core
COPY mopl-api mopl-api

# 빌드 실행 (테스트는 제외하고 실행 가능한 jar만 생성)
RUN ./gradlew :mopl-api:clean :mopl-api:bootJar --no-daemon -x test

# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 실행 가능한 jar만 복사 (plain.jar 제외를 위해 패턴 구체화)
COPY --from=builder /build/mopl-api/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]