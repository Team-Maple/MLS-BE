FROM eclipse-temurin:21-jre-jammy

WORKDIR /

COPY ./build/libs/api-0.0.1-SNAPSHOT.jar /app.jar

ENTRYPOINT ["java","-Dspring.profiles.active=prod","-jar","/app.jar"]
