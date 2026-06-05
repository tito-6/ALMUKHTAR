'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { apiClient } from '@/lib/api-client';
import { useState } from 'react';
import { toast } from 'sonner';
import { currencyFlag, currencyFlagUrl, currencyName, currencySymbol, formatCurrency } from '@/lib/currencies';
import { CurrencyFlag } from '@/components/currency-flag';
import type { ExchangeRate, PageResponse, WalletBalance, WalletResponse, WalletTransaction } from './types';
import { useBackend } from './use-backend';

const NUM_FMT = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });

function fmt(n?: number | string, c = 'USD') {
  return formatCurrency(Number(n ?? 0), c);
}

function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

const CURRENCIES = ['USD', 'EUR', 'GBP', 'TRY'];

const TX_TYPE_LABEL: Record<string, string> = {
  TRANSFER_IN: 'تحويل وارد',
  TRANSFER_OUT: 'تحويل صادر',
  CASHOUT: 'سحب نقدي',
  TOPUP: 'إيداع',
  EXCHANGE: 'تحويل عملة',
  FEE: 'رسوم'
};

function BalanceCard({ balance }: { balance: WalletBalance }) {
  const isLocked = Number(balance.lockedBalance) > 0;
  const name = currencyName(balance.currencyCode);
  const symbol = currencySymbol(balance.currencyCode);
  return (
    <div className={`gov-panel rounded-md p-3 ${isLocked ? 'border-amber-500/30' : 'border-emerald-600/20'}`}>
      <div className='flex items-center justify-between mb-2'>
        <div className='flex items-center gap-1.5'>
          <CurrencyFlag code={balance.currencyCode} size={22} />
          <div className='text-xs font-bold tracking-widest'>{balance.currencyCode}</div>
        </div>
        <Badge variant={isLocked ? 'secondary' : 'outline'} className='text-xs'>
          {name}
        </Badge>
      </div>
      <div className='text-xl font-bold tabular-nums'>
        {symbol} {NUM_FMT.format(Number(balance.availableBalance ?? 0))}
      </div>
      {isLocked && (
        <div className='mt-1.5 text-xs text-amber-600'>
          🔒 مجمد: {symbol} {NUM_FMT.format(Number(balance.lockedBalance ?? 0))}
        </div>
      )}
    </div>
  );
}

