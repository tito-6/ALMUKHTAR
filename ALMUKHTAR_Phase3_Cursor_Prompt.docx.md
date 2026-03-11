

| 🏛 ALMUKHTAR ELITE PHASE III  —  THE FULL ECOSYSTEM Micro-Lending  ·  Merchant Network  ·  Escrow  ·  AML  ·  Family Wallets  ·  Savings Forward Contracts  ·  Cards  ·  Disputes  ·  Referrals  ·  Batch Transfers  ·  i18n ────────────────────────────────────────────────────── COMPREHENSIVE CURSOR IMPLEMENTATION PROMPT 12 Modules  ·  Spring Boot 3.3.4  ·  Java 21  ·  Next.js 14  ·  PostgreSQL Version 3.0  ·  Senior Fintech Architecture Division |
| :---: |

| 🎯  PRIORITY ORDER — START HERE Build in this exact sequence: (1) Micro-Lending Engine, (2) Bill Payments & Merchant Network, (3) Recurring Transfers — these three alone will make ALMUKHTAR the dominant platform in the region. The remaining modules follow in the order they appear in this document. |
| :---- |

| A | HIGHEST-IMPACT ADDITIONS Micro-Lending  ·  Merchant Network  ·  Bulk Transfers |
| :---: | :---- |

| MODULE P1  ·  HIGHEST IMPACT — PRIORITY BUILD Micro-Lending & Credit Scoring Engine AI Credit Score · Loan Disbursement · Repayment Engine · Interest Revenue |
| :---- |

##   **P1.1  Business Model & Revenue Logic**

Users with verified wallets and **Trust Scores ≥ 50** (from the existing Gamification Module) become eligible for micro-loans disbursed directly to their wallet. The platform earns **interest income** on outstanding loan balances. The credit score is built entirely from data ALMUKHTAR already owns: transaction volume, transfer frequency, repayment history, KYC tier, wallet age, and gamification score. No third-party credit bureau needed.

| 💰  Revenue Model Platform earns: (1) Origination fee: 1–3% of loan amount, collected at disbursement. (2) Monthly interest: configurable APR per risk tier (e.g. TIER\_A: 18% APR, TIER\_B: 24% APR, TIER\_C: 36% APR). (3) Late payment fee: flat fee per missed instalment. All fees flow through PlatformRevenueService (Phase II Module 10). |
| :---- |

##   **P1.2  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| credit\_profiles | id, user\_id (unique), credit\_score (0–1000), risk\_tier (A/B/C/INELIGIBLE), max\_loan\_amount\_usd, calculated\_at, next\_review\_at, score\_components (JSONB with breakdown) |
| loan\_products | id, name, min\_amount, max\_amount, min\_term\_days, max\_term\_days, apr\_rate, origination\_fee\_pct, late\_fee\_amount, risk\_tier (A/B/C), is\_active |
| loan\_applications | id, user\_id, product\_id, requested\_amount, requested\_term\_days, purpose, status (PENDING/APPROVED/REJECTED/DISBURSED/CANCELLED), reviewed\_by, decision\_reason, created\_at |
| loans | id, application\_id, user\_id, principal\_amount, currency, disbursed\_at, term\_days, apr\_rate, origination\_fee, monthly\_payment, outstanding\_balance, status (ACTIVE/PAID\_OFF/DEFAULTED/RESTRUCTURED) |
| loan\_repayment\_schedule | id, loan\_id, instalment\_number, due\_date, principal\_due, interest\_due, total\_due, paid\_amount, paid\_at, status (PENDING/PAID/PARTIAL/MISSED/WAIVED) |
| loan\_repayments | id, loan\_id, schedule\_id, amount, payment\_method (WALLET\_AUTO/WALLET\_MANUAL/BRANCH), processed\_at, wallet\_transaction\_id |
| loan\_late\_fees | id, loan\_id, schedule\_id, fee\_amount, applied\_at, collected (boolean) |

##   **P1.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/lending/CreditScoringService.java | Calculate credit score from internal data, determine risk tier and max amount |
| service/lending/LoanApplicationService.java | Apply, review, approve/reject loan applications |
| service/lending/LoanDisbursementService.java | Disburse approved loan to wallet, generate repayment schedule, collect origination fee |
| service/lending/LoanRepaymentService.java | Process manual and auto repayments, update outstanding balance |
| service/lending/LoanSchedulerService.java | @Scheduled daily: check due dates, auto-debit wallet, apply late fees, flag defaults |
| service/lending/LoanRiskService.java | Default risk monitoring, restructuring logic, debt recovery escalation |
| controller/LoanController.java | Full lifecycle endpoints for users and admins |
| entity/CreditProfile.java | JPA entity |
| entity/Loan.java | JPA entity |
| entity/LoanRepaymentSchedule.java | JPA entity |
| entity/enums/RiskTier.java | TIER\_A, TIER\_B, TIER\_C, INELIGIBLE |
| dto/lending/LoanApplicationRequest.java | amount, termDays, purpose |
| dto/lending/LoanOfferResponse.java | approvedAmount, monthlyPayment, totalInterest, apr, schedule preview |
| dto/lending/RepaymentScheduleResponse.java | List of instalments with dates and amounts |

##   **P1.4  Credit Scoring Algorithm**

// CreditScoringService.calculateScore(Long userId):  
// Returns CreditScore { score: 0-1000, tier, maxLoanUsd, components }  
//  
// COMPONENT 1 — Transaction Volume Score (max 250 pts)  
//   totalUsdVolume \= SUM of all completed transactions (last 12 months)  
//   score \= MIN(250, totalUsdVolume / 400\)  // $100k volume \= 250 pts  
//  
// COMPONENT 2 — Transaction Frequency Score (max 200 pts)  
//   txCount \= COUNT of completed transactions (last 12 months)  
//   score \= MIN(200, txCount \* 4\)  // 50 transactions \= 200 pts  
//  
// COMPONENT 3 — Gamification Trust Score (max 200 pts)  
//   gamificationScore \= userGamificationProfile.trustScore  // 0-100  
//   score \= gamificationScore \* 2  
//  
// COMPONENT 4 — Repayment History Score (max 200 pts — 0 for new borrowers)  
//   If no prior loans: 100 pts (neutral)  
//   missedPayments \= COUNT of MISSED repayment schedules  
//   onTimePayments \= COUNT of PAID schedules with paid\_at \<= due\_date  
//   score \= (onTimePayments / totalSchedules) \* 200 \- (missedPayments \* 40\)  
//   score \= MAX(0, score)  
//  
// COMPONENT 5 — KYC Tier Score (max 100 pts)  
//   BASIC: 20pts  STANDARD: 60pts  PREMIUM: 100pts  
//  
// COMPONENT 6 — Wallet Age Score (max 50 pts)  
//   ageMonths \= months since wallet.createdAt  
//   score \= MIN(50, ageMonths \* 5\)  // 10 months \= 50 pts  
//  
// TOTAL \= SUM of all components  
//  
// RISK TIERS:  
//   TIER\_A: score \>= 700  maxLoan \= $5,000  APR \= 18%  
//   TIER\_B: score \>= 500  maxLoan \= $2,000  APR \= 24%  
//   TIER\_C: score \>= 300  maxLoan \= $500    APR \= 36%  
//   INELIGIBLE: score \< 300  
//  
// Recalculate: @Scheduled weekly \+ on every completed transaction  
// Cache in credit\_profiles table, next\_review\_at \= now() \+ 7 days

##   **P1.5  Loan Repayment Auto-Debit Scheduler**

// LoanSchedulerService — @Scheduled(cron \= '0 0 8 \* \* \*') — runs daily at 8am:  
//  
// Step 1 — Find all PENDING schedules WHERE due\_date \= TODAY  
//   For each: attempt auto-debit from user wallet (pessimistic lock)  
//   If wallet has sufficient balance:  
//     Debit wallet, update schedule to PAID, update loan outstanding\_balance  
//     Create LoanRepayment record  
//     Credit platform interest income via PlatformRevenueService  
//     Send in-app notification: 'Loan instalment paid successfully'  
//   If insufficient balance:  
//     Mark schedule as MISSED  
//     Apply LoanLateFee record  
//     Send urgent WhatsApp \+ in-app notification  
//     Reduce credit score by 40 points immediately  
//  
// Step 2 — Find MISSED schedules older than 30 days  
//   Flag loan as DEFAULTED  
//   Escalate to MOTHER\_BRANCH\_ADMIN via notification campaign  
//   Freeze user wallet (WalletService.freezeWallet())  
//   Audit: action='LOAN\_DEFAULTED'  
//  
// Step 3 — Find loans WHERE outstanding\_balance \<= 0  
//   Mark loan as PAID\_OFF  
//   Increase credit score by 50 points  
//   Award 'Debt Free' badge (GamificationService)  
//   Send congratulatory AI message (Module 12 AIMessageGeneratorService)

