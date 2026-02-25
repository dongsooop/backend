FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 헬스체크용 curl 설치 및 비루트 사용자 생성
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd --system app && useradd --system --gid app --home /app app

COPY --chown=app:app build/libs/*.jar app.jar

EXPOSE 8080

USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
