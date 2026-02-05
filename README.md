

---

# 💳 Secure Payment Gateway – Spring Boot + Razorpay

![License](https://img.shields.io/badge/License-MIT-green.svg)
![Java](https://img.shields.io/badge/Java-17+-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Razorpay](https://img.shields.io/badge/Razorpay-Integration-blue)
![Redis](https://img.shields.io/badge/Redis-Idempotency-red)
![Grafana](https://img.shields.io/badge/Monitoring-Grafana-purple)

---

## 📌 Project Overview

**Secure Payment Gateway** is a production-style backend payment system built using **Spring Boot + Razorpay** that simulates how real fintech/payment gateway systems work.

It implements real-world payment architecture including:

* Razorpay payment integration
* Idempotent payments using Redis
* Payment state machine
* Webhook verification & DB updates
* Refund workflow with Admin approval
* Email notifications
* Monitoring using Prometheus + Grafana

This project demonstrates **backend engineering for fintech systems**.

---

## ✨ Features

### 💰 Razorpay Payment Integration

* Create Razorpay orders securely
* Capture payment via webhook
* Store transaction details in database
* Production-style payment lifecycle

---

### 🔁 Idempotent Payments (Redis)

Prevents duplicate payments if user clicks Pay multiple times.

* Redis stores idempotency keys
* Duplicate requests return cached response
* Prevents double charges 💯

---

### 🔐 Payment State Machine

Implements real payment lifecycle:

```
CREATED → CAPTURED → SUCCESS → REFUND_REQUESTED → REFUNDED / REJECTED
```

Prevents invalid state transitions and ensures **data integrity**.

---

### 🌐 Razorpay Webhook Integration

* Secure signature verification
* Updates DB automatically on payment success
* No manual polling required

---

### 🔄 Refund Workflow (Admin Approval)

Real-world refund flow implemented:

1. User requests refund
2. Admin receives email notification
3. Admin approves/rejects via API
4. User receives final email

---

### 📧 Email Notifications

Automated email system for:

* Refund requested → Admin email
* Refund approved → User email
* Refund rejected → User email

---

### 📊 Monitoring & Metrics

Full observability stack included:

* Spring Boot Actuator metrics
* Prometheus scraping
* Grafana dashboards

Track:

* Payment success rate
* Payment failures
* Application health

---

### 🧠 Production-Ready Backend Design

* JWT Authentication
* REST API architecture
* MySQL/PostgreSQL support
* Clean layered architecture

---

## 🛠 Tech Stack

### 🔹 Backend

* **Java 17**
* **Spring Boot**
* Spring Security + JWT
* Spring Data JPA (Hibernate)

### 🔹 Payment Gateway

* **Razorpay Orders API**
* **Razorpay Webhooks**
* Razorpay Refund API

### 🔹 Database

* **MySQL / PostgreSQL**

### 🔹 Idempotency & Caching

* **Redis**

### 🔹 Monitoring

* **Spring Boot Actuator**
* **Prometheus**
* **Grafana**

### 🔹 Tools

* Postman
* IntelliJ IDEA
* Git & GitHub

---

## ⚙️ How It Works

### 1️⃣ Create Payment

Client sends request to backend:

```json
POST /api/payment/create
{
  "amount": 500
}
```

Backend:

* Generates idempotency key
* Checks Redis
* Creates Razorpay Order
* Stores payment in DB

---

### 2️⃣ Payment Checkout

Frontend opens Razorpay checkout using:

```
orderId
razorpayKey
```

User completes payment.

---

### 3️⃣ Razorpay Webhook Triggered

Razorpay sends secure webhook:

```
payment.captured
```

Backend:

* Verifies signature 🔐
* Updates DB → SUCCESS
* Stores paymentId

---

### 4️⃣ Refund Workflow

#### User Requests Refund

```
POST /api/payment/refund/request/{paymentId}
```

Backend:

* Moves payment → REFUND_REQUESTED
* Sends email to Admin

---

#### Admin Approves Refund

```
POST /api/admin/refund/approve/{paymentId}
```

Backend:

* Calls Razorpay Refund API
* Updates DB → REFUNDED
* Sends email to user

---

#### Admin Rejects Refund

```
POST /api/admin/refund/reject/{paymentId}
```

User receives rejection email.

---

### 5️⃣ Metrics & Monitoring

Prometheus scrapes metrics → Grafana visualizes dashboards.

---

## 🚀 How to Run

## 🧩 1. Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/payment-gateway.git
cd payment-gateway
```

---

## 🧩 2. Configure application.properties

Open:

```
src/main/resources/application.properties
```

Add Razorpay keys:

```properties
razorpay.key.id=YOUR_KEY
razorpay.key.secret=YOUR_SECRET
razorpay.webhook.secret=YOUR_WEBHOOK_SECRET
```

Configure database:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/paymentdb
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
```

Configure Redis:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

---

## 🧩 3. Run Backend

```bash
mvn spring-boot:run
```

Backend runs on:

```
http://localhost:8080
```

---

## 🧩 4. Run Prometheus

```bash
prometheus.exe --config.file=prometheus.yml
```

Open:

```
http://localhost:9090
```

---

## 🧩 5. Run Grafana

Open:

```
http://localhost:3000
```

Login:

```
admin / admin
```

Add Prometheus datasource:

```
http://localhost:9090
```

Create dashboards 🎉

---

## 📸 Screenshots

(Add your Grafana & Postman screenshots here)

```
screenshots/grafana-dashboard.png
screenshots/payment-success.png
screenshots/refund-flow.png
```

---

## 👨‍💻 Author

**Nihal Singh**
Built as a personal fintech backend project for portfolio.

© 2026 Nihal Singh. All rights reserved.

---

## 📄 License

This project is licensed under the **MIT License**.
