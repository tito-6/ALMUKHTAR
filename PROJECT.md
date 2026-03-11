# ALMUKHTAR Elite Money Transfer System — Project Documentation

## 1. Project Overview

**ALMUKHTAR** is an enterprise-grade financial transfer platform for secure, auditable money transfers across branches. It supports multi-currency operations, fee management, release passcodes, and role-based access control.

### Purpose

- Secure peer-to-peer money transfers between users
- Multi-branch operations with branch-specific fees
- Multi-currency support with exchange rates and forex spreads
- Full audit trail for compliance and reporting
- Release passcode workflow for transaction completion

### Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.3.4 |
| Language | Java 21 LTS |
| Database | H2 (development) / PostgreSQL (production) |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| ORM | Hibernate/JPA |
| Build | Maven |
| Validation | Jakarta Bean Validation |

---

## 2. Architecture

### High-Level Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST API (port 8080)                      │
├─────────────────────────────────────────────────────────────────┤
│  Controllers: Auth, Fund, User, Transaction, Fee, ExchangeRate,  │
│              Audit                                               │
├─────────────────────────────────────────────────────────────────┤
│  Services: Auth, Transaction, Fund, User, FeeCalculation,        │
│            ExchangeRate, CurrencyConversion, Audit, etc.          │
├─────────────────────────────────────────────────────────────────┤
│  Repositories (JPA)                                               │
├─────────────────────────────────────────────────────────────────┤
│  Database: H2 (dev) / PostgreSQL (prod)                          │
└─────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.mycompany.transfersystem/
├── config/          # SecurityConfig, DataInitializer, JWT filter
├── controller/      # REST endpoints
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities
├── entity/enums/    # UserRole, FundStatus, TransactionStatus, CommissionScope
├── exception/       # GlobalExceptionHandler, custom exceptions
├── repository/      # JPA repositories
└── service/         # Business logic
```

---

## 3. Domain Model

### Entities

| Entity | Description | Key Fields |
|--------|-------------|------------|
| **User** | System user with role and branch association | username, password, role, fundId, branch |
| **Fund** | Financial fund with balance | name, balance, status |
| **Branch** | Physical or logical branch | name |
| **Transaction** | Money transfer record | sender, receiver, fund, amount, status, releasePasscode |
| **Currency** | Supported currency with rates | code, exchangeRateToUsd, forexBuyingToUsd, forexSellingToUsd |
| **CommissionRate** | Fee configuration per branch/scope | branch, scope, ratePerThousand |
| **BranchFeeRate** | Branch-specific fee overrides | branch, branchFeeRate |
| **AuditLog** | Audit trail record | action, entityType, entityId, details, timestamp |
| **RefreshToken** | JWT refresh token | token, userId, expiry |

### Entity Relationships

```
User ────┬───► Branch (ManyToOne)
         └───► fundId (Long, references Fund)

Transaction ────► User (sender)
Transaction ────► User (receiver)
Transaction ────► Fund

CommissionRate ────► Branch
BranchFeeRate ────► Branch
```

### Enums

- **UserRole**: SUPER_ADMIN, BRANCH_MANAGER, CASHIER, AUDITOR
- **FundStatus**: ACTIVE, INACTIVE
- **TransactionStatus**: PENDING, COMPLETED, FAILED
- **CommissionScope**: PLATFORM_BASE_FEE, PLATFORM_EXCHANGE_PROFIT, SENDING_BRANCH_FEE, RECEIVING_BRANCH_FEE

---

## 4. API Reference

### Authentication

Base URL: `http://localhost:8080/api`

#### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "role": "SUPER_ADMIN",
  "expiresIn": 86400000
}
```

Use `Authorization: Bearer <token>` for subsequent requests.

---

### Fund Management

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/funds` | List all funds | All |
| GET | `/api/funds/{id}` | Get fund by ID | All |
| POST | `/api/funds` | Create fund | SUPER_ADMIN, BRANCH_MANAGER |
| PUT | `/api/funds/{id}` | Update fund | SUPER_ADMIN, BRANCH_MANAGER |
| DELETE | `/api/funds/{id}` | Delete fund | SUPER_ADMIN |

**Create Fund Request:**
```json
{
  "name": "صندوق الشريك الجديد",
  "balance": 100000.00,
  "status": "ACTIVE"
}
```

---

