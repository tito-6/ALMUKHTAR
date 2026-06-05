# ALMUKHTAR Backend — Situation Report

**Date:** 2026-06-05
**Repo:** `d:\almukhtar`
**Stack:** Spring Boot 3.3.4 · Java 21 · PostgreSQL 16 · Flyway · Redis (optional) · JWT/RBAC · ShedLock · WebFlux client (WhatsApp)
**Build status:** `mvnw test` → **96 tests, 0 failures, 0 errors, 7 skipped** (green)
**Migrations:** V1 → V51 (51 Flyway scripts)
**Controllers (API surface):** ~57 REST controllers + 2 webhooks

---

## 1. Executive Summary

The backend is **feature-complete across ~40 functional modules** and is now hardened on the three highest-risk areas from the audit:

1. **Transaction lifecycle correctness** — transfers are no longer marked `COMPLETED` before the cash is actually released.
2. **Mandatory idempotency** — all money-moving endpoints reject duplicate requests and never double-debit.
3. **Notification backbone (WhatsApp + in-app)** — a persistent, retrying outbox decoupled from the financial commit, covering virtually every money event.

The system **compiles and all tests pass**. What remains is mostly **hardening, exposure of a few flows via REST, broader integration tests, observability, and production cutover** (live WhatsApp credentials, deployment, docs). Detailed gaps and a step-by-step finishing plan are in Sections 5–6.

---

## 2. Technology & Architecture

| Concern | Implementation |
|---|---|
| Framework / Language | Spring Boot 3.3.4, Java 21 |
| Persistence | PostgreSQL 16, Spring Data JPA, Flyway (V1–V51) |
| Dev/Test DB | H2 (`MODE=PostgreSQL`), `ddl-auto=create-drop`, Flyway disabled in tests |
| Security | JWT auth, method-level RBAC (`@PreAuthorize`), Bucket4j rate limiting, MDC correlation, tenant resolver |
| Money safety | `IdempotencyService` + `@RequireIdempotencyKey` AOP, pessimistic locks, double-entry ledger, `@PlatformRevenue` aspect |
| Notifications | Event-driven (`AFTER_COMMIT`), outbox table + scheduled worker, WhatsApp provider abstraction, in-app store |
| Async / Scheduling | 4 dedicated executors (batch, scoring, notification, trading), `@Scheduled` + ShedLock distributed locks |
| External | Meta WhatsApp Cloud API, exchange-rate API, ElevenLabs voice, Spring AI (toggleable) |
| Packaging | `Dockerfile` + `docker-compose.yml` present |

---

## 3. Complete Feature Inventory (all program features)

### A. Core money movement
- **Core Transfer Engine** — simple transfer, transfer-for-QR, comprehensive branch transfer
- **Transaction Lifecycle** — `DRAFT → PENDING → PENDING_PAYOUT → READY_FOR_PICKUP → RELEASED → COMPLETED` (+ `FAILED / CANCELLED / EXPIRED`)
- **Payout Completion Service** — single idempotent command for QR-scan and passcode releases
- **QR Smart Bridge** — QR generation, encrypted tokens, scan validation, TOTP
- **Release Passcode** — counter payout with role-based passcode visibility
- **Branch Cash Inventory / Reservations / Movements** — vault & drawer balances
- **Cashier Shifts & Ledger** — open/close shift, shift balances, drawer movements
- **Liquidity Management** — cash transfer orders, reconciliation, liquidity alerts
- **Payout Reservations**

### B. Wallet & customer products
- **Wallet & KYC** — balances, freeze, exchange, applications
- **Wallet Top-Up** — request + cashier completion
- **Cash-Out / Withdrawal**
- **Prepaid Cards**
- **Family Wallets** — group + members + spending limits
- **Savings Goals**
- **Split Payments**
- **Recurring Transfers**
- **Referrals**
- **Gamification** — trust score, badges

### C. Merchant / corporate / billing
- **Merchant Payments** — pay by merchant QR, receipts
- **Merchants** — onboarding/management
- **Bill Payments** — providers, pay, manual completion
- **Batch Transfers** — corporate jobs, templates, rows, execution
- **Corporate Accounts**

### D. Lending & markets
- **Micro-Lending (Loans)** — application, credit scoring, approval, disbursement, repayment, schedules, due/late handling
- **Trading Platform** — accounts, order placement/execution, market data
- **Trading Risk Profiles**, **Tradable Asset Admin**, **Price Alerts**, **Trading Assistant**
- **Forward Contracts**
- **Escrow Contracts** (engine present; see gap in §5)

