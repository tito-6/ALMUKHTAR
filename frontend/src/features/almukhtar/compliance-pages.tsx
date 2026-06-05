'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { apiClient } from '@/lib/api-client';
import { FormEvent, useCallback, useState } from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import type { AmlAlert, Dispute, FreezeCase, FreezeCaseEvent } from './types';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const moneyFmt = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });
const fmt = (v?: number | string) => moneyFmt.format(Number(v ?? 0));

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

function toArray<T>(raw: T[] | { content?: T[]; data?: T[] } | null | undefined): T[] {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw;
  const r = raw as { content?: T[]; data?: T[] };
  if (Array.isArray(r.content)) return r.content;
  if (Array.isArray(r.data)) return r.data;
  return [];
}

const SEVERITY_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  CRITICAL: 'destructive',
  HIGH: 'destructive',
  MEDIUM: 'secondary',
  LOW: 'outline',
};

const SEVERITY_LABEL: Record<string, string> = {
  CRITICAL: 'حرج',
  HIGH: 'عالٍ',
  MEDIUM: 'متوسط',
  LOW: 'منخفض',
};

const STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  OPEN: 'destructive',
  UNDER_REVIEW: 'secondary',
  RESOLVED: 'default',
  CLOSED: 'outline',
  ESCALATED: 'destructive',
  PENDING: 'secondary',
  IN_PROGRESS: 'secondary',
  REJECTED: 'outline',
  ACTIVE: 'destructive',
  LIFTED: 'default',
};

const STATUS_LABEL: Record<string, string> = {
  OPEN: 'مفتوح',
  UNDER_REVIEW: 'قيد المراجعة',
  RESOLVED: 'محلول',
  CLOSED: 'مغلق',
  ESCALATED: 'مُصعَّد',
  PENDING: 'معلق',
  IN_PROGRESS: 'قيد المعالجة',
  REJECTED: 'مرفوض',
  ACTIVE: 'نشط',
  LIFTED: 'مرفوع',
};

// ─── AML Alerts ────────────────────────────────────────────────────────────────

