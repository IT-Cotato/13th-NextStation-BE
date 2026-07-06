FROM amazoncorretto:17-alpine

# 1. 타임존 설정 (Asia/Seoul)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 2. 보안을 위한 Non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# 3. JAR 파일 복사
ARG JAR_FILE=build/libs/*-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 4. 애플리케이션 실행 (프로필은 SPRING_PROFILES_ACTIVE 환경변수로 주입)
ENTRYPOINT ["java", "-jar", "app.jar"]