##   **P1.6  REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| GET /api/lending/my-credit-score | INDIVIDUAL\_USER — Own credit score, tier, max loan, score breakdown |
| GET /api/lending/products | INDIVIDUAL\_USER — Available loan products for my tier |
| POST /api/lending/apply | INDIVIDUAL\_USER — Submit loan application |
| GET /api/lending/my-loans | INDIVIDUAL\_USER — All loans with status and outstanding balance |
| GET /api/lending/my-loans/{id}/schedule | INDIVIDUAL\_USER — Full repayment schedule with status per instalment |
| POST /api/lending/my-loans/{id}/repay | INDIVIDUAL\_USER — Manual repayment (partial or full) |
| GET /api/lending/admin/applications | MOTHER\_BRANCH\_ADMIN, BRANCH\_MANAGER — Pending applications queue |
| PUT /api/lending/admin/applications/{id}/approve | MOTHER\_BRANCH\_ADMIN — Approve with offer terms |
| PUT /api/lending/admin/applications/{id}/reject | MOTHER\_BRANCH\_ADMIN — Reject with reason |
| GET /api/lending/admin/portfolio | PLATFORM\_OWNER — Entire loan book: active, defaulted, paid off, interest earned |
| GET /api/lending/admin/at-risk | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — Loans at risk of default (missed 1+ payment) |

| MODULE P2  ·  HIGHEST IMPACT — DAILY USAGE DRIVER Bill Payments & Merchant Network Utility Bills · Phone Top-Up · Merchant QR · Merchant Onboarding · Processing Fee |
| :---- |

##   **P2.1  Overview**

Transform ALMUKHTAR into a **daily-use super-app** by letting users pay bills and purchase from merchants directly from their wallet. Merchants register, receive a unique QR code, and accept wallet payments. Every payment earns the platform a processing fee. This is the feature that creates **daily active user behaviour** rather than monthly remittance behaviour.

##   **P2.2  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| bill\_providers | id, name, category (ELECTRICITY/WATER/INTERNET/MOBILE\_TOPUP/GAS/TAX/OTHER), country, city, api\_integration (DIRECT\_API/MANUAL), api\_endpoint, api\_key\_ref, logo\_url, is\_active, processing\_fee\_pct |
| bill\_payment\_requests | id, user\_id, provider\_id, account\_reference (bill account number), amount, currency, status (PENDING/PROCESSING/PAID/FAILED), payment\_ref, platform\_fee, paid\_at, created\_at |
| merchants | id, owner\_user\_id, business\_name, category, registration\_number, address, city, country, branch\_id (nearest branch), qr\_code\_data, qr\_code\_image\_path, status (PENDING/ACTIVE/SUSPENDED), kyc\_approved, created\_at |
| merchant\_transactions | id, merchant\_id, payer\_user\_id, amount, currency, description, platform\_fee, merchant\_net\_amount, status, qr\_scan\_ref, created\_at |
| merchant\_settlements | id, merchant\_id, period\_start, period\_end, gross\_amount, total\_fees, net\_amount, status (PENDING/SETTLED/FAILED), settlement\_wallet\_id, settled\_at |

##   **P2.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/bills/BillPaymentService.java | Validate bill account, process payment via provider API or manual, collect fee |
| service/bills/BillProviderService.java | CRUD and integration layer for bill providers |
| service/merchant/MerchantOnboardingService.java | Application, KYC review, QR generation for merchants |
| service/merchant/MerchantPaymentService.java | QR-based and direct payment processing between user wallet and merchant |
| service/merchant/MerchantSettlementService.java | @Scheduled daily: batch settle merchant net amounts to their wallets |
| controller/BillPaymentController.java | Bill payment endpoints for users |
| controller/MerchantController.java | Merchant onboarding and management |
| controller/MerchantPaymentController.java | Payment processing endpoints |
| entity/Merchant.java | JPA entity |
| entity/BillProvider.java | JPA entity |
| entity/MerchantTransaction.java | JPA entity |
| dto/bills/PayBillRequest.java | providerId, accountReference, amount, currency |
| dto/merchant/MerchantRegistrationRequest.java | businessName, category, address, documents |
| dto/merchant/MerchantPaymentRequest.java | merchantQrData or merchantId, amount, description |

##   **P2.4  Merchant Payment Flow**

// MerchantPaymentService.processQrPayment(String qrData, Long payerUserId, BigDecimal amount):  
// @Transactional(isolation \= Isolation.SERIALIZABLE)  
//  
// Step 1 — Decode QR: extract merchantId \+ merchant signature  
//   Verify HMAC signature (same mechanism as QR Withdrawal in Phase I)  
//  
// Step 2 — Load Merchant, verify status \== ACTIVE  
//  
// Step 3 — Load payer Wallet, verify status \== ACTIVE, balance \>= amount  
//  
// Step 4 — Calculate platform processing fee:  
//   fee \= amount \* merchant.processingFeePct  (configurable per merchant category)  
//   merchantNet \= amount \- fee  
//  
// Step 5 — Pessimistic lock payer wallet balance (SELECT FOR UPDATE NOWAIT)  
//  
// Step 6 — Debit payer wallet: amount (principal \+ fee combined from payer perspective)  
//   Actually: debit amount total, credit merchant wallet merchantNet, credit platform fee  
//  
// Step 7 — Credit merchant's wallet: merchantNet (to their settlement buffer balance)  
//  
// Step 8 — Credit PLATFORM\_OWNER revenue: fee (via PlatformRevenueService)  
//  
// Step 9 — Create MerchantTransaction record  
//  
// Step 10 — Push receipt to both payer and merchant via in-app \+ WhatsApp  
//  
// Step 11 — Audit: action='MERCHANT\_PAYMENT\_PROCESSED'

##   **P2.5  Bill Payment Integration Strategy**

// BillPaymentService.payBill(PayBillRequest req, Long userId):  
//  
// For providers with DIRECT\_API integration:  
//   POST to provider.apiEndpoint with account\_reference, amount, correlation\_id  
//   Poll or webhook for confirmation  
//   On success: debit wallet, record BillPaymentRequest as PAID  
//  
// For providers with MANUAL integration (most common in Syrian market):  
//   Create BillPaymentRequest with status PROCESSING  
//   Reserve amount in wallet (move to locked\_balance)  
//   Notify CASHIER at nearest branch to process manually  
//   Cashier marks as completed via PUT /api/bills/admin/{id}/complete  
//   On completion: finalise wallet debit, unlock balance, mark PAID  
//  
// Mobile Top-Up: integrate with local carrier APIs (Syriatel, MTN Syria)  
//   Direct API call with MSISDN (phone number) and amount  
//   Near-instant confirmation  
//  
// Platform fee: collected on all bill payments regardless of method  
// Rate: configurable per provider category in bill\_providers.processing\_fee\_pct

##   **P2.6  REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| GET /api/bills/providers | Authenticated — List all active bill providers by category |
| POST /api/bills/pay | INDIVIDUAL\_USER, CORPORATE\_ADMIN — Pay a bill |
| GET /api/bills/my-history | INDIVIDUAL\_USER — Bill payment history |
| GET /api/bills/admin | CASHIER, BRANCH\_MANAGER — Pending manual bill payments at my branch |
| PUT /api/bills/admin/{id}/complete | CASHIER — Mark manual bill payment as processed |
| POST /api/merchants/register | INDIVIDUAL\_USER, CORPORATE\_ADMIN — Apply for merchant account |
| GET /api/merchants/my-merchant | Merchant owner — Own merchant dashboard: sales, settlements, QR |
| GET /api/merchants/my-merchant/qr | Merchant owner — Download merchant QR code as PNG |
| POST /api/merchants/pay | INDIVIDUAL\_USER — Pay a merchant by QR or ID |
| GET /api/merchants/admin | MOTHER\_BRANCH\_ADMIN — All merchants with status filter |
| PUT /api/merchants/admin/{id}/approve | MOTHER\_BRANCH\_ADMIN — Activate merchant account |
| GET /api/merchants/admin/settlements/pending | MOTHER\_BRANCH\_ADMIN — Pending merchant settlement batches |

| MODULE P3  ·  HIGHEST IMPACT — ENTERPRISE REVENUE Bulk & Batch Transfer Engine CSV Upload · 500+ Recipients · Single Approval · Enterprise API |
| :---- |

##   **P3.1  Overview**

Corporate clients upload a CSV or JSON file with up to **10,000 recipient rows** and submit the entire batch with a single authorisation. The system validates, queues, and processes all transfers asynchronously with a **real-time progress stream**. This is a raw bulk API separate from the payroll module — it supports any use case: supplier payments, agent commissions, mass refunds, humanitarian disbursements.

