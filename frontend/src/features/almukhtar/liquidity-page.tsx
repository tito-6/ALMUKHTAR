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
import type { BranchSummary, BranchVaultBalance, CashTransferOrder, LiquidityAlert, LiquidityForecastPoint } from './types';

const NUM_FMT = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });
const fmt = (n?: number | string, c = '') => `${NUM_FMT.format(Number(n ?? 0))}${c ? ` ${c}` : ''}`;

const SEVERITY_LABEL: Record<string, string> = {
  LOW: 'منخفضة',
  MEDIUM: 'متوسطة',
  HIGH: 'عالية',
  CRITICAL: 'حرجة'
};

const STATUS_LABEL: Record<string, string> = {
  OPEN: 'مفتوح',
  RESOLVED: 'محلول',
  ACKNOWLEDGED: 'في المراجعة',
  ESCALATED: 'مُصعَّد'
};

function severityVariant(s?: string): 'default' | 'secondary' | 'destructive' | 'outline' {
  if (s === 'CRITICAL') return 'destructive';
  if (s === 'HIGH') return 'destructive';
  if (s === 'MEDIUM') return 'secondary';
  return 'outline';
}

function severityTone(s?: string): string {
  if (s === 'CRITICAL') return 'border-destructive/30 bg-destructive/5';
  if (s === 'HIGH') return 'border-orange-500/30 bg-orange-500/5';
  if (s === 'MEDIUM') return 'border-amber-500/30 bg-amber-500/5';
  return '';
}

const CURRENCIES = ['SYP', 'USD', 'EUR', 'TRY', 'SAR', 'AED'];

// ─── Alert Card ───────────────────────────────────────────────────────────────

function AlertCard({
  alert,
  onResolve
}: {
  alert: LiquidityAlert;
  onResolve: (id: number) => void;
}) {
  const [resolving, setResolving] = useState(false);

  async function handleResolve() {
    setResolving(true);
    try {
      await apiClient(`/liquidity/alerts/${alert.id}/resolve`, { method: 'POST' });
      toast.success('تم حل التنبيه بنجاح');
      onResolve(alert.id);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'تعذر حل التنبيه');
    } finally {
      setResolving(false);
    }
  }

  const isOpen = !alert.status || alert.status === 'OPEN';

  return (
    <div className={`gov-panel rounded-md p-3 ${severityTone(alert.severity)}`}>
      <div className='flex items-start justify-between gap-3'>
        <div className='flex-1 min-w-0'>
          <div className='flex items-center gap-2 mb-1.5'>
            <Badge variant={severityVariant(alert.severity)} className='text-xs'>
              {SEVERITY_LABEL[alert.severity ?? ''] ?? alert.severity ?? 'INFO'}
            </Badge>
            <Badge variant='outline' className='text-xs'>
              {STATUS_LABEL[alert.status ?? ''] ?? alert.status ?? 'OPEN'}
            </Badge>
            {alert.currency && (
              <span className='text-xs text-muted-foreground font-mono'>{alert.currency}</span>
            )}
          </div>
          <p className='text-sm font-medium truncate'>{alert.message ?? 'تنبيه سيولة'}</p>
          <div className='text-xs text-muted-foreground mt-1 flex items-center gap-3'>
            {alert.branchId && <span>الفرع #{alert.branchId}</span>}
            {alert.createdAt && <span>{alert.createdAt.split('T')[0]}</span>}
          </div>
        </div>
        {isOpen && (
          <Button
            variant='outline'
            size='sm'
            className='h-7 px-2 text-xs shrink-0'
            onClick={() => { void handleResolve(); }}
            disabled={resolving}
          >
            {resolving ? (
              <Icons.spinner className='size-3 animate-spin' />
            ) : (
              <Icons.check className='size-3' />
            )}
            <span className='ms-1'>حل</span>
          </Button>
        )}
      </div>
    </div>
  );
}

// ─── Cash Adjustment Dialog ───────────────────────────────────────────────────

