# ALMUKHTAR Elite Codex Guide

This file is the operating map for future Codex sessions working on this repository. It explains what the backend is, what exists in code today, where the weak spots are, and how to build the next backend phases before frontend work.

## Product North Star

ALMUKHTAR Elite is a Syria-first financial super-app and money-transfer middleware for a cash-heavy, liquidity-constrained economy. The product must serve daily users, cashiers, branch managers, corporate payroll operators, merchants, auditors, and the platform owner.

The winning strategy is not just "more features." In Syria the main competitive pressure is trust, liquidity, cash-out reliability, transparent fees, privacy, and branch availability. Competitor research on ShamCash shows a narrow public offering around wallet transfers, basic send/receive, account creation/upgrade, support, and no hidden fees, while public reporting has highlighted outages, account freezes, unclear transparency, privacy concerns, and withdrawal/support complaints. ALMUKHTAR should win by being operationally dependable, auditable, privacy-forward, and useful even when internet or cash liquidity is unstable.

Research references used for this guide:

- https://shamcash.sy/en
- https://smex.org/sham-cash-under-scrutiny-a-forensic-analysis-of-syrias-new-e-wallet/
- https://smex.org/syrians-push-back-against-sham-cash-app-failures/

## Repository Snapshot

- Backend only. No Next.js frontend exists in this repository yet.
- Framework: Spring Boot 3.3.4.
- Language: Java 21.
- Build: Maven wrapper and Maven project in `pom.xml`.
- Database: H2 for local dev, PostgreSQL 16 intended for production.
- Schema: Flyway migrations under `src/main/resources/db/migration`, currently V1 through V40 but only 34 migration files are present because version numbers skip V24-V29.
- Security: Spring Security, JWT, BCrypt, method security with `@PreAuthorize`.
- Async/schedulers: Spring `@Async`, `@Scheduled`, ShedLock.
- Domain breadth: about 382 Java source files, 43 REST controllers, and 26 test files.

Main package:

```text
src/main/java/com/mycompany/transfersystem
```

Important folders:

```text
annotation/       Platform revenue annotation
aop/              Platform revenue aspect
config/           Security, JWT, async, AI, ShedLock, data initialization
context/          Tenant context holder
controller/       REST API surface
dto/              Request and response models
entity/           JPA entities
entity/enums/     Roles, statuses, event types, account types
event/            Domain events
exception/        App exception hierarchy and global handler
filter/           Correlation and tenant filters
repository/       Spring Data repositories
service/          Business services and subdomains
util/             Security and QR encryption helpers
```

## Core Business Rules

The most important invariant is the "Sender Branch Pays All" accounting model.

For a transfer from Branch A to Branch B:

1. Branch A/source side is responsible for the principal plus sending fee, platform base fee, exchange spread/profit, and receiving branch fee.
2. Branch B/destination side receives the clean principal for payout. The receiver should not discover hidden deductions at the counter.
3. Platform owner revenue must be recorded as first-class revenue, not hidden inside branch fund movement.
4. Every financial state change must create an audit entry and should create balanced ledger entries.
5. Transfers that require pickup must remain pending until a valid release passcode or Smart-Bridge QR scan completes the payout.

When editing financial code, preserve these rules even if the existing code is incomplete or inconsistent.

## Roles And Access Model

Current enum: `UserRole`.

```text
PLATFORM_OWNER       Software owner, global revenue, SaaS tenants, full oversight
MOTHER_BRANCH_ADMIN  Main branch operator, branches, corporates, bulk approvals
SUPER_ADMIN          Deprecated compatibility role; prefer PLATFORM_OWNER
BRANCH_MANAGER       One branch operations, liquidity, cashier oversight
CASHIER              Front desk cash-in, cash-out, QR scan, bill queues
CORPORATE_ADMIN      Payroll, sub-accounts, merchant-like enterprise workflows
INDIVIDUAL_USER      Wallet, payments, transfer, trading, loans, vault
AUDITOR              Read-only audit and compliance
```

Security is enforced mostly in controllers with `@PreAuthorize`. `SecurityConfig` permits only `/api/auth/**` and `/h2-console/**`; all other endpoints require authentication.

Codex rule: when adding endpoints, add method-level `@PreAuthorize` immediately. Do not rely only on the global authenticated rule.

## Implemented API Areas

Controllers currently cover these areas:

```text
/api/auth
/api/users
/api/funds
/api/transactions
/api/qr
/api/audit
/api/admin/fees
/api/exchange-rates
/api/wallet
/api/wallet/topup
/api/branches
/api/trading
/api/market
/api/fee-zones
/api/owner
/api/lending
/api/bills
/api/merchants
/api/batch
/api/campaigns
/api/escrow
/api/fx/forward
/api/platform/tenants
/api/cards
/api/aml
/api/referral
/api/ai
/api/vault
/api/biometric
/api/family
/api/savings-goals
/api/split
/api/recurring-transfers
/api/analytics
/api/liquidity
/api/disputes
/api/i18n
/api/notifications
/api/sync
```

