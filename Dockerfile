FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# gradle wrapper과 설정, 소스 복사
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src


# 권한 부여 및 빌드 (테스트 생략)
RUN chmod +x ./gradlew && ./gradlew clean bootJar

# 빌드 결과 확인 (디버깅용)
RUN ls -la /app/build/libs

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 런타임에 필요한 네이티브 라이브러리 설치 (libwebp 런타임)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libwebp7 ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# 빌드 스테이지의 JAR 복사 (패턴 사용)
COPY --from=build /app/build/libs/*.jar /app/app.jar

# 복사 확인 (디버깅용)
RUN ls -la /app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
