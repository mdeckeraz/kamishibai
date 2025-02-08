# Kamishibai

A modern card management system built with Spring Boot and Thymeleaf.

## Features

- User Authentication and Authorization
- Board Management
- Card Creation and Management
- Real-time Updates
- Responsive Design

## Technology Stack

- Java 17
- Spring Boot 3.x
- Spring Security
- Thymeleaf
- H2 Database
- Bootstrap 5
- Gradle

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle 8.x

### Running Locally

1. Clone the repository:
   ```bash
   git clone https://github.com/mdeckeraz/kamishibai.git
   cd kamishibai
   ```

2. Build the project:
   ```bash
   ./gradlew clean build
   ```

3. Run the application:
   ```bash
   ./gradlew bootRun
   ```

The application will be available at `http://localhost:8080`

### Default Credentials

- Username: admin
- Password: admin

## Development

### Project Structure

- `src/main/java/com/kamishibai/` - Main application code
  - `config/` - Configuration classes
  - `controller/` - REST and MVC controllers
  - `dto/` - Data Transfer Objects
  - `model/` - Domain models
  - `repository/` - Data access layer
  - `security/` - Security configuration
  - `service/` - Business logic

### Testing

Run tests with:
```bash
./gradlew test
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