Existing service subdomains:

```text
accounting/       Double-entry ledger
ai/               RAG-style assistant and data ingestion
analytics/        Branch analytics and materialized view refresh
batch/            CSV/bulk transfer validation and async execution
bills/            Bill providers and payment requests
campaign/         AI campaign messages and notification broadcasts
dispute/          Dispute lifecycle and SLA scheduler
family/           Family groups and member controls
feezone/          Geographic/time/Islamic calendar fee zones
geo/              Nearby branch lookup with privacy-oriented handling
lending/          Credit scoring, applications, disbursement, repayment
liquidity/        Liquidity alerts and forecast endpoint
merchant/         Merchant onboarding, QR payment, settlement
notification/     WhatsApp provider
recurring/        Recurring transfers
revenue/          Platform revenue recording/reporting
savings/          Savings goals
split/            Split payments
storage/          Document storage, local dev implementation
trading/          Market data, price cache, order execution
vault/            Document vault and WebAuthn-like flows
wallet/           Wallet application, KYC documents, balances, top-up, exchange
```

## Reality Check: Strong Areas

These parts are structurally present and closer to usable:

- JWT login, role enum, method-level authorization.
- Core transfer entities and basic transfer flows.
- Fee calculation concepts and branch/platform fee routing.
- QR generation/scan flow with encrypted payload utilities.
- Wallet balances with pessimistic locking in repository methods.
- Wallet exchange and wallet credit/debit helpers.
- Double-entry ledger service with balance assertion for transaction-linked entries.
- KYC document upload path with local storage in dev.
- Batch transfer upload/progress shape with async executor.
- Trading account/order/position structure with serializable order execution.
- Lending score/application/repayment structure.
- Merchant onboarding/payment/settlement structure.
- Audit logging and broad exception hierarchy.
- Offline sync queue concept with encrypted payloads.
- Schedulers with ShedLock annotations in many newer services.

## Reality Check: Weak Spots To Fix Before Frontend

The `readiness_report.md` says all modules are complete, but code inspection shows several areas are still stub-level or need hardening. Treat the readiness report as optimistic, not authoritative.

High-risk gaps:

1. Liquidity forecasting is mostly a placeholder. `LiquidityForecastService.getForecast` returns a note, not a real cash depletion forecast.
2. AML monitoring is only a high-value single-transaction alert. It does not yet implement structuring, velocity, watchlist, geolocation, counterparty graph, cashier collusion, or SAR/CTR workflows.
3. Offline sync idempotency appears flawed: it checks `existsByPayloadChecksumAndStatus(idemKey, APPLIED)` even though `idemKey` is not the checksum. Add an explicit idempotency key column or deterministic checksum strategy.
4. Core transfer fund routing in `TransactionService.executeTransfer` mixes the requested `fund`, sender branch fund, and receiver branch fund in a way that may double-debit. This must be reconciled against the real ledger model.
5. Ledger posting is too generic. Auto-created accounts default to ASSET, and many flows do not post full fee/principal/revenue legs.
6. Platform revenue AOP exists, but every revenue event must be verified with tests so platform owner income is complete and not duplicated.
7. WebAuthn service is a flow skeleton; real FIDO2 attestation/assertion validation requires a proper library and challenge storage discipline.
8. Multi-tenancy exists as filters/context/entities, but data isolation must be audited across repositories and queries.
9. Trading uses Yahoo-like market data, but production trading in a regulated or sanctioned market needs stronger disclaimers, custody model, asset availability controls, and order state reconciliation.
10. External integrations are mostly stubs or optional: WhatsApp, OpenAI/Spring AI, Redis, storage, market data, and bill providers need adapter contracts and failure handling.
11. H2 dev disables Flyway. PostgreSQL migrations must be validated in Testcontainers, especially pgvector, materialized views, JSON, and earthdistance-related SQL.
12. No frontend exists yet, so API response shapes should be stabilized before UI work.

## Backend Development Plan

Build backend completion in risk order, not feature glamour order.

### Phase A: Financial Correctness And Auditability

Goal: every monetary movement is deterministic, balanced, testable, and explainable to a branch manager or auditor.

Tasks:

1. Define a formal chart of accounts:
   - Platform revenue.
   - Platform settlement/cash clearing.
   - Branch cash accounts by branch and currency.
   - Wallet liability accounts by wallet and currency.
   - Merchant payable accounts.
   - Loan principal/interest/late fee accounts.
   - Escrow holding accounts.
   - FX spread revenue.