### User Management

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/users` | List all users | SUPER_ADMIN, ADMIN |
| GET | `/api/users/{id}` | Get user by ID | SUPER_ADMIN, ADMIN |
| GET | `/api/users/fund/{fundId}` | Get users by fund | SUPER_ADMIN, ADMIN, BRANCH_MANAGER |
| POST | `/api/users` | Create user | SUPER_ADMIN, ADMIN |
| PUT | `/api/users/{id}` | Update user | SUPER_ADMIN, ADMIN |
| DELETE | `/api/users/{id}` | Delete user | SUPER_ADMIN |

**Create User Request:**
```json
{
  "username": "partner1",
  "password": "SecurePass123",
  "role": "CASHIER",
  "fundId": 4
}
```

---

### Transaction Processing

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/transactions` | List all transactions | SUPER_ADMIN, BRANCH_MANAGER, AUDITOR |
| GET | `/api/transactions/{id}` | Get transaction by ID | All |
| GET | `/api/transactions/user/{userId}` | Get transactions by user | All |
| GET | `/api/transactions/{id}/record` | Get transaction record (with passcode) | SUPER_ADMIN, BRANCH_MANAGER, CASHIER |
| POST | `/api/transactions/transfer` | Simple transfer | SUPER_ADMIN, BRANCH_MANAGER, CASHIER |
| POST | `/api/transactions/transfer-comprehensive` | Transfer with fees & currency | SUPER_ADMIN, BRANCH_MANAGER, CASHIER |
| POST | `/api/transactions/{id}/release` | Release with passcode | SUPER_ADMIN, BRANCH_MANAGER, CASHIER |

**Simple Transfer Request:**
```json
{
  "senderId": 1,
  "receiverId": 2,
  "fundId": 1,
  "amount": 1000.00
}
```

**Comprehensive Transfer Request:**
```json
{
  "senderId": 1,
  "receiverId": 2,
  "fundId": 1,
  "amount": 1000.00,
  "sourceCurrency": "USD",
  "destinationCurrency": "TL",
  "senderBranchId": 2,
  "receiverBranchId": 3
}
```

**Release Request:**
```json
{
  "passcode": "123456",
  "receiverId": 2
}
```

---

### Exchange Rates

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/exchange-rates/{from}/{to}` | Get rate between currencies |
| POST | `/api/exchange-rates/convert` | Convert amount |
| GET | `/api/exchange-rates/currencies` | List currencies |
| GET | `/api/exchange-rates/all/{baseCurrency}` | All rates for base currency |
| GET | `/api/exchange-rates/health` | API health check |

---

### Fee Configuration

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/fees/{branchId}` | Get all fees for branch |
| GET | `/api/admin/fees/{branchId}/{scope}` | Get fee for scope |
| PUT | `/api/admin/fees/{branchId}/{scope}` | Update fee |

---

### QR Withdrawal

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transactions/transfer-for-qr` | Create PENDING transfer for QR release |
| POST | `/api/qr/generate/{transactionId}` | Generate QR code for transaction |
| POST | `/api/qr/scan` | Validate and release via QR scan |

### AI Assistant

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/chat` | Chat with Almukhtar AI (RAG) |
| POST | `/api/ai/corporate/forecast` | Corporate forecast (BRANCH_MANAGER+) |

### Gamification

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/gamification/my-score` | Get current user trust score |
| GET | `/api/gamification/my-badges` | Get current user badges |
| GET | `/api/gamification/leaderboard` | Leaderboard (BRANCH_MANAGER+) |

### Offline Sync

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/sync/queue` | Queue offline transaction |
| POST | `/api/sync/process/{deviceId}` | Process pending sync for device |
| GET | `/api/sync/status/{deviceId}` | Get sync status for device |

### Corporate Accounts

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/corporate/sub-accounts` | Create sub-account |
| GET | `/api/corporate/{parentUserId}/sub-accounts` | List sub-accounts |
| POST | `/api/corporate/payroll/{parentUserId}` | Process payroll batch |
| GET | `/api/corporate/{parentUserId}/ledger-summary` | Get ledger summary |

### Audit

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/audit/logs` | List audit logs |
| GET | `/api/audit/funds/{branchId}` | Fund audit for branch |
| GET | `/api/audit/transactions/search` | Search transactions |
| GET | `/api/audit/fees/history` | Fee history |
| GET | `/api/audit/platform-summary` | Platform summary |
| GET | `/api/audit/branches/{branchId}/transactions` | Branch transactions |
| GET | `/api/audit/branches/{branchId}/commission-rates` | Branch commission rates |

---

## 5. Security

### Authentication

