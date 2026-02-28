# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Important: Ignore Frontend

**DO NOT look at, modify, or engage with the `muse-ai-front/` directory.** This repository contains both frontend (Vue) and backend (Spring Boot) code, but when working with this codebase, only the backend Spring Boot application should be touched.

## Project Overview

This is a Spring Boot 3.5.11 REST API backend service for "Muse AI", built with Java 21 and Maven. It uses MyBatis-Flex as the ORM framework for MySQL database operations.

**Key Technologies:**
- Spring Boot 3.5.11 (requires Java 21)
- MyBatis-Flex 1.11.6 (modern ORM, not MyBatis-Plus)
- MySQL with HikariCP connection pooling
- Knife4j 4.4.0 (OpenAPI 3 / Swagger UI)
- Hutool 5.8.40 (utility library)
- Lombok for code generation

## Running the Application

### Prerequisites
- Java 21
- Maven 3.9+
- MySQL database

### Environment Variable
Set the `MYSQL_PASSWORD` environment variable for database connection:
```bash
# Windows
set MYSQL_PASSWORD=your_password

# Linux/Mac
export MYSQL_PASSWORD=your_password
```

### Commands
```bash
# Run the application (on port 7777)
mvn spring-boot:run

# Build JAR
mvn clean package

# Run JAR
java -jar target/muse-ai-0.0.1-SNAPSHOT.jar

# Run tests
mvn test
```

### API Documentation
- Swagger UI: http://localhost:7777/api/swagger-ui.html
- OpenAPI docs: http://localhost:7777/api/v3/api-docs

## Architecture

### Package Structure (cn.edu.sxu.museai)
```
├── controller/      # REST endpoints
├── service/         # Business logic
│   └── impl/        # Service implementations
├── mapper/          # MyBatis-Flex data access layer
├── model/
│   ├── entity/      # Database entities (e.g., User.java)
│   ├── dto/         # Request objects (XxxRequest)
│   ├── vo/          # Response objects (XxxVO)
│   └── enums/       # Enumerations
├── aop/             # @AuthCheck annotation for role-based access
├── interceptor/     # HTTP interceptors (currently disabled)
├── exception/       # Global exception handling
├── common/          # BaseResponse, ResultUtils, PageRequest
├── config/          # Spring configuration (CORS, MVC)
├── constant/        # Constants
└── generator/       # MyBatis-Flex code generator
```

### Layered Architecture
1. **Controller** → Receives HTTP requests, returns `BaseResponse<T>`
2. **Service** → Business logic implementation
3. **Mapper** → MyBatis-Flex database operations

### Response Pattern
All API responses use `BaseResponse<T>` wrapper via `ResultUtils`:
```java
return ResultUtils.success(data);
return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, "message");
```

## Security & Authentication

### Session-Based Auth
- Uses HTTP session for user authentication
- User stored in session as `UserConstant.USER_INFO` attribute
- Two roles: `user` (default) and `admin`

### @AuthCheck Annotation
Use the `@AuthCheck` annotation on controller methods for role-based access:
```java
@AuthCheck(UserRoleEnum.ADMIN)  // Requires admin role
@AuthCheck(UserRoleEnum.USER)   // Requires logged in user
```

The `AuthAspect` AOP class handles the authorization logic.

### Interceptors
`UserInterceptor` and `AdminInterceptor` exist but are currently **disabled** in `SpringMvcConfiguration`.

## Database

### Connection
- URL: `jdbc:mysql://106.54.215.151:3306/muse_ai`
- Schema file: `sql/create.sql`

### MyBatis-Flex Code Generation
To generate entity, mapper, service, and controller code from database tables:
1. Edit `generator/Codegen.java` to specify tables in `generateTables` array
2. Run the `main` method in `Codegen.java`
3. Generated code goes to `cn.edu.sxu.museai.gen` package (configurable)

## Code Conventions

### Model Layer Pattern
- **Entity** - Database table mapping (e.g., `User.java`)
- **DTO Request** - Input data (e.g., `UserRegisterRequest.java`, `UserQueryRequest.java`)
- **VO** - Output data (e.g., `UserVO.java`, `LoginUserVO.java`)

### Exception Handling
Use `BusinessException` for business logic errors:
```java
throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid parameter");
```

Global exception handler in `GlobalExceptionHandler` catches and formats all exceptions.

### Logical Delete
Entities use `isDelete` (tinyint) for soft deletes. MyBatis-Flex is configured with:
```java
globalConfig.setLogicDeleteColumn("isDelete");
```