##   **P3.2  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| batch\_jobs | id, submitted\_by, batch\_type (CSV\_UPLOAD/API/TEMPLATE), filename, total\_rows, processed\_rows, success\_count, failed\_count, total\_amount\_usd, status (VALIDATING/PENDING\_APPROVAL/APPROVED/PROCESSING/COMPLETED/FAILED/CANCELLED), approved\_by, approved\_at, created\_at, completed\_at |
| batch\_job\_rows | id, batch\_job\_id, row\_number, receiver\_identifier (username/wallet/phone), amount, currency, description, status (PENDING/SUCCESS/FAILED), error\_message, transaction\_id (nullable FK), processed\_at |
| batch\_templates | id, owner\_user\_id, name, description, column\_mapping (JSONB), sample\_rows (JSONB), created\_at |

##   **P3.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/batch/BatchUploadService.java | Parse CSV/JSON, validate each row, create BatchJob \+ rows |
| service/batch/BatchValidationService.java | Validate receivers exist, amounts valid, fund sufficient for entire batch |
| service/batch/BatchExecutionService.java | @Async: process approved batch row-by-row with progress updates |
| service/batch/BatchProgressService.java | SSE streaming of real-time progress to submitter |
| controller/BatchTransferController.java | Upload, approve, monitor, cancel endpoints |
| entity/BatchJob.java | JPA entity |
| entity/BatchJobRow.java | JPA entity |
| dto/batch/BatchUploadResponse.java | jobId, totalRows, validRows, invalidRows, estimatedTotalUsd, validationErrors |
| dto/batch/BatchProgressResponse.java | jobId, processedRows, successCount, failedCount, percentComplete, estimatedRemaining |

##   **P3.4  Batch Processing Logic**

// BatchExecutionService.executeBatch(Long batchJobId):  
// @Async — runs in separate thread pool (executor: batchExecutor, pool size: 5\)  
//@Transactional per row — NOT one transaction for the whole batch  
//  
// Step 1 — Load BatchJob, verify status \== APPROVED  
//  
// Step 2 — Lock source fund (pessimistic) for total batch amount  
//  
// Step 3 — Process rows in pages of 100:  
//   for each BatchJobRow:  
//     try {  
//       transaction \= transactionService.processSimpleTransfer(...)  
//       row.status \= SUCCESS, row.transactionId \= transaction.id  
//     } catch (Exception e) {  
//       row.status \= FAILED, row.errorMessage \= e.getMessage()  
//     }  
//     batchJob.processedRows++  
//     batchProgressService.broadcast(batchJobId, progressDto)  
//     // SSE push to /api/batch/{id}/progress stream  
//  
// Step 4 — After all rows: update BatchJob status to COMPLETED  
//   Generate summary report: successCount, failedCount, totalDisburse  
//  
// Step 5 — Send completion notification to submitter (WhatsApp \+ in-app)  
//  
// Step 6 — Audit: action='BATCH\_JOB\_COMPLETED', details={summary}  
//  
// CSV FORMAT EXPECTED:  
// receiver\_username, amount, currency, description  
// OR: receiver\_wallet\_number, amount, currency, description  
// OR: receiver\_phone, amount, currency, description

##   **P3.5  REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| POST /api/batch/upload | CORPORATE\_ADMIN, MOTHER\_BRANCH\_ADMIN — Upload CSV/JSON file, get validation report |
| POST /api/batch/upload/validate-only | CORPORATE\_ADMIN — Dry-run validation without creating a job |
| POST /api/batch/{id}/approve | MOTHER\_BRANCH\_ADMIN, PLATFORM\_OWNER — Approve validated batch for execution |
| POST /api/batch/{id}/execute | MOTHER\_BRANCH\_ADMIN — Trigger execution of approved batch |
| GET /api/batch/{id}/progress | SSE — Real-time progress stream during execution |
| GET /api/batch/{id}/report | CORPORATE\_ADMIN — Final report: per-row success/failure with transaction IDs |
| GET /api/batch | CORPORATE\_ADMIN — All my batch jobs with status |
| DELETE /api/batch/{id}/cancel | CORPORATE\_ADMIN — Cancel a PENDING\_APPROVAL or APPROVED batch |
| GET /api/batch/templates | CORPORATE\_ADMIN — Saved column mapping templates |

| B | REVENUE-EXPANDING FEATURES Escrow  ·  Forward Contracts  ·  White-Label  ·  Prepaid Cards |
| :---: | :---- |

| MODULE P4  ·  REVENUE EXPANSION Escrow & Conditional Transfers Hold Funds · Release Conditions · Daily Holding Fee · Trade Finance |
| :---- |

##   **P4.1  Overview**

Funds are locked in a platform-controlled escrow wallet until a **release condition** is satisfied. Conditions include: manual confirmation by both parties, a specific date, an uploaded proof document, or a CASHIER witness confirmation. The platform earns a **daily holding fee** on all escrow balances — typically 0.05–0.1% per day.

##   **P4.2  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| escrow\_contracts | id, initiator\_user\_id, beneficiary\_user\_id, amount, currency, title, description, condition\_type (DATE/MANUAL\_BOTH/MANUAL\_INITIATOR/DOCUMENT\_PROOF/CASHIER\_WITNESS), condition\_details (JSONB), release\_date (nullable), status (ACTIVE/RELEASED/DISPUTED/CANCELLED/EXPIRED), daily\_fee\_rate, total\_fees\_collected, funded\_at, released\_at, expires\_at |
| escrow\_events | id, contract\_id, event\_type (FUNDED/PROOF\_UPLOADED/APPROVED\_BY\_INITIATOR/APPROVED\_BY\_BENEFICIARY/RELEASED/DISPUTED/CANCELLED), performed\_by, notes, created\_at |
| escrow\_fee\_accruals | id, contract\_id, date, fee\_amount, currency, collected (boolean), collected\_at |

##   **P4.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/escrow/EscrowService.java | Create, fund, release, dispute, and cancel escrow contracts |
| service/escrow/EscrowFeeAccrualService.java | @Scheduled daily: accrue holding fees on all ACTIVE escrow balances |
| service/escrow/EscrowDisputeService.java | Manage disputes: freeze both wallets, escalate to MOTHER\_BRANCH\_ADMIN |
| controller/EscrowController.java | Full escrow lifecycle endpoints |
| entity/EscrowContract.java | JPA entity |
| entity/EscrowEvent.java | Immutable event log entity |
| dto/escrow/CreateEscrowRequest.java | beneficiaryId, amount, currency, title, conditionType, details, expiresAt |
| dto/escrow/EscrowReleaseRequest.java | contractId, notes, proofDocumentRef (optional) |

##   **P4.4  Fee Accrual & Release Logic**

// EscrowFeeAccrualService — @Scheduled(cron \= '0 0 1 \* \* \*') daily at 1am:  
//   For each ACTIVE escrow contract:  
//     feeAmount \= contract.amount \* contract.dailyFeeRate  
//     Debit escrow balance by feeAmount (reduce locked funds)  
//     Credit PLATFORM\_OWNER revenue via PlatformRevenueService  
//     Create EscrowFeeAccrual record  
//     If escrow balance drops below minimum: notify both parties  
//  
// EscrowService.release(Long contractId, Long requestedBy):  
//   Verify condition met based on condition\_type:  
//     DATE: LocalDate.now() \>= contract.releaseDate  
//     MANUAL\_BOTH: both parties have approved in escrow\_events  
//     DOCUMENT\_PROOF: at least one PROOF\_UPLOADED event exists  
//     CASHIER\_WITNESS: event from CASHIER role exists  
//   Debit remaining escrow balance  
//   Credit beneficiary wallet  
//   Mark contract RELEASED  
//   Audit: action='ESCROW\_RELEASED'

##   **P4.5  REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| POST /api/escrow | INDIVIDUAL\_USER, CORPORATE\_ADMIN — Create and fund escrow contract |
| GET /api/escrow/my-contracts | Own user — All my escrow contracts (as initiator or beneficiary) |
| POST /api/escrow/{id}/approve | Own user — Approve release (for MANUAL\_BOTH condition) |
| POST /api/escrow/{id}/upload-proof | Own user — Upload proof document for DOCUMENT\_PROOF condition |
| POST /api/escrow/{id}/witness | CASHIER — Witness confirmation for CASHIER\_WITNESS condition |
| POST /api/escrow/{id}/release | Own user, BRANCH\_MANAGER — Trigger release when condition is met |
| POST /api/escrow/{id}/dispute | Own user — Open dispute, freeze contract |
| GET /api/escrow/admin/disputes | MOTHER\_BRANCH\_ADMIN — All disputed contracts |
| PUT /api/escrow/admin/{id}/resolve | MOTHER\_BRANCH\_ADMIN — Resolve dispute with decision |

