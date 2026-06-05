

| ⚙ ALMUKHTAR ELITE BACKEND CONTROL PROMPT Audit  ·  Complete Missing Features  ·  Fix Gaps  ·  Full Test Suite Phase I  ·  Phase II  ·  Phase III  ·  All 30+ Modules  ·  Spring Boot 3.3.4  ·  Java 21 ──────────────────────────────────────────────────── COMPREHENSIVE CURSOR IMPLEMENTATION PROMPT Run every task in order. The backend is not done until Task 12 shows zero failures. Version 1.0  ·  Backend Engineering Division |
| :---: |

| 📋  HOW TO USE THIS DOCUMENT This prompt is structured as 12 sequential tasks. Each task is a self-contained Cursor Composer instruction. Paste each task prompt into Cursor exactly as written. Cursor will audit the existing code, identify gaps, implement any missing pieces, and run the required tests. Do not skip a task or reorder them — each builds on the previous. The goal is a fully implemented, fully tested backend ready for frontend integration. |
| :---- |

##   **Complete Module Inventory — What Must Exist**

The backend must implement **all modules across all three phases**. Use this inventory as the master checklist. Any module not fully implemented will be caught and completed in Task 01\.

| No. | Module | Key Service(s) | Key Entity/Table(s) | Status |
| :---: | :---- | :---- | :---- | :---: |
| **01** | **Auth & JWT** | AuthService, JwtService | users, refresh\_tokens | **□ AUDIT** |
| **02** | **Fund Management** | FundService | funds | **□ AUDIT** |
| **03** | **User Management** | UserService | users | **□ AUDIT** |
| **04** | **Transaction (Simple)** | TransactionService | transactions | **□ AUDIT** |
| **05** | **Transaction (Comprehensive)** | TransactionService, FeeCalculationService | transactions, commission\_rates | **□ AUDIT** |
| **06** | **Exchange Rates** | ExchangeRateService, CurrencyConversionService | currencies | **□ AUDIT** |
| **07** | **Fee Configuration** | FeeService | commission\_rates, branch\_fee\_rates | **□ AUDIT** |
| **08** | **Audit System** | AuditService | audit\_logs | **□ AUDIT** |
| **09** | **Smart-Bridge QR** | BarcodeGenerationService, ScannerValidationService | qr\_tokens | **□ AUDIT** |
| **10** | **Almukhtar AI (RAG)** | AlmukhtarAIService, FinancialContextRetriever | (PGVector store) | **□ AUDIT** |
| **11** | **Double-Entry Accounting** | AccountingLedgerService | ledger\_accounts, ledger\_entries | **□ AUDIT** |
| **12** | **Corporate Accounts** | CorporateAccountService | corporate\_accounts, sub\_accounts | **□ AUDIT** |
| **13** | **Gamification** | GamificationService, FeeDiscountService | user\_gamification\_profile, badges | **□ AUDIT** |
| **14** | **Offline Sync Queue** | SyncQueueService, SyncConflictResolver | sync\_queue | **□ AUDIT** |
| **15** | **WhatsApp Notifications** | WhatsAppNotificationProvider | (external API) | **□ AUDIT** |
| **16** | **Role Hierarchy** | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN | users (role enum) | **□ AUDIT** |
| **17** | **Digital Wallet \+ KYC** | WalletService, DocumentVerificationService | wallets, wallet\_balances, kyc\_documents | **□ AUDIT** |
| **18** | **Geo Branch Finder** | BranchGeoService, PrivacyLocationService | branches (lat/lng) | **□ AUDIT** |
| **19** | **Real-Time Trading** | YahooFinanceService, OrderExecutionService | orders, positions, trading\_accounts | **□ AUDIT** |
| **20** | **Platform Revenue AOP** | PlatformRevenueService, PlatformRevenueAspect | platform\_revenue\_entries | **□ AUDIT** |
| **21** | **Geo-Temporal Fee Zones** | FeeZoneEvaluationService, IslamicCalendarService | fee\_zones, fee\_zone\_rules | **□ AUDIT** |
| **22** | **AI Push Notifications** | CampaignService, AIMessageGeneratorService | notification\_campaigns, in\_app\_notifications | **□ AUDIT** |
| **23** | **Micro-Lending** | CreditScoringService, LoanSchedulerService | loans, credit\_profiles | **□ AUDIT** |
| **24** | **Bill Payments & Merchants** | BillPaymentService, MerchantPaymentService | merchants, bill\_payment\_requests | **□ AUDIT** |
| **25** | **Bulk Batch Transfers** | BatchUploadService, BatchExecutionService | batch\_jobs, batch\_job\_rows | **□ AUDIT** |
| **26** | **Escrow** | EscrowService, EscrowFeeAccrualService | escrow\_contracts, escrow\_events | **□ AUDIT** |
| **27** | **Forward Contracts** | ForwardContractService, ForwardRateService | forward\_contracts | **□ AUDIT** |
| **28** | **White-Label Tenancy** | TenantProvisioningService, TenantBillingService | tenants, tenant\_billing | **□ AUDIT** |
| **29** | **Prepaid Cards** | CardIssuingService, CardTransactionService | prepaid\_cards, card\_transactions | **□ AUDIT** |
| **30** | **AML Monitoring** | AmlRuleEngine, AmlMonitoringService | aml\_alerts, aml\_rules | **□ AUDIT** |
| **31** | **Document Vault & Biometric** | DocumentVaultService, WebAuthnService | vault\_documents, webauthn\_credentials | **□ AUDIT** |
| **32** | **Family Wallets** | FamilyWalletService | family\_groups, family\_members | **□ AUDIT** |
| **33** | **Savings Goals** | SavingsGoalService | savings\_goals | **□ AUDIT** |
| **34** | **Split Payments** | SplitPaymentService | split\_requests, split\_participants | **□ AUDIT** |
| **35** | **Recurring Transfers** | RecurringTransferService | recurring\_transfers | **□ AUDIT** |
| **36** | **Referral System** | ReferralService | referral\_codes, referrals | **□ AUDIT** |
| **37** | **Branch Analytics** | BranchAnalyticsService, PeakPredictionService | branch\_hourly\_stats (mat. view) | **□ AUDIT** |
| **38** | **Liquidity Management** | LiquidityForecastService | liquidity\_alerts | **□ AUDIT** |
| **39** | **Dispute Management** | DisputeService, DisputeSlaScheduler | disputes | **□ AUDIT** |
| **40** | **Multi-Language (i18n)** | TranslationService, AITranslationService | translations, notification\_templates | **□ AUDIT** |

| TASK 01  ·  Scan every module · Map missing files · Generate gap report Full Backend Structural Audit |
| :---- |

