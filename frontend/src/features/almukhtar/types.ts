export type BackendState<T> = {
  data?: T;
  error?: string;
  loading: boolean;
};

export type LoginResponse = {
  token: string;
  username: string;
  role: string;
  expiresIn: number;
};

export type WalletBalance = {
  currencyCode: string;
  availableBalance: number;
  lockedBalance: number;
};

export type WalletResponse = {
  id: number;
  userId: number;
  walletNumber: string;
  status: string;
  kycTier: string;
  dailyLimit: number;
  monthlyLimit: number;
  balances: WalletBalance[];
};

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
};

export type WalletTransaction = {
  id: number;
  amount: number;
  currencyCode: string;
  type: string;
  createdAt?: string;
  status?: string;
};

export type Fund = {
  id: number;
  name: string;
  balance: number;
  currency?: string;
  status: string;
};

export type Transaction = {
  id: number;
  senderId?: number;
  receiverId?: number;
  senderName?: string;
  senderPhone?: string;
  receiverName?: string;
  receiverPhone?: string;
  amount: number;
  status: string;
  currencyCode?: string;
  fee?: number;
  note?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type LiquidityAlert = {
  id: number;
  branchId?: number;
  currency?: string;
  severity?: string;
  status?: string;
  message?: string;
  createdAt?: string;
};

export type Notification = {
  id: number;
  title?: string;
  message?: string;
  read?: boolean;
  type?: string;
  createdAt?: string;
  link?: string;
};

export type DashboardSnapshot = Record<string, unknown>;

// AML
export type AmlAlert = {
  id: number;
  transactionId?: number;
  userId?: number;
  ruleTriggered?: string;
  severity?: string;
  status?: string;
  createdAt?: string;
  details?: string;
  alertType?: string;
};

// Transfers
export type TransferPayload = {
  senderName: string;
  senderPhone: string;
  receiverName: string;
  receiverPhone: string;
  amount: number;
  currency: string;
  fundId: number;
  senderId?: number;
  receiverId?: number;
};

export type CashOutPickupPayload = {
  passcode: string;
  qrToken: string;
  branchId: number;
};

export type FeeBreakdown = {
  baseAmount: number;
  sendingFee: number;
  platformFee: number;
  receivingFee: number;
  totalFee: number;
  totalCharged: number;
};

// Bill Payments & Top-Up
export type BillProvider = {
  id: number;
  name: string;
  category: string;
  isActive: boolean;
  logoUrl?: string;
  accountLabel?: string;
};

export type TopUpRequest = {
  providerId: number;
  phoneNumber: string;
  amount: number;
  currency: string;
};

export type BillPaymentRequest = {
  providerId: number;
  accountNumber: string;
  amount: number;
  currency: string;
};

// Lending
export type LoanProduct = {
  id: number;
  name: string;
  description?: string;
  maxAmount: number;
  minAmount?: number;
  interestRatePercent: number;
  termMonths: number;
  currency: string;
  isActive?: boolean;
};

export type LoanInstallment = {
  id: number;
  loanId: number;
  dueDate: string;
  amount: number;
  principal?: number;
  interest?: number;
  status: string;
  paidAt?: string;
};

export type CreditProfile = {
  userId?: number;
  creditScore: number;
  scoreBand?: string;
  maxLoanAmount?: number;
  currency?: string;
  lastUpdated?: string;
};

export type ActiveLoan = {
  id: number;
  productName?: string;
  principalAmount: number;
  remainingBalance: number;
  currency: string;
  status: string;
  startDate?: string;
  nextDueDate?: string;
  installments?: LoanInstallment[];
};

// Liquidity & Branches
export type LiquidityForecastPoint = {
  label: string;
  balance: number;
  threshold?: number;
  forecast?: number;
};

export type BranchSummary = {
  id: number;
  name: string;
  city?: string;
  cashBalance?: number;
  currency?: string;
  status?: string;
  operatingHours?: string;
  latitude?: number;
  longitude?: number;
};

// AI Assistant
export type AiChatMessage = {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
};

export type AiChatResponse = {
  response?: string;
  message?: string;
  answer?: string;
  reply?: string;
  [key: string]: unknown;
};

// Exchange
export type ExchangeRate = {
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  updatedAt?: string;
};

// Disputes
export type Dispute = {
  id: number;
  transactionId?: number;
  userId?: number;
  reason?: string;
  status?: string;
  assignedTo?: string;
  resolution?: string;
  createdAt?: string;
  updatedAt?: string;
};

// Account Freeze
export type FreezeCase = {
  id: number;
  publicCaseId?: string;
  userId?: number;
  reason?: string;
  status?: string;
  initiatedBy?: string;
  createdAt?: string;
  resolvedAt?: string;
};

export type FreezeCaseEvent = {
  id: number;
  eventType?: string;
  description?: string;
  performedBy?: string;
  createdAt?: string;
};

// Cashier Shifts
export type CashierShift = {
  id: number;
  cashierId?: number;
  branchId?: number;
  approvedById?: number;
  status?: string;
  openedAt?: string;
  closedAt?: string;
  approvedAt?: string;
  notes?: string;
};

export type OpenShiftRequest = {
  branchId: number;
  openingBalances?: Record<string, number>;
  notes?: string;
};

export type CloseShiftRequest = {
  shiftId: number;
  countedBalances?: Record<string, number>;
  notes?: string;
};

export type CashierShiftBalance = {
  currencyCode?: string;
  openingBalance?: number;
  closingBalance?: number;
  expectedBalance?: number;
  difference?: number;
};

export type CashierShiftEntry = {
  id: number;
  type?: string;
  amount?: number;
  currencyCode?: string;
  note?: string;
  createdAt?: string;
};

// Branch Cash
export type BranchVaultBalance = {
  id?: number;
  branchId?: number;
  currency?: string;
  vaultBalance?: number;
  lastReconciledAt?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type BranchCashInventory = {
  id?: number;
  branchId?: number;
  currency?: string;
  availableBalance?: number;
  reservedBalance?: number;
  lowCashThreshold?: number;
  highCashThreshold?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type CashDrawerMovement = {
  id: number;
  drawerId?: number;
  currency?: string;
  signedDelta?: number;
  type?: string;
  status?: string;
  note?: string;
  performedBy?: string;
  approvedBy?: string;
  createdAt?: string;
};

export type CashTransferOrder = {
  id: number;
  fromBranchId?: number;
  toBranchId?: number;
  requestedById?: number;
  approvedById?: number;
  amount?: number;
  currency?: string;
  status?: string;
  notes?: string;
  createdAt?: string;
  approvedAt?: string;
  dispatchedAt?: string;
  receivedAt?: string;
};

export type ReconciliationReport = {
  branchId?: number;
  reconciledAt?: string;
  differences?: Record<string, number>;
  physicalVault?: Record<string, number>;
  systemVault?: Record<string, number>;
};

// Batch Transfers
export type BatchJob = {
  id: number;
  name?: string;
  status?: string;
  totalRows?: number;
  successRows?: number;
  failedRows?: number;
  createdAt?: string;
  executedAt?: string;
};

export type BatchTemplate = {
  id: number;
  name?: string;
  description?: string;
  columns?: string[];
};

// Trading
export type TradingAccount = {
  id: number;
  userId?: number;
  accountNumber?: string;
  status?: string;
  currency?: string;
  balance?: number;
  equity?: number;
  marginUsed?: number;
  buyingPower?: number;
  buyingPowerUsd?: number;
};

export type TradingOrder = {
  id: number;
  symbol?: string;
  type?: string;
  side?: string;
  quantity?: number;
  price?: number;
  status?: string;
  filledAt?: string;
  createdAt?: string;
};

export type TradingPosition = {
  id: number;
  symbol?: string;
  side?: string;
  quantity?: number;
  entryPrice?: number;
  currentPrice?: number;
  pnl?: number;
  currency?: string;
};

export type TradableAsset = {
  id: number;
  symbol?: string;
  name?: string;
  assetType?: string;
  isActive?: boolean;
  currentPrice?: number;
  currency?: string;
};

export type PriceAlert = {
  id: number;
  symbol?: string;
  targetPrice?: number;
  direction?: string;
  isActive?: boolean;
  createdAt?: string;
};

// Savings Goals
export type SavingsGoal = {
  id: number;
  name?: string;
  targetAmount?: number;
  savedAmount?: number;
  currentAmount?: number; // alias kept for backwards compatibility
  currency?: string;
  status?: string;
  deadline?: string;
  targetDate?: string; // alias kept for backwards compatibility
  autoSweep?: boolean;
  createdAt?: string;
};

// Recurring Transfers
export type RecurringTransfer = {
  id: number;
  receiverId?: number;
  amount?: number;
  currency?: string;
  frequency?: string;
  status?: string;
  nextRunDate?: string;
  createdAt?: string;
};

// Split Payments
export type SplitRequest = {
  id: number;
  createdBy?: number;
  totalAmount?: number;
  currency?: string;
  description?: string;
  status?: string;
  participants?: SplitParticipant[];
  createdAt?: string;
};

export type SplitParticipant = {
  id: number;
  userId?: number;
  amount?: number;
  status?: string;
};

// Escrow
export type EscrowContract = {
  id: number;
  buyerId?: number;
  sellerId?: number;
  amount?: number;
  currency?: string;
  description?: string;
  status?: string;
  releaseDate?: string;
  createdAt?: string;
};

// Merchants
export type Merchant = {
  id: number;
  businessName?: string;
  name?: string; // legacy alias
  category?: string;
  businessType?: string; // legacy alias
  registrationNumber?: string;
  address?: string;
  city?: string;
  country?: string;
  status?: string;
  userId?: number;
  qrCodeData?: string;
  qrCode?: string; // legacy alias
  branchId?: number;
  createdAt?: string;
};

export type MerchantRegistrationRequest = {
  businessName: string;
  category?: string;
  registrationNumber?: string;
  address?: string;
  city?: string;
  country?: string;
  branchId?: number;
};

// Campaigns
export type Campaign = {
  id: number;
  name?: string;
  targetAudience?: string;
  messageTemplate?: string;
  status?: string;
  scheduledAt?: string;
  executedAt?: string;
};

export type CreateCampaignRequest = {
  name: string;
  targetAudience: string;
  messageTemplate: string;
};

// QR Codes
export type QrGenerationResponse = {
  qrImageBase64?: string;
  tokenHash?: string;
  expiresAt?: string;
  transactionId?: number;
};

export type ScanValidationRequest = {
  encryptedPayload: string;
  tokenHash: string;
  totpCode: string;
};

export type ScanValidationResponse = {
  success: boolean;
  transactionId?: number;
  receiverName?: string;
  amount?: number;
  message?: string;
};

// Wallet Admin (Top-Up / Cash-Out)
export type TopupRequestAdmin = {
  id: number;
  walletId?: number;
  userId?: number;
  username?: string;
  branchId?: number;
  amount?: number;
  currency?: string;
  status?: string;
  createdAt?: string;
};

export type CashOutRequestAdmin = {
  id: number;
  walletId?: number;
  userId?: number;
  username?: string;
  branchId?: number;
  amount?: number;
  currency?: string;
  status?: string;
  passcode?: string;
  createdAt?: string;
};

// Gamification
export type GamificationScore = {
  userId?: number;
  points?: number;
  level?: string;
  rank?: number;
};

export type Badge = {
  id: number;
  name?: string;
  description?: string;
  iconUrl?: string;
  awardedAt?: string;
};

export type LeaderboardEntry = {
  rank?: number;
  userId?: number;
  username?: string;
  points?: number;
};

// Family Wallet
export type FamilyGroup = {
  id: number;
  name?: string;
  ownerId?: number;
  ownerUser?: { id?: number; username?: string };
  currency?: string;
  monthlySpendingLimit?: number;
  memberCount?: number;
  createdAt?: string;
};

export type FamilyMember = {
  id: number;
  userId?: number;
  role?: string;
  joinedAt?: string;
};

// Corporate
export type CorporateAccount = {
  id: number;
  parentUserId?: number;
  companyName?: string;
  registrationNumber?: string;
  creditLimit?: number;
  usedCredit?: number;
  status?: string;
  currency?: string;
  createdAt?: string;
};

export type CorporateStatement = {
  id: number;
  date?: string;
  description?: string;
  amount?: number;
  balance?: number;
};

export type CorporateSubAccount = {
  id: number;
  accountNumber?: string;
  label?: string;
  balance?: number;
  currency?: string;
  status?: string;
};

// Referral
export type ReferralInfo = {
  referralCode?: string;
  totalReferrals?: number;
  rewardsEarned?: number;
  currency?: string;
};

// Audit
export type AuditLog = {
  id: number;
  action?: string;
  entityType?: string;
  entityId?: string;
  performedBy?: string;
  ipAddress?: string;
  createdAt?: string;
  details?: string;
};

// Analytics
export type BranchDailyStat = {
  date?: string;
  transactionCount?: number;
  transactionVolume?: number;
  currency?: string;
  newCustomers?: number;
};

export type BranchHourlyStat = {
  hour?: number;
  transactionCount?: number;
  avgAmount?: number;
};

// Payout Reservations
export type PayoutReservation = {
  id: number;
  amount?: number;
  currency?: string;
  branchId?: number;
  status?: string;
  reservedUntil?: string;
  createdAt?: string;
};

// Sync Queue
export type SyncQueueItem = {
  id: number;
  deviceId?: string;
  entityType?: string;
  operation?: string;
  status?: string;
  conflictData?: unknown;
  createdAt?: string;
};

export type SyncDevice = {
  deviceId?: string;
  deviceName?: string;
  lastSeen?: string;
  pendingItems?: number;
  status?: string;
};

export type SyncStatus = {
  pendingCount?: number;
  lastSyncAt?: string;
  failedCount?: number;
  deviceName?: string;
};

export type SyncConflict = {
  id: number;
  conflictId?: string;
  deviceId?: string;
  entityType?: string;
  localData?: unknown;
  remoteData?: unknown;
  createdAt?: string;
};

// Fee Zones
export type FeeZone = {
  id: number;
  name?: string;
  description?: string;
  isDefault?: boolean;
};

export type FeeRule = {
  id: number;
  minAmount?: number;
  maxAmount?: number;
  feeType?: string;
  feeValue?: number;
  currency?: string;
};

// Users
export type User = {
  id: number;
  username?: string;
  fullName?: string;
  phone?: string;
  email?: string;
  role?: string;
  status?: string;
  branchId?: number;
  createdAt?: string;
  walletId?: number;
};

// Tenants
export type Tenant = {
  id: number;
  name?: string;
  domain?: string;
  status?: string;
  plan?: string;
  contactEmail?: string;
  createdAt?: string;
};

// Branches (admin CRUD)
export type Branch = {
  id: number;
  name?: string;
  city?: string;
  country?: string;
  addressLine?: string;
  phone?: string;
  latitude?: number;
  longitude?: number;
  opensAt?: string;
  closesAt?: string;
  services?: string;
};

export type BranchPayload = {
  name: string;
  city?: string;
  country?: string;
  addressLine?: string;
  phone?: string;
  latitude?: number;
  longitude?: number;
  opensAt?: string;
  closesAt?: string;
  services?: string;
};

// Commission Rates (platform & branch fee configuration)
export type CommissionScope =
  | 'PLATFORM_BASE_FEE'
  | 'PLATFORM_EXCHANGE_PROFIT'
  | 'WALLET_EXCHANGE'
  | 'SENDING_BRANCH_FEE'
  | 'RECEIVING_BRANCH_FEE';

export type CommissionRate = {
  id: number;
  branch?: { id?: number; name?: string };
  commissionScope?: CommissionScope;
  rateValue?: number;
};
