'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { apiClient } from '@/lib/api-client';
import { useCallback, useMemo, useState } from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import { useSession } from './session';
import type { BillProvider } from './types';

function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

const CURRENCIES = ['USD', 'EUR', 'SYP', 'TRY', 'SAR', 'AED'];

type BillHistoryItem = {
  id: number;
  providerId?: number;
  providerName?: string;
  accountNumber?: string;
  accountReference?: string;
  amount?: number;
  currency?: string;
  status?: string;
  createdAt?: string;
};

type AdminBillItem = BillHistoryItem & {
  userId?: number;
};

const STATUS_LABEL: Record<string, string> = {
  COMPLETED: 'مكتملة', SUCCESS: 'ناجحة',
  FAILED: 'فاشلة', PENDING: 'معلقة',
  MANUAL_PROCESSING: 'تحت المعالجة'
};

const CATEGORY_ICONS: Record<string, string> = {
  ELECTRICITY: '⚡', WATER: '💧', GAS: '🔥',
  INTERNET: '📡', MOBILE: '📱', PHONE: '☎️',
  TV: '📺', INSURANCE: '🛡️', RENT: '🏠', OTHER: '🧾'
};

function categoryIcon(cat?: string) {
  if (!cat) return '🧾';
  const upper = cat.toUpperCase();
  for (const [k, v] of Object.entries(CATEGORY_ICONS)) {
    if (upper.includes(k)) return v;
  }
  return '🧾';
}

function statusVariant(s?: string): 'default' | 'secondary' | 'destructive' | 'outline' {
  if (s === 'COMPLETED' || s === 'SUCCESS') return 'default';
  if (s === 'FAILED') return 'destructive';
  if (s === 'PENDING' || s === 'MANUAL_PROCESSING') return 'secondary';
  return 'outline';
}

const NUM_FMT = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });

// ─── Provider Card ──────────────────────────────────────────────────────────

function ProviderCard({
  provider,
  selected,
  onClick
}: {
  provider: BillProvider;
  selected: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type='button'
      onClick={onClick}
      className={`w-full flex items-center gap-3 px-4 py-3 text-right transition-colors hover:bg-muted/40 ${
        selected ? 'bg-muted/60 border-l-2 border-primary' : ''
      } ${!provider.isActive ? 'opacity-50' : ''}`}
    >
      <span className='text-xl shrink-0'>{categoryIcon(provider.category)}</span>
      <div className='flex-1 min-w-0'>
        <div className='text-sm font-medium truncate'>{provider.name}</div>
        <div className='text-xs text-muted-foreground'>{provider.category}</div>
      </div>
      <Badge variant={provider.isActive ? 'outline' : 'secondary'} className='text-xs shrink-0'>
        {provider.isActive ? 'نشط' : 'متوقف'}
      </Badge>
    </button>
  );
}

// ─── Pay Form ───────────────────────────────────────────────────────────────

