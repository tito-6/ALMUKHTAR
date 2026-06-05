'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { apiClient } from '@/lib/api-client';
import { FormEvent, useCallback, useState } from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import type {
  BranchVaultBalance,
  CashierShift,
  CashierShiftBalance,
  CashierShiftEntry,
  CashDrawerMovement,
  CashTransferOrder,
  ReconciliationReport,
} from './types';

const moneyFmt = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });
const fmt = (v?: number | string, cur = '') =>
  `${moneyFmt.format(Number(v ?? 0))}${cur ? ' ' + cur : ''}`;

function Field({
  id, label, value, onChange, type = 'text', placeholder, required,
}: {
  id: string; label: string; value: string; onChange: (v: string) => void;
  type?: string; placeholder?: string; required?: boolean;
}) {
  return (
    <div className='space-y-1.5'>
      <Label htmlFor={id} className='text-xs'>
        {label}{required && <span className='text-destructive ml-0.5'>*</span>}
      </Label>
      <Input id={id} type={type} value={value} placeholder={placeholder} required={required}
        onChange={(e) => onChange(e.target.value)} className='h-9 rounded-sm' />
    </div>
  );
}

/** Parses a currency-amount string like "USD:1000,SYP:500000" into a Record */
function parseCurrencyAmounts(raw: string): Record<string, number> {
  const result: Record<string, number> = {};
  if (!raw.trim()) return result;
  raw.split(',').forEach((pair) => {
    const [cur, amt] = pair.trim().split(':');
    if (cur && amt && !isNaN(Number(amt))) {
      result[cur.trim().toUpperCase()] = Number(amt.trim());
    }
  });
  return result;
}

const SHIFT_STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  OPEN: 'default',
  CLOSED: 'secondary',
  APPROVED: 'default',
  PENDING_APPROVAL: 'outline',
  REJECTED: 'destructive',
};

const SHIFT_STATUS_LABEL: Record<string, string> = {
  OPEN: 'مفتوحة',
  CLOSED: 'مغلقة',
  APPROVED: 'معتمدة',
  PENDING_APPROVAL: 'بانتظار الاعتماد',
  REJECTED: 'مرفوضة',
};

// ─── Cashier Shifts ────────────────────────────────────────────────────────────