### E. Trust, risk & compliance
- **Double-Entry Ledger (Accounting)**
- **Platform Revenue tracking** + AOP aspect
- **Fee Calculation**, **Fee Zones**, **Fee Configuration**
- **AML Monitoring** — rule engine, alerts
- **Account Freeze** — cases, events, public views
- **Dispute Management** — open/resolve
- **Audit** — audit service + reporting endpoints
- **Idempotency** — records, conflict/replay handling

### F. Notifications & messaging
- **WhatsApp** — provider interface, Meta provider, mock provider, template service/registry, send results
- **Notification Outbox** — entity, service, scheduled worker, processor, retry/backoff, dead-letter
- **Notification Delivery Logs** — per-attempt status + provider message id
- **Notification Preferences** — whatsapp / transaction / security / marketing toggles, language
- **Recipient Resolver & Policy** — role-based routing (sender/receiver/cashier/manager/admin/owner/merchant/auditor)
- **WhatsApp Webhook** — delivery-status callback + signature verification
- **In-App Notifications**, **Notification Campaigns**, **Notification Admin**

### G. Platform, ops & misc
- **Auth & User Management**
- **Fund & Branch Management**, **Branch Geo / Branch Finder**, **Branch Analytics / Hourly Stats**
- **Platform Owner Dashboard**
- **Multi-Tenancy**
- **Multi-Language i18n** (Arabic-first, `ar` default)
- **Document Vault & Biometric (WebAuthn)**
- **Offline Sync Queue** (device sync, idempotency)
- **AI Assistant (RAG)**, **AI Helper**, **ElevenLabs voice webhook**
- **Status / health endpoints**

---

## 4. What We Implemented Recently (P0 Trust + WhatsApp + Lifecycle)

| Area | Delivered |
|---|---|
| Lifecycle | New `TransactionStatus` values; `READY_FOR_PICKUP` / `RELEASED` semantics; `PayoutCompletionService` unifying QR + passcode payout (transactional + idempotent) |
| Events | `TransferReadyForPickupEvent`, `TransferReleasedEvent` (+ full financial event family) published `AFTER_COMMIT` |
| Security | Role/branch-based release-passcode visibility (`getTransactionRecordForActor`) |
| Idempotency | `IdempotencyService` reconciled to the AOP contract (status+JSON-body envelope; replay, 409 on body mismatch, 409 on in-progress, retry on failure) |
| Idempotency wiring | `@RequireIdempotencyKey` enforced on: `transfer`, `transfer-for-qr`, `transfer-comprehensive`, `{id}/release`, `qr/scan`, wallet `topup/request` + `topup/{id}/complete` + `exchange`, `merchant/pay`, `bills/pay` + `bills/admin/{id}/complete`, `lending/.../repay`, `trading/orders`, `batch .../execute` |
| Notification backbone | Outbox worker (`@Scheduled` + ShedLock), processor with retry/backoff/dead-letter, delivery logs, `almukhtar.whatsapp.*` config namespace (default language `ar`), mock + Meta providers, webhook signature verification |
| Tests | `IdempotencyHttpIT` (missing key → 400, same key/body → replay & no double debit, different body → 409, per-user scoping); outbox worker/service/preference/webhook tests |

---

## 5. What's Missing / Honest Gaps

> Severity: **P1** = needed before production · **P2** = strongly recommended · **P3** = nice-to-have