- **JWT Tokens**: 24-hour expiration (configurable via `jwt.expiration`)
- **Stateless**: Session creation policy is STATELESS
- **Public endpoints**: `/api/auth/**`, `/h2-console/**`

### Authorization (Roles)

| Role | Access Level |
|------|--------------|
| SUPER_ADMIN | Full system access |
| BRANCH_MANAGER | Branch operations, fund/user management |
| CASHIER | Transaction processing |
| AUDITOR | Read-only audit access |

### Password Security

- BCrypt encoding with salt
- Minimum 6 characters for new passwords

### Release Passcode Security

- Receiving-branch employees cannot see the release passcode
- Only sender-branch or admin can view the passcode

---

## 6. Features

### Transfer Flow

1. **Simple transfer** (`/transfer`): Deducts from fund, credits receiver, no fees or currency conversion.
2. **Comprehensive transfer** (`/transfer-comprehensive`): Calculates platform fees, branch fees, exchange profit; supports multi-currency; generates release passcode.

### Fee Model

- **Platform Base Fee**: Per $1000 USD (configurable)
- **Platform Exchange Profit**: Per $1000 USD (configurable)
- **Sending Branch Fee**: Per branch
- **Receiving Branch Fee**: Per branch

### Release Passcode

- Generated for comprehensive transfers
- Required to release funds at receiving branch
- Hidden from receiving-branch employees

### Audit System

- Complete audit trail for all system activities
- User action logging with timestamps
- Compliance-ready reporting
- Advanced filtering and search

### Multi-Currency

- Supports USD, EUR, GBP, TL (and others)
- Exchange rates per currency
- Forex buying/selling spreads
- Manual or API-based rate updates

---

## 7. Setup & Run

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- Git

### Development (H2 In-Memory)

```bash
git clone https://github.com/tito-6/ALMUKHTAR.git
cd ALMUKHTAR
mvn spring-boot:run
```

- **API Base URL**: `http://localhost:8080/api`
- **H2 Console**: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`
  - Password: `password`

### Production (PostgreSQL + Docker)

```bash
docker-compose up --build
```

- PostgreSQL on port 5432
- App on port 8080
- Uses `application-prod.properties`

### Build JAR

```bash
mvn clean package
java -jar target/transfer-system-0.0.1-SNAPSHOT.jar
```

---

## 8. Default Data (DataInitializer)

On first run, the application seeds sample data:

### Users

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | SUPER_ADMIN |
| manager | manager123 | BRANCH_MANAGER |
| cashier | cashier123 | CASHIER |
| auditor | auditor123 | AUDITOR |

### Branches

- MAIN_ADMIN_BRANCH
- BRANCH_A
- BRANCH_B

### Funds

| Name | Balance | Status |
|------|---------|--------|
| General Fund | $100,000 | ACTIVE |
| Emergency Fund | $50,000 | ACTIVE |
| Inactive Fund | $25,000 | INACTIVE |

### Currencies

| Code | Name | Rate to USD |
|------|------|-------------|
| USD | US Dollar | 1.00 |
| EUR | Euro | 1.08 |
| GBP | British Pound | 1.25 |
| TL | Turkish Lira | 0.033 |

### Commission Rates

- Platform Base Fee: $1.50/1000 USD (MAIN_ADMIN_BRANCH)
- Platform Exchange Profit: $1.50/1000 USD (MAIN_ADMIN_BRANCH)
- Branch A Sending: $1.50/1000 USD
- Branch A Receiving: $4.00/1000 USD
- Branch B Sending: $1.50/1000 USD
- Branch B Receiving: $4.00/1000 USD

---

## 9. Configuration

### application.properties (Development)

- `spring.datasource.url`: H2 in-memory
- `spring.jpa.hibernate.ddl-auto`: create-drop
- `server.port`: 8080
- `jwt.secret`: Token signing key
- `jwt.expiration`: 86400000 (24 hours)

### application-prod.properties

- PostgreSQL datasource
- `spring.jpa.hibernate.ddl-auto`: update
- Hikari connection pool settings

---

## 10. Database Schema

| Table | Purpose |
|-------|---------|
| users | User accounts with roles and fund associations |
| funds | Financial funds with balances and status |
| transactions | Money transfer records |
| branches | Branch definitions |
| currencies | Currency definitions and exchange rates |
| commission_rates | Fee configuration per branch/scope |
| branch_fee_rates | Branch-specific fee overrides |
| audit_logs | Complete audit trail |
| refresh_tokens | JWT refresh tokens |

---

*© 2025 ALMUKHTAR Elite Money Transfer System*