function Field({
  id,
  label,
  value,
  onChange,
  type = 'text',
  placeholder = ''
}: {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  placeholder?: string;
}) {
  return (
    <div className='space-y-1'>
      <Label htmlFor={id} className='text-xs font-medium'>{label}</Label>
      <Input
        id={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className='h-8 rounded-sm text-sm'
        required
      />
    </div>
  );
}

export function WalletPage() {
  const wallet = useBackend<WalletResponse>('/wallet/my-wallet');
  const history = useBackend<PageResponse<WalletTransaction>>('/wallet/my-wallet/transactions?size=12');
  const rates = useBackend<ExchangeRate[]>('/exchange-rates');

  const [cashOut, setCashOut] = useState({ branchId: '1', amount: '25', currency: 'USD' });
  const [exchange, setExchange] = useState({ from: 'USD', to: 'EUR', amount: '100' });
  const [exchangeResult, setExchangeResult] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [tab, setTab] = useState<'cashout' | 'exchange'>('cashout');

  const balances = wallet.data?.balances ?? [];

  function getRate(from: string, to: string): number | null {
    if (!rates.data) return null;
    const r = rates.data.find((x) => x.fromCurrency === from && x.toCurrency === to);
    return r?.rate ?? null;
  }

  function previewExchange() {
    const rate = getRate(exchange.from, exchange.to);
    if (rate) {
      setExchangeResult(Number(exchange.amount) * rate);
    } else {
      setExchangeResult(null);
      toast.info('سعر الصرف غير متاح للزوج المحدد');
    }
  }

  async function submitCashOut(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      await apiClient('/wallet/cashout/request', {
        method: 'POST',
        headers: { 'X-Idempotency-Key': idempotencyKey() },
        body: JSON.stringify({
          branchId: Number(cashOut.branchId),
          amount: Number(cashOut.amount),
          currency: cashOut.currency
        })
      });
      toast.success('تم تسجيل طلب السحب النقدي بنجاح');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل طلب السحب');
    } finally {
      setSubmitting(false);
    }
  }

  async function submitExchange(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const result = await apiClient<{ convertedAmount?: number }>('/wallet/exchange', {
        method: 'POST',
        headers: { 'X-Idempotency-Key': idempotencyKey() },
        body: JSON.stringify({
          fromCurrency: exchange.from,
          toCurrency: exchange.to,
          amount: Number(exchange.amount)
        })
      });
      toast.success(`تم التحويل: ${fmt(result?.convertedAmount, exchange.to)}`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تحويل العملة');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <PageContainer
      pageTitle='المحافظ والخدمات المالية'
      pageDescription='أرصدة العملات المتعددة، تحويل الأموال، والسحب النقدي من الفروع.'
    >
      <div className='space-y-4'>
        {/* Wallet Header */}
        {wallet.data && (
          <div className='gov-panel rounded-md px-4 py-3 flex items-center justify-between gap-4'>
            <div className='flex items-center gap-4'>
              <div>
                <div className='text-xs text-muted-foreground'>رقم المحفظة</div>
                <div className='font-mono font-semibold text-sm'>{wallet.data.walletNumber}</div>
              </div>
              <div className='h-6 w-px bg-border' />
              <div>
                <div className='text-xs text-muted-foreground'>الحالة</div>
                <Badge variant={wallet.data.status === 'ACTIVE' ? 'default' : 'secondary'}>
                  {wallet.data.status}
                </Badge>
              </div>
              <div className='h-6 w-px bg-border' />
              <div>
                <div className='text-xs text-muted-foreground'>مستوى KYC</div>
                <div className='font-semibold text-sm'>{wallet.data.kycTier}</div>
              </div>
            </div>
            <div className='text-xs text-muted-foreground text-end'>
              <div>الحد اليومي: {fmt(wallet.data.dailyLimit)}</div>
              <div>الحد الشهري: {fmt(wallet.data.monthlyLimit)}</div>
            </div>
          </div>
        )}

        {/* Balance Cards */}
        <div className={`grid gap-3 ${balances.length <= 2 ? 'grid-cols-2' : 'grid-cols-2 xl:grid-cols-4'}`}>
          {balances.length > 0 ? (
            balances.map((b) => <BalanceCard key={b.currencyCode} balance={b} />)
          ) : (
            CURRENCIES.map((c) => (
              <BalanceCard
                key={c}
                balance={{ currencyCode: c, availableBalance: 0, lockedBalance: 0 }}
              />
            ))
          )}
        </div>

        <div className='grid gap-4 xl:grid-cols-[360px_1fr]'>
          {/* ── Action Panel ── */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-2'>
              <div className='flex gap-1'>
                <button
                  type='button'
                  onClick={() => setTab('cashout')}
                  className={`rounded-sm px-3 py-1.5 text-xs font-semibold transition-colors ${
                    tab === 'cashout' ? 'bg-foreground text-background' : 'text-muted-foreground hover:text-foreground'
                  }`}
                >
                  سحب نقدي
                </button>
                <button
                  type='button'
                  onClick={() => setTab('exchange')}
                  className={`rounded-sm px-3 py-1.5 text-xs font-semibold transition-colors ${
                    tab === 'exchange' ? 'bg-foreground text-background' : 'text-muted-foreground hover:text-foreground'
                  }`}
                >
                  تحويل عملة
                </button>
              </div>
            </div>

            {tab === 'cashout' ? (
              <form className='grid gap-3 p-4' onSubmit={(e) => { void submitCashOut(e); }}>
                <Field
                  id='cobranchId'
                  label='معرف الفرع'
                  value={cashOut.branchId}
                  onChange={(v) => setCashOut((p) => ({ ...p, branchId: v }))}
                  type='number'
                  placeholder='1'
                />
                <div className='grid grid-cols-2 gap-3'>
                  <Field
                    id='coamount'
                    label='المبلغ'
                    value={cashOut.amount}
                    onChange={(v) => setCashOut((p) => ({ ...p, amount: v }))}
                    type='number'
                    placeholder='25'
                  />
                  <div className='space-y-1'>
                    <Label className='text-xs font-medium'>العملة</Label>
                    <Select value={cashOut.currency} onValueChange={(v) => setCashOut((p) => ({ ...p, currency: v }))}>
                      <SelectTrigger className='h-8 rounded-sm text-sm'>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {CURRENCIES.map((c) => (
                          <SelectItem key={c} value={c}>
                            <span className='inline-flex items-center gap-1.5'>
                              {currencyFlagUrl(c)
                                ? <img src={currencyFlagUrl(c)!} alt={c} width={16} height={12} className='inline rounded-[1px]' />
                                : <span className='text-xs'>{currencyFlag(c)}</span>}
                              {c} — {currencyName(c)}
                            </span>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <Button type='submit' className='h-9 rounded-sm mt-1' disabled={submitting}>
                  {submitting ? <Icons.spinner className='me-2 size-4 animate-spin' /> : null}
                  تثبيت طلب السحب
                </Button>
              </form>
            ) : (
              <form className='grid gap-3 p-4' onSubmit={(e) => { void submitExchange(e); }}>
                <div className='grid grid-cols-2 gap-3'>
                  <div className='space-y-1'>
                    <Label className='text-xs font-medium'>من</Label>
                    <Select value={exchange.from} onValueChange={(v) => setExchange((p) => ({ ...p, from: v }))}>
                      <SelectTrigger className='h-8 rounded-sm text-sm'>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {CURRENCIES.map((c) => (
                          <SelectItem key={c} value={c}>
                            <span className='inline-flex items-center gap-1.5'>
                              {currencyFlagUrl(c)
                                ? <img src={currencyFlagUrl(c)!} alt={c} width={16} height={12} className='inline rounded-[1px]' />
                                : <span className='text-xs'>{currencyFlag(c)}</span>}
                              {c}
                            </span>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className='space-y-1'>
                    <Label className='text-xs font-medium'>إلى</Label>
                    <Select value={exchange.to} onValueChange={(v) => setExchange((p) => ({ ...p, to: v }))}>
                      <SelectTrigger className='h-8 rounded-sm text-sm'>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {CURRENCIES.map((c) => (
                          <SelectItem key={c} value={c}>
                            <span className='inline-flex items-center gap-1.5'>
                              {currencyFlagUrl(c)
                                ? <img src={currencyFlagUrl(c)!} alt={c} width={16} height={12} className='inline rounded-[1px]' />
                                : <span className='text-xs'>{currencyFlag(c)}</span>}
                              {c}
                            </span>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <Field
                  id='exchAmount'
                  label='المبلغ'
                  value={exchange.amount}
                  onChange={(v) => setExchange((p) => ({ ...p, amount: v }))}
                  type='number'
                />
                {exchangeResult !== null && (
                  <div className='bg-emerald-500/10 border border-emerald-700/20 rounded-sm px-3 py-2 text-sm'>
                    <span className='text-muted-foreground'>يعادل تقريباً: </span>
                    <span className='font-semibold tabular-nums'>{fmt(exchangeResult, exchange.to)}</span>
                  </div>
                )}
                <div className='grid grid-cols-2 gap-2'>
                  <Button type='button' variant='outline' className='h-8 rounded-sm text-xs' onClick={previewExchange}>
                    معاينة السعر
                  </Button>
                  <Button type='submit' className='h-8 rounded-sm text-xs' disabled={submitting}>
                    تنفيذ التحويل
                  </Button>
                </div>
              </form>
            )}
          </div>

          {/* ── Transaction History ── */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <div>
                <h2 className='text-sm font-semibold'>حركة الحساب</h2>
                <p className='text-muted-foreground text-xs'>
                  {history.data?.totalElements ?? 0} حركة | آخر {history.data?.content?.length ?? 0} عملية
                </p>
              </div>
              {history.error && <Badge variant='destructive'>تعذر التحميل</Badge>}
            </div>
            <div className='overflow-x-auto'>
              <table className='gov-table'>
                <thead>
                  <tr>
                    <th>النوع</th>
                    <th>المبلغ</th>
                    <th>العملة</th>
                    <th>الحالة</th>
                    <th>التاريخ</th>
                  </tr>
                </thead>
                <tbody>
                  {(history.data?.content ?? []).map((tx) => (
                    <tr key={tx.id} className='hover:bg-muted/20 transition-colors'>
                      <td className='font-medium'>
                        {TX_TYPE_LABEL[tx.type] ?? tx.type}
                      </td>
                      <td className='tabular-nums'>{fmt(tx.amount, tx.currencyCode)}</td>
                      <td>{tx.currencyCode}</td>
                      <td>
                        <Badge
                          variant={tx.status === 'COMPLETED' ? 'default' : tx.status === 'FAILED' ? 'destructive' : 'secondary'}
                          className='text-xs'
                        >
                          {tx.status ?? '—'}
                        </Badge>
                      </td>
                      <td className='text-muted-foreground text-xs'>{tx.createdAt ?? '—'}</td>
                    </tr>
                  ))}
                  {!history.loading && !history.data?.content?.length && (
                    <tr>
                      <td colSpan={5} className='text-center text-muted-foreground text-sm py-6'>
                        لا توجد حركات مسجلة.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
