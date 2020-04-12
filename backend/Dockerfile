FROM openjdk:12-alpine
COPY ./backend-server/target/scala-2.13/events-api.jar /app/events-api.jar
WORKDIR /
EXPOSE 8080
CMD ["java", "-jar", "/app/events-api.jar"]