| MODULE P5  ·  REVENUE EXPANSION Currency Forward Contracts Lock Exchange Rate · 30/60/90 Day Contracts · Spread Revenue · Corporate Treasury |
| :---- |

##   **P5.1  Overview**

Corporate clients and high-tier individual users can lock today's exchange rate for a future transfer. The platform earns the **spread between the locked rate and the actual market rate** at execution time, plus a contract fee. This is standard treasury management for importers and exporters — a product with zero regional digital competitors in the Syrian market.

##   **P5.2  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| forward\_contracts | id, user\_id, from\_currency, to\_currency, notional\_amount, locked\_rate, contract\_fee, execution\_date, status (ACTIVE/EXECUTED/CANCELLED/EXPIRED), spread\_profit, executed\_at, created\_at |
| forward\_contract\_rates | id, from\_currency, to\_currency, spot\_rate, forward\_30d\_rate, forward\_60d\_rate, forward\_90d\_rate, platform\_spread\_bps, captured\_at |

##   **P5.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/fx/ForwardContractService.java | Create contracts, calculate forward rates with spread, execute on maturity date |
| service/fx/ForwardRateService.java | Calculate forward rates using interest rate parity or fixed platform spread model |
| scheduler/ForwardContractScheduler.java | @Scheduled daily: execute matured contracts, expire unexecuted |
| controller/ForwardContractController.java | Create, list, cancel contracts |
| entity/ForwardContract.java | JPA entity |
| dto/fx/ForwardContractRequest.java | fromCurrency, toCurrency, notionalAmount, termDays (30/60/90) |
| dto/fx/ForwardRateQuoteResponse.java | lockedRate, contractFee, estimatedPlatformSpread, executionDate, termsText |

##   **P5.4  Forward Rate Calculation**

// ForwardRateService.getForwardRate(String from, String to, int termDays):  
//  
// Model: Simple Spread Model (no interest rate parity complexity for MVP)  
//  
// spotRate \= ExchangeRateService.getRate(from, to)  // current live rate  
//  
// platformSpreadBps \= forward\_contract\_rates.platform\_spread\_bps  // e.g. 150 bps \= 1.5%  
//  
// termMultiplier:  
//   30 days: 1.0 (base spread)  
//   60 days: 1.2 (20% more spread for longer exposure)  
//   90 days: 1.5 (50% more spread)  
//  
// lockedRate \= spotRate \* (1 \- (platformSpreadBps / 10000\) \* termMultiplier)  
//   (client gets slightly worse rate; platform earns the spread at execution)  
//  
// At execution (maturity date):  
//   actualRate \= ExchangeRateService.getRate(from, to)  // live rate at maturity  
//   platformProfit \= (actualRate \- lockedRate) \* contract.notionalAmount  
//   Credit PLATFORM\_OWNER platformProfit via PlatformRevenueService  
//   Execute the transfer at lockedRate for the client  
//   If actualRate \< lockedRate: platform absorbs the loss (hedging risk config)

##   **P5.5  REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| GET /api/fx/forward/quote | CORPORATE\_ADMIN, INDIVIDUAL\_USER (TIER\_A) — Get forward rate quote |
| POST /api/fx/forward | CORPORATE\_ADMIN, INDIVIDUAL\_USER (TIER\_A) — Lock rate, create contract |
| GET /api/fx/forward/my-contracts | Own user — All forward contracts with status |
| DELETE /api/fx/forward/{id}/cancel | Own user — Cancel before execution (fee applies) |
| GET /api/fx/forward/admin | PLATFORM\_OWNER — All contracts, total exposure, P\&L |

| MODULE P6  ·  REVENUE EXPANSION White-Label Branch Licensing SaaS Model · Custom Branding · Per-Transaction Royalty · Operator Onboarding |
| :---- |

##   **P6.1  Overview & Business Model**

Other money transfer operators can license ALMUKHTAR's infrastructure. They get their own tenant namespace with custom branding, their own branch and user network, but run entirely on your engine. You earn a **monthly SaaS fee \+ per-transaction royalty**. Implemented as **multi-tenancy** at the database level using a  tenant\_id  column on all key tables.

##   **P6.2  Multi-Tenancy Architecture**

// APPROACH: Discriminator-column multi-tenancy (single database, shared schema)  
// Add tenant\_id BIGINT NOT NULL to: users, branches, transactions, wallets,  
//   funds, audit\_logs, commission\_rates, wallet\_balances, orders, loans  
//  
// TenantContext (ThreadLocal):  
//   TenantContextHolder.setTenantId(tenantId)  // set in JWT filter  
//   All JPA repositories add WHERE tenant\_id \= :currentTenant automatically  
//   via @Filter('tenant\_filter') Hibernate filter  
//  
// DEFAULT tenant\_id \= 1 (ALMUKHTAR platform itself)  
// All existing data gets tenant\_id \= 1 via Flyway migration  
//  
// New tenants get unique tenant\_id, their own Commission Rates, Fee Zones,  
// branding config, and admin user seeded by PLATFORM\_OWNER

##   **P6.3  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| tenants | id, name, slug (unique URL prefix), logo\_url, primary\_color, secondary\_color, contact\_email, monthly\_saas\_fee\_usd, per\_transaction\_royalty\_bps, status (TRIAL/ACTIVE/SUSPENDED), contract\_start, contract\_end |
| tenant\_billing | id, tenant\_id, period\_month, total\_transactions, total\_volume\_usd, saas\_fee, royalty\_amount, total\_billed, status (PENDING/INVOICED/PAID), invoice\_url, created\_at |

##   **P6.4  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/tenant/TenantProvisioningService.java | Create tenant: seed admin user, default config, branding |
| service/tenant/TenantBillingService.java | @Scheduled monthly: calculate usage, generate invoices |
| filter/TenantResolutionFilter.java | Extract tenantId from JWT, set TenantContextHolder |
| config/TenantHibernateFilter.java | Register Hibernate @Filter for automatic tenant isolation |
| controller/TenantAdminController.java | PLATFORM\_OWNER: create, suspend, configure tenants |
| entity/Tenant.java | JPA entity |
| dto/tenant/TenantProvisionRequest.java | name, slug, adminEmail, contractTerms |

##   **P6.5  REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| POST /api/platform/tenants | PLATFORM\_OWNER — Provision new white-label tenant |
| GET /api/platform/tenants | PLATFORM\_OWNER — All tenants with billing status |
| PUT /api/platform/tenants/{id}/suspend | PLATFORM\_OWNER — Suspend a tenant (e.g. non-payment) |
| GET /api/platform/tenants/{id}/billing | PLATFORM\_OWNER — Usage and billing history for tenant |
| GET /api/tenant/my-config | MOTHER\_BRANCH\_ADMIN of tenant — Own tenant config and branding |
| PUT /api/tenant/my-config/branding | MOTHER\_BRANCH\_ADMIN — Update logo, colors, name |

| MODULE P7  ·  REVENUE EXPANSION Prepaid Virtual & Physical Cards Wallet-Linked · Virtual Instant · Physical Ordered · Interchange Revenue |
| :---- |

##   **P7.1  Overview**

Issue Visa/Mastercard prepaid cards linked directly to user wallets via a card-issuing partner (**Marqeta, Nuvei, or regional equivalent**). Virtual cards are issued instantly for online use. Physical cards are ordered and mailed. Every card swipe earns **interchange revenue** for the platform. Cards are managed entirely within ALMUKHTAR — top-up, freeze, set limits, view transactions.

##   **P7.2  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| prepaid\_cards | id, user\_id, wallet\_id, card\_type (VIRTUAL/PHYSICAL), card\_last4, card\_network (VISA/MASTERCARD), expiry\_month, expiry\_year, card\_token (encrypted, from issuer), status (PENDING/ACTIVE/FROZEN/CANCELLED), daily\_limit, monthly\_limit, created\_at |
| card\_transactions | id, card\_id, merchant\_name, merchant\_category, amount, currency, transaction\_type (PURCHASE/REFUND/ATM/ONLINE), status, interchange\_earned, processed\_at |
| card\_orders | id, user\_id, card\_id, delivery\_address, estimated\_delivery, tracking\_number, status (ORDERED/SHIPPED/DELIVERED) |

##   **P7.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/card/CardIssuingService.java | Interface with card issuing API (Marqeta/Nuvei): create, freeze, cancel cards |
| service/card/CardTransactionService.java | Webhook receiver for card transaction events, wallet balance sync |
| service/card/CardLimitService.java | Enforce daily/monthly spend limits with Redis counters |
| controller/CardController.java | Issue, manage, freeze, cancel cards |
| controller/CardWebhookController.java | POST /api/webhooks/card — receive card transaction events from issuer |
| entity/PrepaidCard.java | JPA entity |
| entity/CardTransaction.java | JPA entity |
| dto/card/IssueCardRequest.java | cardType, deliveryAddress (for physical) |
| dto/card/CardTransactionWebhookPayload.java | Issuer webhook payload structure |

