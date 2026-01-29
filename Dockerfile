# Use Eclipse Temurin base image for Java 8
FROM eclipse-temurin:8-jdk

# Set working directory
WORKDIR /app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Install Maven & build the JAR
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests

# Run the app
CMD ["java", "-cp", "target/NonFollowersFinderBot-1.0-SNAPSHOT.jar", "com.bot.insta.BotLauncher"]