2. Replace ad hoc fund routing in transfers with ledger-first posting.
3. Make "Sender Branch Pays All" a tested domain service:
   - Debit sender branch cash/clearing for principal plus all fees.
   - Credit receiver branch payable/cash-clearing for principal only.
   - Credit platform revenue for base fee and spread.
   - Credit receiving branch fee income from sender branch side, if that fee belongs to Branch B.
4. Add transaction idempotency keys for all money-moving endpoints.
5. Add tests for same-branch transfer, cross-branch transfer, multi-currency transfer, QR release, failed release, and duplicate release.
6. Add audit assertions to every financial test.

Acceptance criteria:

- No financial mutation can happen without an audit log.
- Every financial mutation posts balanced ledger entries.
- Duplicate client requests do not create duplicate payouts.
- Receiver payout amount always equals agreed principal.

### Phase B: Syria Cash Operations

Goal: make the system useful during cash shortages, blackouts, weak connectivity, and branch congestion.

Tasks:

1. Implement real liquidity forecasting:
   - Use transaction history, cash-in/cash-out mix, branch opening hours, weekday effects, payroll cycles, holidays, and city demand.
   - Forecast per branch and currency for 48 hours and 7 days.
   - Create alerts when projected cash falls below configurable threshold.
2. Improve offline cashier queue:
   - Persist idempotency key separately.
   - Sign cashier device payloads.
   - Add per-device sequence numbers.
   - Add conflict states that can be resolved by branch manager.
   - Prevent offline payout above device cash limit.
3. Add branch cash inventory:
   - Physical cash counted by cashier drawer, branch vault, and currency.
   - Shift open/close reconciliation.
   - Cash transfer between branches.
   - Cash pickup/delivery orders.
4. Add payout reservation:
   - Receiver can reserve cash pickup at a branch/time.
   - Branch manager sees expected cash-out load.
   - Reservation expires and returns liquidity to available pool.

Acceptance criteria:

- Cashier can continue limited work offline.
- Branch manager can see projected cash depletion.
- Platform owner can see city-level liquidity stress.
- Customer can know where money can actually be collected.

### Phase C: Trust, Privacy, And Compliance

Goal: directly exploit competitor weaknesses around transparency, privacy, freezes, and support.

Tasks:

1. Build transparent account freeze workflow:
   - Freeze reason.
   - Who froze it.
   - Customer-visible case ID.
   - SLA and escalation path.
   - Auditor report.
2. Expand AML:
   - Structuring: many transfers under threshold in rolling windows.
   - Velocity: unusual frequency/volume increases.
   - Counterparty concentration.
   - Circular transfers.
   - Cashier-assisted suspicious behavior.
   - Watchlist hits.
   - SAR/CTR export.
3. KYC privacy hardening:
   - Document encryption metadata.
   - Retention rules.
   - Access audit for document views/downloads.
   - Data minimization for branch finder: never persist raw coordinates.
4. Implement customer dispute center fully:
   - Lost QR/passcode.
   - Wrong receiver.
   - Delayed payout.
   - Frozen account.
   - Merchant payment dispute.
   - Loan repayment dispute.
5. Add status pages/incident records:
   - Public operational status.
   - Branch outage reporting.
   - Integration failure reporting.

Acceptance criteria:

- Every freeze or rejection has a traceable reason.
- AML alerts can become SAR/CTR records.
- Sensitive data access is auditable.
- Disputes have enforced SLAs.

### Phase D: Daily-Driver Wallet And Merchant Network

Goal: make ALMUKHTAR a daily payment habit, not only a hawala transfer tool.

Tasks:

1. Stabilize wallet APIs:
   - Wallet-to-wallet transfer.
   - Wallet-to-cash withdrawal.
   - Cash-to-wallet top-up.
   - Multi-currency balances.
   - Wallet limits by KYC tier.
2. Merchant network:
   - Static and dynamic merchant QR.
   - Instant customer receipt.
   - Merchant settlement ledger.
   - Merchant refund flow.
   - Fee transparency.
3. Bill payments:
   - Provider catalog.
   - Manual cashier queue for providers without APIs.
   - API adapter interface for Syriatel/MTN or similar providers.
   - Payment reversal flow.
4. Personal finance retention:
   - Savings goals.
   - Recurring rent/allowance transfers.
   - Family wallet limits.
   - Split payments.
   - Referral rewards tied to completed legitimate activity, not signup spam.

Acceptance criteria:

- A daily user can get paid, send money, pay a merchant, pay a bill, and cash out.
- A merchant can accept QR payments and reconcile settlement.
- Fees and limits are visible and auditable.

