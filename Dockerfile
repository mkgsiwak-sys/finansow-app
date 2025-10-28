# Etap 1: Budowanie aplikacji za pomocą Maven i JDK 17
FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /app
COPY pom.xml .
COPY .mvn/ .mvn/
RUN mvn dependency:go-offline -B
COPY src/ ./src/
RUN mvn package -DskipTests -Dmaven.main.skip=true

# Etap 2: Uruchomienie aplikacji w lekkim środowisku JRE 17
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Skopiuj zbudowany JAR z etapu budowania
COPY --from=builder /app/target/finansow-0.0.1-SNAPSHOT.jar app.jar
# Ustaw port, na którym nasłuchuje aplikacja (domyślnie 8080 dla Spring Boot)
EXPOSE 8080
# Komenda uruchamiająca aplikację
ENTRYPOINT ["java", "-jar", "app.jar"]