export function AmlPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);
  const [statusFilter, setStatusFilter] = useState('OPEN');
  const [severityFilter, setSeverityFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedAlert, setSelectedAlert] = useState<AmlAlert | null>(null);
  const [resolveForm, setResolveForm] = useState<{ id: number; note: string } | null>(null);

  const alerts = useBackend<AmlAlert[] | { content?: AmlAlert[] }>(
    `/aml/alerts?${statusFilter ? `status=${statusFilter}&` : ''}${severityFilter ? `severity=${severityFilter}&` : ''}_t=${tick}`
  );

  const alertList = toArray(alerts.data);

  const filteredAlerts = searchQuery.trim()
    ? alertList.filter((a) => {
        const q = searchQuery.toLowerCase();
        return (
          String(a.id).includes(q) ||
          String(a.transactionId ?? '').includes(q) ||
          String(a.userId ?? '').includes(q) ||
          (a.ruleTriggered ?? '').toLowerCase().includes(q) ||
          (a.alertType ?? '').toLowerCase().includes(q)
        );
      })
    : alertList;

  const statsMap = alertList.reduce<Record<string, number>>((acc, a) => {
    const s = a.severity ?? 'LOW';
    acc[s] = (acc[s] ?? 0) + 1;
    return acc;
  }, {});

  const statusCounts = alertList.reduce<Record<string, number>>((acc, a) => {
    const s = a.status ?? 'OPEN';
    acc[s] = (acc[s] ?? 0) + 1;
    return acc;
  }, {});

  async function investigate(id: number) {
    try {
      await apiClient(`/aml/alerts/${id}/investigate`, { method: 'POST' });
      toast.success('تم تعيين التنبيه للمراجعة');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإجراء');
    }
  }

  async function escalate(id: number) {
    try {
      await apiClient(`/aml/alerts/${id}/escalate`, { method: 'POST' });
      toast.success('تم تصعيد التنبيه');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التصعيد');
    }
  }

  async function resolve(id: number, resolutionNote: string) {
    try {
      await apiClient(`/aml/alerts/${id}/resolve`, {
        method: 'POST',
        body: JSON.stringify({ resolutionNote: resolutionNote || undefined }),
      });
      toast.success('تم إغلاق تنبيه AML');
      setResolveForm(null);
      setSelectedAlert(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإغلاق');
    }
  }

  async function dismiss(id: number) {
    try {
      await apiClient(`/aml/alerts/${id}/dismiss`, { method: 'POST' });
      toast.success('تم رفض التنبيه');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الرفض');
    }
  }

  return (
    <PageContainer
      pageTitle='تنبيهات AML'
      pageDescription='مراجعة تنبيهات مكافحة غسل الأموال، التحقيق فيها، تصعيدها أو إغلاقها.'
    >
      <div className='space-y-4'>
        {/* Severity Stats */}
        <div className='grid grid-cols-2 gap-3 md:grid-cols-5'>
          {[
            { key: 'CRITICAL', label: 'حرج', cls: 'text-destructive' },
            { key: 'HIGH', label: 'عالٍ', cls: 'text-orange-600' },
            { key: 'MEDIUM', label: 'متوسط', cls: 'text-amber-600' },
            { key: 'LOW', label: 'منخفض', cls: 'text-muted-foreground' },
            { key: '_total', label: 'الإجمالي', cls: 'text-foreground' },
          ].map(({ key, label, cls }) => (
            <button key={key}
              className={`gov-panel rounded-md p-3 text-center transition-all ${severityFilter === key && key !== '_total' ? 'ring-2 ring-primary/40' : ''}`}
              onClick={() => key !== '_total' && setSeverityFilter((s) => s === key ? '' : key)}>
              <div className={`text-2xl font-bold tabular-nums ${cls}`}>
                {key === '_total' ? alertList.length : (statsMap[key] ?? 0)}
              </div>
              <div className='text-xs text-muted-foreground mt-0.5'>{label}</div>
            </button>
          ))}
        </div>

        {/* Status Filter Tabs */}
        <div className='flex gap-1 border-b border-border pb-0 flex-wrap'>
          {[
            ['', 'الكل'],
            ['OPEN', 'مفتوح'],
            ['UNDER_REVIEW', 'قيد المراجعة'],
            ['ESCALATED', 'مُصعَّد'],
            ['RESOLVED', 'محلول'],
            ['CLOSED', 'مغلق'],
          ].map(([val, label]) => (
            <button key={val}
              className={`px-3 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
                statusFilter === val
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => setStatusFilter(val)}>
              {label}
              {val && (statusCounts[val] ?? 0) > 0 && (
                <span className='mr-1.5 rounded-full bg-muted px-1.5 py-0.5 text-xs'>
                  {statusCounts[val]}
                </span>
              )}
            </button>
          ))}
        </div>

        <div className='flex gap-2 flex-wrap items-center'>
          <Input value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
            placeholder='بحث: رقم التنبيه، المعاملة، المستخدم، القاعدة...'
            className='h-8 rounded-sm flex-1 min-w-60 text-xs' />
          {severityFilter && (
            <Button size='sm' variant='ghost' className='h-8 text-xs text-muted-foreground'
              onClick={() => setSeverityFilter('')}>
              مسح فلتر الشدة
            </Button>
          )}
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
        </div>

        {/* Resolve Form */}
        {resolveForm && (
          <div className='gov-panel rounded-md max-w-lg border-emerald-200 bg-emerald-50/50 dark:bg-emerald-950/10'>
            <div className='gov-panel-header rounded-t-md px-4 py-3 border-emerald-200'>
              <h2 className='text-sm font-semibold text-emerald-700 dark:text-emerald-400'>
                إغلاق التنبيه #{resolveForm.id}
              </h2>
            </div>
            <div className='p-4 space-y-3'>
              <div className='space-y-1.5'>
                <Label className='text-xs'>ملاحظة الإغلاق / نتيجة التحقيق</Label>
                <Textarea value={resolveForm.note}
                  onChange={(e) => setResolveForm((r) => r ? { ...r, note: e.target.value } : r)}
                  placeholder='نتيجة التحقيق وسبب الإغلاق...'
                  className='min-h-20 rounded-sm text-sm' />
              </div>
              <div className='flex gap-2'>
                <Button size='sm' className='h-8 text-xs rounded-sm bg-emerald-600 hover:bg-emerald-700 text-white'
                  onClick={() => { void resolve(resolveForm.id, resolveForm.note); }}>
                  <Icons.check className='ml-1 size-3.5' />
                  تأكيد الإغلاق
                </Button>
                <Button size='sm' variant='outline' className='h-8 text-xs rounded-sm'
                  onClick={() => setResolveForm(null)}>
                  إلغاء
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Alert Detail */}
        {selectedAlert && !resolveForm && (
          <div className='gov-panel rounded-md border-amber-200 bg-amber-50/40 dark:bg-amber-950/10'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3 border-amber-200'>
              <h2 className='text-sm font-semibold'>تفاصيل التنبيه #{selectedAlert.id}</h2>
              <Button size='sm' variant='ghost' className='h-7 text-xs'
                onClick={() => setSelectedAlert(null)}>
                إغلاق
              </Button>
            </div>
            <div className='p-4 grid gap-3 sm:grid-cols-2 text-sm'>
              {[
                ['القاعدة', selectedAlert.ruleTriggered ?? selectedAlert.alertType ?? '-'],
                ['نوع التنبيه', selectedAlert.alertType ?? '-'],
                ['المستخدم', selectedAlert.userId ? `#${selectedAlert.userId}` : '-'],
                ['المعاملة', selectedAlert.transactionId ? `#${selectedAlert.transactionId}` : '-'],
                ['الشدة', SEVERITY_LABEL[selectedAlert.severity ?? ''] ?? selectedAlert.severity ?? '-'],
                ['الحالة', STATUS_LABEL[selectedAlert.status ?? ''] ?? selectedAlert.status ?? '-'],
                ['التاريخ', selectedAlert.createdAt ?? '-'],
              ].map(([k, v]) => (
                <div key={k} className='flex justify-between gap-2'>
                  <span className='text-muted-foreground'>{k}:</span>
                  <span className='font-medium text-right'>{v}</span>
                </div>
              ))}
              {selectedAlert.details && (
                <div className='sm:col-span-2'>
                  <div className='text-muted-foreground mb-1 text-xs'>تفاصيل إضافية:</div>
                  <div className='bg-muted/60 rounded-sm p-2 text-xs font-mono whitespace-pre-wrap'>
                    {selectedAlert.details}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <div>
              <h2 className='text-sm font-semibold'>تنبيهات AML</h2>
              <p className='text-muted-foreground text-xs'>
                {filteredAlerts.length} تنبيه
                {statusFilter ? ` • ${STATUS_LABEL[statusFilter] ?? statusFilter}` : ''}
                {severityFilter ? ` • ${SEVERITY_LABEL[severityFilter] ?? severityFilter}` : ''}
              </p>
            </div>
            {alerts.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>المعاملة</th>
                  <th>المستخدم</th>
                  <th>القاعدة / النوع</th>
                  <th>الشدة</th>
                  <th>الحالة</th>
                  <th>التاريخ</th>
                  <th>إجراءات</th>
                </tr>
              </thead>
              <tbody>
                {filteredAlerts.map((alert) => (
                  <tr key={alert.id}
                    className={selectedAlert?.id === alert.id ? 'bg-amber-50/60 dark:bg-amber-950/10' : ''}>
                    <td className='text-muted-foreground'>{alert.id}</td>
                    <td>{alert.transactionId ? `#${alert.transactionId}` : '-'}</td>
                    <td>{alert.userId ? `#${alert.userId}` : '-'}</td>
                    <td className='max-w-44 truncate text-xs'>
                      {alert.ruleTriggered ?? alert.alertType ?? '-'}
                    </td>
                    <td>
                      <Badge variant={SEVERITY_VARIANT[alert.severity ?? ''] ?? 'outline'} className='text-xs'>
                        {SEVERITY_LABEL[alert.severity ?? ''] ?? alert.severity ?? '-'}
                      </Badge>
                    </td>
                    <td>
                      <Badge variant={STATUS_VARIANT[alert.status ?? ''] ?? 'outline'} className='text-xs'>
                        {STATUS_LABEL[alert.status ?? ''] ?? alert.status ?? '-'}
                      </Badge>
                    </td>
                    <td className='text-muted-foreground text-xs'>{alert.createdAt ?? '-'}</td>
                    <td>
                      <div className='flex items-center gap-1 flex-wrap'>
                        <Button size='sm' variant='outline' className='h-7 text-xs'
                          onClick={() => setSelectedAlert(
                            selectedAlert?.id === alert.id ? null : alert
                          )}>
                          {selectedAlert?.id === alert.id ? 'إخفاء' : 'تفاصيل'}
                        </Button>
                        {(alert.status === 'OPEN') && (
                          <Button size='sm' variant='ghost' className='h-7 text-xs text-blue-600'
                            onClick={() => { void investigate(alert.id); }}>
                            تحقيق
                          </Button>
                        )}
                        {(alert.status === 'OPEN' || alert.status === 'UNDER_REVIEW') && (
                          <>
                            <Button size='sm' variant='ghost'
                              className='h-7 text-xs text-amber-600'
                              onClick={() => { void escalate(alert.id); }}>
                              تصعيد
                            </Button>
                            <Button size='sm' variant='ghost'
                              className='h-7 text-xs text-emerald-600'
                              onClick={() => setResolveForm({ id: alert.id, note: '' })}>
                              إغلاق
                            </Button>
                            <Button size='sm' variant='ghost'
                              className='h-7 text-xs text-muted-foreground'
                              onClick={() => { void dismiss(alert.id); }}>
                              رفض
                            </Button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
                {!alerts.loading && !filteredAlerts.length && (
                  <tr>
                    <td colSpan={8} className='py-10 text-center text-muted-foreground text-sm'>
                      لا توجد تنبيهات{statusFilter ? ` بحالة "${STATUS_LABEL[statusFilter] ?? statusFilter}"` : ''}.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}

// ─── Disputes ─────────────────────────────────────────────────────────────────

export function DisputesPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);
  const [tab, setTab] = useState<'my' | 'all'>('my');
  const [statusFilter, setStatusFilter] = useState('');
  const [form, setForm] = useState({ transactionId: '', reason: '' });
  const [submitting, setSubmitting] = useState(false);
  const [selectedDispute, setSelectedDispute] = useState<Dispute | null>(null);

  type DisputeResponse = Dispute[] | { content?: Dispute[]; data?: Dispute[] };
  const myDisputes = useBackend<DisputeResponse>(
    `/disputes/my?${statusFilter ? `status=${statusFilter}&` : ''}_t=${tick}`
  );
  const allDisputes = useBackend<DisputeResponse>(
    tab === 'all' ? `/disputes?${statusFilter ? `status=${statusFilter}&` : ''}_t=${tick}` : null
  );

  const rawData = tab === 'my' ? myDisputes.data : allDisputes.data;
  const isLoading = tab === 'my' ? myDisputes.loading : allDisputes.loading;
  const disputes = toArray<Dispute>(rawData as Dispute[] | { content?: Dispute[]; data?: Dispute[] } | null);

  const statusCounts = toArray<Dispute>(
    (myDisputes.data ?? []) as Dispute[] | { content?: Dispute[]; data?: Dispute[] } | null
  ).reduce<Record<string, number>>((acc, d) => {
    const s = d.status ?? 'PENDING';
    acc[s] = (acc[s] ?? 0) + 1;
    return acc;
  }, {});

  // Admin actions
  const [assignForm, setAssignForm] = useState<{ id: number; assignee: string } | null>(null);
  const [resolveDisputeForm, setResolveDisputeForm] = useState<{
    id: number; action: 'resolve' | 'reject'; resolution: string;
  } | null>(null);

  async function openDispute(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!form.transactionId || !form.reason.trim()) {
      toast.error('يرجى ملء جميع الحقول المطلوبة');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient('/disputes', {
        method: 'POST',
        body: JSON.stringify({
          transactionId: Number(form.transactionId),
          reason: form.reason,
        }),
      });
      toast.success('تم رفع الاعتراض بنجاح');
      setForm({ transactionId: '', reason: '' });
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل رفع الاعتراض');
    } finally {
      setSubmitting(false);
    }
  }

  async function assignDispute(id: number, assignee: string) {
    try {
      await apiClient(`/disputes/${id}/assign`, {
        method: 'POST',
        body: JSON.stringify({ assignee }),
      });
      toast.success('تم تعيين المعالج');
      setAssignForm(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التعيين');
    }
  }

  async function resolveDispute(id: number, action: 'resolve' | 'reject', resolution: string) {
    try {
      await apiClient(`/disputes/${id}/${action}`, {
        method: 'POST',
        body: JSON.stringify({ resolution: resolution || undefined }),
      });
      toast.success(action === 'resolve' ? 'تم حل الاعتراض' : 'تم رفض الاعتراض');
      setResolveDisputeForm(null);
      setSelectedDispute(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإجراء');
    }
  }

  return (
    <PageContainer
      pageTitle='الاعتراضات والمطالبات'
      pageDescription='رفع اعتراض على معاملة، متابعة الحالة، ومعالجة الاعتراضات (للمشرف).'
    >
      <div className='grid gap-4 xl:grid-cols-[360px_1fr]'>
        {/* Form */}
        <div className='space-y-4'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>فتح اعتراض جديد</h2>
              <p className='text-muted-foreground text-xs'>اعترض على معاملة تعتقد أنها خاطئة</p>
            </div>
            <form className='grid gap-3 p-4' onSubmit={(e) => { void openDispute(e); }}>
              <Field id='disputeTxId' label='رقم المعاملة' required value={form.transactionId}
                onChange={(v) => setForm((c) => ({ ...c, transactionId: v }))} type='number'
                placeholder='مثال: 1234' />
              <div className='space-y-1.5'>
                <Label htmlFor='disputeReason' className='text-xs'>
                  سبب الاعتراض <span className='text-destructive'>*</span>
                </Label>
                <Textarea id='disputeReason' value={form.reason}
                  onChange={(e) => setForm((c) => ({ ...c, reason: e.target.value }))}
                  placeholder='اشرح سبب الاعتراض بالتفصيل...'
                  className='min-h-28 rounded-sm text-sm' />
              </div>
              <Button type='submit' className='h-9 rounded-sm' disabled={submitting}>
                {submitting && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                <Icons.flag className='ml-2 size-4' />
                رفع الاعتراض
              </Button>
            </form>
          </div>

          {/* Stats mini cards */}
          <div className='grid grid-cols-3 gap-2'>
            {[
              ['PENDING', 'معلق', 'text-amber-600'],
              ['IN_PROGRESS', 'قيد المعالجة', 'text-blue-600'],
              ['RESOLVED', 'محلول', 'text-emerald-600'],
            ].map(([s, label, cls]) => (
              <div key={s} className='gov-panel rounded-md p-3 text-center'>
                <div className={`text-xl font-bold tabular-nums ${cls}`}>{statusCounts[s] ?? 0}</div>
                <div className='text-xs text-muted-foreground mt-0.5'>{label}</div>
              </div>
            ))}
          </div>
        </div>

        {/* List */}
        <div className='space-y-4'>
          {/* Inline action forms */}
          {assignForm && (
            <div className='gov-panel rounded-md border-blue-200 bg-blue-50/40 dark:bg-blue-950/10 max-w-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3 border-blue-200'>
                <h2 className='text-sm font-semibold text-blue-700'>تعيين معالج للاعتراض #{assignForm.id}</h2>
              </div>
              <div className='p-4 space-y-3'>
                <Field id='assignee' label='اسم المعالج / المعرف' value={assignForm.assignee}
                  onChange={(v) => setAssignForm((r) => r ? { ...r, assignee: v } : r)}
                  placeholder='مثال: ahmed.ali' />
                <div className='flex gap-2'>
                  <Button size='sm' className='h-8 text-xs rounded-sm'
                    onClick={() => { void assignDispute(assignForm.id, assignForm.assignee); }}>
                    تعيين
                  </Button>
                  <Button size='sm' variant='outline' className='h-8 text-xs rounded-sm'
                    onClick={() => setAssignForm(null)}>
                    إلغاء
                  </Button>
                </div>
              </div>
            </div>
          )}

          {resolveDisputeForm && (
            <div className={`gov-panel rounded-md max-w-md ${
              resolveDisputeForm.action === 'resolve'
                ? 'border-emerald-200 bg-emerald-50/40 dark:bg-emerald-950/10'
                : 'border-destructive/20 bg-destructive/5'
            }`}>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className={`text-sm font-semibold ${resolveDisputeForm.action === 'resolve' ? 'text-emerald-700' : 'text-destructive'}`}>
                  {resolveDisputeForm.action === 'resolve' ? 'حل' : 'رفض'} الاعتراض #{resolveDisputeForm.id}
                </h2>
              </div>
              <div className='p-4 space-y-3'>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>القرار / ملاحظات</Label>
                  <Textarea value={resolveDisputeForm.resolution}
                    onChange={(e) => setResolveDisputeForm((r) => r ? { ...r, resolution: e.target.value } : r)}
                    placeholder='أدخل القرار والسبب...'
                    className='min-h-20 rounded-sm text-sm' />
                </div>
                <div className='flex gap-2'>
                  <Button size='sm'
                    className={`h-8 text-xs rounded-sm ${resolveDisputeForm.action === 'resolve' ? 'bg-emerald-600 hover:bg-emerald-700 text-white' : ''}`}
                    variant={resolveDisputeForm.action === 'reject' ? 'destructive' : 'default'}
                    onClick={() => {
                      void resolveDispute(
                        resolveDisputeForm.id,
                        resolveDisputeForm.action,
                        resolveDisputeForm.resolution
                      );
                    }}>
                    <Icons.check className='ml-1 size-3.5' />
                    {resolveDisputeForm.action === 'resolve' ? 'تأكيد الحل' : 'تأكيد الرفض'}
                  </Button>
                  <Button size='sm' variant='outline' className='h-8 text-xs rounded-sm'
                    onClick={() => setResolveDisputeForm(null)}>
                    إلغاء
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* Detail expand */}
          {selectedDispute && !resolveDisputeForm && !assignForm && (
            <div className='gov-panel rounded-md border-muted'>
              <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>تفاصيل الاعتراض #{selectedDispute.id}</h2>
                <Button size='sm' variant='ghost' className='h-7 text-xs'
                  onClick={() => setSelectedDispute(null)}>
                  إغلاق
                </Button>
              </div>
              <div className='p-4 space-y-2 text-sm'>
                {[
                  ['المعاملة', selectedDispute.transactionId ? `#${selectedDispute.transactionId}` : '-'],
                  ['المستخدم', selectedDispute.userId ? `#${selectedDispute.userId}` : '-'],
                  ['الحالة', STATUS_LABEL[selectedDispute.status ?? ''] ?? selectedDispute.status ?? '-'],
                  ['المُكلَّف', selectedDispute.assignedTo ?? '-'],
                  ['تاريخ الرفع', selectedDispute.createdAt ?? '-'],
                  ['آخر تحديث', selectedDispute.updatedAt ?? '-'],
                ].map(([k, v]) => (
                  <div key={k} className='flex justify-between gap-2'>
                    <span className='text-muted-foreground'>{k}:</span>
                    <span className='font-medium text-right'>{v}</span>
                  </div>
                ))}
                {selectedDispute.reason && (
                  <div className='pt-1'>
                    <div className='text-muted-foreground text-xs mb-1'>السبب:</div>
                    <div className='bg-muted/60 rounded-sm p-2 text-sm leading-relaxed'>
                      {selectedDispute.reason}
                    </div>
                  </div>
                )}
                {selectedDispute.resolution && (
                  <div className='pt-1'>
                    <div className='text-muted-foreground text-xs mb-1'>القرار:</div>
                    <div className='bg-emerald-50 dark:bg-emerald-950/20 rounded-sm p-2 text-sm leading-relaxed border border-emerald-200'>
                      {selectedDispute.resolution}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Tabs + filter */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <div className='flex gap-1'>
                {(['my', 'all'] as const).map((t) => (
                  <Button key={t} size='sm'
                    variant={tab === t ? 'default' : 'outline'}
                    className='h-7 text-xs'
                    onClick={() => setTab(t)}>
                    {t === 'my' ? 'اعتراضاتي' : 'الكل (مشرف)'}
                  </Button>
                ))}
              </div>
              <div className='flex items-center gap-2'>
                <select value={statusFilter}
                  onChange={(e) => { setStatusFilter(e.target.value); refresh(); }}
                  className='h-7 rounded-sm border border-border bg-background px-2 text-xs'>
                  <option value=''>كل الحالات</option>
                  <option value='PENDING'>معلق</option>
                  <option value='IN_PROGRESS'>قيد المعالجة</option>
                  <option value='RESOLVED'>محلول</option>
                  <option value='REJECTED'>مرفوض</option>
                  <option value='CLOSED'>مغلق</option>
                </select>
                {isLoading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
                <Button size='sm' variant='ghost' className='h-7 text-xs' onClick={refresh}>
                  تحديث
                </Button>
              </div>
            </div>
            <div className='overflow-x-auto'>
              <table className='gov-table'>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>المعاملة</th>
                    <th>السبب</th>
                    <th>الحالة</th>
                    <th>المُكلَّف</th>
                    <th>التاريخ</th>
                    <th>إجراءات</th>
                  </tr>
                </thead>
                <tbody>
                  {disputes.map((d) => (
                    <tr key={d.id}
                      className={selectedDispute?.id === d.id ? 'bg-muted/40' : ''}>
                      <td className='text-muted-foreground'>{d.id}</td>
                      <td>{d.transactionId ? `#${d.transactionId}` : '-'}</td>
                      <td className='max-w-48 truncate text-xs text-muted-foreground'>
                        {d.reason ?? '-'}
                      </td>
                      <td>
                        <Badge variant={STATUS_VARIANT[d.status ?? ''] ?? 'outline'} className='text-xs'>
                          {STATUS_LABEL[d.status ?? ''] ?? d.status ?? '-'}
                        </Badge>
                      </td>
                      <td className='text-xs'>{d.assignedTo ?? '-'}</td>
                      <td className='text-xs text-muted-foreground'>{d.createdAt ?? '-'}</td>
                      <td>
                        <div className='flex items-center gap-1 flex-wrap'>
                          <Button size='sm' variant='outline' className='h-7 text-xs'
                            onClick={() => setSelectedDispute(
                              selectedDispute?.id === d.id ? null : d
                            )}>
                            {selectedDispute?.id === d.id ? 'إخفاء' : 'عرض'}
                          </Button>
                          {tab === 'all' && (d.status === 'PENDING' || d.status === 'IN_PROGRESS') && (
                            <>
                              <Button size='sm' variant='ghost' className='h-7 text-xs text-blue-600'
                                onClick={() => setAssignForm({ id: d.id, assignee: d.assignedTo ?? '' })}>
                                تعيين
                              </Button>
                              <Button size='sm' variant='ghost'
                                className='h-7 text-xs text-emerald-600'
                                onClick={() => setResolveDisputeForm({ id: d.id, action: 'resolve', resolution: '' })}>
                                حل
                              </Button>
                              <Button size='sm' variant='ghost'
                                className='h-7 text-xs text-destructive hover:text-destructive'
                                onClick={() => setResolveDisputeForm({ id: d.id, action: 'reject', resolution: '' })}>
                                رفض
                              </Button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {!isLoading && !disputes.length && (
                    <tr>
                      <td colSpan={7} className='py-8 text-center text-muted-foreground text-sm'>
                        لا توجد اعتراضات{statusFilter ? ` بحالة "${STATUS_LABEL[statusFilter] ?? statusFilter}"` : ''}.
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

// ─── Account Freeze ────────────────────────────────────────────────────────────

export function FreezeCasesPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);
  const [statusFilter, setStatusFilter] = useState('ACTIVE');
  const [form, setForm] = useState({ userId: '', reason: '' });
  const [submitting, setSubmitting] = useState(false);

  const [lookup, setLookup] = useState('');
  const [lookupData, setLookupData] = useState<FreezeCase | null>(null);
  const [looking, setLooking] = useState(false);

  const [selectedCase, setSelectedCase] = useState<FreezeCase | null>(null);
  const [caseEvents, setCaseEvents] = useState<FreezeCaseEvent[]>([]);
  const [loadingEvents, setLoadingEvents] = useState(false);

  const [unfreezeForm, setUnfreezeForm] = useState<{ id: number; reason: string } | null>(null);

  type FreezeCaseResponse = FreezeCase[] | { content?: FreezeCase[]; data?: FreezeCase[] };
  const cases = useBackend<FreezeCaseResponse>(
    `/compliance/freeze-cases?${statusFilter ? `status=${statusFilter}&` : ''}_t=${tick}`
  );
  const caseList = toArray<FreezeCase>(cases.data as FreezeCase[] | { content?: FreezeCase[]; data?: FreezeCase[] } | null);

  async function createCase(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!form.userId || !form.reason.trim()) {
      toast.error('يرجى ملء جميع الحقول');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient('/compliance/freeze-cases', {
        method: 'POST',
        body: JSON.stringify({ userId: Number(form.userId), reason: form.reason }),
      });
      toast.success('تم فتح قضية التجميد بنجاح');
      setForm({ userId: '', reason: '' });
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل فتح القضية');
    } finally {
      setSubmitting(false);
    }
  }

  async function lookupCase() {
    if (!lookup.trim()) return;
    setLooking(true);
    setLookupData(null);
    try {
      const data = await apiClient<FreezeCase>(`/compliance/freeze-cases/public/${lookup.trim()}`);
      setLookupData(data);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'لم يتم العثور على القضية');
    } finally {
      setLooking(false);
    }
  }

  async function loadEvents(caseId: number) {
    setLoadingEvents(true);
    try {
      const events = await apiClient<FreezeCaseEvent[]>(`/compliance/freeze-cases/${caseId}/events`);
      setCaseEvents(Array.isArray(events) ? events : []);
    } catch {
      setCaseEvents([]);
    } finally {
      setLoadingEvents(false);
    }
  }

  async function selectCase(c: FreezeCase) {
    if (selectedCase?.id === c.id) {
      setSelectedCase(null);
      setCaseEvents([]);
      return;
    }
    setSelectedCase(c);
    setCaseEvents([]);
    await loadEvents(c.id);
  }

  async function unfreezeCase(id: number, reason: string) {
    try {
      await apiClient(`/compliance/freeze-cases/${id}/unfreeze`, {
        method: 'POST',
        body: JSON.stringify({ reason: reason || undefined }),
      });
      toast.success('تم رفع التجميد عن الحساب');
      setUnfreezeForm(null);
      setSelectedCase(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل رفع التجميد');
    }
  }

  async function addEvent(caseId: number, description: string) {
    try {
      await apiClient(`/compliance/freeze-cases/${caseId}/events`, {
        method: 'POST',
        body: JSON.stringify({ description }),
      });
      toast.success('تم إضافة الحدث');
      await loadEvents(caseId);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إضافة الحدث');
    }
  }

  const [eventNote, setEventNote] = useState('');

  return (
    <PageContainer
      pageTitle='تجميد الحسابات'
      pageDescription='فتح قضايا التجميد، مراجعتها، رفعها، وتتبع مسار كل قضية.'
    >
      <div className='grid gap-4 xl:grid-cols-[340px_1fr]'>
        {/* Left panel */}
        <div className='space-y-4'>
          {/* Create Freeze */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>فتح قضية تجميد</h2>
            </div>
            <form className='grid gap-3 p-4' onSubmit={(e) => { void createCase(e); }}>
              <Field id='freezeUserId' label='معرف المستخدم' required value={form.userId}
                onChange={(v) => setForm((c) => ({ ...c, userId: v }))} type='number'
                placeholder='مثال: 42' />
              <div className='space-y-1.5'>
                <Label htmlFor='freezeReason' className='text-xs'>
                  سبب التجميد <span className='text-destructive'>*</span>
                </Label>
                <Textarea id='freezeReason' value={form.reason}
                  onChange={(e) => setForm((c) => ({ ...c, reason: e.target.value }))}
                  className='min-h-20 rounded-sm text-sm'
                  placeholder='أدخل سبب التجميد بالتفصيل...' />
              </div>
              <Button type='submit' variant='destructive' className='h-9 rounded-sm' disabled={submitting}>
                {submitting && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                <Icons.lock className='ml-2 size-4' />
                تجميد الحساب
              </Button>
            </form>
          </div>

          {/* Public Lookup */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>استعلام بالرقم العام</h2>
              <p className='text-muted-foreground text-xs'>متاح للعموم دون تسجيل دخول</p>
            </div>
            <div className='p-4 space-y-3'>
              <div className='flex gap-2'>
                <Input value={lookup} onChange={(e) => setLookup(e.target.value)}
                  placeholder='الرقم العام للقضية...'
                  className='h-9 rounded-sm flex-1 text-sm' />
                <Button className='h-9 rounded-sm' onClick={() => { void lookupCase(); }} disabled={looking}>
                  {looking && <Icons.spinner className='ml-1 size-4 animate-spin' />}
                  بحث
                </Button>
              </div>
              {lookupData && (
                <div className='border-border rounded-sm border p-3 space-y-2 text-sm'>
                  <div className='flex justify-between'>
                    <span className='text-muted-foreground'>رقم القضية</span>
                    <span className='font-mono font-medium'>{lookupData.publicCaseId ?? lookupData.id}</span>
                  </div>
                  <div className='flex justify-between'>
                    <span className='text-muted-foreground'>الحالة</span>
                    <Badge variant={STATUS_VARIANT[lookupData.status ?? ''] ?? 'outline'}>
                      {STATUS_LABEL[lookupData.status ?? ''] ?? lookupData.status ?? '-'}
                    </Badge>
                  </div>
                  {lookupData.reason && (
                    <div className='flex justify-between gap-4'>
                      <span className='text-muted-foreground shrink-0'>السبب</span>
                      <span className='text-right text-xs'>{lookupData.reason}</span>
                    </div>
                  )}
                  <div className='flex justify-between'>
                    <span className='text-muted-foreground'>تاريخ الفتح</span>
                    <span className='text-xs'>{lookupData.createdAt ?? '-'}</span>
                  </div>
                  {lookupData.resolvedAt && (
                    <div className='flex justify-between'>
                      <span className='text-muted-foreground'>تاريخ الرفع</span>
                      <span className='text-xs'>{lookupData.resolvedAt}</span>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right panel */}
        <div className='space-y-4'>
          {/* Unfreeze form */}
          {unfreezeForm && (
            <div className='gov-panel rounded-md border-emerald-200 bg-emerald-50/40 dark:bg-emerald-950/10 max-w-lg'>
              <div className='gov-panel-header rounded-t-md px-4 py-3 border-emerald-200'>
                <h2 className='text-sm font-semibold text-emerald-700 dark:text-emerald-400'>
                  رفع التجميد عن القضية #{unfreezeForm.id}
                </h2>
              </div>
              <div className='p-4 space-y-3'>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>سبب رفع التجميد</Label>
                  <Textarea value={unfreezeForm.reason}
                    onChange={(e) => setUnfreezeForm((r) => r ? { ...r, reason: e.target.value } : r)}
                    placeholder='سبب رفع التجميد...' className='min-h-16 rounded-sm text-sm' />
                </div>
                <div className='flex gap-2'>
                  <Button size='sm'
                    className='h-8 text-xs rounded-sm bg-emerald-600 hover:bg-emerald-700 text-white'
                    onClick={() => { void unfreezeCase(unfreezeForm.id, unfreezeForm.reason); }}>
                    <Icons.unlock className='ml-1 size-3.5' />
                    رفع التجميد
                  </Button>
                  <Button size='sm' variant='outline' className='h-8 text-xs rounded-sm'
                    onClick={() => setUnfreezeForm(null)}>
                    إلغاء
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* Case detail + timeline */}
          {selectedCase && (
            <div className='grid gap-4 md:grid-cols-2'>
              <div className='gov-panel rounded-md'>
                <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                  <h2 className='text-sm font-semibold'>تفاصيل القضية #{selectedCase.id}</h2>
                  <Button size='sm' variant='ghost' className='h-7 text-xs'
                    onClick={() => { setSelectedCase(null); setCaseEvents([]); }}>
                    إغلاق
                  </Button>
                </div>
                <div className='p-4 space-y-2 text-sm'>
                  {[
                    ['الرقم العام', selectedCase.publicCaseId ?? '-'],
                    ['المستخدم', selectedCase.userId ? `#${selectedCase.userId}` : '-'],
                    ['الحالة', STATUS_LABEL[selectedCase.status ?? ''] ?? selectedCase.status ?? '-'],
                    ['بدأ بواسطة', selectedCase.initiatedBy ?? '-'],
                    ['تاريخ الفتح', selectedCase.createdAt ?? '-'],
                    ['تاريخ الرفع', selectedCase.resolvedAt ?? '-'],
                  ].map(([k, v]) => (
                    <div key={k} className='flex justify-between gap-2'>
                      <span className='text-muted-foreground'>{k}:</span>
                      <span className='font-medium text-right'>{v}</span>
                    </div>
                  ))}
                  {selectedCase.reason && (
                    <div>
                      <div className='text-muted-foreground text-xs mb-1'>السبب:</div>
                      <div className='bg-muted/60 rounded-sm p-2 text-xs leading-relaxed'>
                        {selectedCase.reason}
                      </div>
                    </div>
                  )}
                  {selectedCase.status === 'ACTIVE' && (
                    <Button size='sm' variant='outline'
                      className='mt-2 h-8 text-xs text-emerald-600 border-emerald-200 w-full'
                      onClick={() => setUnfreezeForm({ id: selectedCase.id, reason: '' })}>
                      <Icons.unlock className='ml-1 size-3.5' />
                      رفع التجميد
                    </Button>
                  )}
                </div>
              </div>

              <div className='gov-panel rounded-md'>
                <div className='gov-panel-header rounded-t-md px-4 py-3'>
                  <h2 className='text-sm font-semibold'>مسار الأحداث</h2>
                  {loadingEvents && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
                </div>
                <div className='p-4 space-y-3'>
                  <div className='relative space-y-3'>
                    {caseEvents.map((ev, i) => (
                      <div key={ev.id} className='flex gap-3'>
                        <div className='flex flex-col items-center'>
                          <div className='size-2 rounded-full bg-primary mt-1.5 shrink-0' />
                          {i < caseEvents.length - 1 && (
                            <div className='w-0.5 flex-1 bg-border mt-1' />
                          )}
                        </div>
                        <div className='pb-3'>
                          <div className='text-xs font-medium'>{ev.eventType ?? '-'}</div>
                          <div className='text-xs text-muted-foreground mt-0.5'>
                            {ev.description ?? '-'}
                          </div>
                          <div className='text-xs text-muted-foreground/60 mt-0.5'>
                            {ev.performedBy && `${ev.performedBy} · `}{ev.createdAt ?? ''}
                          </div>
                        </div>
                      </div>
                    ))}
                    {!loadingEvents && !caseEvents.length && (
                      <p className='text-muted-foreground text-xs'>لا أحداث مسجلة.</p>
                    )}
                  </div>
                  {/* Add event */}
                  <div className='border-t border-border pt-3 space-y-2'>
                    <Label className='text-xs'>إضافة ملاحظة/حدث</Label>
                    <div className='flex gap-2'>
                      <Input value={eventNote} onChange={(e) => setEventNote(e.target.value)}
                        placeholder='ملاحظة...' className='h-8 rounded-sm text-xs flex-1' />
                      <Button size='sm' variant='outline' className='h-8 text-xs rounded-sm'
                        onClick={() => {
                          if (!eventNote.trim()) return;
                          void addEvent(selectedCase.id, eventNote);
                          setEventNote('');
                        }}>
                        إضافة
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Cases Table */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>
                قضايا التجميد
                <span className='text-muted-foreground font-normal mr-1 text-xs'>
                  ({caseList.length})
                </span>
              </h2>
              <div className='flex items-center gap-2'>
                <select value={statusFilter}
                  onChange={(e) => { setStatusFilter(e.target.value); refresh(); }}
                  className='h-7 rounded-sm border border-border bg-background px-2 text-xs'>
                  <option value=''>كل القضايا</option>
                  <option value='ACTIVE'>نشطة (مجمدة)</option>
                  <option value='LIFTED'>مرفوع التجميد</option>
                  <option value='PENDING'>معلقة</option>
                </select>
                {cases.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
                <Button size='sm' variant='ghost' className='h-7 text-xs' onClick={refresh}>
                  تحديث
                </Button>
              </div>
            </div>
            <div className='overflow-x-auto'>
              <table className='gov-table'>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>الرقم العام</th>
                    <th>المستخدم</th>
                    <th>السبب</th>
                    <th>الحالة</th>
                    <th>بدأ بواسطة</th>
                    <th>التاريخ</th>
                    <th>إجراءات</th>
                  </tr>
                </thead>
                <tbody>
                  {caseList.map((c) => (
                    <tr key={c.id} className={selectedCase?.id === c.id ? 'bg-muted/40' : ''}>
                      <td className='text-muted-foreground'>{c.id}</td>
                      <td className='font-mono text-xs'>{c.publicCaseId ?? '-'}</td>
                      <td>{c.userId ? `#${c.userId}` : '-'}</td>
                      <td className='max-w-44 truncate text-xs text-muted-foreground'>{c.reason ?? '-'}</td>
                      <td>
                        <Badge variant={STATUS_VARIANT[c.status ?? ''] ?? 'outline'} className='text-xs'>
                          {STATUS_LABEL[c.status ?? ''] ?? c.status ?? '-'}
                        </Badge>
                      </td>
                      <td className='text-xs'>{c.initiatedBy ?? '-'}</td>
                      <td className='text-xs text-muted-foreground'>{c.createdAt ?? '-'}</td>
                      <td>
                        <div className='flex items-center gap-1'>
                          <Button size='sm' variant='outline' className='h-7 text-xs'
                            onClick={() => { void selectCase(c); }}>
                            {selectedCase?.id === c.id ? 'إخفاء' : 'تفاصيل'}
                          </Button>
                          {c.status === 'ACTIVE' && (
                            <Button size='sm' variant='ghost'
                              className='h-7 text-xs text-emerald-600'
                              onClick={() => setUnfreezeForm({ id: c.id, reason: '' })}>
                              رفع
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {!cases.loading && !caseList.length && (
                    <tr>
                      <td colSpan={8} className='py-8 text-center text-muted-foreground text-sm'>
                        لا توجد قضايا تجميد{statusFilter === 'ACTIVE' ? ' نشطة' : ''}.
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
