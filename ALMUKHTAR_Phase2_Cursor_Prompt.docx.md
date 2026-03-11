

| 🏛 ALMUKHTAR ELITE PHASE II  —  MARKET DOMINATION EXPANSION Real-Time Trading  ·  Digital Wallets  ·  KYC  ·  Geo-Intelligence  ·  AI Messaging  ·  Platform Revenue ───────────────────────────────────────────────────────── COMPREHENSIVE CURSOR IMPLEMENTATION PROMPT 6 New Game-Changing Modules  ·  Spring Boot 3.3.4  ·  Java 21  ·  Next.js 14  ·  PostgreSQL  ·  WebSocket Version 2.0  ·  Senior Fintech Architecture Division |
| :---: |

| ⚠️  CRITICAL ARCHITECTURE CORRECTION — READ BEFORE ANYTHING ELSE The existing SUPER\_ADMIN role must be refactored. The true top of the hierarchy is a new role: PLATFORM\_OWNER (the software author). Below that is the MOTHER\_BRANCH\_ADMIN — who runs the main/mother branch like any other branch but has the power to onboard corporations and open new branches. SUPER\_ADMIN is deprecated and merged into PLATFORM\_OWNER. Every single transaction, trade, wallet operation, and fee event in the entire system generates a revenue share credited to the PLATFORM\_OWNER fund automatically. |
| :---- |

| NEW ROLE HIERARCHY  (Top → Bottom) ① PLATFORM\_OWNER  — Software author. One account. Receives platform revenue from every event globally. ② MOTHER\_BRANCH\_ADMIN  — Runs the mother/main branch. Can create corporations, branches, onboard clients. Same daily ops as any branch manager. ③ BRANCH\_MANAGER  — Unchanged. Manages one branch. ④ CASHIER  — Unchanged. Transaction processing. ⑤ CORPORATE\_ADMIN  — NEW. Manages a corporate account, sub-accounts, payroll. ⑥ INDIVIDUAL\_USER  — NEW. End-user with digital wallet, trading account. ⑦ AUDITOR  — Unchanged. Read-only audit. |
| :---- |

| MODULE 07   ·   Document Upload · Identity Verification · Multi-Currency Wallets · Branch Top-Up Digital Wallet System & KYC Onboarding |
| :---- |

##   **7.1  Business Requirements**

**Every individual and corporate user** can apply for a digital wallet entirely online. They upload their national ID, passport, and a selfie. The system runs automated pre-screening using AI document analysis and then queues the application for a human CASHIER or BRANCH\_MANAGER to perform final KYC approval. Once approved, the user has a verified wallet that can hold multiple currencies simultaneously and be topped up from the nearest branch.

##   **7.2  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| wallet\_applications | id, user\_id, status (DRAFT/SUBMITTED/UNDER\_REVIEW/APPROVED/REJECTED), submitted\_at, reviewed\_by, reviewed\_at, rejection\_reason |
| kyc\_documents | id, application\_id, doc\_type (NATIONAL\_ID/PASSPORT/SELFIE/PROOF\_OF\_ADDRESS), file\_reference (S3/MinIO path), upload\_at, ai\_confidence\_score, ai\_flags (JSON), verified\_by\_human |
| wallets | id, user\_id, wallet\_number (UUID), status (ACTIVE/FROZEN/CLOSED), kyc\_tier (BASIC/STANDARD/PREMIUM), daily\_limit, monthly\_limit, created\_at |
| wallet\_balances | id, wallet\_id, currency\_code, available\_balance, locked\_balance, last\_updated |
| wallet\_transactions | id, wallet\_id, type (TOPUP/WITHDRAWAL/EXCHANGE/TRANSFER/TRADE\_FEE), amount, currency, reference\_id, description, created\_at |
| topup\_requests | id, wallet\_id, branch\_id, amount, currency, status (PENDING/COMPLETED/CANCELLED), cashier\_id, created\_at, completed\_at |

##   **7.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/wallet/WalletApplicationService.java | KYC application lifecycle: submit, review, approve/reject |
| service/wallet/DocumentVerificationService.java | AI pre-screening: call Vision AI API to check ID authenticity, extract name/DOB, flag anomalies |
| service/wallet/WalletService.java | Create wallets, manage balances, enforce daily/monthly limits |
| service/wallet/WalletTopUpService.java | Branch top-up flow: request → cashier confirmation → credit wallet |
| controller/WalletApplicationController.java | POST apply, GET status, GET my-application, PATCH review (BRANCH\_MANAGER+) |
| controller/WalletController.java | GET my-wallet, GET balances, GET transaction-history, POST exchange |
| controller/WalletTopUpController.java | POST request-topup, PUT complete-topup (CASHIER), GET pending-topups |
| entity/Wallet.java | JPA entity |
| entity/WalletBalance.java | JPA entity per currency balance |
| entity/KycDocument.java | JPA entity with AI analysis results |
| entity/enums/WalletStatus.java | ACTIVE, FROZEN, CLOSED |
| entity/enums/KycTier.java | BASIC (ID only), STANDARD (+selfie), PREMIUM (+proof of address) |
| dto/wallet/WalletApplicationRequest.java | Personal info \+ document references |
| dto/wallet/KycReviewRequest.java | approve/reject \+ notes from reviewer |

##   **7.4  DocumentVerificationService — AI Pre-Screening Logic**

// DocumentVerificationService.analyzeDocument(KycDocument doc):  
//  
// Step 1 — Retrieve file from MinIO/S3 as byte\[\]  
//  
// Step 2 — Call Spring AI Vision model (GPT-4o or Claude claude-sonnet-4-6):  
//   SystemPrompt: You are a KYC document analyst. Analyze the provided identity  
//   document image. Return ONLY JSON with fields:  
//   { documentType, isAuthentic (true/false), confidenceScore (0.0-1.0),  
//     extractedName, extractedDob, extractedDocNumber, expiryDate,  
//     isExpired, flags: \[list of anomalies\], fraudRisk (LOW/MEDIUM/HIGH) }  
//  
// Step 3 — Parse JSON response  
//  
// Step 4 — Update KycDocument:  
//   aiConfidenceScore \= response.confidenceScore  
//   aiFlags \= JSON.stringify(response.flags)  
//   If fraudRisk \== HIGH: flag application for manual priority review  
//   If isExpired: auto-reject with reason 'Document expired'  
//  
// Step 5 — Audit log: action='KYC\_AI\_SCREENED', entityType='WALLET\_APPLICATION'  
//  
// IMPORTANT: AI result is advisory only. Final decision is always human.

##   **7.5  Multi-Currency Wallet Exchange**

