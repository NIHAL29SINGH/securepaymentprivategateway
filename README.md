

# 💳 Secure Payment Gateway – Spring Boot + Razorpay

![License](https://img.shields.io/badge/License-MIT-green.svg)
![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![Razorpay](https://img.shields.io/badge/Razorpay-Integration-blue)
![Redis](https://img.shields.io/badge/Redis-Idempotency-red)
![Grafana](https://img.shields.io/badge/Monitoring-Grafana-purple)

---

# 📌 Project Overview

**Secure Payment Gateway** is a production-style backend payment system built using **Spring Boot + Razorpay** that simulates how real fintech/payment gateway systems work.

This project demonstrates **real-world payment architecture + observability**.

It includes:

* Razorpay payment lifecycle
* Idempotent payments using Redis
* Payment state machine
* Webhook verification
* Refund workflow with admin approval
* Email notifications
* Monitoring using Prometheus + Grafana

---

# 🏛️ Architecture

The application is a stateless Spring Boot payment API. PostgreSQL is the system of record, Redis provides short-lived idempotency and webhook replay protection, and Razorpay remains the external payment provider. Secrets and integration settings are supplied through environment variables.

## System Context

```mermaid
flowchart LR
    customer[Customer]
    admin[Administrator]
    api[Spring Boot Payment API]
    db[(PostgreSQL)]
    redis[(Redis)]
    razorpay[Razorpay APIs]
    mail[SMTP Email Provider]
    metrics[Prometheus]
    dashboards[Grafana]

    customer -->|HTTPS + JWT| api
    admin -->|HTTPS + JWT + ADMIN role| api
    razorpay -->|Signed webhook| api

    api -->|JPA transactions| db
    api -->|Idempotency keys and replay keys| redis
    api -->|Create orders and refunds| razorpay
    api -->|Refund, KYC, invoice notifications| mail
    api -->|Micrometer metrics| metrics
    metrics --> dashboards
```

## Runtime Component Architecture

```mermaid
flowchart TB
    subgraph clients[Clients]
        user[User or Admin]
        checkout[Razorpay Checkout Page]
    end

    subgraph app[Payment Gateway Application]
        security[Spring Security Filter Chain]
        jwt[JwtFilter + JwtUtil]
        controllers[REST Controllers]
        services[Application Services]
        state[PaymentStateMachine]
        metrics[PaymentMetrics + UserMetrics]
        email[EmailService]
        razorConfig[RazorpayConfig]
    end

    subgraph data[Persistence and Cache]
        repositories[Spring Data JPA Repositories]
        postgres[(PostgreSQL)]
        redis[(Redis)]
    end

    razorpay[Razorpay Orders, Payments, Refunds]
    smtp[SMTP Provider]
    prometheus[Prometheus]
    grafana[Grafana]

    user -->|Bearer token and API request| security
    security --> jwt
    jwt --> controllers
    controllers --> services
    services --> state
    services --> repositories
    repositories --> postgres
    services -->|Payment idempotency| redis
    services --> razorConfig
    razorConfig --> razorpay
    razorpay -->|X-Razorpay-Signature webhook| controllers
    services --> email
    email --> smtp
    services --> metrics
    metrics --> prometheus
    prometheus --> grafana
    checkout -->|Hosted checkout interaction| razorpay
```

## Payment Creation and Idempotency Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant API as Spring Boot API
    participant JWT as JwtFilter
    participant Redis
    participant DB as PostgreSQL
    participant RZ as Razorpay

    User->>API: POST /api/payment/create<br/>Authorization + Idempotency-Key
    API->>JWT: Validate JWT and load principal
    JWT-->>API: Authenticated user
    API->>Redis: GET idem:{key}

    alt Cached response exists
        Redis-->>API: Previously created order
        API-->>User: Return same order response
    else First request
        API->>DB: Load user and approved KYC
        DB-->>API: User and KYC status
        API->>RZ: Create order
        RZ-->>API: Razorpay order ID
        API->>DB: Save payment as CREATED
        API->>Redis: SET idem:{key} with short TTL
        API-->>User: Return order ID and amount
    end
```

## Razorpay Webhook Verification and State Flow

```mermaid
sequenceDiagram
    autonumber
    participant RZ as Razorpay
    participant API as Webhook Controller
    participant Redis
    participant DB as PostgreSQL
    participant Metrics as Micrometer

    RZ->>API: POST /api/payment/webhook<br/>Payload + X-Razorpay-Signature
    API->>API: Compute HMAC-SHA256(payload)
    API->>API: Constant-time signature comparison

    alt Invalid signature
        API-->>RZ: Reject request
    else Valid signature
        API->>Redis: SETNX webhook:{payload SHA-256}<br/>TTL 7 days
        alt Duplicate delivery
            Redis-->>API: Key already exists
            API-->>RZ: OK without reprocessing
        else First delivery
            Redis-->>API: Key stored
            API->>DB: Find payment by Razorpay order ID
            API->>DB: Transition payment state
            API->>Metrics: Increment success/failure counters
            API-->>RZ: OK
        end
    end
```

## Refund and Notification Flow

```mermaid
flowchart LR
    user[Authenticated User]
    payment[Payment API]
    db[(PostgreSQL)]
    adminUser[Administrator]
    adminApi[Admin API]
    razorpay[Razorpay Refund API]
    email[EmailService]
    smtp[SMTP Provider]

    user -->|Request refund| payment
    payment -->|Validate ownership and state| db
    payment -->|Set REFUND_REQUESTED| db
    payment -->|Notify admin| email
    email --> smtp
    adminUser -->|Approve or reject| adminApi
    adminApi -->|Update refund state| db
    adminApi -->|Approved refund| razorpay
    razorpay -->|Refund result| adminApi
    adminApi -->|Notify user| email
```

## Deployment and Observability

```mermaid
flowchart TB
    developer[Developer] -->|Push or pull request| github[GitHub]
    github --> actions[GitHub Actions]
    actions --> validate[Maven validate]
    actions --> tests[Maven tests]
    actions --> package[Spring Boot package]
    package --> artifact[Spring Boot JAR artifact]

    subgraph environment[Docker or cloud runtime]
        app[Payment Gateway container]
        postgres[(PostgreSQL container or managed DB)]
        redis[(Redis container or managed cache)]
    end

    docker[Docker/Compose build] --> app
    artifact --> docker
    app --> postgres
    app --> redis
    app --> actuator[Actuator endpoints]
    actuator --> prometheus[Prometheus]
    prometheus --> grafana[Grafana dashboards]
```

### Request and Data Boundaries

| Boundary | Responsibility |
| --- | --- |
| Security filter | Validates JWTs and establishes the authenticated principal |
| REST controllers | Accepts HTTP requests and delegates to services |
| Application services | Applies payment, KYC, refund, email, and idempotency rules |
| State machine | Rejects invalid payment-state transitions |
| PostgreSQL | Durable users, payments, KYC, tokens, and transaction records |
| Redis | Short-lived idempotency responses and webhook replay keys |
| Razorpay | External order, payment, webhook, and refund provider |
| Micrometer | Business and JVM metrics for Prometheus and Grafana |

These diagrams describe the implemented architecture. GitHub Actions validates, tests, and packages the Spring Boot JAR; Docker Compose builds the runtime image from the application Dockerfile. External provider credentials, SMTP, PostgreSQL, and Redis are required for a complete local or cloud runtime.

---

# ✨ Features

## 💰 Razorpay Payment Integration

* Secure Razorpay order creation
* Webhook based payment capture
* Production-style payment lifecycle

---

## 🔁 Idempotent Payments (Redis)

Prevents duplicate payments when users retry requests.

* Redis stores idempotency keys
* Duplicate requests return cached response
* Prevents double charging 💯

---

## 🔐 Payment State Machine

```
CREATED → CAPTURED → SUCCESS → REFUND_REQUESTED → REFUNDED / REJECTED
```

Ensures valid state transitions and data integrity.

---

## 🌐 Razorpay Webhook Integration

* Secure signature verification
* Automatic DB update on payment success
* Event-driven architecture

---

## 🔄 Refund Workflow (Admin Approval)

Real fintech-style refund system:

1. User requests refund
2. Admin receives email
3. Admin approves/rejects refund
4. Razorpay refund triggered
5. User notified via email

---

## 📧 Email Notifications

Automated emails for:

* Refund requested → Admin
* Refund approved → User
* Refund rejected → User
* Invoice PDF emails

---

# 📊 Observability & Monitoring

Full **production monitoring stack** implemented.

### Stack Used

* Spring Boot Actuator
* Micrometer Metrics
* Prometheus
* Grafana

---

## 📈 Custom Business Metrics

| Metric                         | Description            |
| ------------------------------ | ---------------------- |
| payments_total                 | Total payments created |
| payments_success_total         | Successful payments    |
| payments_failure_total         | Failed payments        |
| payments_refunded_total        | Refund count           |
| payments_revenue_total         | Revenue generated      |
| payments_refunded_amount_total | Total refunded money   |
| total_users                    | Total registered users |

These simulate **real fintech KPIs**.

---

## 📊 Grafana Dashboards

### Business Metrics Dashboard

* Payments per minute/hour
* Revenue growth
* Refund trends
* Success vs failure rate
* Total users growth

### System Metrics Dashboard

* JVM memory & CPU
* DB connection pool
* HTTP request metrics
* Thread usage

---

# 🛠 Tech Stack

## Backend

* Java 17
* Spring Boot 4.x
* Spring Security + JWT
* Spring Data JPA

## Payment Gateway

* Razorpay Orders API
* Razorpay Webhooks
* Razorpay Refund API

## Database

* PostgreSQL

## Caching

* Redis

## Monitoring

* Spring Boot Actuator
* Micrometer
* Prometheus
* Grafana

---

# ⚙️ How It Works

## Create Payment

```
POST /api/payment/create
```

Creates Razorpay order and stores payment.

## Payment Success Webhook

Razorpay triggers → DB updated → metrics updated.

## Refund Flow

User → Request refund → Admin → Approve/Reject → Email notifications.

## Monitoring Pipeline

```
Spring Boot → Micrometer → Prometheus → Grafana
```

Prometheus scrapes:

```
/actuator/prometheus
```

---

# 🚀 How to Run

## Clone Repo

```bash
git clone https://github.com/NIHAL29SINGH/securepaymentprivategateway.git
cd securepaymentprivategateway
docker compose up --build
```

## Configure application.properties

```properties
razorpay.key.id=YOUR_KEY
razorpay.key.secret=YOUR_SECRET
razorpay.webhook.secret=YOUR_SECRET
```

Database:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/paymentdb
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
```

Redis:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

## Run Backend

```bash
mvn spring-boot:run
```
## Run Prometheus

```bash
prometheus.exe --config.file=prometheus.yml
```


Open → [http://localhost:9090](http://localhost:9090)

---

## Run Grafana

Open → [http://localhost:3000](http://localhost:3000)
Login → **admin / admin**

---

# 📸 Screenshots

## 📊 Grafana Dashboard

<img src="paymentgateway/screenshots/grafana-dashboard.png" width="900"/>


---

<img src="paymentgateway/screenshots/grafana-dashboard2.png" width="900"/>

---

## 💳 Razorpay Checkout

<img src="paymentgateway/screenshots/razorpay-checkout.png" width="700"/>

---

## ✅ Payment Success

<img src="paymentgateway/screenshots/payment-success.png" width="700"/>

---

## 🔐 OAuth Playground (OAuth 2.0)

<img src="paymentgateway/screenshots/oauth-playground.png" width="900"/>

---

## 🌐 Redis For Idempotency

<img src="paymentgateway/screenshots/redis-working(for idempotency).png" width="900"/>

---

## 🌐 Webhook via Ngrok

<img src="paymentgateway/screenshots/webhook-ngrok.png" width="900"/>

---

## 🚀 Webhook Working

<img src="paymentgateway/screenshots/webhook-working.png" width="900"/>

---

=======

## Run Prometheus

[http://localhost:9090](http://localhost:9090)

## Run Grafana

[http://localhost:3000](http://localhost:3000)
Login → admin / admin


# 👨‍💻 Author

**Nihal Singh**

---

# 📄 License

This project is licensed under the **MIT License**.

---

## Third-Party Licenses

This project uses the following open-source software:

### Prometheus

Licensed under the **Apache License 2.0**
[https://github.com/prometheus/prometheus](https://github.com/prometheus/prometheus)

### Grafana

Licensed under the **Apache License 2.0**
[https://github.com/grafana/grafana](https://github.com/grafana/grafana)
