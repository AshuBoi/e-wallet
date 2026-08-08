# AshuBoi Wallet

Event-driven e-wallet microservices that plug into **any payment infrastructure** through a provider port, with strict **Idempotency-Key** protection on transfers and top-up credits.

Built as a portfolio showcase: Spring Boot + Kafka + MySQL backend, Next.js demo UI, one-command Docker Compose, free deploy path on Oracle Cloud Always Free.

## Why this project

| Concern | How it is handled |
| --- | --- |
| Provider lock-in | `PaymentGatewayPort` + `MockPaymentGateway` (Stripe/Razorpay stubs) |
| Double spend / double credit | `Idempotency-Key` store + unique provider payment ids |
| Money correctness | `BigDecimal` balances, optimistic locking (`@Version`) |
| Async settlement | Kafka topics: `USER-CREATED`, `TXN-INIT`, `TXN-COMPLETED`, `WALLET-UPDATED` |
| Auth | JWT register/login; transfer sender taken from token |

## Architecture / System Design

My original system design diagram:

![Screenshot 2023-09-07 185710](https://github.com/user-attachments/assets/93c92b76-910d-4924-a29f-d158ed4df1a6)

```text
Browser (Next.js)
    └─ Nginx gateway :9080
         ├─ /user-service/**        → user-service :8081
         ├─ /wallet-service/**      → wallet-service :8082
         └─ /transaction-service/** → transaction-service :8083

Kafka fan-out
  USER-CREATED  → wallet (create) + notification (welcome email)
  TXN-INIT      → wallet (debit/credit) → TXN-COMPLETED → transaction status
  WALLET-UPDATED → notification (balance email)
```

Interactive storyboard (register → Kafka → wallet init → transfer → notify): open `/architecture` in the Next.js app after Compose is up.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for sequence diagrams and idempotency semantics.

## Quick start (Docker)

Prerequisites: Docker Desktop / Docker Engine + Compose.

```bash
cp .env.example .env
docker compose up --build
```

Open:

- App + API gateway: http://localhost:9080
- Mailhog UI: http://localhost:8025
- Swagger (user-service): http://localhost:8081/swagger-ui.html

Demo users (seeded automatically when `SEED_DEMO=true`):

| Email | Password | Notes |
| --- | --- | --- |
| `alice@demo.local` | `password123` | Starting balance 100.00 |
| `bob@demo.local` | `password123` | Starting balance 100.00 |

### Demo script for recruiters

1. Sign in as Alice.
2. Top up → mock PG → Pay successfully → refresh balance.
3. Transfer to Bob’s `userId` with the shown Idempotency-Key.
4. Click **Replay request** — same `txnId`, no second debit.
5. Open Mailhog to see welcome / balance emails.

## Local development (without full Compose)

Needs JDK 17, MySQL, Kafka on `localhost:9092`.

```bash
# terminals
./mvnw -pl user-service spring-boot:run
./mvnw -pl wallet-service spring-boot:run
./mvnw -pl transaction-service spring-boot:run
./mvnw -pl notification-service spring-boot:run

cd web && npm install && npm run dev
```

Set `NEXT_PUBLIC_API_BASE=http://localhost:8081` only if talking to services directly; behind the gateway leave it empty and use http://localhost:9080 for the UI.

To change the public gateway port, set `GATEWAY_PORT` in `.env` (default `9080`).

## Key APIs

```http
POST /user-service/auth/register
POST /user-service/auth/login
GET  /wallet-service/balance
POST /wallet-service/topups
     Idempotency-Key: <uuid>
POST /wallet-service/webhooks/{provider}
POST /transaction-service/transfers
     Idempotency-Key: <uuid>
GET  /transaction-service/status/{txnId}
GET  /transaction-service/history
```

Idempotency rules:

- Same key + same body → original response
- Same key + different body → `409 Conflict`
- Webhook credits keyed by `provider:providerPaymentId`

## Free deployment (Oracle Always Free)

Full guide: [docs/DEPLOY_ORACLE_FREE.md](docs/DEPLOY_ORACLE_FREE.md)

Summary:

1. Create an Always Free Ampere A1 VM.
2. Install Docker + Compose.
3. Clone this repo, copy `.env`, set `PAYMENT_PUBLIC_BASE_URL` to your public IP/domain.
4. `docker compose up -d --build`
5. Open port 80 in the VCN security list.

Managed PaaS free tiers usually cannot host Kafka + 4 JVMs + MySQL reliably; the free VM path matches this architecture.

## Project layout

```text
common-code/           shared payloads, JWT, filters, errors
user-service/           register/login + USER-CREATED
wallet-service/          ledger, top-ups, PaymentGatewayPort
transaction-service/     transfer orchestration + status
notification-service/    email consumers (Mailhog in Compose)
web/                     Next.js portfolio UI
docker/                  nginx + MySQL init
```

## Portfolio talking points

- Adapter pattern for payment providers without touching ledger code
- Idempotent APIs suitable for flaky mobile clients and webhook retries
- Microservice boundaries with Kafka as the async backbone
- Production hygiene: env-based secrets, Dockerized stack, OpenAPI

## License

Personal portfolio project. Use and adapt freely with attribution.