// WalletService.exchangeCurrency(Long walletId, String fromCurrency,  
//                                String toCurrency, BigDecimal amount):  
//  
// 1\. Load WalletBalance for fromCurrency — check available\_balance \>= amount  
// 2\. Load exchange rate from existing CurrencyConversionService  
// 3\. Calculate converted amount using forex SELLING rate (platform buys from user)  
// 4\. Calculate PLATFORM\_EXCHANGE\_FEE \= amount \* platformExchangeFeeRate  
//    (configurable in commission\_rates, scope \= WALLET\_EXCHANGE)  
// 5\. Debit fromCurrency balance: amount \+ fee  
// 6\. Credit toCurrency balance: convertedAmount  
// 7\. Credit PLATFORM\_OWNER fee account: fee amount  
// 8\. Record WalletTransaction entries for debit, credit, and fee  
// 9\. Double-entry ledger entries (Module 3 integration)  
//10. Audit: action='WALLET\_EXCHANGE', details={from,to,amount,fee,rate}

##   **7.6  KYC REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| POST /api/wallet/apply | PUBLIC (authenticated) — Submit wallet application |
| POST /api/wallet/apply/upload-doc | INDIVIDUAL\_USER — Upload KYC document file |
| GET /api/wallet/my-application | INDIVIDUAL\_USER — Track own application status |
| GET /api/wallet/pending-reviews | BRANCH\_MANAGER, MOTHER\_BRANCH\_ADMIN — List applications awaiting review |
| PUT /api/wallet/review/{appId} | BRANCH\_MANAGER+ — Approve or reject with notes |
| GET /api/wallet/my-wallet | INDIVIDUAL\_USER — Wallet info \+ all currency balances |
| GET /api/wallet/my-wallet/transactions | INDIVIDUAL\_USER — Paginated wallet transaction history |
| POST /api/wallet/exchange | INDIVIDUAL\_USER — Exchange between currencies in wallet |
| POST /api/wallet/topup/request | INDIVIDUAL\_USER — Request branch top-up |
| PUT /api/wallet/topup/{id}/complete | CASHIER — Mark top-up as completed (cash received) |
| GET /api/wallet/topup/pending | CASHIER, BRANCH\_MANAGER — Pending top-ups at my branch |

| MODULE 08   ·   Nearest Branch Discovery · Approximate Location · No Tracking Privacy-First Geolocation Branch Finder |
| :---- |

##   **8.1  Business Requirements**

Users need to find the nearest branch to top up their wallet or complete a transfer. However, **exact user coordinates must never be stored**. The system accepts the user's location, computes the nearest branches in real time, returns results, and **immediately discards the coordinates**. No location data is persisted in any table.

##   **8.2  New Database Table — Branches Extended**

\-- Add to branches table:  
ALTER TABLE branches ADD COLUMN latitude       DECIMAL(10,8);  
ALTER TABLE branches ADD COLUMN longitude      DECIMAL(11,8);  
ALTER TABLE branches ADD COLUMN city           VARCHAR(100);  
ALTER TABLE branches ADD COLUMN country        VARCHAR(100);  
ALTER TABLE branches ADD COLUMN address\_line   VARCHAR(255);  
ALTER TABLE branches ADD COLUMN phone          VARCHAR(30);  
ALTER TABLE branches ADD COLUMN opens\_at       TIME;  
ALTER TABLE branches ADD COLUMN closes\_at      TIME;  
ALTER TABLE branches ADD COLUMN services       JSONB; \-- \[TOPUP, TRANSFER, QR\_RELEASE, EXCHANGE\]  
ALTER TABLE branches ADD COLUMN is\_open\_now    BOOLEAN GENERATED ALWAYS AS (... computed) STORED;  
   
\-- Index for fast geo queries  
CREATE INDEX idx\_branches\_location ON branches USING GIST(  
    ll\_to\_earth(latitude, longitude)  
);

##   **8.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/geo/BranchGeoService.java | Compute nearest branches using Haversine formula in PostgreSQL |
| service/geo/PrivacyLocationService.java | Validate \+ immediately discard coordinates — never logs them |
| controller/BranchFinderController.java | POST /api/branches/nearby — accepts lat/lng, returns sorted branches |
| dto/geo/NearbyBranchRequest.java | userLat, userLng, radiusKm (max 50), servicesRequired (optional filter) |
| dto/geo/NearbyBranchResponse.java | branchId, name, address, city, distanceKm, isOpenNow, services, phone |

##   **8.4  BranchGeoService — Privacy-Preserving Implementation**

// POST /api/branches/nearby  
// NEVER persist userLat/userLng — process and discard immediately  
   
// BranchGeoService.findNearest(NearbyBranchRequest req):  
//  
// Step 1 — Validate radius: clamp to max 50km (prevent large-area data harvesting)  
//  
// Step 2 — Execute PostgreSQL Haversine query:  
//   SELECT id, name, city, address\_line, phone,  
//          opens\_at, closes\_at, services,  
//          (earth\_distance(ll\_to\_earth(latitude, longitude),  
//                          ll\_to\_earth(:userLat, :userLng)) / 1000\) AS distance\_km  
//   FROM branches  
//   WHERE status \= 'ACTIVE'  
//     AND earth\_box(ll\_to\_earth(:userLat, :userLng), :radiusMeters)  
//         @\> ll\_to\_earth(latitude, longitude)  
//   ORDER BY distance\_km ASC  
//   LIMIT 10  
//  
// Step 3 — Map to NearbyBranchResponse (NO coordinates in response)  
//   Include distanceKm rounded to 1 decimal (e.g. '2.3 km away')  
//   Include isOpenNow computed from current server time vs opens\_at/closes\_at  
//  
// Step 4 — DO NOT write to audit\_logs (location queries are private)  
//   The only audit that occurs is at the application layer if user  
//   explicitly submits a top-up request from a branch  
//  
// Step 5 — Return sorted list — userLat/userLng are LOCAL variables only,  
//   never assigned to any field, entity, or log.

| 🔒  Privacy Guarantee Implementation The NearbyBranchRequest DTO must be annotated @JsonIgnoreProperties. The controller method parameter is @RequestBody NearbyBranchRequest req — after the service call returns, req is immediately eligible for GC. Add a @AfterReturning AOP aspect on BranchFinderController to null out the request fields post-execution as a belt-and-suspenders measure. Never add user\_id to the nearby query. The endpoint is accessible to authenticated users only (no anonymous location queries). |
| :---- |

| MODULE 09   ·   Yahoo Finance · WebSocket · Individual & Corporate · Platform Revenue on Every Trade Real-Time Trading Platform |
| :---- |

##   **9.1  Business Requirements & Platform Revenue Model**