| ⚡  CURSOR TASK 01  —  FULL STRUCTURAL AUDIT *"Perform a complete structural audit of the entire Spring Boot project. For every module listed in the inventory table above (01 through 40), scan the src/main/java directory and verify: (1) the service class exists and is annotated with @Service; (2) the controller class exists and is annotated with @RestController with the correct @RequestMapping path; (3) all JPA entities listed exist, are annotated with @Entity and @Table, and have the correct fields; (4) all repositories exist as interfaces extending JpaRepository or PagingAndSortingRepository; (5) all DTOs referenced in the spec exist with correct field names and @Valid annotations. After scanning, produce a structured gap report in this exact format for every missing item: \[GAP\] Module {number} \- {ModuleName}: Missing {file type} \- {ClassName} \- Action: CREATE. Also list: \[INCOMPLETE\] for classes that exist but are missing methods. Do not implement anything yet — audit only and report."* |
| :---- |

##   **Task 01 — What the Audit Must Verify**

### **◆  Package Structure Check**

Confirm the following package structure exists and every class is in the correct package:

com.mycompany.transfersystem/  
├── annotation/          → @PlatformRevenue  
├── aop/                 → PlatformRevenueAspect  
├── config/              → SecurityConfig, DataInitializer, JwtFilter, SpringAIConfig,  
│                            TenantHibernateFilter, WebSocketConfig, I18nConfig  
├── controller/          → All 30+ controllers  
├── dto/                 → All request/response DTOs grouped by domain  
├── entity/              → All JPA entities  
├── entity/enums/        → UserRole, FundStatus, TransactionStatus, CommissionScope,  
│                            RiskTier, SyncStatus, KycTier, WalletStatus, EntryType,  
│                            AccountType, NotificationType, UserTier  
├── event/               → TransactionCompletedEvent (ApplicationEvent subclass)  
├── exception/           → GlobalExceptionHandler \+ all custom exception classes  
├── filter/              → TenantResolutionFilter, JwtAuthenticationFilter  
├── repository/          → All JPA repositories  
├── scheduler/           → All @Scheduled classes (separate from services)  
├── service/             → Domain-grouped service classes  
│   ├── aml/  
│   ├── analytics/  
│   ├── auth/  
│   ├── batch/  
│   ├── bills/  
│   ├── card/  
│   ├── dispute/  
│   ├── escrow/  
│   ├── family/  
│   ├── feezone/  
│   ├── fx/  
│   ├── geo/  
│   ├── i18n/  
│   ├── lending/  
│   ├── liquidity/  
│   ├── merchant/  
│   ├── notification/  
│   ├── recurring/  
│   ├── referral/  
│   ├── revenue/  
│   ├── savings/  
│   ├── split/  
│   ├── tenant/  
│   ├── trading/  
│   └── vault/  
└── websocket/           → PriceStreamHandler, TradingWebSocketConfig

### **◆  Critical Cross-Cutting Concerns — Must All Exist**

* **TransactionCompletedEvent** — ApplicationEvent fired after EVERY completed transaction, consumed by AmlMonitoringService, CreditScoringService, GamificationService, ReferralService

* **@PlatformRevenue annotation** — applied on: OrderExecutionService.placeOrder(), WalletService.exchangeCurrency(), TransactionService.processComprehensiveTransfer(), LoanDisbursementService.disburse(), MerchantPaymentService.processPayment(), EscrowFeeAccrualService.accrue()

* **GlobalExceptionHandler** — must handle: all custom exceptions \+ MethodArgumentNotValidException \+ DataIntegrityViolationException \+ PessimisticLockingFailureException \+ every external API exception

* **TenantContextHolder (ThreadLocal)** — set in TenantResolutionFilter, cleared in finally block after every request

* **MDC Correlation ID Filter** — adds X-Request-ID to every log line and response header for tracing

* **AuditService.log()** — called in every service method that mutates state, with correct action, entityType, entityId, and details

| TASK 02  ·  Create every missing file · Complete every partial implementation Implement All Identified Gaps |
| :---- |

| ⚡  CURSOR TASK 02  —  IMPLEMENT ALL GAPS FROM AUDIT REPORT *"Using the gap report produced in Task 01, implement every missing and incomplete item. Follow this exact priority order: (1) Missing entities and enums first — no service can work without them; (2) Missing repositories second; (3) Missing service classes third, implementing every method stub with TODO if the full logic is complex; (4) Missing controllers fourth, wiring them to existing services; (5) Missing DTOs last. For every file you create: add @Slf4j logging, add full JavaDoc on the class explaining its role, ensure it is in the correct package, and import only what is needed. After creating each file, verify it compiles by mentally tracing its dependencies. Do NOT leave any import unresolved. After completing all gaps, re-run the structural audit mentally and confirm the gap report would now be empty."* |
| :---- |

##   **Task 02 — Implementation Rules**

### **◆  Rule 1 — Entity Completeness**

Every JPA entity must have: **@Entity, @Table(name), @Id, @GeneratedValue, @CreatedDate, @LastModifiedDate**, all fields with correct **@Column** constraints (nullable, unique, length), correct **@ManyToOne / @OneToMany** relationships with explicit **fetch \= FetchType.LAZY** on all collection associations, and a **@Version** field for optimistic locking on entities that handle concurrent updates (Wallet, Fund, Loan, TradingAccount).

### **◆  Rule 2 — Service Method Completeness**

Every service method must: wrap the body in **try-catch with log.error()**, call **auditService.log()** for any mutation, throw a **typed custom exception** (never RuntimeException directly), and be annotated with **@Transactional** with explicit isolation level where required (SERIALIZABLE for financial mutations, READ\_COMMITTED for reads).

### **◆  Rule 3 — Controller Completeness**

Every controller endpoint must have: **@PreAuthorize** with explicit role check, **@Valid** on @RequestBody parameters, a correct **@Operation** annotation for OpenAPI, and return **ResponseEntity\<T\>** with explicit HTTP status (never implicit 200 for POSTs — use 201 Created).

### **◆  Rule 4 — Missing Event Wiring**

// TransactionCompletedEvent must be published in TransactionService  
// after EVERY status change to COMPLETED:  
   
@Autowired ApplicationEventPublisher eventPublisher;  
   
// After transaction.setStatus(TransactionStatus.COMPLETED):  
eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction));  
   
// Listeners (all annotated with @EventListener @Async):  
// \- AmlMonitoringService.onTransactionCompleted()  
// \- CreditScoringService.onTransactionCompleted()  // trigger score update  
// \- GamificationService.onTransactionCompleted()   // update stats  
// \- ReferralService.onFirstTransactionCompleted()  // check referral reward  
// \- SavingsGoalService.onWalletCredited()          // trigger auto-sweep

| TASK 03  ·  Flyway scripts · Schema completeness · Indexes · Constraints Database & Migration Layer Audit |
| :---- |

