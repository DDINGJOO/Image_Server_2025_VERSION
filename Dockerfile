FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 런타임에 필요한 네이티브 라이브러리 설치 (libwebp 런타임)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libwebp7 ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# 호스트에서 빌드된 JAR을 복사 (호스트에서 ./gradlew clean bootJar 실행 필요)
COPY build/libs/*.jar /app/app.jar

# 복사 확인 (디버깅용)
RUN ls -la /app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
