# Kamishibai

A Spring Boot application designed for Google App Engine.

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Google Cloud SDK
- Google Cloud Project with App Engine enabled

## Local Development

1. Build the project:
   ```bash
   mvn clean install
   ```

2. Run locally:
   ```bash
   mvn spring-boot:run
   ```

The application will be available at `http://localhost:8080`

## Deployment to Google App Engine

1. Configure your Google Cloud project:
   ```bash
   gcloud config set project YOUR_PROJECT_ID
   ```

2. Deploy to App Engine:
   ```bash
   mvn package appengine:deploy
   ```

## Project Structure

- `src/main/java/com/kamishibai` - Java source files
- `src/main/resources` - Configuration files and static resources
- `src/main/appengine` - App Engine configuration
- `src/test` - Test files

## Features

- Spring Boot 3.2.2
- Spring Security
- Spring Data JPA
- Thymeleaf templating
- H2 Database (dev)
- Google App Engine configuration

## Development

The project uses:
- Lombok for reducing boilerplate code
- Spring DevTools for hot reloading
- H2 Console for database inspection (dev only)

## Security

The default security configuration (dev only) is:
- Username: admin
- Password: admin

**Note:** Update these credentials before deploying to production.
