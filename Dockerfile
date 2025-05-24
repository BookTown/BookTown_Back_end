FROM openjdk:17
WORKDIR /app
COPY target/*.jar app.jar
COPY src/main/resources/key/stt.json /app/resources/key/stt.json
CMD ["java", "-jar", "app.jar"]