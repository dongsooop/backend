# [Stage 1] 빌드 단계 (JDK 포함 이미지 사용)
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build

# 1. Gradle 래퍼와 설정 파일만 먼저 복사 (캐싱 효율을 위해)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 2. 실행 권한 부여 (윈도우에서 넘어온 파일일 경우 대비)
RUN chmod +x ./gradlew

# 3. 의존성 다운로드 (소스코드 변경 시에도 이 레이어는 캐싱됨)
# --no-daemon: 빌드 후 데몬 프로세스 종료
RUN ./gradlew dependencies --no-daemon

# 4. 소스 코드 복사 및 빌드
COPY src src
# -x test: 테스트는 CI/CD에서 돌리고, 로컬 빌드는 스킵해서 속도 향상
RUN ./gradlew bootJar -PincludeH2 -x test --no-daemon

# -----------------------------------------------------------

# [Stage 2] 실행 단계 (가벼운 JRE 이미지 사용)
FROM eclipse-temurin:17-jre

WORKDIR /app

# 빌드 단계에서 생성된 JAR 파일만 쏙 가져옴
COPY --from=builder ../../build/build/libs/*.jar app.jar

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# 컨테이너 환경을 위한 JVM 옵션 설정
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]