| ⚡  CURSOR TASK 03  —  DATABASE & MIGRATION LAYER COMPLETION *"Audit the entire Flyway migration directory (src/main/resources/db/migration). Verify every table referenced in any entity class has a corresponding Flyway migration script. For any missing table, create the migration script with the correct V{n}\_\_ naming convention. Then verify: (1) All critical performance indexes exist — add any missing ones in a new migration file; (2) All financial table constraints are present (CHECK amount \> 0, CHECK balance \>= 0); (3) The earthdistance PostgreSQL extension is enabled via migration for geo queries; (4) The pgvector extension is enabled for AI vector storage; (5) All materialized views (branch\_hourly\_stats, cashier\_daily\_stats) have creation scripts with CONCURRENTLY-safe refresh triggers; (6) The tenant\_id column exists on all multi-tenant tables with a default of 1; (7) All JSONB columns have GIN indexes for fast querying; (8) The DataInitializer seeds all 7 user roles, 3 branches, 4 currencies, 2 funds, all commission rate scopes, 5 AML rules, 3 loan products, and 5 bill providers. Fix everything found."* |
| :---- |

##   **Task 03 — Complete Index Requirements**

| Table | Required Indexes — Create if Missing |
| :---- | :---- |
| transactions | (sender\_id), (receiver\_id), (status), (created\_at DESC), (fund\_id), (tenant\_id) |
| wallet\_balances | (wallet\_id, currency\_code) UNIQUE, (wallet\_id) |
| orders | (trading\_account\_id), (symbol), (status), (created\_at DESC) |
| positions | (trading\_account\_id, symbol) UNIQUE |
| price\_snapshots | (symbol, captured\_at DESC) — partition by day recommended |
| loans | (user\_id), (status), (disbursed\_at) |
| loan\_repayment\_schedule | (loan\_id), (due\_date) WHERE status='PENDING' — partial index |
| aml\_alerts | (user\_id), (status), (severity), (created\_at DESC) |
| qr\_tokens | (token\_hash) UNIQUE, (expires\_at) WHERE used=false — partial index |
| sync\_queue | (sync\_status), (cashier\_id, created\_at), (offline\_timestamp) |
| recurring\_transfers | (next\_run\_at, status) WHERE status='ACTIVE' — partial index |
| disputes | (status), (sla\_deadline), (assigned\_branch\_id) |
| fee\_zone\_schedules | (fee\_zone\_id, is\_active) |
| in\_app\_notifications | (user\_id, delivery\_status), (created\_at DESC) |
| batch\_job\_rows | (batch\_job\_id, status) |
| credit\_profiles | (user\_id) UNIQUE, (next\_review\_at) |
| escrow\_contracts | (initiator\_user\_id), (beneficiary\_user\_id), (status) |
| forward\_contracts | (user\_id), (execution\_date) WHERE status='ACTIVE' |
| split\_requests | (initiator\_user\_id), (payee\_user\_id), (expires\_at) WHERE status='PENDING' |
| referral\_codes | (code) UNIQUE, (user\_id) UNIQUE |
| savings\_goals | (user\_id), (status) WHERE status='ACTIVE' |
| merchant\_transactions | (merchant\_id), (payer\_user\_id), (created\_at DESC) |
| bill\_payment\_requests | (user\_id), (status), (created\_at DESC) |

| TASK 04  ·  SecurityConfig · @PreAuthorize · JWT · Tenant Filter · Rate Limiting Security & RBAC Full Completion |
| :---- |

| ⚡  CURSOR TASK 04  —  SECURITY & RBAC COMPLETION *"Perform a complete security audit and fix all gaps. Step 1: Open SecurityConfig and verify the filterChain correctly orders filters as: CorsFilter → TenantResolutionFilter → JwtAuthenticationFilter → MDCCorrelationFilter. Step 2: Verify every controller endpoint has @PreAuthorize with the correct roles — add any missing annotations. Step 3: Verify TenantResolutionFilter sets TenantContextHolder BEFORE JwtAuthenticationFilter runs (it needs tenant context for user lookup). Step 4: Implement rate limiting on high-risk endpoints using Bucket4j (add Maven dependency io.github.bucket4j:bucket4j-core:8.9.0) — apply to: POST /api/auth/login (10 req/min per IP), POST /api/qr/scan (10 req/min per device), POST /api/biometric/auth/verify (5 req/min per user). Step 5: Verify the @PlatformRevenue AOP aspect is registered in a @Configuration class with @EnableAspectJAutoProxy. Step 6: Verify Hibernate @Filter is applied to all multi-tenant entities and the filter is enabled for every request via AOP or a request interceptor. Step 7: Add HSTS, X-Frame-Options, X-Content-Type-Options headers in SecurityConfig. Step 8: Verify /api/webhooks/card validates HMAC signature before processing."* |
| :---- |

##   **Task 04 — Complete RBAC Matrix**

