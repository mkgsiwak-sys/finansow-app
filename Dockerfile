# Etap 1: Budowanie aplikacji za pomocą Maven Wrapper i JDK 17
FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /app
# Skopiuj wrapper i pliki projektu
COPY .mvn/ .mvn/
COPY mvnw .
COPY pom.xml .
# Nadaj uprawnienia wykonywania
RUN chmod +x mvnw
# Uruchom pobieranie zależności za pomocą wrappera
RUN ./mvnw dependency:go-offline -B
COPY src/ ./src/
# Uruchom budowanie za pomocą wrappera - USUNIĘTO -Dmaven.main.skip=true
RUN ./mvnw package -DskipTests

# Etap 2: Uruchomienie aplikacji w lekkim środowisku JRE 17
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Skopiuj zbudowany JAR z etapu budowania
COPY --from=builder /app/target/finansow-0.0.1-SNAPSHOT.jar app.jar
# Ustaw port, na którym nasłuchuje aplikacja (domyślnie 8080 dla Spring Boot)
EXPOSE 8080
# Komenda uruchamiająca aplikację
ENTRYPOINT ["java", "-jar", "app.jar"]