Every individual and corporate user can trade stocks, forex, and crypto in real-time. **The platform earns a commission on EVERY trade**. The trading engine connects to  Yahoo Finance WebSocket  for live price feeds. Orders are executed via a thin abstraction layer that routes through the platform's own order ledger before confirmations, ensuring the platform can apply, collect, and audit its fee on every single transaction before settlement.

| 💰  Platform Revenue Per Trade Trading Commission \= tradeAmount × platformTradingFeeRate (configurable per asset class: STOCK/FOREX/CRYPTO). This fee is deducted from the user's wallet balance at order submission. The fee is credited to the PLATFORM\_OWNER revenue account in real time via the AccountingLedgerService. No trade can execute without this fee being successfully reserved first (pessimistic locking on wallet balance). |
| :---- |

##   **9.2  Maven Dependencies — Add to pom.xml**

\<\!-- WebSocket for real-time price streaming \--\>  
\<dependency\>  
    \<groupId\>org.springframework.boot\</groupId\>  
    \<artifactId\>spring-boot-starter-websocket\</artifactId\>  
\</dependency\>  
   
\<\!-- Yahoo Finance Java client \--\>  
\<dependency\>  
    \<groupId\>tech.tablesaw\</groupId\>  
    \<artifactId\>tablesaw-core\</artifactId\>  
    \<version\>0.43.1\</version\>  
\</dependency\>  
   
\<\!-- OR use direct HTTP polling with WebClient \--\>  
\<\!-- Yahoo Finance v8 API: https://query1.finance.yahoo.com/v8/finance/chart/{symbol} \--\>  
\<\!-- Yahoo Finance WebSocket: wss://streamer.finance.yahoo.com \--\>  
   
\<\!-- Redis for price cache and pub/sub \--\>  
\<dependency\>  
    \<groupId\>org.springframework.boot\</groupId\>  
    \<artifactId\>spring-boot-starter-data-redis\</artifactId\>  
\</dependency\>

##   **9.3  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| trading\_accounts | id, user\_id, account\_type (INDIVIDUAL/CORPORATE), status, buying\_power\_usd, total\_portfolio\_value, created\_at |
| watchlists | id, user\_id, name, symbols (TEXT ARRAY), created\_at |
| orders | id, trading\_account\_id, symbol, order\_type (MARKET/LIMIT/STOP), side (BUY/SELL), quantity, limit\_price, status (PENDING/FILLED/CANCELLED/FAILED), filled\_price, filled\_at, platform\_fee, platform\_fee\_currency, created\_at |
| positions | id, trading\_account\_id, symbol, quantity, avg\_entry\_price, current\_price, unrealized\_pnl, last\_updated |
| price\_snapshots | id, symbol, price, change\_pct, volume, market\_cap, currency, source (YAHOO), captured\_at — partitioned by day |
| platform\_trading\_fees | id, asset\_class (STOCK/FOREX/CRYPTO/ETF), fee\_rate\_bps (basis points), min\_fee\_usd, max\_fee\_usd, effective\_from, effective\_to |

##   **9.4  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/trading/YahooFinanceService.java | REST polling \+ WebSocket connection to Yahoo Finance |
| service/trading/PriceCacheService.java | Redis-backed price cache: GET/SET with 1-second TTL |
| service/trading/OrderExecutionService.java | Place, validate, fill, and fee-collect for all orders |
| service/trading/PortfolioService.java | Real-time P\&L calculation, position tracking |
| service/trading/TradingFeeService.java | Calculate and collect platform fee before order execution |
| service/trading/MarketDataService.java | Aggregate: quote, chart data, company info, news headlines |
| controller/TradingController.java | Order CRUD, portfolio, P\&L, positions endpoints |
| controller/MarketDataController.java | Quotes, charts, search tickers, watchlist management |
| websocket/PriceStreamHandler.java | STOMP WebSocket handler: broadcast live prices to subscribed clients |
| websocket/TradingWebSocketConfig.java | Configure STOMP broker: /topic/prices/{symbol}, /topic/portfolio/{userId} |
| scheduler/PriceFeedScheduler.java | @Scheduled every 2s: poll Yahoo Finance for watchlisted symbols, cache, broadcast |
| entity/Order.java | JPA entity with full order lifecycle |
| entity/Position.java | JPA entity with real-time P\&L |
| entity/TradingAccount.java | JPA entity |
| dto/trading/PlaceOrderRequest.java | symbol, side, orderType, quantity, limitPrice (optional) |
| dto/trading/OrderResponse.java | Full order details with fee breakdown |
| dto/trading/LiveQuoteResponse.java | symbol, price, change, changePct, volume, high, low, marketCap |
| dto/trading/PortfolioSummaryResponse.java | totalValue, dayPnl, totalPnl, positions list, cashBalance |

##   **9.5  Yahoo Finance Integration — Data Flow**

// YahooFinanceService:  
//  
// A) REST Quote (on-demand):  
//    GET https://query1.finance.yahoo.com/v8/finance/chart/{symbol}  
//    ?interval=1m\&range=1d\&includePrePost=false  
//    Parse: chart.result\[0\].meta.regularMarketPrice, previousClose, etc.  
//    Cache in Redis: key='quote:{symbol}', TTL=2 seconds  
//  
// B) Price Streaming (WebSocket to clients):  
//    PriceFeedScheduler runs every 2 seconds:  
//      1\. Collect all symbols from all active watchlists (distinct)  
//      2\. Batch call Yahoo Finance: up to 20 symbols per request  
//         GET https://query1.finance.yahoo.com/v7/finance/quote  
//         ?symbols=AAPL,MSFT,NVDA,AMZN,...  
//      3\. Parse and update Redis cache  
//      4\. Broadcast via STOMP: messagingTemplate.convertAndSend(  
//             '/topic/prices/' \+ symbol, liveQuoteDto)  
//  
// C) Historical Chart Data:  
//    GET .../v8/finance/chart/{symbol}?interval={1m|5m|1h|1d}\&range={1d|5d|1mo|6mo|1y|5y}  
//  
// D) Ticker Search:  
//    GET https://query2.finance.yahoo.com/v1/finance/search?q={query}\&quotesCount=10  
//  
// SUPPORTED ASSET CLASSES:  
//   \- US Stocks: AAPL, MSFT, NVDA, AMZN, GOOGL, META, TSLA, etc.  
//   \- ETFs: SPY, QQQ, VTI, etc.  
//   \- Forex: USDTRY=X, EURUSD=X, GBPUSD=X, etc.  
//   \- Crypto: BTC-USD, ETH-USD, etc.  
//   \- Local/Regional: Istanbul Stock Exchange via .IS suffix (e.g. THYAO.IS)  
//   \- Middle East: Tadawul (Saudi: .SR), DFM (UAE: .AE), etc.

