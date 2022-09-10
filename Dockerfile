FROM gradle:7.4.2-jdk18 as build

RUN mkdir /app
WORKDIR /app
COPY build.gradle.kts    /app/build.gradle.kts
COPY settings.gradle.kts /app/settings.gradle.kts
RUN gradle clean build || return 0
COPY src /app/src
RUN gradle bootJar


FROM openjdk:18-slim

RUN mkdir /app
COPY --from=build /app/build/libs/verification-*.jar /app/verification-bot.jar
ENTRYPOINT ["java", "-jar", "/app/verification-bot.jar"]