##   **P7.4  REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| POST /api/cards/issue-virtual | INDIVIDUAL\_USER — Instantly issue virtual card |
| POST /api/cards/order-physical | INDIVIDUAL\_USER — Order physical card (requires STANDARD KYC+) |
| GET /api/cards/my-cards | INDIVIDUAL\_USER — All cards with status and balances |
| GET /api/cards/{id}/transactions | INDIVIDUAL\_USER — Card transaction history |
| PUT /api/cards/{id}/freeze | INDIVIDUAL\_USER — Freeze/unfreeze card instantly |
| PUT /api/cards/{id}/limits | INDIVIDUAL\_USER — Set daily/monthly spend limits |
| DELETE /api/cards/{id}/cancel | INDIVIDUAL\_USER — Permanently cancel card |
| POST /api/webhooks/card | System — Receive transaction events from card issuer (secured by webhook secret) |

| C | TRUST & COMPLIANCE FEATURES AML Monitoring  ·  Compliance Reports  ·  Document Vault  ·  Biometric Auth |
| :---: | :---- |

| MODULE P8  ·  COMPLIANCE — NON-NEGOTIABLE AML Transaction Monitoring Engine Structuring Detection · Velocity Rules · Risk Scoring · SAR/CTR Generation |
| :---- |

##   **P8.1  Overview**

Automated monitoring for money laundering patterns. Flags **structuring** (multiple transactions just under reporting thresholds), **unusual velocity** (sudden spike in transfer frequency), and **high-risk geographic corridors**. Generates **Suspicious Activity Reports (SAR)** and **Currency Transaction Reports (CTR)** formatted for Syrian and regional regulatory standards.

##   **P8.2  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| aml\_rules | id, rule\_name, rule\_type (STRUCTURING/VELOCITY/AMOUNT\_THRESHOLD/GEOGRAPHY/PATTERN), threshold\_value, time\_window\_hours, severity (LOW/MEDIUM/HIGH/CRITICAL), is\_active, description |
| aml\_alerts | id, user\_id, rule\_id, triggered\_transactions (JSONB array of tx IDs), severity, status (OPEN/UNDER\_REVIEW/ESCALATED/CLEARED/SAR\_FILED), assigned\_to, notes, created\_at, resolved\_at |
| compliance\_reports | id, report\_type (SAR/CTR/MONTHLY\_SUMMARY), reference\_number, subject\_user\_id, transactions (JSONB), narrative, status (DRAFT/SUBMITTED/ACCEPTED), created\_by, created\_at, submitted\_at |
| user\_risk\_profiles | id, user\_id, overall\_risk (LOW/MEDIUM/HIGH/CRITICAL), last\_assessed\_at, contributing\_factors (JSONB), review\_required (boolean) |

##   **P8.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/aml/AmlMonitoringService.java | Real-time rule evaluation on every transaction event |
| service/aml/AmlRuleEngine.java | Pluggable rule evaluator: structuring, velocity, geography checks |
| service/aml/ComplianceReportService.java | Generate SAR/CTR documents with required fields and narrative |
| service/aml/UserRiskScoringService.java | Aggregate risk signals into a user risk profile |
| controller/AmlController.java | Alert management, rule config, report generation — AUDITOR, PLATFORM\_OWNER |
| entity/AmlAlert.java | JPA entity |
| entity/ComplianceReport.java | JPA entity |
| entity/AmlRule.java | JPA entity |
| dto/aml/AmlAlertResponse.java | Full alert with triggered transactions and risk factors |
| dto/aml/SarRequest.java | subjectUserId, narrative, selectedAlertIds |

##   **P8.4  AML Rule Engine — Detection Logic**

// AmlRuleEngine.evaluate(Transaction tx) — called AFTER every completed transaction:  
//  
// RULE 1 — STRUCTURING DETECTION:  
//   timeWindow \= 24 hours  
//   threshold \= platform CTR reporting limit (e.g. $10,000 USD equivalent)  
//   userDailyTotal \= SUM(amount\_usd) WHERE sender\_id \= tx.senderId  
//                    AND created\_at \> NOW() \- 24h  
//   If userDailyTotal \>= threshold \* 0.8 AND tx.amount \< threshold \* 0.2:  
//     ALERT: STRUCTURING (sender staying just under the limit)  
//  
// RULE 2 — VELOCITY SPIKE:  
//   txCountThisHour \= COUNT(\*) WHERE sender\_id \= tx.senderId AND created\_at \> NOW()-1h  
//   avgTxPerHour \= average over last 30 days for this user  
//   If txCountThisHour \> avgTxPerHour \* 5: ALERT VELOCITY  
//  
// RULE 3 — LARGE AMOUNT:  
//   If tx.amountUsd \> configurable threshold (default $5,000): ALERT HIGH\_VALUE  
//  
// RULE 4 — ROUND AMOUNT PATTERN:  
//   If last 5 txs from same sender all end in 000: ALERT ROUND\_AMOUNT\_PATTERN  
//  
// RULE 5 — NEW ACCOUNT HIGH VOLUME:  
//   If user.createdAt \< 30 days ago AND tx.amountUsd \> $2,000: ALERT NEW\_HIGH\_VALUE  
//  
// On any ALERT: create AmlAlert, notify AUDITOR and MOTHER\_BRANCH\_ADMIN  
// CRITICAL severity: immediately flag user risk profile as HIGH

##   **P8.5  REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| GET /api/aml/alerts | AUDITOR, PLATFORM\_OWNER — All alerts with severity filter, pagination |
| GET /api/aml/alerts/{id} | AUDITOR — Alert detail with full transaction context |
| PUT /api/aml/alerts/{id}/review | AUDITOR — Add notes, change status to UNDER\_REVIEW |
| PUT /api/aml/alerts/{id}/clear | AUDITOR — Clear alert with explanation |
| POST /api/aml/sar | AUDITOR — Generate SAR from one or more alerts |
| POST /api/aml/ctr | AUDITOR — Generate CTR for large transaction |
| GET /api/aml/reports | AUDITOR, PLATFORM\_OWNER — All compliance reports |
| GET /api/aml/user-risk/{userId} | AUDITOR — User risk profile and contributing factors |
| GET /api/aml/rules | PLATFORM\_OWNER — View and configure AML rules |
| PUT /api/aml/rules/{id} | PLATFORM\_OWNER — Adjust rule thresholds |

| MODULE P9  ·  TRUST FEATURES Document Vault & Biometric Authentication Secure Storage · Annual Fee · Face ID / Fingerprint · High-Value TX Protection |
| :---- |

##   **P9.1  Document Vault**

Users upload and securely store important documents: KYC files, property deeds, contracts, trade receipts. Documents are AES-256 encrypted at rest. A **Basic tier** is free (5 documents), **Premium tier** supports unlimited documents for a small annual subscription fee collected via wallet auto-debit.

##   **P9.2  Document Vault — New Tables & Files**

| Table / File | Purpose |
| :---- | :---- |
| Table: vault\_documents | id, user\_id, filename, doc\_type, encrypted\_path, file\_size\_bytes, upload\_at, expires\_at (nullable), tags (TEXT ARRAY), is\_kyc\_linked (boolean) |
| Table: vault\_subscriptions | id, user\_id, tier (FREE/PREMIUM), auto\_renew, next\_billing\_date, amount\_usd, status |
| service/vault/DocumentVaultService.java | Upload, encrypt, download, delete documents via MinIO/S3 |
| service/vault/VaultSubscriptionService.java | Manage tiers, @Scheduled annual billing via wallet debit |
| controller/DocumentVaultController.java | Upload, list, download, delete, share endpoints |
| dto/vault/UploadDocumentRequest.java | filename, docType, tags, expiresAt |

##   **P9.3  Biometric Authentication**

For transactions above a configurable threshold (default $500), users must complete biometric verification. On mobile: **Face ID or fingerprint** via the device's native WebAuthn/FIDO2 API. On web: WebAuthn hardware key or device biometric. The platform stores only the **credential ID and public key** — never biometric data itself.

##   **P9.4  Biometric — New Tables & Files**

| Table / File | Purpose |
| :---- | :---- |
| Table: webauthn\_credentials | id, user\_id, credential\_id (Base64), public\_key (Base64), device\_name, sign\_count, created\_at, last\_used\_at |
| Table: biometric\_challenges | id, user\_id, challenge (Base64), expires\_at, used (boolean) — one-time challenge nonces |
| service/auth/WebAuthnService.java | Generate registration options, verify registration, generate authentication challenge, verify assertion |
| controller/BiometricController.java | POST /register/options, POST /register/verify, POST /auth/options, POST /auth/verify |
| dto/auth/WebAuthnRegistrationRequest.java | credential from client (attestation response) |
| dto/auth/WebAuthnAuthenticationRequest.java | assertion response from client |