##   **9.6  Order Execution — Platform Fee Collection (CRITICAL)**

// OrderExecutionService.placeOrder(PlaceOrderRequest req, Long userId):  
// @Transactional(isolation \= Isolation.SERIALIZABLE)  
//  
// Step 1 — Load TradingAccount, verify status \== ACTIVE  
//  
// Step 2 — Get live quote for symbol from PriceCacheService  
//          If cache miss: fetch from Yahoo Finance directly  
//  
// Step 3 — Calculate order value:  
//          estimatedValue \= quantity \* (limitPrice ?? currentPrice)  
//  
// Step 4 — Calculate PLATFORM FEE (TradingFeeService):  
//          assetClass \= detectAssetClass(symbol)  // STOCK/FOREX/CRYPTO/ETF  
//          feeRateBps \= platformTradingFees.findByAssetClass(assetClass)  
//          fee \= MAX(min\_fee\_usd, MIN(max\_fee\_usd, estimatedValue \* feeRateBps / 10000))  
//  
// Step 5 — RESERVE funds (pessimistic lock on wallet\_balances):  
//          SELECT ... FOR UPDATE NOWAIT  
//          If BUY: lock available\_balance \>= estimatedValue \+ fee  
//          Move (estimatedValue \+ fee) to locked\_balance  
//  
// Step 6 — Create Order record with status \= PENDING  
//          Store fee, estimatedValue, symbol, side, quantity  
//  
// Step 7 — Simulate fill (market order fills immediately at live price):  
//          filledPrice \= currentMarketPrice  
//          actualValue \= quantity \* filledPrice  
//  
// Step 8 — Settle:  
//          Debit locked\_balance: actualValue \+ fee  
//          Credit position or cash as appropriate  
//          Credit PLATFORM\_OWNER revenue account: fee (via AccountingLedgerService)  
//  
// Step 9 — Update Position record (upsert)  
//  
// Step 10 — Update Order: status=FILLED, filledPrice, filledAt  
//  
// Step 11 — Audit: action='ORDER\_FILLED', details={symbol,side,qty,price,fee}  
//  
// Step 12 — Push via WebSocket to /topic/portfolio/{userId}

##   **9.7  REST & WebSocket Endpoints**

| Endpoint | Access / Description |
| :---- | :---- |
| POST /api/trading/account/open | INDIVIDUAL\_USER, CORPORATE\_ADMIN — Open trading account |
| GET /api/trading/account | Own role — Get trading account summary \+ buying power |
| POST /api/trading/orders | INDIVIDUAL\_USER, CORPORATE\_ADMIN — Place order (market/limit/stop) |
| GET /api/trading/orders | Own role — Order history with filters (status, symbol, date) |
| DELETE /api/trading/orders/{id}/cancel | Own role — Cancel a PENDING order |
| GET /api/trading/portfolio | Own role — Full portfolio: positions, cash, total P\&L |
| GET /api/trading/positions | Own role — Current positions with live P\&L |
| GET /api/market/quote/{symbol} | Authenticated — Live quote for any Yahoo Finance symbol |
| GET /api/market/chart/{symbol} | Authenticated — OHLCV chart data (interval \+ range params) |
| GET /api/market/search | Authenticated — Search tickers: ?q=apple |
| POST /api/market/watchlist | Own role — Create/update watchlist |
| GET /api/market/watchlist | Own role — Get watchlist with live prices |
| GET /api/trading/admin/fees | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — View/edit trading fee rates |
| WS  /ws/prices  STOMP topic: /topic/prices/{symbol} | Authenticated — Subscribe to live price stream |
| WS  /ws/prices  STOMP topic: /topic/portfolio/{userId} | Own user — Live portfolio value updates |

| MODULE 10   ·   Every Transaction  ·  Every Trade  ·  Every Exchange  ·  Software Author Always Earns Platform Owner Revenue Layer |
| :---- |

##   **10.1  Revenue Architecture**

The **PLATFORM\_OWNER** account is a system-level account that automatically receives a configured revenue share from every billable event. This is implemented as a **cross-cutting concern** via AOP, not scattered through individual service methods. A single  @PlatformRevenue  annotation triggers the revenue collection interceptor.

##   **10.2  Revenue Events & Sources**

| Revenue Event | Collection Point  ·  Configurable Rate Key |
| :---- | :---- |
| Money Transfer (comprehensive) | TransactionService — PLATFORM\_BASE\_FEE rate in commission\_rates |
| Wallet Currency Exchange | WalletService — WALLET\_EXCHANGE\_FEE in commission\_rates |
| Trading Commission (stock/ETF) | OrderExecutionService — TRADING\_FEE\_STOCK bps in platform\_trading\_fees |
| Trading Commission (forex) | OrderExecutionService — TRADING\_FEE\_FOREX bps in platform\_trading\_fees |
| Trading Commission (crypto) | OrderExecutionService — TRADING\_FEE\_CRYPTO bps in platform\_trading\_fees |
| QR Withdrawal (if fee-eligible) | QRController — QR\_RELEASE\_FEE in commission\_rates |
| Wallet Top-Up (optional fee) | WalletTopUpService — TOPUP\_FEE in commission\_rates (can be 0\) |

##   **10.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/revenue/PlatformRevenueService.java | Core: collect revenue, credit PLATFORM\_OWNER ledger account |
| service/revenue/RevenueReportService.java | Daily/monthly/yearly revenue reports for PLATFORM\_OWNER |
| aop/PlatformRevenueAspect.java | @Around aspect — intercepts @PlatformRevenue annotated methods |
| annotation/PlatformRevenue.java | Custom annotation: @PlatformRevenue(event=TRADE, rateKey='TRADING\_FEE\_STOCK') |
| controller/PlatformOwnerDashboardController.java | Revenue dashboard, global stats, fee config — PLATFORM\_OWNER only |
| dto/revenue/RevenueSnapshotResponse.java | todayRevenue, weekRevenue, monthRevenue, allTimeRevenue, byEventType |
| entity/PlatformRevenueEntry.java | Immutable record of every platform revenue collection event |

##   **10.4  PlatformRevenueAspect — AOP Implementation**