### Phase E: Corporate, Lending, And Treasury

Goal: own payroll, aid distribution, SME cashflow, and FX risk management.

Tasks:

1. Bulk batch transfers:
   - CSV validation with row-level errors.
   - Approval workflow.
   - Async execution with SSE progress.
   - Reversal/retry rules.
2. Corporate accounts:
   - Departments/sub-accounts.
   - Payroll templates.
   - Approval matrix.
   - Statements and ledger export.
3. Micro-lending:
   - Score explainability.
   - Product eligibility by KYC/trust score/wallet age.
   - Repayment scheduler with grace days.
   - Partial repayment.
   - Default workflow and wallet freeze rules.
4. Forward contracts:
   - Corporate lock-in quote.
   - Expiry and settlement.
   - Risk limits.
   - Audit trail.
5. Escrow:
   - Date/document/witness conditions.
   - Dispute handling.
   - Holding yield.

Acceptance criteria:

- Corporate payroll can process 10,000 rows safely.
- Lending can disburse and collect without manual database intervention.
- Treasury features have clear exposure limits.

### Phase F: SaaS, AI, And Operational Intelligence

Goal: make the platform own the market and license the rails.

Tasks:

1. Harden tenant isolation:
   - Every table needing tenant scope has tenant reference.
   - Tests prove tenant A cannot read tenant B.
   - Platform owner can aggregate across tenants.
2. AI assistant:
   - Restrict RAG data by role and tenant.
   - No private customer data in prompts unless explicitly needed and audited.
   - Add operator workflows: "why is branch cash low", "show suspicious velocity", "draft customer reply".
3. AI notifications:
   - Localization for Arabic, Kurdish, Turkish, English.
   - Human approval for campaigns.
   - City/role/customer segment targeting.
4. Analytics:
   - Branch heatmaps.
   - Cashier performance.
   - Profitability by corridor/currency/branch.
   - Liquidity stress dashboard.

Acceptance criteria:

- SaaS tenants are isolated.
- AI cannot leak cross-tenant or unauthorized data.
- Platform owner has actionable operational intelligence.

## Testing Strategy

Use this order:

```powershell
.\mvnw.cmd test
```

For PostgreSQL-only behavior, prefer Testcontainers-backed integration tests instead of H2:

```powershell
.\mvnw.cmd -Dspring.profiles.active=test test
```

Important test categories to add or preserve:

- Unit tests for pure rules: fee calculation, credit score, fee zones, AML rules.
- Service tests for money movement: transfers, wallet debit/credit, merchant payment, loan repayment.
- Integration tests for DB locking and migrations: wallet balances, trading orders, offline sync, QR release.
- Security tests for RBAC: every controller role boundary.
- Audit tests: every important state mutation logs correctly.
- Multi-tenant tests: tenant isolation on every scoped table.

Do not mark backend ready for frontend until these pass:

```text
Core transfer integration
Wallet debit/credit concurrency
QR release happy path and replay attack
Offline sync idempotency and conflict
AML structuring and velocity
Merchant payment and settlement
Loan disbursement and repayment scheduler
Tenant isolation
RBAC matrix
PostgreSQL Flyway migration validation
```

## Coding Conventions For Future Codex

- Keep backend changes scoped and test-driven around financial invariants.
- Prefer constructor injection for new services.
- Use `BigDecimal` for money. Always set scale/rounding explicitly at currency boundaries.
- Put all state-changing service methods behind `@Transactional`.
- Use pessimistic locks or serializable isolation for wallet balances, branch cash, order reservations, and payout release.
- Add idempotency to every endpoint that moves money.
- Add audit logging inside the same transaction unless the audit itself intentionally uses a separate transaction.
- Add domain events only after the core transaction commits for side effects such as notifications, scoring, AML, and gamification.
- External integrations must have interfaces/adapters and graceful failure behavior.
- Do not add frontend code in this repo until backend API contracts and tests are stable.
- Do not treat H2 success as production readiness. PostgreSQL is the real target.

## Current Next Backend Tickets

Start here:

1. Fix transfer accounting and ledger posting for "Sender Branch Pays All".
2. Fix offline sync idempotency and add manager conflict resolution.
3. Implement real liquidity forecast and branch cash inventory.
4. Expand AML from one high-value rule to a proper rule engine with SAR/CTR records.
5. Harden wallet limits, wallet-to-wallet transfer endpoint, and cash-out reservation.
6. Add PostgreSQL Testcontainers migration validation.
7. Audit tenant isolation and add cross-tenant access tests.
8. Replace WebAuthn skeleton with real FIDO2 verification or clearly mark it as simulated.
9. Stabilize API response DTOs for frontend integration.
10. Build an executable RBAC matrix test covering all controllers.

