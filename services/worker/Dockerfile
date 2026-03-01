FROM eclipse-temurin:17-jdk AS build
WORKDIR /repo

COPY mvnw mvnw
COPY .mvn .mvn
RUN chmod +x mvnw

COPY pom.xml pom.xml
COPY services/core/pom.xml services/core/pom.xml
COPY services/worker/pom.xml services/worker/pom.xml

RUN ./mvnw -q -DskipTests -pl services/worker -am dependency:go-offline

# Copy sources
COPY services/core services/core
COPY services/worker services/worker

RUN ./mvnw -q -DskipTests -pl services/worker -am package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /repo/services/worker/target/worker-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
