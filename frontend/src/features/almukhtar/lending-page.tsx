'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { apiClient } from '@/lib/api-client';
import { useCallback, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import { useSession } from './session';
import type { ActiveLoan, CreditProfile, LoanInstallment, LoanProduct } from './types';

const NUM_FMT = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });
const fmt = (v?: number | string, cur = '') =>
  `${NUM_FMT.format(Number(v ?? 0))}${cur ? ' ' + cur : ''}`;

const CURRENCIES = ['USD', 'EUR', 'SYP', 'TRY', 'SAR'];

type LoanApplication = {
  id: number;
  userId?: number;
  productId?: number;
  productName?: string;
  requestedAmount?: number;
  currency?: string;
  status?: string;
  createdAt?: string;
  notes?: string;
};

type LoanPortfolioSummary = Record<string, unknown>;

// ─── Credit Score Card ──────────────────────────────────────────────────────

function CreditScoreCard({ profile }: { profile: CreditProfile }) {
  const score = profile.creditScore ?? 0;
  const band = profile.scoreBand ?? '';
  const pct = Math.min(100, Math.round((score / 850) * 100));

  const color =
    score >= 700 ? 'bg-emerald-500' :
    score >= 500 ? 'bg-amber-500' :
    'bg-destructive';

  const variant =
    score >= 700 ? ('default' as const) :
    score >= 500 ? ('secondary' as const) :
    ('destructive' as const);

  return (
    <div className='gov-panel rounded-md p-4'>
      <div className='flex items-center justify-between mb-3'>
        <div className='text-xs text-muted-foreground font-medium'>درجة الائتمان</div>
        <Badge variant={variant} className='text-xs'>{band || 'غير محدد'}</Badge>
      </div>
      <div className='text-3xl font-bold tabular-nums mb-2'>{score}</div>
      <div className='h-2 bg-muted rounded-full overflow-hidden'>
        <div className={`h-full rounded-full transition-all ${color}`} style={{ width: `${pct}%` }} />
      </div>
      <div className='mt-2 flex justify-between text-xs text-muted-foreground'>
        <span>300</span>
        <span>850</span>
      </div>
    </div>
  );
}

// ─── Loan Product Card ──────────────────────────────────────────────────────

function LoanProductCard({
  product,
  selected,
  maxAllowed,
  onApply
}: {
  product: LoanProduct;
  selected: boolean;
  maxAllowed?: number;
  onApply: (p: LoanProduct) => void;
}) {
  const isEligible = !maxAllowed || Number(product.minAmount ?? 0) <= maxAllowed;

  return (
    <div className={`gov-panel rounded-md transition-all ${selected ? 'ring-2 ring-primary' : ''} ${!isEligible ? 'opacity-60' : ''}`}>
      <div className='gov-panel-header rounded-t-md px-4 py-3 flex items-center justify-between'>
        <h3 className='text-sm font-semibold'>{product.name}</h3>
        {!isEligible && <Badge variant='secondary' className='text-xs'>خارج حد الائتمان</Badge>}
      </div>
      <div className='p-4 space-y-2 text-sm'>
        <div className='flex justify-between'>
          <span className='text-muted-foreground'>الحد الأدنى</span>
          <span className='font-medium tabular-nums'>{fmt(product.minAmount, product.currency)}</span>
        </div>
        <div className='flex justify-between'>
          <span className='text-muted-foreground'>الحد الأقصى</span>
          <span className='font-medium tabular-nums'>{fmt(product.maxAmount, product.currency)}</span>
        </div>
        <div className='flex justify-between'>
          <span className='text-muted-foreground'>نسبة الفائدة</span>
          <span className='font-medium text-amber-600'>{product.interestRatePercent}% سنوياً</span>
        </div>
        <div className='flex justify-between'>
          <span className='text-muted-foreground'>المدة</span>
          <span className='font-medium'>{product.termMonths} شهر</span>
        </div>
        {product.description && (
          <p className='text-xs text-muted-foreground pt-1 border-t border-border'>{product.description}</p>
        )}
        <Button
          className='w-full h-8 rounded-sm mt-2'
          size='sm'
          disabled={!isEligible || !product.isActive}
          onClick={() => onApply(product)}
        >
          {product.isActive ? 'تقديم طلب' : 'غير متاح'}
        </Button>
      </div>
    </div>
  );
}