| Endpoint Group | @PreAuthorize Expression Required |
| :---- | :---- |
| /api/auth/\*\* | permitAll() |
| /api/owner/\*\* | hasRole('PLATFORM\_OWNER') |
| /api/platform/tenants/\*\* | hasRole('PLATFORM\_OWNER') |
| /api/aml/\*\* | hasAnyRole('PLATFORM\_OWNER','AUDITOR') |
| /api/aml/rules PUT | hasRole('PLATFORM\_OWNER') |
| /api/lending/admin/\*\* | hasAnyRole('PLATFORM\_OWNER','MOTHER\_BRANCH\_ADMIN') |
| /api/batch/{id}/approve | hasAnyRole('PLATFORM\_OWNER','MOTHER\_BRANCH\_ADMIN') |
| /api/wallet/review/\*\* | hasAnyRole('MOTHER\_BRANCH\_ADMIN','BRANCH\_MANAGER') |
| /api/merchants/admin/\*\* | hasAnyRole('MOTHER\_BRANCH\_ADMIN','BRANCH\_MANAGER') |
| /api/analytics/platform | hasRole('PLATFORM\_OWNER') |
| /api/analytics/my-branch/\*\* | hasAnyRole('BRANCH\_MANAGER','MOTHER\_BRANCH\_ADMIN') |
| /api/qr/scan | hasAnyRole('CASHIER','BRANCH\_MANAGER','MOTHER\_BRANCH\_ADMIN','PLATFORM\_OWNER') |
| /api/escrow/{id}/witness | hasAnyRole('CASHIER','BRANCH\_MANAGER') |
| /api/fx/forward (POST) | hasAnyRole('CORPORATE\_ADMIN') or (hasRole('INDIVIDUAL\_USER') and @creditService.isTierA(\#userId)) |
| /api/audit/\*\* | hasAnyRole('PLATFORM\_OWNER','MOTHER\_BRANCH\_ADMIN','AUDITOR') |
| /api/cards/order-physical | hasRole('INDIVIDUAL\_USER') and @kycService.isStandardOrAbove(\#userId) |
| /api/webhooks/\*\* | hasRole('SYSTEM') — validated by HMAC, not JWT |
| /api/branches/nearby | isAuthenticated() |
| /actuator/health | permitAll() |

| TASK 05  ·  Every service method · Every workflow · Every edge case handled Business Logic Deep Completion |
| :---- |

| ⚡  CURSOR TASK 05  —  BUSINESS LOGIC DEEP COMPLETION — ALL SERVICES *"Go through every service class in the project and for each method that contains a TODO comment, placeholder return, or incomplete logic: implement it fully according to the specification documents. Focus especially on the following known complex areas that are commonly left incomplete. After completing each service, read through its methods top to bottom and verify: no method returns null when it should throw an exception, no method silently swallows an exception, every @Transactional method has the correct isolation level, and every mutation method calls auditService.log() with a meaningful action string."* |
| :---- |

##   **Task 05 — High-Risk Incomplete Areas**

### **◆  Area 1 — FeeZoneEvaluationService Integration**

Verify that **FeeZoneEvaluationService.getEffectiveDiscount()** is actually called inside: **TransactionService.calculateComprehensiveFees()**, **WalletService.exchangeCurrency()**, and **OrderExecutionService.calculateTradingFee()**. This is the most commonly omitted wiring. If missing, add the call after the base fee is calculated and before it is deducted from the wallet.

### **◆  Area 2 — PlatformRevenueAspect Annotation Coverage**

// Verify @PlatformRevenue is applied on ALL of these — add if missing:  
   
@PlatformRevenue(event \= RevenueEvent.TRANSFER, rateKey \= "PLATFORM\_BASE\_FEE")  
public TransferResponse processComprehensiveTransfer(...)  // TransactionService  
   
@PlatformRevenue(event \= RevenueEvent.WALLET\_EXCHANGE, rateKey \= "WALLET\_EXCHANGE\_FEE")  
public ExchangeResponse exchangeCurrency(...)  // WalletService  
   
@PlatformRevenue(event \= RevenueEvent.TRADE, rateKey \= "TRADING\_FEE\_STOCK")  
public OrderResponse placeOrder(...)  // OrderExecutionService  
   
@PlatformRevenue(event \= RevenueEvent.LOAN\_INTEREST, rateKey \= "LOAN\_INTEREST")  
public void processInstalment(...)  // LoanSchedulerService  
   
@PlatformRevenue(event \= RevenueEvent.MERCHANT\_FEE, rateKey \= "MERCHANT\_FEE")  
public MerchantPaymentResponse processQrPayment(...)  // MerchantPaymentService  
   
@PlatformRevenue(event \= RevenueEvent.ESCROW\_FEE, rateKey \= "ESCROW\_DAILY\_FEE")  
public void accrueHoldingFees(...)  // EscrowFeeAccrualService

### **◆  Area 3 — Wallet Credit/Debit Hook Chain**

// WalletService.credit(walletId, amount, currency) must call in order:  
// 1\. SELECT wallet FOR UPDATE NOWAIT (pessimistic lock)  
// 2\. Update available\_balance \+= amount  
// 3\. Create WalletTransaction record  
// 4\. Create LedgerEntry pair (debit source, credit wallet)  
// 5\. eventPublisher.publishEvent(new WalletCreditedEvent(walletId, amount))  
//    → SavingsGoalService listens: auto-sweep if goals active  
//    → FamilyWalletService listens: enforce incoming limit if member  
   
// WalletService.debit(walletId, amount, currency) must call in order:  
// 1\. SELECT wallet FOR UPDATE NOWAIT  
// 2\. Check available\_balance \>= amount → throw InsufficientFundsException  
// 3\. Check family spending limit if user is family member  
//    → throw FamilySpendingLimitExceededException  
// 4\. Update available\_balance \-= amount  
// 5\. Create WalletTransaction record  
// 6\. Create LedgerEntry pair

### **◆  Area 4 — CreditScoringService Post-Transaction Hook**

Verify **CreditScoringService** listens to **TransactionCompletedEvent** and updates the user's credit score asynchronously after every completed transaction. The score update should be non-blocking — use **@Async** with a dedicated **scoringExecutor** thread pool (coreSize=2). If the scoring fails, log the error but never let it propagate to the transaction thread.

### **◆  Area 5 — IslamicCalendarService Accuracy**

// Verify IslamicCalendarService uses the UmmAlQura library correctly:  
// \<dependency\>  
//   \<groupId\>com.github.msarhan\</groupId\>  
//   \<artifactId\>ummalqura-calendar\</artifactId\>  
//   \<version\>2.0.1\</version\>  
// \</dependency\>  
   
// isEventActive(IslamicEvent.RAMADAN, LocalDate.of(2025, 3, 1)) → true  
// isEventActive(IslamicEvent.EID\_AL\_FITR, LocalDate.of(2025, 3, 31)) → true (approx)  
// isEventActive(IslamicEvent.RAMADAN, LocalDate.of(2025, 4, 10)) → false  
   
// Cache result: Redis key \= 'islamic:{event}:{date}', TTL \= 86400 seconds  
// If Redis unavailable: compute directly without caching

| TASK 06  ·  GlobalExceptionHandler · Custom Exceptions · Error Response Schema Error Handling & Exception Hierarchy |
| :---- |

| ⚡  CURSOR TASK 06  —  ERROR HANDLING COMPLETE IMPLEMENTATION *"Audit GlobalExceptionHandler and the exception package. Ensure the following: (1) Every custom exception class in the system extends a base AppException that holds an errorCode (String), httpStatus (HttpStatus), and optional details map; (2) GlobalExceptionHandler has a @ExceptionHandler method for every custom exception type, plus handlers for MethodArgumentNotValidException, ConstraintViolationException, DataIntegrityViolationException, PessimisticLockingFailureException, HttpMessageNotReadableException, and a catch-all for Exception; (3) Every handler returns a consistent ApiErrorResponse DTO with fields: timestamp, status, error, message, errorCode, path, requestId (from MDC); (4) The error response NEVER includes stackTrace, cause chain, or internal class names in production profile; (5) For MethodArgumentNotValidException, the response includes a fieldErrors array: \[{field, rejectedValue, message}\]; (6) PessimisticLockingFailureException returns HTTP 503 with errorCode RESOURCE\_LOCKED and Retry-After: 1 header. Create any missing exception classes now. The complete list of required errorCode values is defined below."* |
| :---- |

##   **Task 06 — Required Exception Classes & Error Codes**

| Exception Class | errorCode  ·  HTTP Status |
| :---- | :---- |
| InsufficientFundsException | INSUFFICIENT\_FUNDS  ·  400 |
| InsufficientWalletBalanceException | INSUFFICIENT\_WALLET\_BALANCE  ·  400 |
| UserNotFoundException | USER\_NOT\_FOUND  ·  404 |
| TransactionNotFoundException | TRANSACTION\_NOT\_FOUND  ·  404 |
| WalletNotFoundException | WALLET\_NOT\_FOUND  ·  404 |
| LoanNotFoundException | LOAN\_NOT\_FOUND  ·  404 |
| QrTokenNotFoundException | QR\_NOT\_FOUND  ·  404 |
| QrAlreadyUsedException | QR\_ALREADY\_USED  ·  409 |
| QrExpiredException | QR\_EXPIRED  ·  400 |
| QrSignatureInvalidException | QR\_SIGNATURE\_INVALID  ·  401 |
| IneligibleCreditTierException | INELIGIBLE\_CREDIT\_TIER  ·  422 |
| ApplicationAlreadyExistsException | APPLICATION\_ALREADY\_EXISTS  ·  409 |
| WalletFrozenException | WALLET\_FROZEN  ·  403 |
| ConditionNotMetException | CONDITION\_NOT\_MET  ·  400 |
| ContractAlreadyExecutedException | CONTRACT\_ALREADY\_EXECUTED  ·  409 |
| FamilySpendingLimitExceededException | FAMILY\_SPENDING\_LIMIT\_EXCEEDED  ·  400 |
| UnsupportedCurrencyException | UNSUPPORTED\_CURRENCY  ·  400 |
| InvalidCsvFormatException | INVALID\_CSV\_FORMAT  ·  400 |
| FileTooLargeException | FILE\_TOO\_LARGE  ·  413 |
| StorageUnavailableException | STORAGE\_UNAVAILABLE  ·  503 |
| MarketDataUnavailableException | MARKET\_DATA\_UNAVAILABLE  ·  503 |
| TenantNotFoundException | TENANT\_NOT\_FOUND  ·  404 |
| DuplicatePlatformOwnerException | DUPLICATE\_PLATFORM\_OWNER  ·  409 |
| BiometricRequiredException | BIOMETRIC\_REQUIRED  ·  403 |
| ResourceLockedException | RESOURCE\_LOCKED  ·  503 |

| TASK 07  ·  All @Scheduled · Thread pools · Retry logic · Silent failure prevention Scheduler & Async Jobs Completion |
| :---- |

| ⚡  CURSOR TASK 07  —  SCHEDULER & ASYNC JOBS FULL COMPLETION *"Audit every @Scheduled and @Async method in the project. For each scheduler: (1) verify the cron expression or fixed rate is correct; (2) verify the method body is wrapped in try-catch(Exception e) with log.error() so a single bad record never kills the entire run; (3) verify it writes a scheduler\_run audit log entry at start and completion with record counts; (4) verify it has a distributed lock using ShedLock (add Maven dependency: net.javacrumbs.shedlock:shedlock-spring:5.13.0 and shedlock-provider-jdbc-template) so that if multiple instances are running, only one executes the scheduler at a time. For each @Async method: verify it uses a named executor bean, not the default SimpleAsyncTaskExecutor. Create the following executor beans if they do not exist: batchExecutor (pool 5-10), scoringExecutor (pool 2), notificationExecutor (pool 3), tradingExecutor (pool 2)."* |
| :---- |

##   **Task 07 — Complete Scheduler Registry**

| Scheduler Class · Method | Cron / Rate  ·  ShedLock Key  ·  Max Duration |
| :---- | :---- |
| LoanSchedulerService.processDueInstalments | 0 0 8 \* \* \*  ·  loan-scheduler  ·  30min |
| LoanSchedulerService.flagDefaults | 0 30 8 \* \* \*  ·  loan-defaults  ·  15min |
| GamificationScheduler.recalculateScores | 0 0 2 \* \* SUN  ·  gamification-scores  ·  60min |
| QrCleanupScheduler.deleteExpiredTokens | 0 \*/30 \* \* \* \*  ·  qr-cleanup  ·  5min |
| PriceFeedScheduler.fetchAndBroadcast | \*/2 \* \* \* \* \*  ·  (no lock, per-instance OK)  ·  1s |
| EscrowFeeAccrualService.accrueAll | 0 0 1 \* \* \*  ·  escrow-fees  ·  20min |
| ForwardContractScheduler.executeMature | 0 0 9 \* \* \*  ·  forward-contracts  ·  15min |
| BirthdayNotificationService.sendBirthdays | 0 5 0 \* \* \*  ·  birthday-notifications  ·  30min |
| HolidayGreetingService.checkAndSend | 0 0 6 \* \* \*  ·  holiday-greetings  ·  10min |
| MonthlyInsightService.generateInsights | 0 0 7 1 \* \*  ·  monthly-insights  ·  60min |
| LiquidityMonitorScheduler.checkAll | 0 0 \*/4 \* \* \*  ·  liquidity-monitor  ·  10min |
| DisputeSlaScheduler.checkDeadlines | 0 0 \* \* \* \*  ·  dispute-sla  ·  5min |
| RecurringTransferService.processDue | 0 \*/15 \* \* \* \*  ·  recurring-transfers  ·  10min |
| MerchantSettlementService.settleAll | 0 0 23 \* \* \*  ·  merchant-settlement  ·  20min |
| TenantBillingService.generateInvoices | 0 0 8 1 \* \*  ·  tenant-billing  ·  30min |
| SyncQueueScheduler.processQueue | \*/30 \* \* \* \* \*  ·  sync-queue  ·  5min |
| VaultSubscriptionService.billDue | 0 0 9 \* \* \*  ·  vault-billing  ·  10min |
| AnalyticsRefreshScheduler.refreshViews | 0 \*/15 \* \* \* \*  ·  analytics-refresh  ·  2min |
| CreditScoringScheduler.weeklyReview | 0 0 3 \* \* MON  ·  credit-scoring  ·  45min |

| TASK 08  ·  Yahoo Finance · WhatsApp · Spring AI · MinIO · Redis · WebAuthn External Integration Completion |
| :---- |

| ⚡  CURSOR TASK 08  —  EXTERNAL INTEGRATION FULL COMPLETION *"For every external integration, verify it is implemented with proper resilience patterns. The following must all be true: (1) Yahoo Finance calls use Spring WebClient with a 5-second timeout, circuit breaker (add Resilience4j: io.github.resilience4j:resilience4j-spring-boot3:2.2.0), and automatic fallback to Redis cache on failure; (2) WhatsApp API calls are fire-and-forget — they run @Async in notificationExecutor, have 2 retries with 30-second delay using Spring Retry (@Retryable), and NEVER block or fail the parent transaction; (3) Spring AI calls have a 30-second timeout and a fallback message if the AI API is unavailable; (4) MinIO operations use the MinIO Java SDK (add dependency io.minio:minio:8.5.9) with server-side AES-256 encryption on every PUT; (5) Redis operations are always wrapped in try-catch — if Redis is down, fall through to the database silently; (6) WebAuthn uses com.yubico:webauthn-server-core:2.5.0 with rpId set from application.properties; (7) ShedLock uses JDBC provider pointing to the same PostgreSQL as the application. Add all missing Maven dependencies now."* |
| :---- |

##   **Task 08 — Required Maven Dependencies Audit**

\<\!-- Verify ALL of these exist in pom.xml — add any that are missing \--\>  
   
\<\!-- ZXing QR Generation \--\>  
\<dependency\>\<groupId\>com.google.zxing\</groupId\>\<artifactId\>core\</artifactId\>\<version\>3.5.3\</version\>\</dependency\>  
\<dependency\>\<groupId\>com.google.zxing\</groupId\>\<artifactId\>javase\</artifactId\>\<version\>3.5.3\</version\>\</dependency\>  
   
\<\!-- TOTP \--\>  
\<dependency\>\<groupId\>dev.samstevens.totp\</groupId\>\<artifactId\>totp-spring-boot-starter\</artifactId\>\<version\>1.7.1\</version\>\</dependency\>  
   
\<\!-- Spring AI \+ PGVector \--\>  
\<dependency\>\<groupId\>org.springframework.ai\</groupId\>\<artifactId\>spring-ai-openai-spring-boot-starter\</artifactId\>\<version\>1.0.0-M6\</version\>\</dependency\>  
\<dependency\>\<groupId\>org.springframework.ai\</groupId\>\<artifactId\>spring-ai-pgvector-store-spring-boot-starter\</artifactId\>\<version\>1.0.0-M6\</version\>\</dependency\>  
   
\<\!-- Redis \--\>  
\<dependency\>\<groupId\>org.springframework.boot\</groupId\>\<artifactId\>spring-boot-starter-data-redis\</artifactId\>\</dependency\>  
   
\<\!-- WebSocket / STOMP \--\>  
\<dependency\>\<groupId\>org.springframework.boot\</groupId\>\<artifactId\>spring-boot-starter-websocket\</artifactId\>\</dependency\>  
   
\<\!-- MinIO \--\>  
\<dependency\>\<groupId\>io.minio\</groupId\>\<artifactId\>minio\</artifactId\>\<version\>8.5.9\</version\>\</dependency\>  
   
\<\!-- WebAuthn (Biometric) \--\>  
\<dependency\>\<groupId\>com.yubico\</groupId\>\<artifactId\>webauthn-server-core\</artifactId\>\<version\>2.5.0\</version\>\</dependency\>  
   
\<\!-- Islamic Calendar \--\>  
\<dependency\>\<groupId\>com.github.msarhan\</groupId\>\<artifactId\>ummalqura-calendar\</artifactId\>\<version\>2.0.1\</version\>\</dependency\>  
   
\<\!-- Resilience4j Circuit Breaker \--\>  
\<dependency\>\<groupId\>io.github.resilience4j\</groupId\>\<artifactId\>resilience4j-spring-boot3\</artifactId\>\<version\>2.2.0\</version\>\</dependency\>  
   
\<\!-- Spring Retry \--\>  
\<dependency\>\<groupId\>org.springframework.retry\</groupId\>\<artifactId\>spring-retry\</artifactId\>\</dependency\>  
   
\<\!-- ShedLock (distributed scheduler lock) \--\>  
\<dependency\>\<groupId\>net.javacrumbs.shedlock\</groupId\>\<artifactId\>shedlock-spring\</artifactId\>\<version\>5.13.0\</version\>\</dependency\>  
\<dependency\>\<groupId\>net.javacrumbs.shedlock\</groupId\>\<artifactId\>shedlock-provider-jdbc-template\</artifactId\>\<version\>5.13.0\</version\>\</dependency\>  
   
\<\!-- Bucket4j Rate Limiting \--\>  
\<dependency\>\<groupId\>com.bucket4j\</groupId\>\<artifactId\>bucket4j-core\</artifactId\>\<version\>8.9.0\</version\>\</dependency\>  
   
\<\!-- CSV Parsing \--\>  
\<dependency\>\<groupId\>org.apache.commons\</groupId\>\<artifactId\>commons-csv\</artifactId\>\<version\>1.11.0\</version\>\</dependency\>  
   
\<\!-- SpringDoc OpenAPI \--\>  
\<dependency\>\<groupId\>org.springdoc\</groupId\>\<artifactId\>springdoc-openapi-starter-webmvc-ui\</artifactId\>\<version\>2.5.0\</version\>\</dependency\>  
   
\<\!-- TestContainers (test scope) \--\>  
\<dependency\>\<groupId\>org.testcontainers\</groupId\>\<artifactId\>postgresql\</artifactId\>\<scope\>test\</scope\>\</dependency\>  
\<dependency\>\<groupId\>org.testcontainers\</groupId\>\<artifactId\>junit-jupiter\</artifactId\>\<scope\>test\</scope\>\</dependency\>  
   
\<\!-- WireMock (test scope) \--\>  
\<dependency\>\<groupId\>com.github.tomakehurst\</groupId\>\<artifactId\>wiremock-jre8\</artifactId\>\<version\>2.35.2\</version\>\<scope\>test\</scope\>\</dependency\>

| TASK 09  ·  Every service · Every method · Every edge case · JUnit 5 \+ Mockito Complete Unit Test Suite |
| :---- |

| ⚡  CURSOR TASK 09  —  WRITE COMPLETE UNIT TEST SUITE *"Write unit tests for every service class in the project using JUnit 5 and Mockito. Use @ExtendWith(MockitoExtension.class) — NOT @SpringBootTest — for unit tests. Every service class must have a corresponding \*ServiceTest class in src/test/java. The test class must cover: the happy path, all error paths (every exception the method can throw), boundary conditions (zero amount, max amount, exactly at limit), and concurrent access where applicable. Specifically, the following methods are highest priority and must have tests completed first: CreditScoringService.calculateScore() with all 6 components tested individually, FeeZoneEvaluationService.getEffectiveDiscount() with all 4 schedule types, AmlRuleEngine.evaluate() with all 5 rules triggering correctly, LoanSchedulerService.processDueInstalments() with wallet-sufficient and wallet-insufficient cases, BatchExecutionService.executeBatch() with mixed success/failure rows, PlatformRevenueAspect advice execution verified via AspectJ test utilities. After writing, run mvn test and fix all failures before proceeding to Task 10."* |
| :---- |

##   **Task 09 — Unit Test Coverage Targets**

| Service | Minimum Test Methods Required |
| :---- | :---- |
| CreditScoringService | calculateScore\_allSixComponentsCorrect, calculateScore\_newUser\_neutralRepaymentScore, calculateScore\_missedPayments\_reducesScore, calculateScore\_ineligibleTier\_belowThreshold, calculateScore\_tierA\_highVolume |
| FeeZoneEvaluationService | getDiscount\_dateRangeActive, getDiscount\_islamicCalendarRamadan, getDiscount\_feeFreeRule\_returns100Pct, getDiscount\_noBranchInZone\_returnsZero, getDiscount\_multipleZones\_highestPriorityWins |
| AmlRuleEngine | structuringRule\_nearThreshold\_triggersAlert, velocityRule\_5xAverage\_triggersAlert, largeAmountRule\_aboveThreshold, roundAmountPattern\_lastFiveTxs, newAccountHighValue\_under30days |
| WalletService | credit\_updatesBalance\_createsLedgerEntry\_firesEvent, debit\_insufficientBalance\_throwsException, debit\_familyLimitExceeded\_throwsException, exchange\_correctFeeCalculation\_platformRevenueCredited, freeze\_frozenWallet\_rejectsFutureDebits |
| OrderExecutionService | placeOrder\_marketBuy\_fills\_deductsWalletAndFee, placeOrder\_insufficientBalance\_throwsException, placeOrder\_platformFeeCollected\_creditedToOwner, placeOrder\_concurrentRequests\_onlyOneSucceeds |
| LoanSchedulerService | processDue\_walletSufficient\_marksPayd\_creditsRevenue, processDue\_walletInsufficient\_marksMissed\_appliesLateFee, flagDefaults\_missedOver30Days\_freezesWallet\_escalates, detectPayoff\_allPaid\_marksLoanPaidOff\_awardsBadge |
| SplitPaymentService | createSplit\_correct4WaySplit, respondAccept\_allPaid\_atomicCollection, respondDecline\_cancelsParticipant, splitExpiry\_unpaidAfterDeadline\_cancels |
| EscrowService | create\_debitsFunderWallet\_createsContract, release\_manualBoth\_bothApproved\_creditsbeneficiary, release\_conditionNotMet\_throwsException, dispute\_freezesBothWallets\_createsAmlAlert |
| BatchExecutionService | executeBatch\_allRowsSuccess\_updatesJob, executeBatch\_mixedResults\_failedRowDoesNotRollbackSuccess, executeBatch\_sseProgressUpdated\_afterEachRow |
| ForwardContractService | createContract\_correctLockedRate\_30DaySpread, executeAtMaturity\_platformProfitCredited, cancel\_beforeExecution\_feeApplied, cancel\_afterExecution\_throwsException |

| TASK 10  ·  TestContainers · PostgreSQL · Redis · WireMock · Real transactions Complete Integration Test Suite |
| :---- |

| ⚡  CURSOR TASK 10  —  WRITE COMPLETE INTEGRATION TEST SUITE *"Write integration tests using @SpringBootTest with TestContainers (real PostgreSQL \+ real Redis). Create a base class AbstractIntegrationTest annotated with @SpringBootTest, @Testcontainers, @ActiveProfiles('test') that starts PostgreSQL and Redis containers, runs all Flyway migrations, and provides helper methods for creating test users, wallets, branches, and funds. Use MockMvc for HTTP testing and WireMock for mocking all external APIs (Yahoo Finance, WhatsApp, OpenAI, MinIO). Every test must be @Transactional where appropriate and must clean up test data via @Sql or @DirtiesContext. The following integration test classes are mandatory. After writing, run mvn verify and fix all failures."* |
| :---- |

##   **Task 10 — Mandatory Integration Test Classes**

| Test Class | Scenarios to Cover |
| :---- | :---- |
| TransactionIntegrationTest | simpleTransfer\_endToEnd, comprehensiveTransfer\_allFeesApplied, comprehensiveTransfer\_feeZoneDiscount, transfer\_insufficientFunds\_balanceUnchanged, concurrentTransfers\_neverNegativeBalance |
| QRWithdrawalIntegrationTest | generateAndScan\_happyPath, scan\_alreadyUsed\_409, scan\_expired\_400, scan\_tamperedSignature\_401, scan\_wrongBranch\_403 |
| WalletKycIntegrationTest | applyForWallet\_submitDocuments\_aiScreened, approve\_walletCreated\_balanceZero, topup\_cashierConfirms\_balanceUpdated, exchange\_feeCollected\_platformRevenueCredited |
| TradingIntegrationTest | openAccount\_placeMarketOrder\_fills\_positionCreated, priceStreamWebSocket\_subscribeAndReceiveUpdate, portfolioPnl\_afterPriceChange\_updatesCorrectly |
| LendingIntegrationTest | applyLoan\_approved\_disbursed\_scheduleGenerated, autoDebit\_sufficient\_instalmentPaid, autoDebit\_insufficient\_missed\_lateFeeApplied, allPaid\_paidOff\_badgeAwarded |
| MerchantPaymentIntegrationTest | registerMerchant\_approve\_payByQr\_receiptSent, settlement\_netAmountTransferredToMerchantWallet |
| BatchTransferIntegrationTest | upload1000RowCsv\_validates\_approved\_executed\_reportGenerated, mixedRows\_failedRowsReported\_successRowsTransacted |
| EscrowIntegrationTest | createEscrow\_fundedAndHeld\_bothApprove\_released, disputeEscrow\_walletsFrozen\_amlAlertCreated, holdingFeeAccrual\_dailyDeductedFromEscrow |
| AmlIntegrationTest | structuringPattern\_triggers\_alertCreated\_auditorNotified, largeAmount\_highSeverityAlert, clearAlert\_statusUpdated\_auditLogged |
| RecurringTransferIntegrationTest | scheduleMonthly\_executeOnDueDate\_nextRunUpdated, insufficientOnDue\_retryAfter4h\_skipIfStillInsufficient |
| FeeZoneIntegrationTest | ramadanZone\_activeOnCorrectDate\_feeReduced, feeFreeZone\_transferFeeZero\_platformRevenueZero |
| RbacIntegrationTest | everyRoleEveryEndpoint\_correctHttpStatus, tamperedJwt\_401, expiredJwt\_401, auditorWriteAttempt\_403 |

##   **Task 10 — AbstractIntegrationTest Base Class**

@SpringBootTest(webEnvironment \= SpringBootTest.WebEnvironment.RANDOM\_PORT)  
@Testcontainers  
@ActiveProfiles("test")  
@Transactional  
public abstract class AbstractIntegrationTest {  
   
    @Container  
    static PostgreSQLContainer\<?\> postgres \= new PostgreSQLContainer\<\>("postgres:16")  
        .withDatabaseName("almukhtar\_test")  
        .withUsername("test").withPassword("test");  
   
    @Container  
    static GenericContainer\<?\> redis \= new GenericContainer\<\>("redis:7")  
        .withExposedPorts(6379);  
   
    @DynamicPropertySource  
    static void properties(DynamicPropertyRegistry r) {  
        r.add("spring.datasource.url", postgres::getJdbcUrl);  
        r.add("spring.datasource.username", postgres::getUsername);  
        r.add("spring.datasource.password", postgres::getPassword);  
        r.add("spring.data.redis.host", redis::getHost);  
        r.add("spring.data.redis.port", () \-\> redis.getMappedPort(6379));  
    }  
   
    @RegisterExtension  
    static WireMockExtension wireMock \= WireMockExtension.newInstance()  
        .options(wireMockConfig().port(9090)).build();  
   
    @Autowired MockMvc mockMvc;  
    @Autowired ObjectMapper objectMapper;  
   
    // Helper: login and return Bearer token  
    protected String loginAs(String username, String password) { ... }  
   
    // Helper: create test user with role  
    protected User createTestUser(UserRole role) { ... }  
   
    // Helper: create wallet with balance  
    protected Wallet createWalletWithBalance(Long userId, BigDecimal amount, String currency) { ... }  
}

| TASK 11  ·  EXPLAIN ANALYZE · N+1 elimination · Slow query log · Benchmark Performance Audit & Query Optimization |
| :---- |

| ⚡  CURSOR TASK 11  —  PERFORMANCE AUDIT & OPTIMIZATION *"Run a comprehensive performance audit on the backend. Step 1: Enable Hibernate statistics in test profile (spring.jpa.properties.hibernate.generate\_statistics=true) and write a test that makes each of the following list endpoints and asserts the query count is \<= 3: GET /api/transactions, GET /api/lending/admin/portfolio, GET /api/aml/alerts, GET /api/batch/{id}/report, GET /api/orders. Fix any N+1 issue found by adding @EntityGraph or JOIN FETCH. Step 2: Enable PostgreSQL slow query log (log\_min\_duration\_statement \= 100ms) and run 1000 transactions through the system, then review pg\_stat\_statements for the top 10 slowest queries. Add any missing indexes. Step 3: Write a load test using JMeter or a Spring test with 50 concurrent threads attempting transfers simultaneously — verify no deadlocks occur and all complete or fail gracefully. Step 4: Verify that materialized views are being refreshed CONCURRENTLY (not locking reads). Step 5: Verify Redis is used as first-tier cache for: exchange rates (TTL 60s), credit scores (TTL 1h), branch geo queries (TTL 300s), translation bundles (TTL 3600s), Islamic calendar events (TTL 86400s)."* |
| :---- |

##   **Task 11 — N+1 Query Fixes Required**

| Endpoint / Query | Fix Required |
| :---- | :---- |
| GET /api/transactions (list) | Add @EntityGraph({sender, receiver, fund}) to TransactionRepository.findAll() |
| GET /api/lending/admin/portfolio | Use JOIN FETCH loan.user, loan.application in JPQL query |
| GET /api/aml/alerts | Add @EntityGraph({user, rule}) to AmlAlertRepository |
| GET /api/batch/{id}/report | Load BatchJobRows in pages — never load all rows into memory at once |
| GET /api/orders (trading) | Add @EntityGraph({tradingAccount.user}) to OrderRepository |
| GET /api/escrow/my-contracts | Use JOIN FETCH escrow.initiator, escrow.beneficiary |
| GET /api/family/groups/{id}/statement | Aggregate in SQL, do not loop wallet queries per member |
| GamificationScheduler (recalculate all) | Process in pages of 500 — never load all users into memory |
| BirthdayNotificationService | Query only users with birthday today using EXTRACT(MONTH/DAY FROM date\_of\_birth) index |

| TASK 12  ·  All tests pass · OpenAPI exported · Zero TODOs · Ready for frontend Final Verification & Backend Readiness Report |
| :---- |

| ⚡  CURSOR TASK 12  —  FINAL BACKEND VERIFICATION — GENERATE READINESS REPORT *"Run the complete final verification sequence and generate a readiness report. Step 1: Run 'mvn clean verify' — zero test failures, zero compiler warnings allowed. Step 2: Run 'mvn checkstyle:check' — zero style violations. Step 3: Search the entire codebase for TODO and FIXME — every occurrence must be resolved or converted to a filed GitHub issue. Step 4: Run 'mvn dependency:analyze' — no unused declared dependencies, no used undeclared dependencies. Step 5: Hit GET /v3/api-docs and verify all 100+ endpoints appear in the OpenAPI spec. Save the spec as openapi.json in the project root — the frontend team will use this for TypeScript type generation. Step 6: Start the application with docker-compose up \--build and confirm all containers reach healthy state. Step 7: Run GET /actuator/health and confirm all sub-components are UP (db, redis, diskSpace). Step 8: Produce a readiness\_report.md in the project root with the following sections: Module Completion Status (all 40 modules listed with COMPLETE/INCOMPLETE), Test Coverage Summary (unit test count, integration test count, pass rate), Known Limitations (anything not implemented), and sign-off line: 'Backend declared ready for frontend integration on {date}'."* |
| :---- |

##   **Task 12 — Final Checklist Before Sign-Off**

| ✓ | Final Verification Item |
| :---: | :---- |
| **□** | 'mvn clean verify' exits with BUILD SUCCESS and zero test failures |
| **□** | Zero TODO / FIXME comments remaining in src/main/java |
| **□** | Zero compiler warnings in build output |
| **□** | All 40 modules confirmed COMPLETE in readiness\_report.md |
| **□** | openapi.json exported to project root with 100+ endpoints documented |
| **□** | GET /actuator/health returns {status: UP} with db, redis, diskSpace all UP |
| **□** | docker-compose up \--build starts all containers (app, postgres, redis, minio) to healthy within 60s |
| **□** | Unit test count \>= 200 tests across all service classes |
| **□** | Integration test count \>= 50 tests covering all critical workflows |
| **□** | No float or double used in any financial calculation — zero grep matches |
| **□** | All @Scheduled methods wrapped in try-catch — zero silent failures possible |
| **□** | All external API calls have circuit breaker or retry — WhatsApp, Yahoo Finance, OpenAI |
| **□** | ShedLock configured for all 19 schedulers — no duplicate execution in multi-instance deployment |
| **□** | Bucket4j rate limiting active on: /api/auth/login, /api/qr/scan, /api/biometric/auth/verify |
| **□** | AuditService.log() called in every service method that mutates state |
| **□** | TransactionCompletedEvent fired after every completed transaction |
| **□** | PlatformRevenueAspect wired to all 6 revenue-generating methods |
| **□** | Tenant Hibernate filter active on all multi-tenant entities |
| **□** | GlobalExceptionHandler covers all 22 custom exception types |
| **□** | readiness\_report.md committed to git on main branch with sign-off line |

| ✅  BACKEND IS DONE WHEN Task 12 checklist is fully checked. readiness\_report.md shows all 40 modules COMPLETE. 'mvn clean verify' exits 0 with zero failures. openapi.json is committed. The git commit message reads: 'feat: backend complete — all 40 modules implemented and tested — frontend integration authorized'. Only after this commit may Next.js development begin. |
| :---- |

| ALMUKHTAR ELITE Backend Control Prompt 40 Modules  ·  12 Tasks  ·  Audit → Complete → Test → Sign Off ──────────────────────────────────────────────────── © 2025 ALMUKHTAR Elite — Senior Backend Engineering Division — Confidential |
| :---: |

