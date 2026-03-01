FROM eclipse-temurin:17-jdk AS build
WORKDIR /repo

COPY mvnw mvnw
COPY .mvn .mvn
RUN chmod +x mvnw
COPY pom.xml pom.xml
COPY services/core/pom.xml services/core/pom.xml
COPY services/api/pom.xml services/api/pom.xml
COPY services/worker/pom.xml services/worker/pom.xml

RUN ./mvnw -q -DskipTests -pl services/api -am dependency:go-offline

COPY services/core services/core
COPY services/api services/api

RUN ./mvnw -q -DskipTests -pl services/api -am package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /repo/services/api/target/api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