// ─── Apply Dialog ────────────────────────────────────────────────────────────

function ApplyDialog({
  product,
  maxAmount,
  onClose,
  onSuccess
}: {
  product: LoanProduct | null;
  maxAmount?: number;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [amount, setAmount] = useState(String(product?.minAmount ?? ''));
  const [currency, setCurrency] = useState(product?.currency ?? 'USD');
  const [submitting, setSubmitting] = useState(false);

  if (!product) return null;

  const max = Math.min(product.maxAmount, maxAmount ?? product.maxAmount);
  const min = product.minAmount ?? 0;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!product) return;
    const amt = Number(amount);
    if (amt < min || amt > max) {
      toast.error(`المبلغ يجب أن يكون بين ${fmt(min)} و ${fmt(max)} ${currency}`);
      return;
    }
    setSubmitting(true);
    try {
      await apiClient('/lending/apply', {
        method: 'POST',
        body: JSON.stringify({ productId: product.id, amount: amt, currency })
      });
      toast.success('✅ تم تقديم طلب القرض بنجاح، سيتم مراجعته قريباً.');
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تقديم طلب القرض');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open onOpenChange={(v) => { if (!v) onClose(); }}>
      <DialogContent className='max-w-md'>
        <DialogHeader>
          <DialogTitle>طلب قرض — {product.name}</DialogTitle>
          <DialogDescription>
            الفائدة: {product.interestRatePercent}% | المدة: {product.termMonths} شهر
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={(e) => { void handleSubmit(e); }} className='space-y-3 mt-2'>
          <div className='grid grid-cols-2 gap-3'>
            <div className='space-y-1.5'>
              <Label className='text-xs'>المبلغ المطلوب *</Label>
              <Input
                type='number'
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                min={min}
                max={max}
                step='0.01'
                className='h-9 rounded-sm'
                required
              />
              <p className='text-xs text-muted-foreground'>{fmt(min)} — {fmt(max)}</p>
            </div>
            <div className='space-y-1.5'>
              <Label className='text-xs'>العملة</Label>
              <Select value={currency} onValueChange={setCurrency}>
                <SelectTrigger className='h-9 rounded-sm'>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {CURRENCIES.map((c) => <SelectItem key={c} value={c}>{c}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Monthly installment estimate */}
          {Number(amount) > 0 && (
            <div className='rounded-sm bg-muted/40 border border-border px-3 py-2 text-xs'>
              <span className='text-muted-foreground'>القسط الشهري التقريبي: </span>
              <span className='font-semibold tabular-nums'>
                {fmt(
                  (Number(amount) * (1 + product.interestRatePercent / 100)) / product.termMonths,
                  currency
                )}
              </span>
            </div>
          )}

          <DialogFooter>
            <Button type='button' variant='outline' onClick={onClose} className='h-8 text-xs'>إلغاء</Button>
            <Button type='submit' className='h-8 text-xs' disabled={submitting}>
              {submitting && <Icons.spinner className='me-1.5 size-3 animate-spin' />}
              تقديم الطلب
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ─── Repay Dialog ────────────────────────────────────────────────────────────

function RepayDialog({
  loan,
  onClose,
  onSuccess
}: {
  loan: ActiveLoan;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!amount || Number(amount) <= 0) { toast.error('المبلغ مطلوب'); return; }
    setSubmitting(true);
    try {
      await apiClient(`/lending/my-loans/${loan.id}/repay`, {
        method: 'POST',
        body: JSON.stringify({ amount: Number(amount) })
      });
      toast.success(`تم تسجيل دفعة ${fmt(amount, loan.currency)} للقرض #${loan.id}`);
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تسجيل الدفعة');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open onOpenChange={(v) => { if (!v) onClose(); }}>
      <DialogContent className='max-w-sm'>
        <DialogHeader>
          <DialogTitle>سداد قسط — قرض #{loan.id}</DialogTitle>
          <DialogDescription>
            المتبقي: {fmt(loan.remainingBalance, loan.currency)}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={(e) => { void handleSubmit(e); }} className='space-y-3 mt-2'>
          <div className='space-y-1.5'>
            <Label className='text-xs'>مبلغ الدفعة ({loan.currency})</Label>
            <Input
              type='number'
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              max={loan.remainingBalance}
              min='0.01'
              step='0.01'
              className='h-9 rounded-sm'
              required
            />
          </div>
          <DialogFooter>
            <Button type='button' variant='outline' onClick={onClose} className='h-8 text-xs'>إلغاء</Button>
            <Button type='submit' className='h-8 text-xs' disabled={submitting}>
              {submitting && <Icons.spinner className='me-1.5 size-3 animate-spin' />}
              سداد القسط
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ─── Admin: Loan Applications ────────────────────────────────────────────────

function AdminApplicationsPanel() {
  const applications = useBackend<LoanApplication[]>('/lending/admin/applications');
  const [actioning, setActioning] = useState<number | null>(null);
  const [rejectNote, setRejectNote] = useState('');
  const [rejectId, setRejectId] = useState<number | null>(null);

  async function approve(appId: number) {
    setActioning(appId);
    try {
      await apiClient(`/lending/admin/applications/${appId}/approve`, { method: 'PUT' });
      toast.success('تم قبول طلب القرض');
      applications.refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل قبول الطلب');
    } finally {
      setActioning(null);
    }
  }

  async function reject(appId: number) {
    setActioning(appId);
    try {
      await apiClient(`/lending/admin/applications/${appId}/reject`, {
        method: 'PUT',
        body: JSON.stringify({ reason: rejectNote || 'مرفوض بواسطة المسؤول' })
      });
      toast.success('تم رفض طلب القرض');
      setRejectId(null);
      setRejectNote('');
      applications.refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل رفض الطلب');
    } finally {
      setActioning(null);
    }
  }

  async function disburse(appId: number) {
    setActioning(appId);
    try {
      await apiClient(`/lending/admin/applications/${appId}/disburse`, { method: 'POST' });
      toast.success('تم صرف القرض للمستفيد');
      applications.refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل صرف القرض');
    } finally {
      setActioning(null);
    }
  }

  return (
    <div className='space-y-4'>
      <div className='gov-panel rounded-md'>
        <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
          <div>
            <h2 className='text-sm font-semibold'>طلبات القروض للمراجعة</h2>
            <p className='text-muted-foreground text-xs'>
              {(applications.data ?? []).filter((a) => a.status === 'PENDING').length} طلب معلق
            </p>
          </div>
          <Button variant='ghost' size='sm' className='h-7 w-7 p-0' onClick={() => applications.refetch()}>
            <Icons.refresh className='size-3.5' />
          </Button>
        </div>
        <div className='overflow-x-auto'>
          <table className='gov-table'>
            <thead>
              <tr>
                <th>#</th>
                <th>المستخدم</th>
                <th>المنتج</th>
                <th>المبلغ</th>
                <th>الحالة</th>
                <th>التاريخ</th>
                <th>إجراءات</th>
              </tr>
            </thead>
            <tbody>
              {(applications.data ?? []).map((app) => (
                <tr key={app.id} className='hover:bg-muted/20 transition-colors'>
                  <td className='font-mono text-xs'>#{app.id}</td>
                  <td>#{app.userId ?? '—'}</td>
                  <td>{app.productName ?? `#${app.productId}`}</td>
                  <td className='tabular-nums font-medium'>{fmt(app.requestedAmount, app.currency ?? '')}</td>
                  <td>
                    <Badge
                      variant={
                        app.status === 'APPROVED' || app.status === 'DISBURSED' ? 'default'
                          : app.status === 'REJECTED' ? 'destructive'
                            : 'secondary'
                      }
                      className='text-xs'
                    >
                      {app.status === 'PENDING' ? 'معلق'
                        : app.status === 'APPROVED' ? 'مقبول'
                          : app.status === 'REJECTED' ? 'مرفوض'
                            : app.status === 'DISBURSED' ? 'مُصرف'
                              : app.status ?? '—'}
                    </Badge>
                  </td>
                  <td className='text-muted-foreground text-xs'>{app.createdAt?.split('T')[0] ?? '—'}</td>
                  <td>
                    {app.status === 'PENDING' && (
                      <div className='flex gap-1'>
                        <Button
                          size='sm'
                          className='h-6 px-2 text-xs bg-emerald-600 hover:bg-emerald-700'
                          onClick={() => { void approve(app.id); }}
                          disabled={actioning === app.id}
                        >
                          {actioning === app.id ? <Icons.spinner className='size-3 animate-spin' /> : 'قبول'}
                        </Button>
                        <Button
                          size='sm'
                          variant='destructive'
                          className='h-6 px-2 text-xs'
                          onClick={() => setRejectId(app.id)}
                          disabled={actioning === app.id}
                        >
                          رفض
                        </Button>
                      </div>
                    )}
                    {app.status === 'APPROVED' && (
                      <Button
                        size='sm'
                        className='h-6 px-2 text-xs'
                        onClick={() => { void disburse(app.id); }}
                        disabled={actioning === app.id}
                      >
                        {actioning === app.id ? <Icons.spinner className='size-3 animate-spin' /> : 'صرف'}
                      </Button>
                    )}
                    {(app.status === 'REJECTED' || app.status === 'DISBURSED') && (
                      <span className='text-muted-foreground text-xs'>—</span>
                    )}
                  </td>
                </tr>
              ))}
              {!applications.loading && !applications.data?.length && (
                <tr>
                  <td colSpan={7} className='text-center text-muted-foreground text-sm py-6'>
                    {applications.error ? `خطأ: ${applications.error}` : 'لا توجد طلبات معلقة.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Reject Reason Dialog */}
      {rejectId !== null && (
        <Dialog open onOpenChange={(v) => { if (!v) setRejectId(null); }}>
          <DialogContent className='max-w-sm'>
            <DialogHeader>
              <DialogTitle>رفض طلب #{rejectId}</DialogTitle>
              <DialogDescription>أدخل سبب الرفض (اختياري)</DialogDescription>
            </DialogHeader>
            <Textarea
              value={rejectNote}
              onChange={(e) => setRejectNote(e.target.value)}
              placeholder='سبب الرفض...'
              className='rounded-sm min-h-[80px]'
            />
            <DialogFooter>
              <Button variant='outline' onClick={() => setRejectId(null)} className='h-8 text-xs'>إلغاء</Button>
              <Button
                variant='destructive'
                className='h-8 text-xs'
                onClick={() => { void reject(rejectId); }}
                disabled={actioning === rejectId}
              >
                {actioning === rejectId && <Icons.spinner className='me-1.5 size-3 animate-spin' />}
                تأكيد الرفض
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}

// ─── Admin: Portfolio ─────────────────────────────────────────────────────────

function AdminPortfolioPanel() {
  const portfolio = useBackend<LoanPortfolioSummary>('/lending/admin/portfolio');
  const atRisk = useBackend<ActiveLoan[]>('/lending/admin/at-risk');

  return (
    <div className='space-y-4'>
      {portfolio.data && (
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>ملخص محفظة القروض</h2>
          </div>
          <div className='grid gap-3 p-4 grid-cols-2 md:grid-cols-4'>
            {Object.entries(portfolio.data).slice(0, 8).map(([k, v]) => (
              <div key={k} className='border border-border rounded-sm p-3'>
                <div className='text-xs text-muted-foreground'>{k}</div>
                <div className='font-semibold text-sm mt-1 tabular-nums'>{String(v)}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className='gov-panel rounded-md'>
        <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
          <div>
            <h2 className='text-sm font-semibold text-destructive'>قروض في خطر التأخر</h2>
            <p className='text-muted-foreground text-xs'>{atRisk.data?.length ?? 0} قرض</p>
          </div>
          {atRisk.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
        </div>
        <div className='overflow-x-auto'>
          <table className='gov-table'>
            <thead>
              <tr><th>#</th><th>المنتج</th><th>المبلغ الأصلي</th><th>المتبقي</th><th>الحالة</th><th>القسط القادم</th></tr>
            </thead>
            <tbody>
              {(atRisk.data ?? []).map((loan) => (
                <tr key={loan.id} className='bg-destructive/5'>
                  <td className='font-mono text-xs'>#{loan.id}</td>
                  <td>{loan.productName ?? '—'}</td>
                  <td className='tabular-nums'>{fmt(loan.principalAmount, loan.currency)}</td>
                  <td className='tabular-nums text-destructive font-semibold'>{fmt(loan.remainingBalance, loan.currency)}</td>
                  <td><Badge variant='destructive'>{loan.status}</Badge></td>
                  <td className='text-xs'>{loan.nextDueDate ?? '—'}</td>
                </tr>
              ))}
              {!atRisk.loading && !atRisk.data?.length && (
                <tr>
                  <td colSpan={6} className='text-center text-muted-foreground text-sm py-6'>
                    ✅ لا توجد قروض في خطر حالياً.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// ─── Main Export ─────────────────────────────────────────────────────────────

export function LendingPage() {
  const { session } = useSession();
  const isAdmin = useMemo(() => {
    const r = session?.role ?? '';
    return ['SUPER_ADMIN', 'ADMIN', 'OWNER', 'PLATFORM_OWNER', 'MOTHER_BRANCH_ADMIN', 'BRANCH_MANAGER'].includes(r);
  }, [session?.role]);

  const creditScore = useBackend<CreditProfile>('/lending/my-credit-score');
  const products = useBackend<LoanProduct[]>('/lending/products');
  const myLoans = useBackend<ActiveLoan[]>('/lending/my-loans');

  const [tab, setTab] = useState<'products' | 'loans' | 'admin-apps' | 'admin-portfolio'>('products');
  const [applyProduct, setApplyProduct] = useState<LoanProduct | null>(null);
  const [repayLoan, setRepayLoan] = useState<ActiveLoan | null>(null);
  const [scheduleData, setScheduleData] = useState<{ loanId: number; items: LoanInstallment[] } | null>(null);
  const [scheduleLoading, setScheduleLoading] = useState<number | null>(null);

  const maxLoanAmount = creditScore.data?.maxLoanAmount;

  const handleLoadSchedule = useCallback(async (loan: ActiveLoan) => {
    setScheduleLoading(loan.id);
    try {
      const items = await apiClient<LoanInstallment[]>(`/lending/my-loans/${loan.id}/schedule`);
      setScheduleData({ loanId: loan.id, items });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تحميل جدول السداد');
    } finally {
      setScheduleLoading(null);
    }
  }, []);

  const loanStatusVariant = (s?: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
    if (s === 'ACTIVE') return 'default';
    if (s === 'OVERDUE') return 'destructive';
    if (s === 'PAID_OFF') return 'secondary';
    return 'outline';
  };

  const loanStatusLabel: Record<string, string> = {
    ACTIVE: 'نشط', OVERDUE: 'متأخر', PAID_OFF: 'مسدد', PENDING: 'معلق'
  };

  const tabs = [
    { key: 'products' as const, label: 'منتجات القروض' },
    { key: 'loans' as const, label: 'قروضي' },
    ...(isAdmin ? [
      { key: 'admin-apps' as const, label: 'طلبات المراجعة' },
      { key: 'admin-portfolio' as const, label: 'محفظة القروض' }
    ] : [])
  ];

  return (
    <PageContainer
      pageTitle='القروض الصغيرة'
      pageDescription='درجة الائتمان، منتجات القروض، طلب تمويل، وسداد الأقساط.'
    >
      <div className='space-y-4'>
        {/* Credit Score Summary */}
        {creditScore.data && (
          <div className='grid gap-3 md:grid-cols-3'>
            <CreditScoreCard profile={creditScore.data} />
            <div className='gov-panel rounded-md p-4'>
              <div className='text-xs text-muted-foreground font-medium mb-1.5'>الحد الأقصى للتمويل</div>
              <div className='text-2xl font-bold tabular-nums'>
                {fmt(creditScore.data.maxLoanAmount, creditScore.data.currency ?? 'USD')}
              </div>
              <p className='text-xs text-muted-foreground mt-1'>بناءً على درجة ائتمانك الحالية</p>
            </div>
            <div className='gov-panel rounded-md p-4'>
              <div className='text-xs text-muted-foreground font-medium mb-1.5'>قروض نشطة</div>
              <div className='text-2xl font-bold tabular-nums'>
                {myLoans.data?.filter((l) => l.status === 'ACTIVE').length ?? '—'}
              </div>
              <p className='text-xs text-muted-foreground mt-1'>
                إجمالي: {fmt(myLoans.data?.reduce((s, l) => s + Number(l.remainingBalance ?? 0), 0))}
              </p>
            </div>
          </div>
        )}

        {/* Tabs */}
        <div className='flex flex-wrap gap-2'>
          {tabs.map(({ key, label }) => (
            <Button key={key} size='sm' variant={tab === key ? 'default' : 'outline'} className='h-8 text-xs'
              onClick={() => setTab(key)}>
              {label}
            </Button>
          ))}
        </div>

        {/* Products Tab */}
        {tab === 'products' && (
          <div className='grid gap-3 md:grid-cols-2 xl:grid-cols-3'>
            {products.loading && (
              <div className='col-span-full flex justify-center py-8'>
                <Icons.spinner className='size-6 animate-spin text-muted-foreground' />
              </div>
            )}
            {(products.data ?? []).map((p) => (
              <LoanProductCard
                key={p.id}
                product={p}
                selected={false}
                maxAllowed={maxLoanAmount}
                onApply={setApplyProduct}
              />
            ))}
            {!products.loading && !products.data?.length && (
              <p className='text-muted-foreground text-sm col-span-full py-4'>
                {products.error ? `خطأ: ${products.error}` : 'لا توجد منتجات قروض متاحة.'}
              </p>
            )}
          </div>
        )}

        {/* My Loans Tab */}
        {tab === 'loans' && (
          <div className='space-y-4'>
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                <div>
                  <h2 className='text-sm font-semibold'>قروضي</h2>
                  <p className='text-muted-foreground text-xs'>{myLoans.data?.length ?? 0} قرض</p>
                </div>
                <Button variant='ghost' size='sm' className='h-7 w-7 p-0' onClick={() => myLoans.refetch()}>
                  <Icons.refresh className='size-3.5' />
                </Button>
              </div>
              <div className='overflow-x-auto'>
                <table className='gov-table'>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>المنتج</th>
                      <th>المبلغ الأصلي</th>
                      <th>المتبقي</th>
                      <th>الحالة</th>
                      <th>القسط القادم</th>
                      <th>إجراءات</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(myLoans.data ?? []).map((loan) => (
                      <tr key={loan.id} className={`hover:bg-muted/20 transition-colors ${loan.status === 'OVERDUE' ? 'bg-destructive/5' : ''}`}>
                        <td className='font-mono text-xs'>#{loan.id}</td>
                        <td>{loan.productName ?? '—'}</td>
                        <td className='tabular-nums'>{fmt(loan.principalAmount, loan.currency)}</td>
                        <td className='tabular-nums font-medium'>{fmt(loan.remainingBalance, loan.currency)}</td>
                        <td>
                          <Badge variant={loanStatusVariant(loan.status)} className='text-xs'>
                            {loanStatusLabel[loan.status ?? ''] ?? loan.status ?? '—'}
                          </Badge>
                        </td>
                        <td className='text-xs'>{loan.nextDueDate ?? '—'}</td>
                        <td>
                          <div className='flex gap-1'>
                            <Button
                              size='sm'
                              variant='outline'
                              className='h-6 px-2 text-xs'
                              onClick={() => { void handleLoadSchedule(loan); }}
                              disabled={scheduleLoading === loan.id}
                            >
                              {scheduleLoading === loan.id
                                ? <Icons.spinner className='size-3 animate-spin' />
                                : 'الجدول'}
                            </Button>
                            {loan.status === 'ACTIVE' || loan.status === 'OVERDUE' ? (
                              <Button
                                size='sm'
                                className='h-6 px-2 text-xs bg-emerald-600 hover:bg-emerald-700'
                                onClick={() => setRepayLoan(loan)}
                              >
                                سداد
                              </Button>
                            ) : null}
                          </div>
                        </td>
                      </tr>
                    ))}
                    {!myLoans.loading && !myLoans.data?.length && (
                      <tr>
                        <td colSpan={7} className='text-center text-muted-foreground text-sm py-6'>
                          {myLoans.error ? `خطأ: ${myLoans.error}` : 'لا توجد قروض مسجلة.'}
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Installment Schedule */}
            {scheduleData && (
              <div className='gov-panel rounded-md'>
                <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                  <h2 className='text-sm font-semibold'>جدول السداد — قرض #{scheduleData.loanId}</h2>
                  <Button variant='ghost' size='sm' className='h-7 w-7 p-0' onClick={() => setScheduleData(null)}>
                    <Icons.close className='size-3.5' />
                  </Button>
                </div>
                <div className='overflow-x-auto'>
                  <table className='gov-table'>
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>تاريخ الاستحقاق</th>
                        <th>المبلغ الكلي</th>
                        <th>الأصل</th>
                        <th>الفائدة</th>
                        <th>الحالة</th>
                        <th>تاريخ الدفع</th>
                      </tr>
                    </thead>
                    <tbody>
                      {scheduleData.items.map((inst) => (
                        <tr
                          key={inst.id}
                          className={`hover:bg-muted/20 ${inst.status === 'PAID' ? 'opacity-60' : inst.status === 'OVERDUE' ? 'bg-destructive/5' : ''}`}
                        >
                          <td>{inst.id}</td>
                          <td>{inst.dueDate}</td>
                          <td className='tabular-nums font-medium'>{fmt(inst.amount)}</td>
                          <td className='tabular-nums'>{fmt(inst.principal)}</td>
                          <td className='tabular-nums text-amber-600'>{fmt(inst.interest)}</td>
                          <td>
                            <Badge
                              variant={inst.status === 'PAID' ? 'default' : inst.status === 'OVERDUE' ? 'destructive' : 'secondary'}
                              className='text-xs'
                            >
                              {inst.status === 'PAID' ? 'مدفوع' : inst.status === 'OVERDUE' ? 'متأخر' : 'قادم'}
                            </Badge>
                          </td>
                          <td className='text-xs text-muted-foreground'>{inst.paidAt ?? '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Admin Tabs */}
        {tab === 'admin-apps' && isAdmin && <AdminApplicationsPanel />}
        {tab === 'admin-portfolio' && isAdmin && <AdminPortfolioPanel />}
      </div>

      {/* Dialogs */}
      {applyProduct && (
        <ApplyDialog
          product={applyProduct}
          maxAmount={maxLoanAmount}
          onClose={() => setApplyProduct(null)}
          onSuccess={() => { myLoans.refetch(); setTab('loans'); }}
        />
      )}
      {repayLoan && (
        <RepayDialog
          loan={repayLoan}
          onClose={() => setRepayLoan(null)}
          onSuccess={() => myLoans.refetch()}
        />
      )}
    </PageContainer>
  );
}
