# Wallet Management API

A comprehensive Spring Boot application for wallet and transaction management with JWT authentication, built for ING Bank case study.

## 📋 Prerequisites

Before running this application, ensure you have:

- **Java 21** or higher
- **Maven 3.6+** 
- **Git**

## 🛠️ Installation & Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/wallet-management-api.git
   cd wallet-management-api
2. **Generate a JWT Secret Key and set to jwt.secret property in application.properties**
3. **No additional setup required** - H2 database runs automatically

## 🏃‍♂️ Running the Application
### Method 1: Using Maven

  ```bash
  # Clean and compile the project
  mvn clean compile

  # Run the application
  mvn spring-boot:run
  ```
### Method 2: Build JAR and Run
  ```bash
  # Package the application
  mvn clean package
  
  # Run the JAR file
  java -jar target/wallet-management-api-1.0.0.jar
  ```
### Method 2: Using IDE
- Import as Maven project in IntelliJ IDEA or Eclipse

- Locate the main class: WalletManagementApplication.java

- Run the main class directly from your IDE

## 📊 API Access

Once the application is running, you can access it at:

- **Application URL**: http://localhost:8080
- **H2 Database Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/mydb`
  - Username: `sa`
  - Password: sa123

## 🔐 Authentication Flow

### 1. Get JWT Token
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin"
  }'
```
### 2. Use JWT in Subsequent Requests
```bash
curl -X GET "http://localhost:8080/api/wallet/**" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```
## 🧪 Testing
### Run All Tests
```bash
mvn test
```
## 🚀 Build for Production
To build a production-ready JAR:
```bash
mvn clean package -DskipTests
```
The executable JAR will be generated in the target/ directory.
## Code Formatting
The project uses Spotless for code formatting:
```bash
# Format code
mvn spotless:apply

# Check code style
mvn spotless:check
```