##   **P9.5  REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| POST /api/vault/upload | INDIVIDUAL\_USER — Upload and encrypt document |
| GET /api/vault/my-documents | INDIVIDUAL\_USER — List all vault documents (metadata only) |
| GET /api/vault/{id}/download | INDIVIDUAL\_USER — Download and decrypt document |
| DELETE /api/vault/{id} | INDIVIDUAL\_USER — Delete document |
| GET /api/vault/subscription | INDIVIDUAL\_USER — View vault tier and storage usage |
| POST /api/vault/upgrade | INDIVIDUAL\_USER — Upgrade to Premium vault |
| POST /api/biometric/register/options | INDIVIDUAL\_USER — Get WebAuthn registration options |
| POST /api/biometric/register/verify | INDIVIDUAL\_USER — Complete biometric registration |
| POST /api/biometric/auth/options | INDIVIDUAL\_USER — Get authentication challenge |
| POST /api/biometric/auth/verify | INDIVIDUAL\_USER — Verify biometric assertion, returns short-lived elevated token |

| D | ENGAGEMENT & RETENTION Family Wallets  ·  Savings Goals  ·  Split Payments  ·  Recurring  ·  Referrals |
| :---: | :---- |

| MODULE P10  ·  RETENTION ENGINE Family Wallets · Savings Goals · Split Payments · Recurring Transfers · Referral System Five features in one module — all drive daily engagement and viral growth |
| :---- |

##   **P10.1  Family Wallet Groups**

A primary wallet holder creates a Family Group and adds members. Each member keeps their own wallet but the group leader can set **spending limits, view balances, and receive consolidated statements**. Parents control children's wallets. This locks entire families into the platform.

| Table / File | Purpose |
| :---- | :---- |
| Table: family\_groups | id, leader\_user\_id, name, created\_at |
| Table: family\_members | id, group\_id, member\_user\_id, role (LEADER/MEMBER/VIEW\_ONLY), spending\_limit\_daily, spending\_limit\_monthly, can\_receive\_only (boolean), joined\_at |
| service/family/FamilyWalletService.java | Create group, invite, set limits, consolidated statement generation |
| controller/FamilyWalletController.java | Group CRUD, member management, family statement endpoints |

##   **P10.2  Savings Goals**

Users create named savings goals with a target amount and optional deadline. The system auto-sweeps a configured percentage of each incoming wallet credit into the locked savings balance. Platform earns a **small annual yield fee** on the total savings pool.

| Table / File | Purpose |
| :---- | :---- |
| Table: savings\_goals | id, user\_id, name, target\_amount, currency, current\_amount, auto\_sweep\_pct (0-100), deadline (nullable), status (ACTIVE/COMPLETED/CANCELLED), completed\_at |
| Table: savings\_contributions | id, goal\_id, amount, source (AUTO\_SWEEP/MANUAL), wallet\_transaction\_id, created\_at |
| service/savings/SavingsGoalService.java | Create/manage goals, trigger auto-sweep on wallet credit events, handle completion |
| controller/SavingsGoalController.java | CRUD for goals, manual deposit, withdraw, progress endpoints |

##   **P10.3  Split Payments**

A user sends a split request to multiple friends: 'We owe Ahmed $120 — split 4 ways'. Each participant receives an in-app request. When all accept, the system automatically collects from each participant's wallet and credits the payee in a single atomic transaction.

| Table / File | Purpose |
| :---- | :---- |
| Table: split\_requests | id, initiator\_user\_id, payee\_user\_id, total\_amount, currency, description, status (PENDING/PARTIAL/COMPLETED/CANCELLED/EXPIRED), expires\_at, created\_at |
| Table: split\_participants | id, split\_request\_id, user\_id, amount\_owed, status (PENDING/PAID/DECLINED), paid\_at, wallet\_transaction\_id |
| service/split/SplitPaymentService.java | Create split request, handle acceptances, execute atomic collection when complete |
| controller/SplitPaymentController.java | Create split, respond (accept/decline), view my splits, cancel |

##   **P10.4  Recurring Transfers**

Users schedule automatic transfers to repeat daily, weekly, or monthly. The system executes them at the scheduled time, deducting from the wallet. If the balance is insufficient, the user is notified and the transfer is retried once after 4 hours before skipping to the next cycle.

| Table / File | Purpose |
| :---- | :---- |
| Table: recurring\_transfers | id, sender\_user\_id, receiver\_user\_id, amount, currency, frequency (DAILY/WEEKLY/MONTHLY), next\_run\_at, last\_run\_at, status (ACTIVE/PAUSED/CANCELLED), retry\_count, created\_at |
| Table: recurring\_transfer\_log | id, recurring\_id, status (SUCCESS/FAILED/SKIPPED), transaction\_id (nullable), reason, executed\_at |
| service/recurring/RecurringTransferService.java | @Scheduled every 15 minutes: find due transfers, execute, handle retry |
| controller/RecurringTransferController.java | Create, pause, resume, cancel recurring transfers |

##   **P10.5  Referral System**

Each user gets a unique **referral code**. When a new user registers with the code and completes their first transaction, **both users receive a reward** — configurable as fee-free transfers for N days, wallet credit, or Trust Score points. Multi-level tracking allows PLATFORM\_OWNER to see which users are driving acquisition.

| Table / File | Purpose |
| :---- | :---- |
| Table: referral\_codes | id, user\_id, code (unique 8-char), total\_referrals, total\_rewards\_given, created\_at |
| Table: referrals | id, referrer\_user\_id, referred\_user\_id, code\_used, status (PENDING/REWARDED/EXPIRED), referrer\_reward\_type, referred\_reward\_type, rewarded\_at, created\_at |
| Table: referral\_rewards | id, user\_id, reward\_type (FEE\_FREE\_DAYS/WALLET\_CREDIT/TRUST\_SCORE\_BONUS), value, valid\_until, is\_consumed, created\_at |
| service/referral/ReferralService.java | Generate codes, track referrals, apply rewards, check eligibility |
| controller/ReferralController.java | Get my code, referral stats, reward history |

##   **P10.6  Combined REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| POST /api/family/groups | INDIVIDUAL\_USER — Create family group |
| POST /api/family/groups/{id}/invite | Leader — Invite member by username or phone |
| PUT /api/family/groups/{id}/members/{uid}/limits | Leader — Set spending limits for member |
| GET /api/family/groups/{id}/statement | Leader — Consolidated family financial statement |
| POST /api/savings/goals | INDIVIDUAL\_USER — Create savings goal |
| GET /api/savings/goals | INDIVIDUAL\_USER — All goals with progress |
| POST /api/savings/goals/{id}/deposit | INDIVIDUAL\_USER — Manual deposit to goal |
| DELETE /api/savings/goals/{id}/withdraw | INDIVIDUAL\_USER — Withdraw from goal (cancels goal) |
| POST /api/split | INDIVIDUAL\_USER — Create split payment request |
| PUT /api/split/{id}/respond | INDIVIDUAL\_USER — Accept or decline a split request |
| GET /api/split/my-requests | INDIVIDUAL\_USER — Outgoing and incoming splits |
| POST /api/recurring | INDIVIDUAL\_USER — Create recurring transfer |
| PUT /api/recurring/{id}/pause | INDIVIDUAL\_USER — Pause recurring transfer |
| GET /api/referral/my-code | INDIVIDUAL\_USER — Get my referral code and stats |
| GET /api/referral/my-rewards | INDIVIDUAL\_USER — Active and consumed referral rewards |

| E | OPERATIONAL INTELLIGENCE Analytics  ·  Liquidity AI  ·  Disputes  ·  Multi-Language |
| :---: | :---- |

| MODULE P11  ·  OPERATIONAL INTELLIGENCE Branch Analytics · Dynamic Liquidity · Dispute Management · Multi-Language Four infrastructure modules that make the platform enterprise-grade at scale |
| :---- |

##   **P11.1  Branch Performance Analytics Dashboard**

Real-time and historical analytics for branch managers and the platform owner. Transaction volume heatmaps by hour and day-of-week, cashier performance rankings, peak prediction, and **branch profitability per operational period**. All powered by pre-aggregated PostgreSQL views refreshed every 15 minutes via materialized views.

