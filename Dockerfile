FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar target/*.jar"]
