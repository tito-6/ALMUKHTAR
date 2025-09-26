# Transfer System

A Spring Boot application for transfer system management.

## Project Structure

# 🏛️ ALMUKHTAR ELITE MONEY TRANSFER SYSTEM

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 🌟 About ALMUKHTAR

**ALMUKHTAR Elite Money Transfer System** is a premium, enterprise-grade financial transfer platform designed for secure, efficient, and auditable money transfers. Built with cutting-edge technology and robust security features, ALMUKHTAR serves as the backbone for modern financial institutions and money transfer services.

## ✨ Premium Features

### 🔐 **Advanced Security**
- JWT-based authentication with role-based access control
- Encrypted password storage using BCrypt
- Comprehensive audit logging for all transactions
- Multi-level authorization (SUPER_ADMIN, BRANCH_MANAGER, CASHIER, AUDITOR)

### 💼 **Fund Management**
- Multi-currency fund support with Arabic language compatibility
- Real-time balance tracking and validation
- Fund status management (ACTIVE/INACTIVE)
- Branch-specific fund allocation

### 💸 **Transaction Processing**
- Secure peer-to-peer money transfers
- Real-time balance validation and updates
- Transaction status tracking (PENDING, COMPLETED, FAILED)
- Comprehensive transaction history

### 📊 **Enterprise Audit System**
- Complete audit trail for all system activities
- User action logging with timestamps
- Compliance-ready reporting
- Advanced audit filtering and search

### 👥 **User Management**
- Multi-role user system
- Partner user creation and management
- Fund-user association tracking
- User activity monitoring

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- Maven 3.6+
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/tito-6/ALMUKHTAR-ELITE-MONEY-TRANSFER.git
   cd ALMUKHTAR-ELITE-MONEY-TRANSFER
   ```

2. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

3. **Access the system**
   - API Base URL: `http://localhost:8080/api`
   - H2 Console: `http://localhost:8080/h2-console`

## 🔑 API Documentation

### Authentication
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### Fund Management
```http
# Get all funds
GET /api/funds
Authorization: Bearer <token>

# Create new fund
POST /api/funds
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "صندوق الشريك الجديد",
  "balance": 100000.00,
  "status": "ACTIVE"
}
```

### User Management
```http
# Create partner user
POST /api/users
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "partner1",
  "password": "SecurePass123",
  "role": "CASHIER",
  "fundId": 4
}
```

### Transaction Processing
```http
# Transfer money
POST /api/transactions/transfer
Authorization: Bearer <token>
Content-Type: application/json

{
  "senderId": 1,
  "receiverId": 2,
  "fundId": 1,
  "amount": 1000.00
}
```

## 💾 Database Schema

- **users**: User accounts with roles and fund associations
- **funds**: Financial funds with balances and status
- **transactions**: Money transfer records
- **audit_logs**: Complete audit trail

## 🔒 Security Features

- **JWT Tokens**: 24-hour expiration with secure signing
- **Password Encryption**: BCrypt with salt
- **Role-Based Access**: Multi-level permission system
- **Audit Logging**: Complete activity tracking
- **Input Validation**: Comprehensive request validation

## 🏢 Default Users

| Username | Password | Role | Access Level |
|----------|----------|------|--------------|
| admin | admin123 | SUPER_ADMIN | Full System Access |
| manager | manager123 | BRANCH_MANAGER | Branch Operations |
| cashier | cashier123 | CASHIER | Transaction Processing |
| auditor | auditor123 | AUDITOR | Read-Only Audit Access |

## 🔧 Technologies

- **Framework**: Spring Boot 3.3.4
- **Language**: Java 21 LTS
- **Database**: H2 (In-Memory)
- **Security**: Spring Security + JWT
- **Build Tool**: Maven
- **ORM**: Hibernate/JPA

---

**© 2025 ALMUKHTAR Elite Money Transfer System. Built with ❤️ for secure financial transactions.**

## Requirements

- Java 25
- Maven 3.6+
- PostgreSQL (optional, for production)

## Running the Application

### Local Development
```bash
mvn spring-boot:run
```

### With Docker Compose (includes PostgreSQL)
```bash
docker-compose up --build
```

## Configuration

Database configuration is commented out in `application.properties`. 
Uncomment and modify as needed for your PostgreSQL setup.

## Access

- Application: http://localhost:8080
- Default security is disabled for development

## Docker

- `Dockerfile` - For building the application image
- `docker-compose.yml` - For running with PostgreSQL database