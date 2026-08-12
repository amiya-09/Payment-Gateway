# Payment Gateway

A Spring Boot payment gateway backend inspired by Razorpay-style payment flows. The project models merchant onboarding, API key management, order creation, payment initiation, payment processors, payment gateway adapters, and operational entities such as refunds, settlements, webhook events, and dead-letter queue events.

## Tech Stack

- Java 25
- Spring Boot 4.0.7
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Lombok
- MapStruct
- Maven Wrapper

## Features

- Merchant signup
- API key creation, listing, revocation, and rotation
- Order creation
- Payment initiation
- Payment method routing for `CARD`, `UPI`, `NETBANKING`, and `WALLET`
- JPA entities for payments, refunds, settlements, webhook events, merchants, customers, and vaulted cards
- Global exception handling with structured error responses

## Project Structure

```text
src/main/java/com/springboot/razorpay
|-- common       Shared entities, enums, utilities, and exceptions
|-- merchant     Merchant, customer, auth, and API key modules
|-- operations   Settlement, webhook, and DLQ entities
|-- payment      Orders, payments, processors, gateway adapters, and DTOs
`-- vault        Card vaulting entities
```

## Prerequisites

- JDK 25
- PostgreSQL
- PowerShell, Git Bash, or another terminal capable of running the Maven wrapper

## Database Setup

Create a PostgreSQL database named `razorpayDB`.

```sql
CREATE DATABASE "razorpayDB";
```

Update `src/main/resources/application.properties` with your local database credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/razorpayDB
spring.datasource.username=your_username
spring.datasource.password=your_password
```

The application currently uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Hibernate will update the schema automatically during development.

## Run the Application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

The service starts on the default Spring Boot port:

```text
http://localhost:8080
```

## Run Tests

On Windows:

```powershell
.\mvnw.cmd test
```

On macOS/Linux:

```bash
./mvnw test
```

## API Overview

### Merchant Signup

```http
POST /v1/auth/signup
```

Example request:

```json
{
  "name": "Acme Payments",
  "email": "admin@acme.example",
  "password": "password123",
  "businessName": "Acme",
  "businessType": "PRIVATE_LIMITED"
}
```

### Create API Key

```http
POST /v1/merchants/{merchantId}/api-keys/create
```

Example request:

```json
{
  "environment": "TEST"
}
```

### List API Keys

```http
GET /v1/merchants/{merchantId}/api-keys
```

### Revoke API Key

```http
DELETE /v1/merchants/{merchantId}/api-keys/{keyId}
```

### Rotate API Key

```http
POST /v1/merchants/{merchantId}/api-keys/{keyId}/rotate
```

### Create Order

```http
POST /v1/orders
```

Example request:

```json
{
  "amount": {
    "amountUnits": 50000,
    "currency": "INR"
  },
  "receipt": "receipt_001",
  "notes": {
    "customerId": "cust_123"
  },
  "expiresAt": "2026-08-13T12:00:00"
}
```

### Initiate Payment

```http
POST /v1/payment
```

Example request:

```json
{
  "orderId": "00000000-0000-0000-0000-000000000000",
  "method": "UPI",
  "methodDetails": {
    "vpa": "customer@upi"
  }
}
```

Supported payment methods:

- `CARD`
- `NETBANKING`
- `UPI`
- `WALLET`

## Current Development Notes

- `OrderController` and `PaymentController` currently use a hard-coded merchant ID. The TODO in the code indicates this should be replaced with a merchant context.
- `ApiKeyController` appears to have a malformed base mapping in the current source: `v1/merchants/{merchantId/api-keys`. It should likely be `v1/merchants/{merchantId}/api-keys`.
- Some payment gateway adapter implementations currently return `null`, so end-to-end payment behavior may still need completion.
- PostgreSQL credentials are intentionally blank in `application.properties`; configure them locally before running.

## Build

Create a packaged application:

```powershell
.\mvnw.cmd package
```

The generated artifact will be written under `target/`.