function CashAdjustmentDialog({
  open,
  onClose,
  onSuccess
}: {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [form, setForm] = useState({
    branchId: '',
    currency: 'SYP',
    amount: '',
    type: 'ADD',
    reason: ''
  });
  const [submitting, setSubmitting] = useState(false);

  const set = (k: string, v: string) => setForm((p) => ({ ...p, [k]: v }));

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.branchId || !form.amount) {
      toast.error('يرجى تعبئة جميع الحقول الإلزامية');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient('/liquidity/branches/cash-adjustments', {
        method: 'POST',
        body: JSON.stringify({
          branchId: Number(form.branchId),
          currency: form.currency,
          amount: Number(form.amount),
          type: form.type,
          reason: form.reason || undefined
        })
      });
      toast.success('تم تسجيل تسوية النقد بنجاح');
      setForm({ branchId: '', currency: 'SYP', amount: '', type: 'ADD', reason: '' });
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تسجيل التسوية');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!v) onClose(); }}>
      <DialogContent className='max-w-md'>
        <DialogHeader>
          <DialogTitle>تسوية رصيد النقد</DialogTitle>
          <DialogDescription>
            تعديل رصيد النقد الفعلي في الفرع مع توثيق السبب.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={(e) => { void handleSubmit(e); }} className='space-y-3 mt-2'>
          <div className='grid grid-cols-2 gap-3'>
            <div className='space-y-1'>
              <Label className='text-xs'>معرف الفرع *</Label>
              <Input
                type='number'
                value={form.branchId}
                onChange={(e) => set('branchId', e.target.value)}
                placeholder='1'
                className='h-8 text-sm rounded-sm'
                required
              />
            </div>
            <div className='space-y-1'>
              <Label className='text-xs'>العملة</Label>
              <Select value={form.currency} onValueChange={(v) => set('currency', v)}>
                <SelectTrigger className='h-8 text-sm rounded-sm'>
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
          <div className='grid grid-cols-2 gap-3'>
            <div className='space-y-1'>
              <Label className='text-xs'>نوع التسوية</Label>
              <Select value={form.type} onValueChange={(v) => set('type', v)}>
                <SelectTrigger className='h-8 text-sm rounded-sm'>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value='ADD'>إضافة رصيد</SelectItem>
                  <SelectItem value='SUBTRACT'>خصم رصيد</SelectItem>
                  <SelectItem value='SET'>تحديد مباشر</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className='space-y-1'>
              <Label className='text-xs'>المبلغ *</Label>
              <Input
                type='number'
                value={form.amount}
                onChange={(e) => set('amount', e.target.value)}
                placeholder='0'
                min='0'
                className='h-8 text-sm rounded-sm'
                required
              />
            </div>
          </div>
          <div className='space-y-1'>
            <Label className='text-xs'>السبب / الملاحظة</Label>
            <Textarea
              value={form.reason}
              onChange={(e) => set('reason', e.target.value)}
              placeholder='سبب التسوية...'
              className='text-sm rounded-sm min-h-[60px]'
            />
          </div>
          <DialogFooter>
            <Button type='button' variant='outline' onClick={onClose} className='h-8 text-xs'>
              إلغاء
            </Button>
            <Button type='submit' className='h-8 text-xs' disabled={submitting}>
              {submitting && <Icons.spinner className='me-1.5 size-3 animate-spin' />}
              تثبيت التسوية
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ─── Cash Transfer Order Dialog ───────────────────────────────────────────────

function CashTransferDialog({
  open,
  onClose,
  onSuccess
}: {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [form, setForm] = useState({
    fromBranchId: '',
    toBranchId: '',
    amount: '',
    currency: 'SYP',
    note: ''
  });
  const [submitting, setSubmitting] = useState(false);

  const set = (k: string, v: string) => setForm((p) => ({ ...p, [k]: v }));

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!form.fromBranchId || !form.toBranchId || !form.amount) {
      toast.error('يرجى تعبئة جميع الحقول الإلزامية');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient('/branch-cash/transfer-orders', {
        method: 'POST',
        body: JSON.stringify({
          fromBranchId: Number(form.fromBranchId),
          toBranchId: Number(form.toBranchId),
          amount: Number(form.amount),
          currency: form.currency,
          note: form.note || undefined
        })
      });
      toast.success('تم إنشاء أمر تحويل النقد بنجاح');
      setForm({ fromBranchId: '', toBranchId: '', amount: '', currency: 'SYP', note: '' });
      onSuccess();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إنشاء أمر التحويل');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!v) onClose(); }}>
      <DialogContent className='max-w-md'>
        <DialogHeader>
          <DialogTitle>أمر تحويل نقدي بين الفروع</DialogTitle>
          <DialogDescription>
            نقل السيولة بين فرعين لتوازن الأرصدة.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={(e) => { void handleSubmit(e); }} className='space-y-3 mt-2'>
          <div className='grid grid-cols-2 gap-3'>
            <div className='space-y-1'>
              <Label className='text-xs'>من الفرع *</Label>
              <Input
                type='number'
                value={form.fromBranchId}
                onChange={(e) => set('fromBranchId', e.target.value)}
                placeholder='رقم الفرع'
                className='h-8 text-sm rounded-sm'
                required
              />
            </div>
            <div className='space-y-1'>
              <Label className='text-xs'>إلى الفرع *</Label>
              <Input
                type='number'
                value={form.toBranchId}
                onChange={(e) => set('toBranchId', e.target.value)}
                placeholder='رقم الفرع'
                className='h-8 text-sm rounded-sm'
                required
              />
            </div>
          </div>
          <div className='grid grid-cols-2 gap-3'>
            <div className='space-y-1'>
              <Label className='text-xs'>المبلغ *</Label>
              <Input
                type='number'
                value={form.amount}
                onChange={(e) => set('amount', e.target.value)}
                placeholder='0'
                min='0'
                className='h-8 text-sm rounded-sm'
                required
              />
            </div>
            <div className='space-y-1'>
              <Label className='text-xs'>العملة</Label>
              <Select value={form.currency} onValueChange={(v) => set('currency', v)}>
                <SelectTrigger className='h-8 text-sm rounded-sm'>
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
          <div className='space-y-1'>
            <Label className='text-xs'>ملاحظة</Label>
            <Input
              value={form.note}
              onChange={(e) => set('note', e.target.value)}
              placeholder='سبب التحويل...'
              className='h-8 text-sm rounded-sm'
            />
          </div>
          <DialogFooter>
            <Button type='button' variant='outline' onClick={onClose} className='h-8 text-xs'>
              إلغاء
            </Button>
            <Button type='submit' className='h-8 text-xs' disabled={submitting}>
              {submitting && <Icons.spinner className='me-1.5 size-3 animate-spin' />}
              إنشاء أمر التحويل
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ─── Branch Cash Lookup Panel ─────────────────────────────────────────────────

function BranchCashPanel() {
  const [branchId, setBranchId] = useState('');
  const [queriedId, setQueriedId] = useState<string | null>(null);

  const branchCash = useBackend<BranchVaultBalance[]>(
    queriedId ? `/liquidity/branches/${queriedId}/cash` : null
  );
  const forecast = useBackend<LiquidityForecastPoint[]>(
    queriedId ? `/liquidity/forecast/${queriedId}` : null
  );

  function handleQuery(e: React.FormEvent) {
    e.preventDefault();
    if (branchId.trim()) setQueriedId(branchId.trim());
  }

  return (
    <div className='gov-panel rounded-md'>
      <div className='gov-panel-header rounded-t-md px-4 py-3'>
        <h2 className='text-sm font-semibold'>فحص رصيد فرع</h2>
        <p className='text-muted-foreground text-xs'>استعلام عن أرصدة الخزنة والتوقعات لفرع محدد</p>
      </div>
      <div className='p-4 space-y-4'>
        <form onSubmit={handleQuery} className='flex items-end gap-2'>
          <div className='flex-1 space-y-1'>
            <Label className='text-xs'>معرف الفرع</Label>
            <Input
              type='number'
              value={branchId}
              onChange={(e) => setBranchId(e.target.value)}
              placeholder='أدخل رقم الفرع...'
              className='h-8 text-sm rounded-sm'
            />
          </div>
          <Button type='submit' className='h-8 text-xs'>
            <Icons.search className='me-1.5 size-3.5' />
            استعلام
          </Button>
        </form>

        {queriedId && (
          <div className='space-y-3'>
            {/* Vault Balances */}
            <div>
              <h3 className='text-xs font-semibold text-muted-foreground mb-2'>أرصدة الخزنة</h3>
              {branchCash.loading && (
                <div className='flex items-center gap-2 text-xs text-muted-foreground py-2'>
                  <Icons.spinner className='size-3 animate-spin' />
                  جارٍ التحميل...
                </div>
              )}
              {branchCash.error && (
                <div className='text-xs text-destructive'>{branchCash.error}</div>
              )}
              {branchCash.data && branchCash.data.length > 0 ? (
                <div className='grid gap-2 grid-cols-2'>
                  {branchCash.data.map((b, i) => (
                    <div key={i} className='border border-border rounded-sm p-2.5 bg-muted/20'>
                      <div className='text-xs text-muted-foreground'>{b.currency ?? '—'}</div>
                      <div className='font-semibold tabular-nums text-sm mt-0.5'>
                        {fmt(b.vaultBalance, b.currency ?? '')}
                      </div>
                      {b.lastReconciledAt && (
                        <div className='text-xs text-muted-foreground mt-1'>
                          {b.lastReconciledAt.split('T')[0]}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                !branchCash.loading && branchCash.data && (
                  <p className='text-xs text-muted-foreground'>لا توجد أرصدة مسجلة لهذا الفرع.</p>
                )
              )}
            </div>

            {/* Forecast */}
            {forecast.data && forecast.data.length > 0 && (
              <div>
                <h3 className='text-xs font-semibold text-muted-foreground mb-2'>توقعات السيولة</h3>
                <div className='overflow-x-auto'>
                  <table className='gov-table text-xs'>
                    <thead>
                      <tr>
                        <th>الفترة</th>
                        <th>الرصيد</th>
                        <th>العتبة</th>
                        <th>التوقع</th>
                      </tr>
                    </thead>
                    <tbody>
                      {forecast.data.map((pt, i) => (
                        <tr key={i} className={Number(pt.balance) < Number(pt.threshold ?? 0) ? 'bg-destructive/5' : ''}>
                          <td>{pt.label}</td>
                          <td className='tabular-nums font-medium'>{fmt(pt.balance)}</td>
                          <td className='tabular-nums text-muted-foreground'>{fmt(pt.threshold)}</td>
                          <td className='tabular-nums'>{fmt(pt.forecast)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Main Export ──────────────────────────────────────────────────────────────

export function LiquidityPage() {
  const { session } = useSession();
  const isAdmin = useMemo(() => {
    const r = session?.role ?? '';
    return ['SUPER_ADMIN', 'ADMIN', 'OWNER', 'PLATFORM_OWNER', 'MOTHER_BRANCH_ADMIN', 'BRANCH_MANAGER'].includes(r);
  }, [session?.role]);

  const [alertsKey, setAlertsKey] = useState(0);
  const [transferKey, setTransferKey] = useState(0);
  const [alertFilter, setAlertFilter] = useState<'ALL' | 'OPEN' | 'RESOLVED'>('OPEN');
  const [showAdjustDialog, setShowAdjustDialog] = useState(false);
  const [showTransferDialog, setShowTransferDialog] = useState(false);

  const alerts = useBackend<LiquidityAlert[]>('/liquidity/alerts');
  const [transferBranchId, setTransferBranchId] = useState<string | null>(null);
  const transferOrders = useBackend<CashTransferOrder[]>(
    transferBranchId ? `/branch-cash/branches/${transferBranchId}/transfer-orders?k=${transferKey}` : null
  );

  void alertsKey;

  const filteredAlerts = useMemo(() => {
    const list = alerts.data ?? [];
    if (alertFilter === 'ALL') return list;
    return list.filter((a) => (alertFilter === 'OPEN' ? (!a.status || a.status === 'OPEN') : a.status === 'RESOLVED'));
  }, [alerts.data, alertFilter]);

  const openCount = useMemo(() =>
    (alerts.data ?? []).filter((a) => !a.status || a.status === 'OPEN').length,
    [alerts.data]
  );

  const criticalCount = useMemo(() =>
    (alerts.data ?? []).filter((a) => (a.severity === 'HIGH' || a.severity === 'CRITICAL') && (!a.status || a.status === 'OPEN')).length,
    [alerts.data]
  );

  const handleAlertResolved = useCallback((id: number) => {
    void id;
    alerts.refetch();
    setAlertsKey((k) => k + 1);
  }, [alerts]);

  return (
    <PageContainer
      pageTitle='الفروع والسيولة'
      pageDescription='مراقبة أرصدة النقد، إنذارات السيولة، توقعات التدفق النقدي، وأوامر التحويل بين الفروع.'
      pageHeaderAction={
        isAdmin ? (
          <div className='flex gap-2'>
            <Button
              variant='outline'
              size='sm'
              onClick={() => setShowAdjustDialog(true)}
            >
              <Icons.edit className='me-1.5 size-4' />
              تسوية نقد
            </Button>
            <Button
              size='sm'
              onClick={() => setShowTransferDialog(true)}
            >
              <Icons.send className='me-1.5 size-4' />
              تحويل بين فروع
            </Button>
          </div>
        ) : undefined
      }
    >
      <div className='space-y-4'>
        {/* ── Summary Metrics ── */}
        <section className='grid gap-3 md:grid-cols-3'>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-muted-foreground text-xs mb-1.5'>إجمالي التنبيهات المفتوحة</div>
            <div className={`text-2xl font-bold tabular-nums ${openCount > 0 ? 'text-amber-600' : 'text-emerald-600'}`}>
              {openCount}
            </div>
            <div className='text-muted-foreground text-xs mt-1'>من أصل {alerts.data?.length ?? 0} تنبيه</div>
          </div>
          <div className={`gov-panel rounded-md p-3 ${criticalCount > 0 ? 'border-destructive/30 bg-destructive/5' : ''}`}>
            <div className='text-muted-foreground text-xs mb-1.5'>تنبيهات حرجة / عالية</div>
            <div className={`text-2xl font-bold tabular-nums ${criticalCount > 0 ? 'text-destructive' : ''}`}>
              {criticalCount}
            </div>
            <div className='text-muted-foreground text-xs mt-1'>تستوجب تدخلاً فورياً</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-muted-foreground text-xs mb-1.5'>أوامر التحويل النقدي</div>
            <div className='text-2xl font-bold tabular-nums'>
              {transferOrders.data?.length ?? '—'}
            </div>
            <div className='text-muted-foreground text-xs mt-1'>أمر تحويل بين الفروع</div>
          </div>
        </section>

        {/* ── Alerts Panel ── */}
        <section className='grid gap-4 xl:grid-cols-[1fr_360px]'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <div>
                <h2 className='text-sm font-semibold'>إنذارات السيولة</h2>
                <p className='text-muted-foreground text-xs'>
                  {filteredAlerts.length} تنبيه معروض
                </p>
              </div>
              <div className='flex items-center gap-2'>
                {alerts.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
                <div className='flex gap-1'>
                  {(['ALL', 'OPEN', 'RESOLVED'] as const).map((f) => (
                    <button
                      key={f}
                      type='button'
                      onClick={() => setAlertFilter(f)}
                      className={`rounded-full px-2.5 py-0.5 text-xs font-medium border transition-colors ${
                        alertFilter === f
                          ? 'bg-foreground text-background border-foreground'
                          : 'text-muted-foreground border-border hover:border-foreground/40'
                      }`}
                    >
                      {f === 'ALL' ? 'الكل' : f === 'OPEN' ? 'مفتوحة' : 'محلولة'}
                    </button>
                  ))}
                </div>
                <Button
                  variant='ghost'
                  size='sm'
                  className='h-7 w-7 p-0'
                  onClick={() => alerts.refetch()}
                  title='تحديث'
                >
                  <Icons.refresh className='size-3.5' />
                </Button>
              </div>
            </div>

            <div className='p-3 space-y-2 max-h-[480px] overflow-y-auto'>
              {filteredAlerts.map((alert) => (
                <AlertCard
                  key={alert.id}
                  alert={alert}
                  onResolve={handleAlertResolved}
                />
              ))}
              {!alerts.loading && filteredAlerts.length === 0 && (
                <div className='text-center text-muted-foreground text-sm py-8'>
                  {alerts.error
                    ? `تعذر تحميل التنبيهات: ${alerts.error}`
                    : alertFilter === 'OPEN'
                      ? '✅ لا توجد تنبيهات مفتوحة — السيولة تحت السيطرة'
                      : 'لا توجد تنبيهات بهذا الفلتر.'}
                </div>
              )}
            </div>
          </div>

          {/* ── Branch Cash Lookup ── */}
          <BranchCashPanel />
        </section>

        {/* ── Transfer Orders Table ── */}
        <section>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex flex-wrap items-center justify-between gap-3 rounded-t-md px-4 py-3'>
              <div>
                <h2 className='text-sm font-semibold'>أوامر التحويل النقدي بين الفروع</h2>
                <p className='text-muted-foreground text-xs'>
                  {transferOrders.data ? `${transferOrders.data.length} أمر مسجل` : 'اختر فرعاً للعرض'}
                </p>
              </div>
              <div className='flex items-center gap-2'>
                <Input
                  type='number'
                  value={transferBranchId ?? ''}
                  onChange={(e) => setTransferBranchId(e.target.value || null)}
                  placeholder='معرف الفرع...'
                  className='h-7 w-32 rounded-sm text-xs'
                />
                {transferOrders.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
                <Button
                  variant='ghost'
                  size='sm'
                  className='h-7 w-7 p-0'
                  onClick={() => setTransferKey((k) => k + 1)}
                  title='تحديث'
                  disabled={!transferBranchId}
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
                    <th>من الفرع</th>
                    <th>إلى الفرع</th>
                    <th>المبلغ</th>
                    <th>العملة</th>
                    <th>الحالة</th>
                    <th>التاريخ</th>
                  </tr>
                </thead>
                <tbody>
                  {(transferOrders.data ?? []).map((order) => (
                    <tr key={order.id} className='hover:bg-muted/20 transition-colors'>
                      <td className='font-mono text-xs'>#{order.id}</td>
                      <td>{order.fromBranchId ?? '—'}</td>
                      <td>{order.toBranchId ?? '—'}</td>
                      <td className='tabular-nums font-medium'>{fmt(order.amount)}</td>
                      <td className='font-mono text-xs'>{order.currency ?? '—'}</td>
                      <td>
                        <Badge
                          variant={
                            order.status === 'COMPLETED' ? 'default'
                              : order.status === 'CANCELLED' ? 'destructive'
                                : 'secondary'
                          }
                          className='text-xs'
                        >
                          {order.status ?? 'PENDING'}
                        </Badge>
                      </td>
                      <td className='text-muted-foreground text-xs'>
                        {order.createdAt?.split('T')[0] ?? '—'}
                      </td>
                    </tr>
                  ))}
                  {!transferOrders.loading && !transferOrders.data?.length && (
                    <tr>
                      <td colSpan={7} className='text-center text-muted-foreground text-sm py-6'>
                        {!transferBranchId
                          ? 'أدخل معرف الفرع في الأعلى لعرض أوامر التحويل.'
                          : transferOrders.error
                            ? `خطأ: ${transferOrders.error}`
                            : 'لا توجد أوامر تحويل لهذا الفرع.'}
                      </td>
                    </tr>
                  )}
                  {transferOrders.loading && (
                    <tr>
                      <td colSpan={7} className='text-center text-muted-foreground text-sm py-4'>
                        <Icons.spinner className='inline me-2 size-4 animate-spin' />
                        جارٍ التحميل...
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>

      {/* Dialogs */}
      <CashAdjustmentDialog
        open={showAdjustDialog}
        onClose={() => setShowAdjustDialog(false)}
        onSuccess={() => { alerts.refetch(); }}
      />
      <CashTransferDialog
        open={showTransferDialog}
        onClose={() => setShowTransferDialog(false)}
        onSuccess={() => setTransferKey((k) => k + 1)}
      />
    </PageContainer>
  );
}
