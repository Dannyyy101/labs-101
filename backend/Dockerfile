# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /app

# Copy the entire project
COPY . .

# Debug: List contents of the current directory
RUN ls -la

# Debug: Print pom.xml content
RUN cat pom.xml

# Ensure correct line endings and make mvnw executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Debug: Check mvnw permissions
RUN ls -l mvnw

# Try to build with mvnw
RUN ./mvnw clean package -DskipTests

# Debug: List contents of target directory
RUN ls -la target

# Debug: Find jar files
RUN find . -name "*.jar"

FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

# Copy the jar file
COPY --from=build /app/target/*.jar app.jar

# Debug: Check if the jar file was copied
RUN ls -la

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]