| Table / File | Purpose |
| :---- | :---- |
| Materialized View: branch\_hourly\_stats | branch\_id, date, hour, tx\_count, total\_volume\_usd, avg\_tx\_usd, fee\_revenue, refreshed every 15 min |
| Materialized View: cashier\_daily\_stats | cashier\_id, branch\_id, date, tx\_count, total\_volume\_usd, qr\_releases, avg\_processing\_time\_seconds |
| service/analytics/BranchAnalyticsService.java | Query materialized views, compute KPIs, generate heatmap data structures |
| service/analytics/PeakPredictionService.java | Use last 8 weeks of hourly data to predict next week's peak hours per branch |
| controller/BranchAnalyticsController.java | Heatmap, cashier leaderboard, profitability, peak prediction endpoints |
| dto/analytics/HeatmapDataResponse.java | 24x7 matrix of tx counts/volumes for heatmap rendering |

##   **P11.2  Dynamic Liquidity Management**

The AI assistant (Phase I Module 2\) is extended to **proactively alert branch managers** when their fund balance is predicted to run low within 48 hours. The prediction model uses historical transaction outflow patterns per branch, day-of-week seasonality, and upcoming recurring transfers that will draw from the fund.

| Table / File | Purpose |
| :---- | :---- |
| Table: liquidity\_alerts | id, branch\_id, fund\_id, predicted\_depletion\_at, current\_balance, predicted\_balance\_48h, alert\_severity (WARNING/CRITICAL), acknowledged\_by, created\_at |
| service/liquidity/LiquidityForecastService.java | 24h/48h balance prediction using weighted moving average of daily outflows |
| scheduler/LiquidityMonitorScheduler.java | @Scheduled every 4 hours: run forecasts for all active funds, create alerts if needed |
| controller/LiquidityController.java | GET /api/liquidity/my-branch/forecast — 48h outlook for branch manager |

##   **P11.3  Chargeback & Dispute Management**

Users file disputes against transactions. The system routes them to the correct branch with a **48-hour SLA timer**. Unresolved disputes auto-escalate to MOTHER\_BRANCH\_ADMIN at 36 hours and to PLATFORM\_OWNER at 48 hours. Full resolution audit trail.

| Table / File | Purpose |
| :---- | :---- |
| Table: disputes | id, user\_id, transaction\_id, dispute\_type (UNAUTHORISED/WRONG\_AMOUNT/NOT\_RECEIVED/OTHER), description, evidence\_url, status (OPEN/UNDER\_REVIEW/RESOLVED\_FOR\_USER/RESOLVED\_FOR\_MERCHANT/ESCALATED/CLOSED), assigned\_branch\_id, assigned\_to, sla\_deadline, resolution\_notes, created\_at, resolved\_at |
| service/dispute/DisputeService.java | File, route, escalate, resolve disputes |
| scheduler/DisputeSlaScheduler.java | @Scheduled hourly: check SLA deadlines, escalate overdue disputes |
| controller/DisputeController.java | File, track, respond to, resolve disputes |

##   **P11.4  Multi-Language Support (AR/EN/TR/FR/KU)**

Full internationalisation across all user-facing content: API error messages, notification templates, AI-generated messages, email templates, and the entire Next.js frontend. Language preference stored per user. RTL layout automatically applied for Arabic and Kurdish.

