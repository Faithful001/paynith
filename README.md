# Paynith Backend

The secure API server and wallet processor powering the **Paynith** bill payment application.

## Features

- **Authentication & Security:** JWT-secured endpoints utilizing Spring Security and `jjwt` with robust request validation.
- **Wallet & Ledger Balance:** Accurate transactional ledger processing for managing wallet balances, deposits, payments, and withdrawals.
- **Payment Gateway Integrations:** Reactive client calls (via WebFlux `WebClient`) integration with Flutterwave and Paystack for processing card charges, OTP/3DS validations, and querying transfer payouts.
- **Database Migrations:** Schema versioning managed via Flyway.
- **Event Streaming:** Apache Kafka integration for message queues and asynchronous processing of transaction updates.
- **API Documentation:** Interactive Swagger UI documentation for testing and integration.

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 4.0.2 (Spring MVC & Spring WebFlux)
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA / Hibernate
- **Migrations:** Flyway
- **Messaging:** Apache Kafka
- **Security:** Spring Security + JSON Web Tokens (JJWT)
- **API Docs:** Springdoc OpenAPI / Swagger UI

## Getting Started

### Prerequisites

Ensure you have the following services installed and running locally:

- [Java 17 JDK](https://adoptium.net/) or higher
- [PostgreSQL](https://www.postgresql.org/) database (running on port `5434` as configured, or modify `application.properties`)
- [Apache Kafka](https://kafka.apache.org/) (running on `localhost:9092`)

### Environment Setup

Create a `.env` file in the root directory of the backend repository (use the following variables as a template):

```env
POSTGRES_DB=paynith_db
POSTGRES_USER=paynith
POSTGRES_PASSWORD=paynith321
PAYSTACK_SEC_KEY=your_paystack_secret_key
FLUTTERWAVE_SEC_KEY=your_flutterwave_secret_key
FLUTTERWAVE_WEBHOOK_HASH=your_webhook_hash
JWT_SEC_KEY=your_jwt_secret_key
WITHDRAWAL_HASH_SEC=your_withdrawal_security_hash
```

### Running the Application

1. Clone the repository and navigate to the backend directory:

   ```bash
   cd paynith
   ```

2. Build the project using Maven:

   ```bash
   ./mvnw clean install
   ```

3. Run the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```
   The server will start on port `8080` (context path `/api/v1`).

### API Documentation

Once the application is running, you can explore the REST endpoints via the Swagger UI dashboard at:
[http://localhost:8080/api/v1/swagger-ui/index.html](http://localhost:8080/api/v1/swagger-ui/index.html)

## License

This project is licensed under the [MIT License](LICENSE).
