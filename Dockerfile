# --- Étape 1 : construction ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY . .
RUN ./mvnw -B -ntp clean package -pl gesta -am -DskipTests

# --- Étape 2 : exécution ---
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 1001 gesta
WORKDIR /app
COPY --from=build /build/gesta/target/gesta.jar app.jar
USER gesta
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]