#
# Build stage
#

FROM maven:3.9.10-eclipse-temurin-21-alpine AS build

WORKDIR /home/app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

#
# Final stage
#

FROM eclipse-temurin:21-jre-alpine

WORKDIR /home/app

COPY upload ./upload

COPY --from=build /home/app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]