// @Aspect @Component  
// @Around("@annotation(platformRevenue)")  
// public Object interceptRevenue(ProceedingJoinPoint jp, PlatformRevenue platformRevenue)  
//  
// 1\. Let the annotated method proceed (jp.proceed())  
// 2\. If method completes without exception:  
//    a. Extract feeAmount from method return value (implement Revenueable interface  
//       or use SpEL expression in annotation to point to fee field)  
//    b. Call platformRevenueService.collect(  
//           event \= platformRevenue.event(),  
//           amount \= feeAmount,  
//           currency \= feeAmount.currency,  
//           sourceEntityId \= extractEntityId(returnValue))  
// 3\. PlatformRevenueService.collect():  
//    a. Credit PLATFORM\_OWNER's ledger account (AccountingLedgerService)  
//    b. Persist PlatformRevenueEntry (immutable)  
//    c. Audit: action='PLATFORM\_REVENUE\_COLLECTED'  
// 4\. If AOP interception fails: LOG ERROR but DO NOT roll back the original transaction  
//    Revenue collection failure must never break user-facing operations.  
//    Use a @Async fallback retry queue for failed collections.

##   **10.5  PLATFORM\_OWNER Dashboard Endpoints**

| Method \+ Path | Description |
| :---- | :---- |
| GET /api/owner/dashboard | Total revenue today/week/month/all-time, active users, volume |
| GET /api/owner/revenue/breakdown | Revenue by event type with chart data |
| GET /api/owner/revenue/timeline | Day-by-day revenue timeline (configurable range) |
| GET /api/owner/fees/config | View all platform fee rates across all modules |
| PUT /api/owner/fees/config/{rateKey} | Update any platform fee rate globally |
| GET /api/owner/users/stats | Total registered users, KYC tiers, trading accounts |
| GET /api/owner/branches/performance | Revenue generated per branch |

| MODULE 11   ·   Geographic Discounts · Holiday Rules · Religious Calendar · Admin Fee Control Smart Geo-Temporal Fee Zone Engine |
| :---- |

##   **11.1  Business Requirements**

The **PLATFORM\_OWNER** and **MOTHER\_BRANCH\_ADMIN** can define fee exemption or discount rules at any geographic granularity: **country → city → district → street**. Rules can be time-bound (specific dates, recurring holidays, Islamic calendar events like Ramadan and Eid) or permanent. When a transaction originates from a branch that falls within a fee-zone rule, the applicable discounts are applied automatically — including **complete fee-free transactions** if configured.

##   **11.2  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| fee\_zones | id, name, description, country, governorate, city, district, street (nullable — progressively specific), status (ACTIVE/INACTIVE), created\_by, created\_at |
| fee\_zone\_rules | id, fee\_zone\_id, rule\_type (PERCENTAGE\_DISCOUNT/FEE\_FREE/FLAT\_REDUCTION), discount\_value (0-100 for %, absolute amount for flat), applies\_to (ALL/TRANSFER/EXCHANGE/TRADING/TOPUP), priority (lower \= higher priority), created\_by, created\_at |
| fee\_zone\_schedules | id, fee\_zone\_id, schedule\_type (DATE\_RANGE/RECURRING\_ANNUAL/ISLAMIC\_CALENDAR/DAY\_OF\_WEEK), start\_date, end\_date, recurring\_month, recurring\_day, islamic\_event (RAMADAN/EID\_AL\_FITR/EID\_AL\_ADHA/NATIONAL\_HOLIDAYS), day\_of\_week, is\_active |
| branch\_fee\_zone\_assignments | id, branch\_id, fee\_zone\_id, assigned\_by, assigned\_at — which branches belong to which zones |

##   **11.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/feezone/FeeZoneService.java | CRUD for fee zones, rules, schedules |
| service/feezone/FeeZoneEvaluationService.java | Real-time: given branchId \+ transactionType \+ date, return effective discount |
| service/feezone/IslamicCalendarService.java | Calculate Islamic calendar dates (Hijri) for rule evaluation |
| controller/FeeZoneController.java | Full CRUD for zones, rules, schedules, assignments |
| entity/FeeZone.java | JPA entity |
| entity/FeeZoneRule.java | JPA entity |
| entity/FeeZoneSchedule.java | JPA entity |
| dto/feezone/FeeZoneRequest.java | Create/update fee zone with rules and schedule |
| dto/feezone/EffectiveFeeResponse.java | originalFee, discountApplied, finalFee, ruleApplied, zoneName |

##   **11.4  FeeZoneEvaluationService — Resolution Logic**

// getEffectiveDiscount(Long branchId, String transactionType, LocalDate date):  
//  
// Step 1 — Get all fee zones the branch belongs to:  
//   SELECT fz.\* FROM fee\_zones fz  
//   JOIN branch\_fee\_zone\_assignments a ON a.fee\_zone\_id \= fz.id  
//   WHERE a.branch\_id \= :branchId AND fz.status \= 'ACTIVE'  
//  
// Step 2 — Filter by active schedules for today's date:  
//   For each zone, check its fee\_zone\_schedules:  
//   \- DATE\_RANGE: startDate \<= date \<= endDate  
//   \- RECURRING\_ANNUAL: month and day match  
//   \- ISLAMIC\_CALENDAR: use IslamicCalendarService.isEventActive(islamicEvent, date)  
//   \- DAY\_OF\_WEEK: date.getDayOfWeek() matches  
//  
// Step 3 — From active zones, load applicable rules:  
//   WHERE applies\_to IN ('ALL', :transactionType)  
//  
// Step 4 — Select highest-priority rule (lowest priority number wins):  
//   If rule\_type \== FEE\_FREE: return 100% discount immediately  
//   If multiple PERCENTAGE\_DISCOUNT rules: apply highest discount value  
//  
// Step 5 — Apply discount to original fee:  
//   finalFee \= originalFee \* (1 \- discountValue/100)  
//   Enforce minimum fee of 0 (never negative)  
//  
// Step 6 — Return EffectiveFeeResponse with full audit trail  
//  
// INTEGRATION: Call this in TransactionService, OrderExecutionService,  
//              WalletService BEFORE charging any fee

##   **11.5  Islamic Calendar Integration**

// IslamicCalendarService.isEventActive(IslamicEvent event, LocalDate gregorianDate):  
//  
// Use Umm al-Qura calendar algorithm or integrate:  
//   \<dependency\>  
//     \<groupId\>com.github.msarhan\</groupId\>  
//     \<artifactId\>ummalqura-calendar\</artifactId\>  
//     \<version\>2.0.1\</version\>  
//   \</dependency\>  
//  
// Event ranges (configurable, defaults):  
//   RAMADAN:      Full Ramadan month (30 days)  
//   EID\_AL\_FITR:  3 days after Ramadan ends  
//   EID\_AL\_ADHA:  4 days (10-13 Dhul Hijja)  
//   MAWLID:       12 Rabi' al-Awwal  
//  
// Cache result for the day in Redis: key='islamic\_event:{event}:{date}', TTL=24h

