FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY mvnw .
RUN chmod +x mvnw
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package


FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/worker-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