1. **(P1) Escrow money endpoints not exposed.** `EscrowController` only has `GET /my-contracts`. There are **no `fund` / `release` / `dispute` POST endpoints**, so escrow money flow can't be driven or made idempotent via REST yet. Events/policies exist but have no HTTP entry point.
2. **(P1) Live WhatsApp not verified end-to-end.** Provider, templates, webhook, and outbox are built, but no run against real Meta credentials / approved templates. Template names must be registered & approved in Meta Business Manager.
3. **(P1) Phase-10 financial integrity ITs incomplete.** `IdempotencyHttpIT` and WhatsApp dispatch ITs exist, but a dedicated **`LedgerRevenueConsistencyIT`** (fee booked exactly once across ledger + `PlatformRevenueService`, receiver principal == promised payout, sender branch pays all fees) and a full **`TransferLifecycleNotificationIT`** (creation sends no completed-receipt; rollback enqueues nothing) are not yet asserted.
4. **(P2) Withdrawal/cash-out idempotency coverage.** Confirm `cash-out` POST endpoints carry `@RequireIdempotencyKey` (top-up + exchange are covered; verify cash-out path).
5. **(P2) Observability gap.** No `spring-boot-actuator`, Micrometer, or Prometheus metrics; no health/readiness probes for outbox lag, dead-letter count, idempotency conflicts.
6. **(P2) API documentation.** No `springdoc`/OpenAPI — frontend integration would benefit from a generated contract.
7. **(P2) Notification preference + i18n fallback audit.** Verify all dispatch paths resolve `ar` fallback and that **security/compliance alerts bypass marketing opt-out** while marketing obeys it.
8. **(P2) Dead-letter operations.** Outbox dead-letters need an admin view + replay action (table/worker exist; ops surface unclear).
9. **(P3) WebAuthn full FIDO2.** Flow scaffolding only; real attestation/assertion library not included.
10. **(P3) Load/perf & concurrency tests.** Pessimistic-lock contention and outbox throughput under load not benchmarked.
11. **(P3) CI pipeline.** Docker present; no confirmed GitHub Actions/CI running `mvnw verify` on PRs.

---

## 6. Step-by-Step Plan to Finish the Backend ASAP

### Phase A — Close P1 correctness gaps (≈1–2 days)
1. **Expose escrow money flow.** Add `POST /api/escrow/{id}/fund`, `POST /api/escrow/{id}/release`, `POST /api/escrow/{id}/dispute`, each `@RequireIdempotencyKey`, routed through the existing escrow service + `EscrowFunded/Released/Disputed` events. Wrap balance moves in the ledger + audit.
2. **Verify cash-out idempotency.** Add `@RequireIdempotencyKey` to any withdrawal/cash-out POST/PUT not yet covered.
3. **Write `LedgerRevenueConsistencyIT`.** Assert: platform fee recorded once (ledger == `PlatformRevenueService`); receiver principal equals promised payout; sender branch bears all fees.
4. **Write `TransferLifecycleNotificationIT`.** Assert: creation sends *no* completed receipt; ready-for-pickup notifies receiver; release notifies sender+receiver; rolled-back transfer enqueues nothing.
5. Run `mvnw verify`; fix root causes (no weakened assertions).

### Phase B — Production WhatsApp cutover (≈1 day, partly external)
6. Register & get approval for all template names in Meta Business Manager (ar + en).
7. Set `almukhtar.whatsapp.provider=meta` + credentials in a secure profile; keep mock for dev/test.
8. Smoke-test one real template send + webhook delivery callback in staging; confirm delivery-log status transitions (`SENT → DELIVERED → READ`).
9. Confirm secrets are never logged and phone numbers are masked everywhere.

### Phase C — Observability & operability (≈1 day)
10. Add `spring-boot-starter-actuator` + Micrometer/Prometheus; expose `/actuator/health`, `/actuator/prometheus`.
11. Add custom gauges: outbox PENDING depth, dead-letter count, idempotency conflicts, payout failures.
12. Add an **admin endpoint to list & replay dead-letter** notifications.

### Phase D — Contract & integration readiness (≈1 day)
13. Add `springdoc-openapi` to publish `/v3/api-docs` + Swagger UI; annotate money endpoints (note the required `Idempotency-Key` header).
14. Audit i18n fallback to `ar` and preference bypass rules (security bypasses marketing opt-out).
15. Write a short **frontend integration guide**: auth flow, required headers (`Authorization`, `Idempotency-Key`), error codes (`IDEMPOTENCY_*`), lifecycle states.

### Phase E — Hardening & ship (≈1–2 days)
16. Concurrency test: parallel duplicate `release` / `transfer` with same key → exactly one payout.
17. Run Flyway against a fresh PostgreSQL 16 to confirm V1–V51 apply cleanly in order.
18. Add CI (GitHub Actions) running `mvnw verify` + Testcontainers on every PR.
19. Finalize `Dockerfile`/`docker-compose` env wiring (DB, Redis, WhatsApp secrets); deploy to staging.
20. Tag release candidate; produce final readiness sign-off.

**Estimated total: ~5–7 focused working days** to a production-ready backend (Phase A is the only hard blocker for *correctness*; B–E are cutover/hardening).

---

## 7. Immediate Next Action (recommended)
Start **Phase A**, items 1–4 — expose the escrow money endpoints with idempotency, then add the two integrity integration tests. This closes the last money-correctness gap and locks in the trust guarantees with automated proof.