##   **11.6  Fee Zone Management Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| POST /api/fee-zones | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — Create new geographic fee zone |
| GET /api/fee-zones | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — List all zones with status |
| PUT /api/fee-zones/{id} | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — Update zone details |
| POST /api/fee-zones/{id}/rules | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — Add discount rule to zone |
| POST /api/fee-zones/{id}/schedules | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — Add schedule (date/holiday/recurring) |
| POST /api/fee-zones/{id}/branches | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — Assign branches to zone |
| GET /api/fee-zones/preview | PLATFORM\_OWNER — Preview effective fees for branch+date+type |
| DELETE /api/fee-zones/{id} | PLATFORM\_OWNER — Deactivate fee zone |

| MODULE 12   ·   Manual Notes · City Broadcasts · AI Birthday Messages · In-App Alerts · WhatsApp AI Push Notifications & Smart Messaging |
| :---- |

##   **12.1  Business Requirements**

**PLATFORM\_OWNER** and **MOTHER\_BRANCH\_ADMIN** can compose and send messages to any targeting segment: **all users, specific countries, specific cities, specific districts/streets, specific branches, individual users, all corporate accounts, all individuals**, or any custom filter combination. Messages can be:

* **Manual broadcasts** — typed by admin, pushed to in-app notifications and/or WhatsApp

* **Birthday messages** — AI-generated, personalised, auto-dispatched at midnight of the user's birthday

* **Transaction alerts** — automated post-transaction summaries

* **Holiday greetings** — AI-generated for Islamic and national holidays, auto-dispatched to all relevant users

* **System announcements** — new features, maintenance windows, rate changes

* **Personalised AI insights** — AI-written monthly financial summary pushed to each user

##   **12.2  New Database Tables**

| Table | Columns & Purpose |
| :---- | :---- |
| notification\_campaigns | id, title, created\_by, target\_type (ALL/COUNTRY/CITY/DISTRICT/BRANCH/USER\_LIST/CORPORATE/INDIVIDUAL/CUSTOM), target\_params (JSONB — city name, branch IDs, user IDs etc.), message\_type (MANUAL/AI\_BIRTHDAY/AI\_HOLIDAY/AI\_MONTHLY\_SUMMARY/TRANSACTION\_ALERT), channels (JSONB array: IN\_APP/WHATSAPP/EMAIL), status (DRAFT/SCHEDULED/SENDING/SENT/FAILED), scheduled\_at, sent\_at, total\_recipients |
| notification\_messages | id, campaign\_id, user\_id, content (TEXT), ai\_generated (boolean), language (AR/EN/TR), delivery\_status (PENDING/DELIVERED/FAILED/READ), delivered\_at, read\_at |
| in\_app\_notifications | id, user\_id, title, body, icon\_type, action\_url (nullable), is\_read, created\_at, expires\_at |
| user\_notification\_preferences | id, user\_id, in\_app\_enabled, whatsapp\_enabled, email\_enabled, birthday\_messages\_enabled, holiday\_messages\_enabled, marketing\_enabled, language\_preference |

##   **12.3  New Files to Create**

| File Path | Purpose |
| :---- | :---- |
| service/notification/CampaignService.java | Create, schedule, and execute notification campaigns |
| service/notification/CampaignTargetingService.java | Resolve target\_type+params into a list of user IDs |
| service/notification/AIMessageGeneratorService.java | Generate personalised messages using Spring AI |
| service/notification/BirthdayNotificationService.java | @Scheduled midnight: find today's birthdays, generate AI messages, dispatch |
| service/notification/HolidayGreetingService.java | @Scheduled daily: check Islamic \+ national calendar, dispatch holiday messages |
| service/notification/MonthlyInsightService.java | @Scheduled 1st of month: generate AI financial summaries per user |
| service/notification/InAppNotificationService.java | Push to in\_app\_notifications, deliver via SSE to active sessions |
| controller/CampaignController.java | Full campaign CRUD \+ send \+ stats |
| controller/NotificationController.java | GET my-notifications, PUT mark-as-read, GET unread-count |
| controller/NotificationPreferencesController.java | GET/PUT user notification preferences |
| sse/NotificationSseController.java | GET /api/notifications/stream — SSE endpoint for real-time in-app delivery |
| entity/NotificationCampaign.java | JPA entity |
| entity/InAppNotification.java | JPA entity |
| entity/UserNotificationPreferences.java | JPA entity |

##   **12.4  CampaignTargetingService — Audience Resolution**

// resolveTargetUserIds(NotificationCampaign campaign):  
//  
// ALL:         SELECT id FROM users WHERE status \= 'ACTIVE'  
//  
// COUNTRY:     SELECT u.id FROM users u  
//              JOIN branches b ON u.branch\_id \= b.id  
//              WHERE b.country \= :country  
//              UNION  
//              SELECT user\_id FROM wallets w  
//              JOIN wallet\_applications wa ON wa.user\_id \= w.user\_id  
//              WHERE wa.country \= :country  
//  
// CITY:        Same as COUNTRY but filter b.city \= :city  
//  
// DISTRICT:    Filter b.district \= :district (or user registration district)  
//  
// BRANCH:      SELECT DISTINCT sender\_id FROM transactions  
//              WHERE (sender\_branch\_id \= :branchId OR receiver\_branch\_id \= :branchId)  
//  
// CORPORATE:   SELECT u.id FROM users u WHERE u.role \= 'CORPORATE\_ADMIN'  
//  
// INDIVIDUAL:  SELECT u.id FROM users u WHERE u.role \= 'INDIVIDUAL\_USER'  
//  
// USER\_LIST:   JSON array of user IDs in target\_params  
//  
// CUSTOM:      Combine multiple filters with AND/OR from target\_params JSON  
//  
// Always filter: respect user notification preferences before adding to list  
// Always deduplicate result list

##   **12.5  AIMessageGeneratorService — Personalised Content**

// generateBirthdayMessage(User user):  
//   Prompt: 'You are Almukhtar Elite's AI assistant. Write a warm, personal birthday  
//   message in {userLanguage} for a user named {user.displayName} who has been a  
//   valued member since {user.createdAt.year}. Mention that we appreciate their trust.  
//   Keep it under 3 sentences. Do not mention finances. Be genuine and warm.'  
//  
// generateHolidayGreeting(IslamicEvent event, String language):  
//   Prompt: 'Write a heartfelt {event.name} greeting in {language} for customers  
//   of a professional financial services platform. 2-3 sentences. Culturally  
//   respectful. Do not include promotional content.'  
//  
// generateMonthlyInsight(User user, MonthlyStats stats):  
//   Prompt: 'Write a friendly monthly financial summary for {user.displayName}.  
//   This month they sent {stats.totalSent} {stats.currency}, received {stats.totalReceived},  
//   completed {stats.txCount} transactions, and their wallet grew by {stats.walletGrowth}%.  
//   Give 1 insight and 1 encouragement. 3 sentences max. Tone: professional but warm.',  
//   Language: {userLanguage}  
//  
// All generated content is stored in notification\_messages.content BEFORE dispatch  
// Admin can preview AI-generated content before a campaign is sent