function PayBillForm({ provider, onSuccess }: { provider: BillProvider | null; onSuccess: () => void }) {
  const [form, setForm] = useState({ accountReference: '', amount: '', currency: 'USD' });
  const [submitting, setSubmitting] = useState(false);
  const set = (k: string, v: string) => setForm((p) => ({ ...p, [k]: v }));

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!provider) { toast.error('اختر مزوداً من القائمة أولاً'); return; }
    if (!provider.isActive) { toast.error('هذا المزود غير نشط حالياً'); return; }
    if (!form.accountReference.trim()) { toast.error('رقم الحساب مطلوب'); return; }
    if (!form.amount || Number(form.amount) <= 0) { toast.error('المبلغ يجب أن يكون أكبر من صفر'); return; }

    setSubmitting(true);
    try {
      await apiClient('/bills/pay', {
        method: 'POST',
        headers: { 'X-Idempotency-Key': idempotencyKey() },
        body: JSON.stringify({
          providerId: provider.id,
          accountReference: form.accountReference.trim(),
          amount: Number(form.amount),
          currency: form.currency
        })
      });
      toast.success(`✅ تم دفع فاتورة ${provider.name} بنجاح`);
      setForm({ accountReference: '', amount: '', currency: 'USD' });
      onSuccess();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل دفع الفاتورة');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className='gov-panel rounded-md'>
      <div className='gov-panel-header rounded-t-md px-4 py-3'>
        <h2 className='text-sm font-semibold'>
          {provider ? (
            <span className='flex items-center gap-2'>
              <span>{categoryIcon(provider.category)}</span>
              دفع فاتورة — {provider.name}
            </span>
          ) : (
            'اختر مزوداً من القائمة لدفع الفاتورة'
          )}
        </h2>
        {provider?.accountLabel && (
          <p className='text-muted-foreground text-xs mt-0.5'>{provider.accountLabel}</p>
        )}
      </div>
      <form className='grid gap-3 p-4 max-w-sm' onSubmit={(e) => { void handleSubmit(e); }}>
        <div className='space-y-1.5'>
          <Label htmlFor='billAccount' className='text-xs'>
            {provider?.accountLabel ?? 'رقم الحساب / المشترك'}
          </Label>
          <Input
            id='billAccount'
            value={form.accountReference}
            onChange={(e) => set('accountReference', e.target.value)}
            className='h-9 rounded-sm'
            placeholder='أدخل رقم المشترك...'
            required
            disabled={!provider}
          />
        </div>
        <div className='grid grid-cols-2 gap-3'>
          <div className='space-y-1.5'>
            <Label htmlFor='billAmount' className='text-xs'>المبلغ</Label>
            <Input
              id='billAmount'
              type='number'
              min='0.01'
              step='0.01'
              value={form.amount}
              onChange={(e) => set('amount', e.target.value)}
              className='h-9 rounded-sm'
              required
              disabled={!provider}
            />
          </div>
          <div className='space-y-1.5'>
            <Label className='text-xs'>العملة</Label>
            <Select value={form.currency} onValueChange={(v) => set('currency', v)} disabled={!provider}>
              <SelectTrigger className='h-9 rounded-sm'>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {CURRENCIES.map((c) => (
                  <SelectItem key={c} value={c}>{c}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {provider && !provider.isActive && (
          <div className='rounded-sm bg-amber-500/10 border border-amber-500/20 px-3 py-2 text-xs text-amber-700 dark:text-amber-400'>
            ⚠️ هذا المزود متوقف مؤقتاً ولا يمكن الدفع له الآن.
          </div>
        )}

        <Button type='submit' className='h-9 rounded-sm' disabled={!provider || submitting}>
          {submitting ? (
            <Icons.spinner className='me-2 size-4 animate-spin' />
          ) : (
            <Icons.check className='me-2 size-4' />
          )}
          دفع الفاتورة
        </Button>
      </form>
    </div>
  );
}

// ─── Main Page ───────────────────────────────────────────────────────────────

export function ServicesPage() {
  const { session } = useSession();
  const isAdmin = useMemo(() => {
    const r = session?.role ?? '';
    return ['SUPER_ADMIN', 'ADMIN', 'OWNER', 'PLATFORM_OWNER', 'MOTHER_BRANCH_ADMIN', 'BRANCH_MANAGER'].includes(r);
  }, [session?.role]);

  const providers = useBackend<BillProvider[]>('/bills/providers');
  const history = useBackend<BillHistoryItem[]>('/bills/my-history');
  const adminBills = useBackend<AdminBillItem[]>(isAdmin ? '/bills/admin' : null);

  const [selected, setSelected] = useState<BillProvider | null>(null);
  const [tab, setTab] = useState<'pay' | 'history' | 'admin'>('pay');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [historyKey, setHistoryKey] = useState(0);
  const [completing, setCompleting] = useState<number | null>(null);

  void historyKey;

  const categories = useMemo(() => {
    const cats = new Set<string>(['ALL']);
    (providers.data ?? []).forEach((p) => { if (p.category) cats.add(p.category); });
    return [...cats];
  }, [providers.data]);

  const filteredProviders = useMemo(() => {
    const list = providers.data ?? [];
    if (categoryFilter === 'ALL') return list;
    return list.filter((p) => p.category === categoryFilter);
  }, [providers.data, categoryFilter]);

  const handlePaySuccess = useCallback(() => {
    history.refetch();
    setHistoryKey((k) => k + 1);
    setTab('history');
  }, [history]);

  async function completeBill(id: number) {
    setCompleting(id);
    try {
      await apiClient(`/bills/admin/${id}/complete`, { method: 'PUT' });
      toast.success(`تم إتمام الفاتورة #${id}`);
      adminBills.refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إتمام الفاتورة');
    } finally {
      setCompleting(null);
    }
  }

  const tabs: { key: 'pay' | 'history' | 'admin'; label: string; adminOnly?: boolean }[] = [
    { key: 'pay', label: 'دفع فاتورة' },
    { key: 'history', label: 'سجل مدفوعاتي' },
    ...(isAdmin ? [{ key: 'admin' as const, label: 'إدارة الفواتير', adminOnly: true }] : [])
  ];

  return (
    <PageContainer
      pageTitle='الفواتير والخدمات'
      pageDescription='دفع فواتير الخدمات العامة، متابعة السجل، وإدارة المعالجة اليدوية.'
    >
      <div className='grid gap-4 xl:grid-cols-[280px_1fr]'>
        {/* ── Providers Sidebar ── */}
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>مزودو الخدمات</h2>
            <p className='text-muted-foreground text-xs'>
              {providers.data?.length ?? 0} مزود متاح
            </p>
          </div>

          {/* Category Filter */}
          {categories.length > 2 && (
            <div className='px-3 py-2 border-b border-border flex flex-wrap gap-1'>
              {categories.slice(0, 5).map((cat) => (
                <button
                  key={cat}
                  type='button'
                  onClick={() => setCategoryFilter(cat)}
                  className={`rounded-full px-2 py-0.5 text-xs border transition-colors ${
                    categoryFilter === cat
                      ? 'bg-foreground text-background border-foreground'
                      : 'border-border text-muted-foreground hover:border-foreground/50'
                  }`}
                >
                  {cat === 'ALL' ? 'الكل' : cat}
                </button>
              ))}
            </div>
          )}

          <div className='divide-y divide-border max-h-[500px] overflow-y-auto'>
            {filteredProviders.map((p) => (
              <ProviderCard
                key={p.id}
                provider={p}
                selected={selected?.id === p.id}
                onClick={() => { setSelected(p); setTab('pay'); }}
              />
            ))}
            {providers.loading && (
              <div className='flex justify-center py-6'>
                <Icons.spinner className='size-5 animate-spin text-muted-foreground' />
              </div>
            )}
            {!providers.loading && filteredProviders.length === 0 && (
              <div className='px-4 py-5 text-sm text-muted-foreground'>
                {providers.error ? 'تعذر تحميل المزودين' : 'لا توجد مزودو خدمات.'}
              </div>
            )}
          </div>
        </div>

        {/* ── Main Content ── */}
        <div className='space-y-4'>
          {/* Tabs */}
          <div className='flex flex-wrap gap-2'>
            {tabs.map(({ key, label }) => (
              <Button
                key={key}
                size='sm'
                variant={tab === key ? 'default' : 'outline'}
                className='h-8 text-xs'
                onClick={() => setTab(key)}
              >
                {label}
              </Button>
            ))}
          </div>

          {/* Pay Tab */}
          {tab === 'pay' && (
            <PayBillForm provider={selected} onSuccess={handlePaySuccess} />
          )}

          {/* History Tab */}
          {tab === 'history' && (
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                <div>
                  <h2 className='text-sm font-semibold'>سجل مدفوعاتي</h2>
                  <p className='text-muted-foreground text-xs'>
                    {history.data?.length ?? 0} عملية مسجلة
                  </p>
                </div>
                <div className='flex items-center gap-2'>
                  {history.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
                  <Button
                    variant='ghost'
                    size='sm'
                    className='h-7 w-7 p-0'
                    onClick={() => history.refetch()}
                  >
                    <Icons.refresh className='size-3.5' />
                  </Button>
                </div>
              </div>
              <div className='overflow-x-auto'>
                <table className='gov-table'>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>المزود</th>
                      <th>رقم الحساب</th>
                      <th>المبلغ</th>
                      <th>الحالة</th>
                      <th>التاريخ</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(history.data ?? []).map((item) => (
                      <tr key={item.id} className='hover:bg-muted/20 transition-colors'>
                        <td className='font-mono text-xs'>#{item.id}</td>
                        <td className='font-medium'>{item.providerName ?? `#${item.providerId}`}</td>
                        <td className='font-mono text-xs'>{item.accountReference ?? item.accountNumber ?? '—'}</td>
                        <td className='tabular-nums font-medium'>
                          {NUM_FMT.format(Number(item.amount ?? 0))} {item.currency}
                        </td>
                        <td>
                          <Badge variant={statusVariant(item.status)} className='text-xs'>
                            {STATUS_LABEL[item.status ?? ''] ?? item.status ?? '—'}
                          </Badge>
                        </td>
                        <td className='text-muted-foreground text-xs'>
                          {item.createdAt?.split('T')[0] ?? '—'}
                        </td>
                      </tr>
                    ))}
                    {!history.loading && !history.data?.length && (
                      <tr>
                        <td colSpan={6} className='text-center text-muted-foreground text-sm py-6'>
                          {history.error ? `خطأ: ${history.error}` : 'لا توجد مدفوعات مسجلة.'}
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Admin Tab */}
          {tab === 'admin' && isAdmin && (
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                <div>
                  <h2 className='text-sm font-semibold'>
                    فواتير تحت المعالجة اليدوية
                    <Badge variant='secondary' className='ms-2 text-xs'>صلاحية إدارية</Badge>
                  </h2>
                  <p className='text-muted-foreground text-xs'>
                    {adminBills.data?.length ?? 0} فاتورة بانتظار الإتمام
                  </p>
                </div>
                <Button
                  variant='ghost'
                  size='sm'
                  className='h-7 w-7 p-0'
                  onClick={() => adminBills.refetch()}
                >
                  <Icons.refresh className='size-3.5' />
                </Button>
              </div>
              <div className='overflow-x-auto'>
                <table className='gov-table'>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>المستخدم</th>
                      <th>المزود</th>
                      <th>الحساب</th>
                      <th>المبلغ</th>
                      <th>الحالة</th>
                      <th>التاريخ</th>
                      <th>إجراء</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(adminBills.data ?? []).map((bill) => (
                      <tr key={bill.id} className='hover:bg-muted/20 transition-colors'>
                        <td className='font-mono text-xs'>#{bill.id}</td>
                        <td>#{bill.userId ?? '—'}</td>
                        <td>{bill.providerName ?? `#${bill.providerId}`}</td>
                        <td className='font-mono text-xs'>{bill.accountReference ?? bill.accountNumber ?? '—'}</td>
                        <td className='tabular-nums'>{NUM_FMT.format(Number(bill.amount ?? 0))} {bill.currency}</td>
                        <td>
                          <Badge variant={statusVariant(bill.status)} className='text-xs'>
                            {STATUS_LABEL[bill.status ?? ''] ?? bill.status ?? '—'}
                          </Badge>
                        </td>
                        <td className='text-muted-foreground text-xs'>
                          {bill.createdAt?.split('T')[0] ?? '—'}
                        </td>
                        <td>
                          <Button
                            size='sm'
                            className='h-7 text-xs'
                            onClick={() => { void completeBill(bill.id); }}
                            disabled={completing === bill.id || bill.status === 'COMPLETED'}
                          >
                            {completing === bill.id ? (
                              <Icons.spinner className='size-3 animate-spin' />
                            ) : (
                              <Icons.check className='size-3' />
                            )}
                            <span className='ms-1'>إتمام</span>
                          </Button>
                        </td>
                      </tr>
                    ))}
                    {!adminBills.loading && !adminBills.data?.length && (
                      <tr>
                        <td colSpan={8} className='text-center text-muted-foreground text-sm py-6'>
                          {adminBills.error ? `خطأ: ${adminBills.error}` : '✅ لا توجد فواتير تحت المعالجة.'}
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </div>
    </PageContainer>
  );
}

export { ServicesPage as BillsPage };