| Table / File | Purpose |
| :---- | :---- |
| Table: translations | id, language\_code, message\_key, message\_value, created\_at, updated\_by — all system messages |
| Table: notification\_templates | id, template\_key, language\_code, subject, body, channel — multilingual notification content |
| service/i18n/TranslationService.java | Resolve message keys with language fallback chain (user pref → branch default → EN) |
| service/i18n/AITranslationService.java | On-demand AI translation for dynamically generated content (AI birthday messages in user's language) |
| config/I18nConfig.java | Spring MessageSource configuration with DB-backed messages |
| Next.js: lib/i18n/ | next-intl configuration: 5 locale files, RTL detection for AR/KU |

##   **P11.5  REST Endpoints — All Operational Intelligence**

| Method \+ Path | Access / Description |
| :---- | :---- |
| GET /api/analytics/my-branch/heatmap | BRANCH\_MANAGER — 24x7 transaction heatmap (current week) |
| GET /api/analytics/my-branch/cashiers | BRANCH\_MANAGER — Cashier performance leaderboard |
| GET /api/analytics/my-branch/peak-prediction | BRANCH\_MANAGER — Predicted busy hours for next 7 days |
| GET /api/analytics/platform | PLATFORM\_OWNER — Cross-all-branches analytics summary |
| GET /api/liquidity/my-branch/forecast | BRANCH\_MANAGER — 48-hour fund balance forecast |
| GET /api/liquidity/alerts | BRANCH\_MANAGER, MOTHER\_BRANCH\_ADMIN — Active liquidity alerts |
| PUT /api/liquidity/alerts/{id}/acknowledge | BRANCH\_MANAGER — Acknowledge alert |
| POST /api/disputes | INDIVIDUAL\_USER, CORPORATE\_ADMIN — File a dispute |
| GET /api/disputes/my-disputes | Own user — All my disputes with status and SLA timer |
| GET /api/disputes/branch | BRANCH\_MANAGER — Disputes assigned to my branch |
| PUT /api/disputes/{id}/resolve | BRANCH\_MANAGER — Resolve dispute with decision |
| GET /api/disputes/escalated | MOTHER\_BRANCH\_ADMIN — All escalated disputes |
| GET /api/i18n/translations/{lang} | PUBLIC — Full translation bundle for a language |
| PUT /api/i18n/translations | PLATFORM\_OWNER — Update a translation string |

| AUDIT LOG REFERENCE — PHASE III Every action below must be written to audit\_logs using the existing AuditService |
| :---: |

| action Value | Module  ·  Trigger |
| :---- | :---- |
| CREDIT\_SCORE\_CALCULATED | P1  ·  Weekly score recalculation or post-transaction update |
| LOAN\_APPLICATION\_SUBMITTED | P1  ·  User applies for micro-loan |
| LOAN\_APPROVED | P1  ·  Admin approves loan with offer |
| LOAN\_DISBURSED | P1  ·  Loan credited to user wallet |
| LOAN\_REPAYMENT\_SUCCESS | P1  ·  Instalment paid on time |
| LOAN\_REPAYMENT\_MISSED | P1  ·  Missed instalment, late fee applied |
| LOAN\_DEFAULTED | P1  ·  Loan escalated to default status |
| LOAN\_PAID\_OFF | P1  ·  Full loan repaid |
| BILL\_PAYMENT\_SUBMITTED | P2  ·  User submits bill payment |
| BILL\_PAYMENT\_COMPLETED | P2  ·  Bill payment confirmed |
| MERCHANT\_REGISTERED | P2  ·  New merchant onboarded |
| MERCHANT\_PAYMENT\_PROCESSED | P2  ·  User pays merchant |
| MERCHANT\_SETTLEMENT\_PROCESSED | P2  ·  Daily merchant settlement |
| BATCH\_JOB\_SUBMITTED | P3  ·  Corporate batch upload submitted |
| BATCH\_JOB\_APPROVED | P3  ·  Batch approved for execution |
| BATCH\_JOB\_COMPLETED | P3  ·  Batch fully processed with summary |
| BATCH\_ROW\_FAILED | P3  ·  Individual row failed in batch |
| ESCROW\_FUNDED | P4  ·  Escrow contract funded |
| ESCROW\_RELEASED | P4  ·  Funds released to beneficiary |
| ESCROW\_DISPUTED | P4  ·  Dispute opened on escrow |
| FORWARD\_CONTRACT\_CREATED | P5  ·  Forward FX contract locked |
| FORWARD\_CONTRACT\_EXECUTED | P5  ·  Contract executed at maturity |
| TENANT\_PROVISIONED | P6  ·  New white-label tenant created |
| CARD\_ISSUED | P7  ·  Virtual or physical card issued |
| CARD\_TRANSACTION | P7  ·  Card purchase or ATM event |
| AML\_ALERT\_TRIGGERED | P8  ·  AML rule fired on transaction |
| AML\_ALERT\_CLEARED | P8  ·  Alert cleared by AUDITOR |
| SAR\_FILED | P8  ·  Suspicious Activity Report generated |
| DISPUTE\_FILED | P11  ·  User opens dispute |
| DISPUTE\_RESOLVED | P11  ·  Branch manager resolves dispute |
| DISPUTE\_ESCALATED | P11  ·  SLA breach, auto-escalated |
| SAVINGS\_GOAL\_COMPLETED | P10  ·  User reaches savings target |
| RECURRING\_TRANSFER\_EXECUTED | P10  ·  Scheduled transfer runs |
| REFERRAL\_REWARDED | P10  ·  Referral reward granted |
| LIQUIDITY\_ALERT\_CREATED | P11  ·  AI predicts fund depletion \< 48h |

| CURSOR EXECUTION PHASES — PASTE IN ORDER 11 phases. Complete and commit each before starting the next. |
| :---: |

| ⚡  CURSOR PHASE H  —  MICRO-LENDING ENGINE — MODULE P1 *"Implement Module P1 completely. Create all entities: CreditProfile, LoanProduct, LoanApplication, Loan, LoanRepaymentSchedule, LoanRepayment, LoanLateFee with full JPA relationships. Build CreditScoringService using the 6-component weighted formula pulling from existing transactions, GamificationService (trustScore), and wallet age. Store scores in credit\_profiles with weekly recalculation via @Scheduled. Build LoanApplicationService with admin approval flow. Build LoanDisbursementService that credits wallet, generates the full amortisation schedule, and collects origination fee via PlatformRevenueService. Build LoanSchedulerService that runs daily at 8am to auto-debit wallet instalments, mark missed payments, apply late fees, flag defaults, and detect pay-off. Connect badge award ('Debt Free') to GamificationService on payoff. Hook LoanLatePayment into credit score reduction immediately. Build all REST endpoints. Annotate disbursement and interest collection with @PlatformRevenue."* |
| :---- |

| ⚡  CURSOR PHASE I  —  BILL PAYMENTS & MERCHANT NETWORK — MODULE P2 *"Implement Module P2 completely. Build BillProviderService with seeded providers for Syria (Syriatel top-up, MTN top-up, electricity, water, internet). Build BillPaymentService with two execution paths: DIRECT\_API (async WebClient call) and MANUAL (branch cashier confirmation flow). Generate merchant QR codes using the existing BarcodeGenerationService from Phase I — reuse the same ZXing \+ HMAC signing approach with a MERCHANT type prefix in the payload. Build MerchantPaymentService with SERIALIZABLE transaction isolation, pessimistic wallet locking, and real-time receipt delivery via WhatsApp and SSE. Build MerchantSettlementService scheduled daily at 11pm to batch-settle all net merchant earnings. Apply @PlatformRevenue to all fee collection points."* |
| :---- |

| ⚡  CURSOR PHASE J  —  BULK BATCH TRANSFERS — MODULE P3 *"Implement Module P3. Build BatchUploadService that accepts multipart file upload, parses CSV using Apache Commons CSV or OpenCSV (add Maven dependency), validates each row (receiver exists, amount valid, currency supported), and returns a BatchUploadResponse with per-row validation summary. Build BatchExecutionService as a @Async service using a dedicated thread pool (configure a ThreadPoolTaskExecutor named batchExecutor with coreSize 5, maxSize 10). Process rows in pages of 100 inside individual @Transactional boundaries — NOT one giant transaction. Broadcast real-time progress via SSE using SseEmitter registered in a ConcurrentHashMap by jobId. Require explicit MOTHER\_BRANCH\_ADMIN approval before execution. Generate a downloadable CSV report on completion with per-row outcome."* |
| :---- |

| ⚡  CURSOR PHASE K  —  ESCROW & FORWARD CONTRACTS — MODULES P4 \+ P5 *"Implement Module P4 (Escrow) and Module P5 (Forward Contracts) together. For Escrow: create EscrowContract as a state machine with events logged in EscrowEvent (immutable append-only). Implement all 5 condition types. Build EscrowFeeAccrualService scheduled daily at 1am to debit holding fees. Build EscrowDisputeService that freezes both parties' wallets (call WalletService.freezeWallet()) and creates an AML alert. For Forward Contracts: build ForwardRateService using the simple spread model with configurable bps per term. Build ForwardContractScheduler to execute matured contracts daily and credit platform spread profit via PlatformRevenueService. Both modules write full audit trails."* |
| :---- |

| ⚡  CURSOR PHASE L  —  WHITE-LABEL TENANCY & PREPAID CARDS — MODULES P6 \+ P7 *"Implement Module P6 (Multi-tenancy) using Hibernate discriminator filters. Add tenant\_id column via Flyway migration to all key tables, default to tenant\_id=1. Build TenantResolutionFilter that reads tenantId from JWT claims and sets TenantContextHolder (ThreadLocal). Register a Hibernate @Filter on all affected entities so every query is automatically scoped. Build TenantProvisioningService to seed a new tenant's admin user, default commission rates, and branding config. Build TenantBillingService scheduled monthly. For Module P7 (Cards): create the CardIssuingService as an interface with a MockCardIssuer implementation for development (returns fake card tokens) and a real Marqeta/Nuvei implementation for production, switchable via Spring profiles. Build CardWebhookController to receive transaction events from the card issuer, verify webhook HMAC signature, and sync wallet balance."* |
| :---- |

| ⚡  CURSOR PHASE M  —  AML MONITORING & COMPLIANCE — MODULE P8 *"Implement Module P8 fully. Build AmlRuleEngine as a pluggable list of Rule implementations (Strategy pattern): each rule takes a Transaction and returns Optional\<AmlAlert\>. Register 5 rules as Spring beans: StructuringRule, VelocityRule, LargeAmountRule, RoundAmountPatternRule, NewAccountHighValueRule. Wire AmlMonitoringService to call all rules after every completed transaction (use ApplicationEventPublisher \+ @EventListener on TransactionCompletedEvent). Implement UserRiskScoringService that aggregates all open alerts for a user into an overall risk profile. Build ComplianceReportService that generates SAR reports as structured JSON/PDF with all required fields. Create AmlController endpoints for the AUDITOR role. Add AML alert creation to EscrowDisputeService and LoanDefaultService."* |
| :---- |

| ⚡  CURSOR PHASE N  —  DOCUMENT VAULT & BIOMETRIC AUTH — MODULE P9 *"Implement Module P9. For Document Vault: build DocumentVaultService using Spring's Resource abstraction over MinIO/S3, encrypting files with AES-256-GCM before upload and decrypting on download. Store encryption key per-document derived from user ID \+ platform master key. Build VaultSubscriptionService with annual wallet auto-debit. For Biometric Auth: integrate java-webauthn-server library (add Maven dependency: com.yubico:webauthn-server-core:2.5.0). Build WebAuthnService with full registration and authentication flows. Generate challenges as cryptographically random 32-byte nonces stored in biometric\_challenges with 2-minute TTL. Return a short-lived 'elevated JWT' on successful biometric assertion that is required for high-value transactions above the configurable threshold."* |
| :---- |

| ⚡  CURSOR PHASE O  —  ENGAGEMENT FEATURES — MODULE P10 (ALL FIVE) *"Implement all five Module P10 features. Family Wallets: create FamilyGroup and FamilyMember entities, implement spending limit enforcement in WalletService.debit() by checking if the user is a family member with a limit set. Savings Goals: hook into WalletService after every incoming credit — if user has active goals with auto\_sweep\_pct \> 0, automatically split the credit and move the sweep amount to the savings balance. Split Payments: implement atomic collection using a single @Transactional method that processes all PAID participants in one database transaction. Recurring Transfers: @Scheduled every 15 minutes query recurring\_transfers WHERE next\_run\_at \<= NOW() AND status='ACTIVE', execute, update next\_run\_at. Referral System: generate 8-character alphanumeric referral codes at user registration, hook reward dispensing into the first transaction completion event."* |
| :---- |

| ⚡  CURSOR PHASE P  —  OPERATIONAL INTELLIGENCE — MODULE P11 *"Implement Module P11 all four features. Analytics: create two PostgreSQL materialized views (branch\_hourly\_stats and cashier\_daily\_stats) with Flyway migration. Schedule REFRESH MATERIALIZED VIEW CONCURRENTLY every 15 minutes using @Scheduled. Build BranchAnalyticsService querying these views. Build PeakPredictionService using a simple 8-week weighted moving average per hour-of-week. Liquidity: implement LiquidityForecastService calculating 48h predicted balance as currentBalance minus average daily outflow (from last 14 days) multiplied by 2, adjusted for day-of-week seasonality. Disputes: implement DisputeService with state machine (OPEN→UNDER\_REVIEW→RESOLVED/ESCALATED). DisputeSlaScheduler checks every hour for disputes approaching deadline and fires escalation events. Multi-Language: add translations table with Flyway seed data for all 5 languages (AR/EN/TR/FR/KU). Build TranslationService as a MessageSource implementation backed by the DB with Redis caching. Update all notification templates to use translation keys. Add RTL detection to Next.js layout."* |
| :---- |

| ALMUKHTAR ELITE Phase III — The Full Ecosystem Micro-Lending  ·  Merchants  ·  Batch  ·  Escrow  ·  Forwards  ·  White-Label  ·  Cards AML  ·  Vault  ·  Biometrics  ·  Family  ·  Savings  ·  Split  ·  Recurring  ·  Referrals  ·  Analytics ────────────────────────────────────────────────────── © 2025 ALMUKHTAR Elite — Confidential — Senior Fintech Architecture Division |
| :---: |