##   **12.6  Real-Time In-App Delivery (SSE)**

// NotificationSseController.stream(HttpServletResponse response, Principal principal):  
//   @GetMapping(value='/api/notifications/stream', produces=TEXT\_EVENT\_STREAM\_VALUE)  
//  
// 1\. Create SseEmitter with 5-minute timeout  
// 2\. Register emitter in a ConcurrentHashMap: userId \-\> SseEmitter  
// 3\. Send any PENDING in\_app\_notifications immediately on connect  
//  
// InAppNotificationService.pushToUser(Long userId, InAppNotification notif):  
// 1\. Save to in\_app\_notifications table  
// 2\. If user has active SSE connection: emitter.send(notif DTO)  
// 3\. If user is offline: notification waits in DB, delivered on next connect  
//  
// Cleanup: @EventListener(SessionDisconnectEvent.class)  
//          Remove emitter from map on disconnect

##   **12.7  Campaign & Notification REST Endpoints**

| Method \+ Path | Access / Description |
| :---- | :---- |
| POST /api/campaigns | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — Create campaign (draft) |
| PUT /api/campaigns/{id}/schedule | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — Set send time |
| POST /api/campaigns/{id}/send-now | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — Immediate dispatch |
| POST /api/campaigns/{id}/preview | PLATFORM\_OWNER — Preview AI-generated content before send |
| GET /api/campaigns | PLATFORM\_OWNER, MOTHER\_BRANCH\_ADMIN — All campaigns with stats |
| GET /api/campaigns/{id}/stats | PLATFORM\_OWNER — Delivery stats: sent, delivered, read rates |
| GET /api/notifications | Own user — My in-app notifications (paginated) |
| GET /api/notifications/unread-count | Own user — Badge count for notification bell |
| PUT /api/notifications/{id}/read | Own user — Mark single notification as read |
| PUT /api/notifications/read-all | Own user — Mark all as read |
| GET /api/notifications/stream | Own user — SSE endpoint for real-time delivery |
| GET /api/notifications/preferences | Own user — Get notification preferences |
| PUT /api/notifications/preferences | Own user — Update notification preferences |

| AUDIT LOG REFERENCE — PHASE II ADDITIONS All actions below MUST be written to audit\_logs using the existing AuditService |
| :---: |

| action Value | Module  ·  Trigger |
| :---- | :---- |
| KYC\_APPLICATION\_SUBMITTED | M7  ·  User submits wallet application |
| KYC\_AI\_SCREENED | M7  ·  AI completes document pre-screening |
| KYC\_APPROVED | M7  ·  Human reviewer approves KYC application |
| KYC\_REJECTED | M7  ·  Human reviewer rejects with reason |
| WALLET\_CREATED | M7  ·  Wallet goes live post-KYC approval |
| WALLET\_EXCHANGE | M7  ·  Currency exchange within wallet |
| WALLET\_TOPUP\_REQUESTED | M7  ·  User requests branch top-up |
| WALLET\_TOPUP\_COMPLETED | M7  ·  Cashier confirms top-up cash received |
| BRANCH\_NEARBY\_QUERY | M8  ·  NOT logged — privacy-preserving |
| TRADING\_ACCOUNT\_OPENED | M9  ·  New trading account created |
| ORDER\_PLACED | M9  ·  Order submitted by user |
| ORDER\_FILLED | M9  ·  Order executed at market price |
| ORDER\_CANCELLED | M9  ·  Order cancelled before fill |
| ORDER\_FAILED | M9  ·  Order failed (insufficient funds, etc.) |
| PLATFORM\_REVENUE\_COLLECTED | M10  ·  Revenue credited to PLATFORM\_OWNER account |
| FEE\_ZONE\_CREATED | M11  ·  New geographic fee zone defined |
| FEE\_ZONE\_RULE\_APPLIED | M11  ·  Fee discount applied to a transaction |
| FEE\_FREE\_OVERRIDE | M11  ·  100% fee waiver applied via zone rule |
| CAMPAIGN\_CREATED | M12  ·  Notification campaign created |
| CAMPAIGN\_SENT | M12  ·  Campaign dispatched to recipients |
| NOTIFICATION\_DELIVERED | M12  ·  In-app notification delivered via SSE |
| AI\_BIRTHDAY\_MESSAGE\_SENT | M12  ·  AI birthday message dispatched |
| AI\_HOLIDAY\_GREETING\_SENT | M12  ·  AI holiday greeting dispatched |
| AI\_MONTHLY\_INSIGHT\_SENT | M12  ·  AI monthly financial summary dispatched |

| NEXT.JS PAGES REQUIRED — ALL NEW MODULES Build these pages in the Next.js 14 App Router structure. All pages are server components by default; mark interactive sections 'use client'. |
| :---: |

| Next.js Route | Description  ·  Key Components |
| :---- | :---- |
| /onboarding/apply | KYC application wizard: personal info → document upload → review → submit. Multi-step form with React Hook Form \+ Zod. |
| /onboarding/status | Application status tracker: animated timeline showing SUBMITTED→UNDER\_REVIEW→APPROVED. |
| /wallet | Wallet dashboard: currency balance cards, exchange panel, transaction history table, top-up request button. |
| /wallet/exchange | Currency exchange UI: from/to selector, live rate preview, fee calculation, confirm modal. |
| /wallet/topup | Branch top-up flow: map showing nearest branches (Module 8 geolocation picker), request form. |
| /branches/nearby | Geolocation branch finder: browser geolocation → send to API → display nearest branches as cards \+ map markers. |
| /trading | Main trading dashboard: live ticker watchlist, chart panel (TradingView-style using recharts), order entry panel, position book, P\&L summary. |
| /trading/markets | Market explorer: search any Yahoo Finance ticker, category browser (US Stocks/Forex/Crypto/MENA), trending movers. |
| /trading/portfolio | Portfolio deep-dive: holdings table, allocation pie chart, performance timeline chart, order history. |
| /admin/fee-zones | Fee zone management: world map with zone overlays, zone builder form, holiday schedule calendar. |
| /admin/campaigns | Campaign manager: audience builder, message composer with AI-generate button, preview, schedule picker, sent campaigns with open/delivery rates. |
| /admin/platform-revenue | PLATFORM\_OWNER only: revenue dashboard with area charts, event breakdown, branch performance leaderboard. |
| /notifications | Notification inbox: read/unread list, mark-all-read, notification preference toggle panel. |