export function CashierShiftsPage() {
  const [branchId, setBranchId] = useState('1');
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);
  const [viewMode, setViewMode] = useState<'open' | 'history'>('open');
  const [historyStatus, setHistoryStatus] = useState('');

  const openShifts = useBackend<CashierShift[]>(
    branchId && viewMode === 'open' ? `/cashier/shifts/open?branchId=${branchId}&_t=${tick}` : null
  );
  const allShifts = useBackend<CashierShift[]>(
    branchId && viewMode === 'history'
      ? `/cashier/shifts?branchId=${branchId}${historyStatus ? `&status=${historyStatus}` : ''}&_t=${tick}`
      : null
  );

  const displayedShifts = viewMode === 'open' ? (openShifts.data ?? []) : (allShifts.data ?? []);
  const isLoading = viewMode === 'open' ? openShifts.loading : allShifts.loading;

  // Open shift form — openingBalances as "USD:1000,SYP:500000"
  const [openForm, setOpenForm] = useState({ openingBalances: 'USD:0', notes: '' });
  const [opening, setOpening] = useState(false);

  // Close shift form
  const [closeForm, setCloseForm] = useState({ shiftId: '', countedBalances: '', notes: '' });
  const [closing, setClosing] = useState(false);

  const [selectedShift, setSelectedShift] = useState<number | null>(null);
  const shiftBalances = useBackend<CashierShiftBalance[]>(
    selectedShift ? `/cashier/shifts/${selectedShift}/balances?_t=${tick}` : null
  );
  const shiftEntries = useBackend<CashierShiftEntry[]>(
    selectedShift ? `/cashier/shifts/${selectedShift}/entries?_t=${tick}` : null
  );

  const [rejectForm, setRejectForm] = useState<{ id: number; reason: string } | null>(null);

  async function openShift(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!branchId) { toast.error('أدخل معرف الفرع'); return; }
    setOpening(true);
    try {
      await apiClient('/cashier/shifts/open', {
        method: 'POST',
        body: JSON.stringify({
          branchId: Number(branchId),
          openingBalances: parseCurrencyAmounts(openForm.openingBalances),
          notes: openForm.notes || undefined,
        })
      });
      toast.success('تم فتح الوردية بنجاح');
      setOpenForm({ openingBalances: 'USD:0', notes: '' });
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل فتح الوردية');
    } finally {
      setOpening(false);
    }
  }

  async function closeShift(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!closeForm.shiftId) { toast.error('أدخل معرف الوردية'); return; }
    setClosing(true);
    try {
      await apiClient('/cashier/shifts/close', {
        method: 'POST',
        body: JSON.stringify({
          shiftId: Number(closeForm.shiftId),
          countedBalances: parseCurrencyAmounts(closeForm.countedBalances),
          notes: closeForm.notes || undefined,
        })
      });
      toast.success('تم إغلاق الوردية بنجاح');
      setCloseForm({ shiftId: '', countedBalances: '', notes: '' });
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إغلاق الوردية');
    } finally {
      setClosing(false);
    }
  }

  async function approveShift(shiftId: number) {
    try {
      await apiClient(`/cashier/shifts/${shiftId}/approve`, { method: 'POST' });
      toast.success('تم اعتماد الوردية');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الاعتماد');
    }
  }

  async function rejectShift(shiftId: number, reason: string) {
    try {
      await apiClient(`/cashier/shifts/${shiftId}/reject`, {
        method: 'POST',
        body: JSON.stringify({ reason: reason || undefined }),
      });
      toast.success('تم رفض الوردية');
      setRejectForm(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الرفض');
    }
  }

  return (
    <PageContainer
      pageTitle='ورديات الصراف'
      pageDescription='إدارة ورديات الصرافين — فتح، إغلاق، اعتماد، رفض، ومراجعة الأرصدة.'
    >
      <div className='space-y-4'>
        {/* Header Controls */}
        <div className='flex flex-wrap items-center gap-3'>
          <Label className='text-xs whitespace-nowrap'>
            معرف الفرع <span className='text-destructive'>*</span>
          </Label>
          <Input value={branchId} onChange={(e) => { setBranchId(e.target.value); refresh(); }}
            className='h-8 w-28 rounded-sm' type='number' placeholder='1' />
          <div className='flex gap-1'>
            {(['open', 'history'] as const).map((m) => (
              <Button key={m} size='sm' variant={viewMode === m ? 'default' : 'outline'}
                className='h-8 text-xs' onClick={() => setViewMode(m)}>
                {m === 'open' ? 'المفتوحة' : 'السجل'}
              </Button>
            ))}
          </div>
          {viewMode === 'history' && (
            <select value={historyStatus}
              onChange={(e) => { setHistoryStatus(e.target.value); refresh(); }}
              className='h-8 rounded-sm border border-border bg-background px-2 text-xs'>
              <option value=''>كل الحالات</option>
              <option value='OPEN'>مفتوحة</option>
              <option value='CLOSED'>مغلقة</option>
              <option value='PENDING_APPROVAL'>بانتظار الاعتماد</option>
              <option value='APPROVED'>معتمدة</option>
              <option value='REJECTED'>مرفوضة</option>
            </select>
          )}
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
        </div>

        <div className='grid gap-4 xl:grid-cols-[320px_1fr]'>
          <div className='space-y-4'>
            {/* Open Shift */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>فتح وردية</h2>
              </div>
              <form className='grid gap-3 p-4' onSubmit={(e) => { void openShift(e); }}>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>أرصدة الافتتاح</Label>
                  <Input value={openForm.openingBalances}
                    onChange={(e) => setOpenForm((c) => ({ ...c, openingBalances: e.target.value }))}
                    className='h-9 rounded-sm font-mono text-sm' placeholder='USD:1000,SYP:500000' />
                  <p className='text-xs text-muted-foreground'>
                    صيغة: <span dir='ltr' className='font-mono'>عملة:مبلغ,عملة:مبلغ</span>
                  </p>
                </div>
                <Field id='openNotes' label='ملاحظات' value={openForm.notes}
                  onChange={(v) => setOpenForm((c) => ({ ...c, notes: v }))} placeholder='اختياري' />
                <Button type='submit' className='h-9 rounded-sm' disabled={opening}>
                  {opening && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                  <Icons.check className='ml-2 size-4' />
                  فتح الوردية
                </Button>
              </form>
            </div>

            {/* Close Shift */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>إغلاق وردية</h2>
              </div>
              <form className='grid gap-3 p-4' onSubmit={(e) => { void closeShift(e); }}>
                <Field id='closeShiftId' label='معرف الوردية' required value={closeForm.shiftId}
                  onChange={(v) => setCloseForm((c) => ({ ...c, shiftId: v }))} type='number' />
                <div className='space-y-1.5'>
                  <Label className='text-xs'>الأرصدة المحسوبة عند الإغلاق</Label>
                  <Input value={closeForm.countedBalances}
                    onChange={(e) => setCloseForm((c) => ({ ...c, countedBalances: e.target.value }))}
                    className='h-9 rounded-sm font-mono text-sm' placeholder='USD:990,SYP:499000' />
                </div>
                <Field id='closeNotes' label='ملاحظات الإغلاق' value={closeForm.notes}
                  onChange={(v) => setCloseForm((c) => ({ ...c, notes: v }))} placeholder='اختياري' />
                <Button type='submit' variant='destructive' className='h-9 rounded-sm' disabled={closing}>
                  {closing && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                  إغلاق الوردية
                </Button>
              </form>
            </div>
          </div>

          <div className='space-y-4'>
            {/* Shifts Table */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                <div>
                  <h2 className='text-sm font-semibold'>
                    {viewMode === 'open' ? 'الورديات المفتوحة' : 'سجل الورديات'}
                  </h2>
                  <p className='text-muted-foreground text-xs'>انقر على وردية لعرض الأرصدة والإدخالات</p>
                </div>
                {isLoading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
              </div>
              <div className='overflow-x-auto'>
                <table className='gov-table'>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>الفرع</th>
                      <th>الصراف</th>
                      <th>الحالة</th>
                      <th>وقت الفتح</th>
                      <th>وقت الإغلاق</th>
                      <th>إجراءات</th>
                    </tr>
                  </thead>
                  <tbody>
                    {displayedShifts.map((shift) => (
                      <tr key={shift.id} className={selectedShift === shift.id ? 'bg-muted/40' : ''}>
                        <td className='text-muted-foreground'>{shift.id}</td>
                        <td>{shift.branchId ?? '-'}</td>
                        <td>{shift.cashierId ?? '-'}</td>
                        <td>
                          <Badge variant={SHIFT_STATUS_VARIANT[shift.status ?? ''] ?? 'outline'}>
                            {SHIFT_STATUS_LABEL[shift.status ?? ''] ?? shift.status ?? '-'}
                          </Badge>
                        </td>
                        <td className='text-xs text-muted-foreground'>{shift.openedAt ?? '-'}</td>
                        <td className='text-xs text-muted-foreground'>{shift.closedAt ?? '-'}</td>
                        <td>
                          <div className='flex items-center gap-1 flex-wrap'>
                            <Button size='sm' variant='outline' className='h-7 text-xs'
                              onClick={() => setSelectedShift(shift.id === selectedShift ? null : shift.id)}>
                              {selectedShift === shift.id ? 'إخفاء' : 'تفاصيل'}
                            </Button>
                            {(shift.status === 'CLOSED' || shift.status === 'PENDING_APPROVAL') && (
                              <>
                                <Button size='sm' variant='outline'
                                  className='h-7 text-xs text-emerald-600 border-emerald-200'
                                  onClick={() => { void approveShift(shift.id); }}>
                                  اعتماد
                                </Button>
                                <Button size='sm' variant='ghost'
                                  className='h-7 text-xs text-destructive hover:text-destructive'
                                  onClick={() => setRejectForm({ id: shift.id, reason: '' })}>
                                  رفض
                                </Button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                    {!isLoading && !displayedShifts.length && (
                      <tr>
                        <td colSpan={7} className='py-8 text-center text-muted-foreground text-sm'>
                          لا توجد ورديات{viewMode === 'open' ? ' مفتوحة' : ''} لهذا الفرع.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Reject Form (inline) */}
            {rejectForm && (
              <div className='gov-panel rounded-md border-destructive/20 bg-destructive/5'>
                <div className='gov-panel-header rounded-t-md px-4 py-3 border-destructive/20'>
                  <h2 className='text-sm font-semibold text-destructive'>رفض الوردية #{rejectForm.id}</h2>
                </div>
                <div className='p-4 space-y-3'>
                  <div className='space-y-1.5'>
                    <Label className='text-xs'>سبب الرفض</Label>
                    <Input value={rejectForm.reason}
                      onChange={(e) => setRejectForm((r) => r ? { ...r, reason: e.target.value } : r)}
                      placeholder='سبب الرفض...' className='h-9 rounded-sm' />
                  </div>
                  <div className='flex gap-2'>
                    <Button size='sm' variant='destructive' className='h-8 text-xs rounded-sm'
                      onClick={() => { void rejectShift(rejectForm.id, rejectForm.reason); }}>
                      تأكيد الرفض
                    </Button>
                    <Button size='sm' variant='outline' className='h-8 text-xs rounded-sm'
                      onClick={() => setRejectForm(null)}>
                      إلغاء
                    </Button>
                  </div>
                </div>
              </div>
            )}

            {/* Shift Details */}
            {selectedShift && (
              <div className='grid gap-4 md:grid-cols-2'>
                <div className='gov-panel rounded-md'>
                  <div className='gov-panel-header rounded-t-md px-4 py-3'>
                    <h2 className='text-sm font-semibold'>أرصدة الوردية #{selectedShift}</h2>
                  </div>
                  <div className='overflow-x-auto'>
                    <table className='gov-table'>
                      <thead>
                        <tr>
                          <th>العملة</th>
                          <th>الافتتاح</th>
                          <th>الإغلاق</th>
                          <th>المتوقع</th>
                          <th>الفرق</th>
                        </tr>
                      </thead>
                      <tbody>
                        {(shiftBalances.data ?? []).map((b, i) => (
                          <tr key={i}>
                            <td className='font-medium'>{b.currencyCode ?? '-'}</td>
                            <td className='tabular-nums'>{fmt(b.openingBalance, b.currencyCode)}</td>
                            <td className='tabular-nums'>{fmt(b.closingBalance, b.currencyCode)}</td>
                            <td className='tabular-nums'>{fmt(b.expectedBalance, b.currencyCode)}</td>
                            <td className={`tabular-nums font-medium ${Number(b.difference) < 0 ? 'text-destructive' : Number(b.difference) > 0 ? 'text-emerald-600' : ''}`}>
                              {Number(b.difference) !== 0
                                ? `${Number(b.difference) > 0 ? '+' : ''}${fmt(b.difference, b.currencyCode)}`
                                : '—'}
                            </td>
                          </tr>
                        ))}
                        {shiftBalances.loading && (
                          <tr><td colSpan={5} className='text-center py-3'>
                            <Icons.spinner className='size-4 animate-spin inline text-muted-foreground' />
                          </td></tr>
                        )}
                        {!shiftBalances.loading && !shiftBalances.data?.length && (
                          <tr><td colSpan={5} className='text-muted-foreground text-sm py-4 text-center'>لا توجد أرصدة.</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>

                <div className='gov-panel rounded-md'>
                  <div className='gov-panel-header rounded-t-md px-4 py-3'>
                    <h2 className='text-sm font-semibold'>إدخالات الوردية #{selectedShift}</h2>
                    <p className='text-muted-foreground text-xs'>
                      {shiftEntries.data?.length ?? 0} إدخال
                    </p>
                  </div>
                  <div className='divide-y divide-border max-h-72 overflow-y-auto'>
                    {(shiftEntries.data ?? []).map((entry) => (
                      <div key={entry.id} className='flex items-center justify-between px-4 py-2.5'>
                        <div>
                          <div className='text-sm font-medium'>{entry.type ?? '-'}</div>
                          <div className='text-xs text-muted-foreground'>{entry.note ?? '-'}</div>
                          <div className='text-xs text-muted-foreground'>{entry.createdAt ?? ''}</div>
                        </div>
                        <Badge variant='outline' className='tabular-nums'>
                          {fmt(entry.amount, entry.currencyCode)}
                        </Badge>
                      </div>
                    ))}
                    {shiftEntries.loading && (
                      <div className='flex justify-center py-4'>
                        <Icons.spinner className='size-4 animate-spin text-muted-foreground' />
                      </div>
                    )}
                    {!shiftEntries.loading && !shiftEntries.data?.length && (
                      <div className='px-4 py-4 text-sm text-muted-foreground'>لا توجد إدخالات.</div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </PageContainer>
  );
}

// ─── Branch Cash ───────────────────────────────────────────────────────────────

const ORDER_STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  PENDING: 'secondary',
  APPROVED: 'default',
  DISPATCHED: 'default',
  RECEIVED: 'default',
  REJECTED: 'destructive',
  CANCELLED: 'outline',
};

const ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING: 'معلق',
  APPROVED: 'موافق عليه',
  DISPATCHED: 'تم الإرسال',
  RECEIVED: 'تم الاستلام',
  REJECTED: 'مرفوض',
  CANCELLED: 'ملغى',
};

export function BranchCashPage() {
  const [branchId, setBranchId] = useState('1');
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);

  const vaultBalance = useBackend<BranchVaultBalance[]>(
    branchId ? `/branch-cash/branches/${branchId}/vault?_t=${tick}` : null
  );
  const cashInventory = useBackend<Array<{
    currency?: string;
    availableBalance?: number;
    reservedBalance?: number;
    lowCashThreshold?: number;
    highCashThreshold?: number;
  }>>(
    branchId ? `/branch-cash/branches/${branchId}/inventory?_t=${tick}` : null
  );
  const transferOrders = useBackend<CashTransferOrder[]>(
    branchId ? `/branch-cash/branches/${branchId}/transfer-orders?_t=${tick}` : null
  );

  const [transferForm, setTransferForm] = useState({
    fromBranchId: '1', toBranchId: '2', amount: '', currency: 'USD', notes: ''
  });
  const [creatingOrder, setCreatingOrder] = useState(false);

  // Vault update form
  const [vaultForm, setVaultForm] = useState({ currency: 'USD', balance: '' });
  const [updatingVault, setUpdatingVault] = useState(false);

  // EOD reconciliation
  const [reconForm, setReconForm] = useState('USD:0');
  const [reconciling, setReconciling] = useState(false);
  const [reconResult, setReconResult] = useState<ReconciliationReport | null>(null);

  // Drawer movements
  const [drawerId, setDrawerId] = useState('');
  const drawerMovements = useBackend<CashDrawerMovement[]>(
    drawerId ? `/branch-cash/drawers/${drawerId}/movements?_t=${tick}` : null
  );
  const [adjForm, setAdjForm] = useState({ currency: 'USD', signedDelta: '', note: '' });

  // Reject order
  const [rejectOrderForm, setRejectOrderForm] = useState<{ id: number; reason: string } | null>(null);

  async function createTransferOrder(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!transferForm.amount) { toast.error('أدخل المبلغ'); return; }
    setCreatingOrder(true);
    try {
      await apiClient('/branch-cash/transfer-orders', {
        method: 'POST',
        body: JSON.stringify({
          fromBranchId: Number(transferForm.fromBranchId),
          toBranchId: Number(transferForm.toBranchId),
          amount: Number(transferForm.amount),
          currency: transferForm.currency,
          notes: transferForm.notes || undefined,
        })
      });
      toast.success('تم إنشاء أمر نقل النقدية');
      setTransferForm((c) => ({ ...c, amount: '', notes: '' }));
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إنشاء الأمر');
    } finally {
      setCreatingOrder(false);
    }
  }

  async function updateVaultBalance(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!vaultForm.balance) { toast.error('أدخل الرصيد'); return; }
    setUpdatingVault(true);
    try {
      await apiClient(
        `/branch-cash/branches/${branchId}/vault/${vaultForm.currency}?balance=${vaultForm.balance}`,
        { method: 'PUT' }
      );
      toast.success(`تم تحديث رصيد ${vaultForm.currency}`);
      setVaultForm((c) => ({ ...c, balance: '' }));
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التحديث');
    } finally {
      setUpdatingVault(false);
    }
  }

  async function reconcileEOD() {
    setReconciling(true);
    try {
      const physicalVault = parseCurrencyAmounts(reconForm);
      const result = await apiClient<ReconciliationReport>(
        `/branch-cash/branches/${branchId}/reconcile-eod`,
        { method: 'POST', body: JSON.stringify(physicalVault) }
      );
      setReconResult(result);
      toast.success('تمت مطابقة نهاية اليوم بنجاح');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشلت المطابقة');
    } finally {
      setReconciling(false);
    }
  }

  async function requestAdjustment() {
    if (!drawerId || !adjForm.signedDelta) { toast.error('يرجى إدخال البيانات'); return; }
    try {
      await apiClient(
        `/branch-cash/drawers/${drawerId}/adjustments?currency=${adjForm.currency}&signedDelta=${adjForm.signedDelta}&note=${encodeURIComponent(adjForm.note)}`,
        { method: 'POST' }
      );
      toast.success('تم طلب التسوية بنجاح');
      setAdjForm({ currency: 'USD', signedDelta: '', note: '' });
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل طلب التسوية');
    }
  }

  async function orderAction(id: number, action: 'approve' | 'dispatch' | 'receive') {
    const successMsgs: Record<string, string> = {
      approve: 'تمت الموافقة على الأمر',
      dispatch: 'تم إرسال الأمر',
      receive: 'تم استلام النقدية'
    };
    try {
      await apiClient(`/branch-cash/transfer-orders/${id}/${action}`, { method: 'POST' });
      toast.success(successMsgs[action]);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإجراء');
    }
  }

  async function rejectOrder(id: number, reason: string) {
    try {
      await apiClient(`/branch-cash/transfer-orders/${id}/reject`, {
        method: 'POST',
        body: JSON.stringify({ reason: reason || undefined }),
      });
      toast.success('تم رفض أمر النقل');
      setRejectOrderForm(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الرفض');
    }
  }

  const hasLowCash = (cashInventory.data ?? []).some(
    (inv) => inv.availableBalance != null && inv.lowCashThreshold != null
    && inv.availableBalance < inv.lowCashThreshold
  );

  return (
    <PageContainer
      pageTitle='نقدية الفروع'
      pageDescription='مراقبة خزينة الفرع، مخزون النقدية، أوامر نقل النقدية، ومطابقة نهاية اليوم.'
    >
      <div className='space-y-4'>
        <div className='flex items-center gap-3 flex-wrap'>
          <Label className='text-xs whitespace-nowrap'>معرف الفرع</Label>
          <Input value={branchId} onChange={(e) => { setBranchId(e.target.value); refresh(); }}
            className='h-8 w-28 rounded-sm' type='number' />
          {hasLowCash && (
            <div className='flex items-center gap-1.5 rounded-sm bg-amber-500/10 px-2.5 py-1 text-xs text-amber-700 dark:text-amber-400'>
              <Icons.warning className='size-3.5' />
              تنبيه: نقدية منخفضة في بعض العملات
            </div>
          )}
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
        </div>

        {/* Cash Inventory */}
        {(cashInventory.data ?? []).length > 0 && (
          <div className='grid gap-3 grid-cols-2 md:grid-cols-4'>
            {(cashInventory.data ?? []).map((inv, i) => {
              const isLow = inv.availableBalance != null && inv.lowCashThreshold != null
                && inv.availableBalance < inv.lowCashThreshold;
              const isHigh = inv.availableBalance != null && inv.highCashThreshold != null
                && inv.availableBalance > inv.highCashThreshold;
              return (
                <div key={i} className={`gov-panel rounded-md p-3 ${isLow ? 'border-amber-500/40' : ''}`}>
                  <div className='flex justify-between items-center mb-1'>
                    <span className='text-xs font-medium'>{inv.currency ?? '-'}</span>
                    {isLow && <Badge variant='secondary' className='text-xs text-amber-700 bg-amber-500/20'>منخفض</Badge>}
                    {isHigh && <Badge variant='outline' className='text-xs'>مرتفع</Badge>}
                  </div>
                  <div className='text-lg font-bold tabular-nums'>{fmt(inv.availableBalance)}</div>
                  <div className='text-xs text-muted-foreground'>
                    محجوز: {fmt(inv.reservedBalance ?? 0)}
                  </div>
                  {inv.lowCashThreshold != null && (
                    <div className='mt-1.5 h-1.5 bg-muted rounded-full overflow-hidden'>
                      <div
                        className={`h-full rounded-full transition-all ${isLow ? 'bg-amber-500' : 'bg-primary'}`}
                        style={{
                          width: `${Math.min(100, ((inv.availableBalance ?? 0) / Math.max(inv.highCashThreshold ?? 1, 1)) * 100)}%`
                        }}
                      />
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        <div className='grid gap-4 xl:grid-cols-[1fr_380px]'>
          {/* Vault Table */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>رصيد الخزينة</h2>
              {vaultBalance.loading && <Icons.spinner className='size-4 animate-spin' />}
            </div>
            <div className='overflow-x-auto'>
              <table className='gov-table'>
                <thead><tr><th>العملة</th><th>الرصيد</th><th>آخر مطابقة</th></tr></thead>
                <tbody>
                  {(vaultBalance.data ?? []).map((b, i) => (
                    <tr key={i}>
                      <td className='font-medium'>{b.currency ?? '-'}</td>
                      <td className='tabular-nums font-semibold'>{fmt(b.vaultBalance, b.currency)}</td>
                      <td className='text-xs text-muted-foreground'>{b.lastReconciledAt ?? '-'}</td>
                    </tr>
                  ))}
                  {!vaultBalance.loading && !vaultBalance.data?.length && (
                    <tr><td colSpan={3} className='text-muted-foreground text-sm py-4 text-center'>لا توجد بيانات.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className='space-y-4'>
            {/* Update Vault */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>تحديث رصيد الخزينة</h2>
              </div>
              <form className='grid gap-3 p-4' onSubmit={(e) => { void updateVaultBalance(e); }}>
                <div className='grid grid-cols-2 gap-2'>
                  <div className='space-y-1.5'>
                    <Label className='text-xs'>العملة</Label>
                    <select value={vaultForm.currency}
                      onChange={(e) => setVaultForm((c) => ({ ...c, currency: e.target.value }))}
                      className='h-9 w-full rounded-sm border border-border bg-background px-2 text-sm'>
                      <option>USD</option><option>SYP</option><option>EUR</option><option>TRY</option>
                    </select>
                  </div>
                  <Field id='vaultBal' label='الرصيد الجديد' value={vaultForm.balance}
                    onChange={(v) => setVaultForm((c) => ({ ...c, balance: v }))} type='number' required />
                </div>
                <Button type='submit' variant='outline' className='h-9 rounded-sm' disabled={updatingVault}>
                  {updatingVault && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                  تحديث الرصيد
                </Button>
              </form>
            </div>

            {/* Create Transfer Order */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>أمر نقل نقدية جديد</h2>
              </div>
              <form className='grid gap-3 p-4' onSubmit={(e) => { void createTransferOrder(e); }}>
                <div className='grid grid-cols-2 gap-3'>
                  <Field id='ctFrom' label='من فرع' value={transferForm.fromBranchId}
                    onChange={(v) => setTransferForm((c) => ({ ...c, fromBranchId: v }))} type='number' />
                  <Field id='ctTo' label='إلى فرع' value={transferForm.toBranchId}
                    onChange={(v) => setTransferForm((c) => ({ ...c, toBranchId: v }))} type='number' />
                </div>
                <div className='grid grid-cols-2 gap-3'>
                  <Field id='ctAmount' label='المبلغ' required value={transferForm.amount}
                    onChange={(v) => setTransferForm((c) => ({ ...c, amount: v }))} type='number' />
                  <div className='space-y-1.5'>
                    <Label className='text-xs'>العملة</Label>
                    <select value={transferForm.currency}
                      onChange={(e) => setTransferForm((c) => ({ ...c, currency: e.target.value }))}
                      className='h-9 w-full rounded-sm border border-border bg-background px-2 text-sm'>
                      <option>USD</option><option>SYP</option><option>EUR</option>
                    </select>
                  </div>
                </div>
                <Field id='ctNotes' label='ملاحظات' value={transferForm.notes}
                  onChange={(v) => setTransferForm((c) => ({ ...c, notes: v }))} placeholder='اختياري' />
                <Button type='submit' className='h-9 rounded-sm' disabled={creatingOrder}>
                  {creatingOrder && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                  إنشاء الأمر
                </Button>
              </form>
            </div>
          </div>
        </div>

        {/* Transfer Orders Table */}
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>أوامر نقل النقدية</h2>
            {transferOrders.loading && <Icons.spinner className='size-4 animate-spin' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>من</th>
                  <th>إلى</th>
                  <th>المبلغ</th>
                  <th>الحالة</th>
                  <th>التاريخ</th>
                  <th>إجراءات</th>
                </tr>
              </thead>
              <tbody>
                {(transferOrders.data ?? []).map((order) => (
                  <tr key={order.id}>
                    <td className='text-muted-foreground'>{order.id}</td>
                    <td>{order.fromBranchId ?? '-'}</td>
                    <td>{order.toBranchId ?? '-'}</td>
                    <td className='tabular-nums font-medium'>{fmt(order.amount, order.currency)}</td>
                    <td>
                      <Badge variant={ORDER_STATUS_VARIANT[order.status ?? ''] ?? 'outline'}>
                        {ORDER_STATUS_LABEL[order.status ?? ''] ?? order.status ?? '-'}
                      </Badge>
                    </td>
                    <td className='text-xs text-muted-foreground'>{order.createdAt ?? '-'}</td>
                    <td>
                      <div className='flex items-center gap-1 flex-wrap'>
                        {order.status === 'PENDING' && (
                          <>
                            <Button size='sm' variant='outline'
                              className='h-7 text-xs text-emerald-600 border-emerald-200'
                              onClick={() => { void orderAction(order.id, 'approve'); }}>
                              موافقة
                            </Button>
                            <Button size='sm' variant='ghost'
                              className='h-7 text-xs text-destructive hover:text-destructive'
                              onClick={() => setRejectOrderForm({ id: order.id, reason: '' })}>
                              رفض
                            </Button>
                          </>
                        )}
                        {order.status === 'APPROVED' && (
                          <Button size='sm' variant='outline' className='h-7 text-xs'
                            onClick={() => { void orderAction(order.id, 'dispatch'); }}>
                            إرسال
                          </Button>
                        )}
                        {order.status === 'DISPATCHED' && (
                          <Button size='sm' variant='outline' className='h-7 text-xs text-emerald-600'
                            onClick={() => { void orderAction(order.id, 'receive'); }}>
                            تأكيد الاستلام
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
                {!transferOrders.loading && !transferOrders.data?.length && (
                  <tr>
                    <td colSpan={7} className='py-8 text-center text-muted-foreground text-sm'>
                      لا توجد أوامر نقل لهذا الفرع.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Reject Order Form */}
        {rejectOrderForm && (
          <div className='gov-panel rounded-md border-destructive/20 bg-destructive/5'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold text-destructive'>رفض أمر النقل #{rejectOrderForm.id}</h2>
            </div>
            <div className='p-4 space-y-3'>
              <div className='space-y-1.5'>
                <Label className='text-xs'>سبب الرفض (اختياري)</Label>
                <Input value={rejectOrderForm.reason}
                  onChange={(e) => setRejectOrderForm((r) => r ? { ...r, reason: e.target.value } : r)}
                  placeholder='سبب الرفض...' className='h-9 rounded-sm' />
              </div>
              <div className='flex gap-2'>
                <Button size='sm' variant='destructive' className='h-8 text-xs rounded-sm'
                  onClick={() => { void rejectOrder(rejectOrderForm.id, rejectOrderForm.reason); }}>
                  تأكيد الرفض
                </Button>
                <Button size='sm' variant='outline' className='h-8 text-xs rounded-sm'
                  onClick={() => setRejectOrderForm(null)}>
                  إلغاء
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* EOD Reconciliation & Drawer */}
        <div className='grid gap-4 md:grid-cols-2'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>مطابقة نهاية اليوم</h2>
              <p className='text-muted-foreground text-xs'>مقارنة الرصيد المادي مع رصيد النظام</p>
            </div>
            <div className='grid gap-3 p-4'>
              <div className='space-y-1.5'>
                <Label className='text-xs'>الرصيد المادي المحسوب</Label>
                <Input value={reconForm} onChange={(e) => setReconForm(e.target.value)}
                  className='h-9 rounded-sm font-mono text-sm' placeholder='USD:1000,SYP:500000' />
                <p className='text-xs text-muted-foreground'>
                  صيغة: <span dir='ltr' className='font-mono'>عملة:مبلغ,عملة:مبلغ</span>
                </p>
              </div>
              <Button variant='outline' className='h-9 rounded-sm'
                onClick={() => { void reconcileEOD(); }} disabled={reconciling}>
                {reconciling && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                تنفيذ المطابقة
              </Button>
              {reconResult && (
                <div className='bg-muted/60 rounded-sm p-3 text-xs space-y-1'>
                  <div className='font-medium mb-2'>نتيجة المطابقة:</div>
                  {reconResult.differences && Object.entries(reconResult.differences).map(([cur, diff]) => (
                    <div key={cur} className='flex justify-between'>
                      <span className='font-mono'>{cur}</span>
                      <span className={diff < 0 ? 'text-destructive' : diff > 0 ? 'text-emerald-600' : ''}>
                        {diff > 0 ? '+' : ''}{fmt(diff)}
                      </span>
                    </div>
                  ))}
                  {(!reconResult.differences || !Object.keys(reconResult.differences).length) && (
                    <div className='text-emerald-600'>لا فروقات ✓</div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Drawer Movements */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>درج الصراف</h2>
              <p className='text-muted-foreground text-xs'>تسويات الدرج النقدي</p>
            </div>
            <div className='grid gap-3 p-4'>
              <Field id='drawerId' label='معرف الدرج' value={drawerId}
                onChange={(v) => { setDrawerId(v); if (v) refresh(); }} type='number'
                placeholder='أدخل معرف الدرج...' />
              {drawerId && (
                <>
                  <div className='grid grid-cols-3 gap-2'>
                    <div className='space-y-1.5'>
                      <Label className='text-xs'>العملة</Label>
                      <select value={adjForm.currency}
                        onChange={(e) => setAdjForm((c) => ({ ...c, currency: e.target.value }))}
                        className='h-9 w-full rounded-sm border border-border bg-background px-2 text-sm'>
                        <option>USD</option><option>SYP</option>
                      </select>
                    </div>
                    <Field id='adjDelta' label='التعديل (±)' value={adjForm.signedDelta}
                      onChange={(v) => setAdjForm((c) => ({ ...c, signedDelta: v }))} type='number'
                      placeholder='+/-100' />
                    <Field id='adjNote' label='الملاحظة' value={adjForm.note}
                      onChange={(v) => setAdjForm((c) => ({ ...c, note: v }))} />
                  </div>
                  <Button variant='outline' className='h-9 rounded-sm'
                    onClick={() => { void requestAdjustment(); }}>
                    طلب تسوية
                  </Button>
                  <div className='overflow-x-auto mt-1'>
                    <table className='gov-table'>
                      <thead>
                        <tr><th>العملة</th><th>الحركة</th><th>النوع</th><th>الحالة</th><th>التاريخ</th></tr>
                      </thead>
                      <tbody>
                        {(drawerMovements.data ?? []).map((m) => (
                          <tr key={m.id}>
                            <td>{m.currency ?? '-'}</td>
                            <td className={`tabular-nums ${Number(m.signedDelta) < 0 ? 'text-destructive' : 'text-emerald-600'}`}>
                              {Number(m.signedDelta) > 0 ? '+' : ''}{fmt(m.signedDelta)}
                            </td>
                            <td className='text-xs'>{m.type ?? '-'}</td>
                            <td><Badge variant='outline' className='text-xs'>{m.status ?? '-'}</Badge></td>
                            <td className='text-xs text-muted-foreground'>{m.createdAt ?? '-'}</td>
                          </tr>
                        ))}
                        {drawerMovements.loading && (
                          <tr><td colSpan={5} className='text-center py-2'>
                            <Icons.spinner className='size-4 animate-spin inline text-muted-foreground' />
                          </td></tr>
                        )}
                        {!drawerMovements.loading && !drawerMovements.data?.length && (
                          <tr><td colSpan={5} className='text-muted-foreground text-sm text-center py-3'>لا حركات.</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