| CURSOR EXECUTION PHASES — PASTE IN ORDER Run each phase as a separate Cursor Composer prompt. Complete and test each before proceeding. |
| :---: |

| ⚡  CURSOR PHASE A — ROLE REFACTORING *"Refactor the existing SUPER\_ADMIN role. Add PLATFORM\_OWNER as a new top-level system role with a single seeded account in DataInitializer. Add MOTHER\_BRANCH\_ADMIN role below it — this user operates the main branch with full branch capabilities PLUS the ability to create corporations and onboard new branches. Add CORPORATE\_ADMIN and INDIVIDUAL\_USER roles. Update SecurityConfig, all @PreAuthorize annotations, and the RBAC tables. PLATFORM\_OWNER can do everything MOTHER\_BRANCH\_ADMIN can plus access the owner revenue dashboard. Do not break any existing endpoints — map old SUPER\_ADMIN permissions to PLATFORM\_OWNER."* |
| :---- |

| ⚡  CURSOR PHASE B — DIGITAL WALLET \+ KYC — MODULE 7 *"Implement Module 7 fully. Create all entities (Wallet, WalletBalance, KycDocument, WalletApplication), repositories, and services: WalletApplicationService, DocumentVerificationService (Spring AI Vision API call), WalletService (multi-currency balances with pessimistic locking on SELECT FOR UPDATE), WalletTopUpService. Create all controllers and DTOs. Add MinIO/S3 integration for document storage using Spring's ResourceLoader abstraction with a configurable storage provider. Every wallet operation writes a WalletTransaction record AND a double-entry LedgerEntry pair (integrate with existing AccountingLedgerService from Phase I Module 3)."* |
| :---- |

| ⚡  CURSOR PHASE C — GEOLOCATION BRANCH FINDER — MODULE 8 *"Extend the branches table with location columns (latitude, longitude, city, country, address\_line, phone, opens\_at, closes\_at, services JSONB). Create the Flyway migration script. Implement BranchGeoService using the PostgreSQL earthdistance extension with ll\_to\_earth() and earth\_box() for indexed proximity queries. Create the PrivacyLocationService AOP aspect that nullifies coordinates after the service call returns. Create BranchFinderController with POST /api/branches/nearby. Store NO user location data anywhere. On the Next.js side, use navigator.geolocation API to get browser coordinates, POST to the endpoint, and render results as cards with a Leaflet.js map."* |
| :---- |

| ⚡  CURSOR PHASE D — REAL-TIME TRADING PLATFORM — MODULE 9 *"Implement Module 9\. Create all trading entities: TradingAccount, Order, Position, PriceSnapshot (day-partitioned), Watchlist, PlatformTradingFee. Build YahooFinanceService using Spring WebClient to poll Yahoo Finance v8 REST API every 2 seconds for all watchlisted symbols. Build PriceCacheService using Spring Data Redis. Configure Spring WebSocket with STOMP: topic /topic/prices/{symbol} for live price broadcast and /topic/portfolio/{userId} for portfolio updates. Build OrderExecutionService with SERIALIZABLE isolation, pessimistic wallet locking, platform fee collection BEFORE fill, and position upsert AFTER fill. Build MarketDataController for quotes, charts, and ticker search. Annotate all fee-collecting methods with @PlatformRevenue. Include support for US stocks, ETFs, forex pairs (e.g. USDTRY=X), crypto (BTC-USD), and regional tickers with .IS, .SR, .AE suffixes."* |
| :---- |

| ⚡  CURSOR PHASE E — PLATFORM OWNER REVENUE LAYER — MODULE 10 *"Implement Module 10\. Create the @PlatformRevenue custom annotation and PlatformRevenueAspect using Spring AOP @Around advice. The aspect intercepts all methods annotated with @PlatformRevenue, extracts the fee amount from the return value (implement a Revenueable marker interface on all fee-bearing response DTOs), calls PlatformRevenueService.collect() in a separate @Transactional(REQUIRES\_NEW) context so revenue collection never rolls back user operations, and persists an immutable PlatformRevenueEntry. Apply @PlatformRevenue to: OrderExecutionService.placeOrder(), WalletService.exchangeCurrency(), TransactionService.processComprehensiveTransfer(), WalletTopUpService (if fee \> 0). Build PlatformOwnerDashboardController with revenue breakdown and timeline endpoints."* |
| :---- |

| ⚡  CURSOR PHASE F — GEO-TEMPORAL FEE ZONES — MODULE 11 *"Implement Module 11\. Create FeeZone, FeeZoneRule, FeeZoneSchedule, BranchFeeZoneAssignment entities with full JPA relationships. Add the Umm al-Qura Maven dependency for Islamic calendar support. Build IslamicCalendarService that converts between Gregorian and Hijri calendars and checks if a given Islamic event (Ramadan, Eid al-Fitr, Eid al-Adha, Mawlid) is active on a given date — cache results in Redis for 24 hours. Build FeeZoneEvaluationService with the 4-step resolution logic (zone lookup → schedule filter → rule selection → discount application). HOOK FeeZoneEvaluationService into: TransactionService.calculateComprehensiveFees(), WalletService.exchangeCurrency(), and OrderExecutionService.calculateTradingFee() — always call it last, after base fee calculation, before collecting."* |
| :---- |

| ⚡  CURSOR PHASE G — AI PUSH NOTIFICATIONS — MODULE 12 *"Implement Module 12 completely. Create all notification entities and repositories. Build CampaignTargetingService with all 8 targeting strategies (ALL/COUNTRY/CITY/DISTRICT/BRANCH/USER\_LIST/CORPORATE/INDIVIDUAL). Build AIMessageGeneratorService using Spring AI with 3 prompt templates: birthday (warm, personal, language-aware), holiday greeting (culturally respectful for Islamic events), and monthly financial insight (personalised stats summary). Build BirthdayNotificationService scheduled at 00:05 daily that queries users whose birthday is today (comparing only month and day from date\_of\_birth column), generates AI messages, and dispatches. Build HolidayGreetingService that checks IslamicCalendarService daily at 06:00 and dispatches if an event starts today. Implement SSE real-time delivery via SseEmitter with a ConcurrentHashMap registry. Connect WhatsApp delivery to the existing WhatsAppNotificationProvider from Phase I Module 6."* |
| :---- |

| ALMUKHTAR ELITE Phase II — Market Domination 6 New Modules  ·  2 Role Additions  ·  Real-Time Trading  ·  Digital KYC  ·  AI Messaging ──────────────────────────────────────────────────── © 2025 ALMUKHTAR Elite — Confidential — Senior Fintech Architecture Division |
| :---: |

