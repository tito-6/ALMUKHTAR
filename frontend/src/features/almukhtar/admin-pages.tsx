'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { apiClient } from '@/lib/api-client';
import { useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import type {
  AuditLog,
  Branch,
  BranchDailyStat,
  BranchHourlyStat,
  BranchSummary,
  CommissionRate,
  CommissionScope,
  CorporateAccount,
  CorporateStatement,
  CorporateSubAccount,
  FeeRule,
  FeeZone,
  SyncConflict,
  SyncDevice,
  SyncStatus,
  Tenant,
  User,
} from './types';

const moneyFmt = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });
const fmt = (v?: number | string) => moneyFmt.format(Number(v ?? 0));

function toArray<T>(raw: T[] | { content?: T[]; data?: T[] } | null | undefined): T[] {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw;
  const r = raw as { content?: T[]; data?: T[] };
  if (Array.isArray(r.content)) return r.content;
  if (Array.isArray(r.data)) return r.data;
  return [];
}

// ─── Audit Logs ───────────────────────────────────────────────────────────────

export function AuditPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);

  const [searchQuery, setSearchQuery] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [entityTypeFilter, setEntityTypeFilter] = useState('');
  const [performerFilter, setPerformerFilter] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const queryString = [
    actionFilter && `action=${encodeURIComponent(actionFilter)}`,
    entityTypeFilter && `entityType=${encodeURIComponent(entityTypeFilter)}`,
    performerFilter && `performedBy=${encodeURIComponent(performerFilter)}`,
    dateFrom && `from=${encodeURIComponent(dateFrom)}`,
    dateTo && `to=${encodeURIComponent(dateTo)}`,
    `_t=${tick}`,
  ].filter(Boolean).join('&');

  const logs = useBackend<AuditLog[] | { content?: AuditLog[]; data?: AuditLog[] }>(`/audit/logs?${queryString}`);
  const platformSummary = useBackend<Record<string, unknown>>('/audit/platform-summary');

  function toLogArray(raw: AuditLog[] | { content?: AuditLog[]; data?: AuditLog[] } | null | undefined): AuditLog[] {
    if (!raw) return [];
    if (Array.isArray(raw)) return raw;
    const r = raw as { content?: AuditLog[]; data?: AuditLog[] };
    if (Array.isArray(r.content)) return r.content;
    if (Array.isArray(r.data)) return r.data;
    return [];
  }

  const logList = toLogArray(logs.data);

  const filteredLogs = searchQuery.trim()
    ? logList.filter((l) => {
        const q = searchQuery.toLowerCase();
        return (
          (l.action ?? '').toLowerCase().includes(q) ||
          (l.entityType ?? '').toLowerCase().includes(q) ||
          (l.entityId ?? '').toLowerCase().includes(q) ||
          (l.performedBy ?? '').toLowerCase().includes(q) ||
          (l.ipAddress ?? '').includes(q)
        );
      })
    : logList;

  // Unique values for filter dropdowns
  const uniqueActions = [...new Set(logList.map((l) => l.action).filter(Boolean))].slice(0, 30);
  const uniqueEntityTypes = [...new Set(logList.map((l) => l.entityType).filter(Boolean))].slice(0, 30);

  function exportCsv() {
    const headers = ['#', 'الإجراء', 'نوع الكيان', 'معرف الكيان', 'المنفذ', 'IP', 'التاريخ'];
    const rows = filteredLogs.map((l) => [
      l.id,
      l.action ?? '',
      l.entityType ?? '',
      l.entityId ?? '',
      l.performedBy ?? '',
      l.ipAddress ?? '',
      l.createdAt ?? '',
    ]);
    const csv = [headers, ...rows].map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n');
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `audit-log-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('تم تصدير السجل');
  }

  return (
    <PageContainer
      pageTitle='سجل التدقيق'
      pageDescription='سجل كامل بجميع الإجراءات التشغيلية والإدارية مع إمكانية البحث والتصفية والتصدير.'
    >
      <div className='space-y-4'>
        {/* Platform Summary */}
        {platformSummary.data && (
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>ملخص المنصة</h2>
            </div>
            <div className='grid gap-3 p-4 grid-cols-2 md:grid-cols-4'>
              {Object.entries(platformSummary.data).slice(0, 8).map(([key, value]) => (
                <div key={key} className='border-border rounded-sm border p-3'>
                  <div className='text-xs text-muted-foreground'>{key}</div>
                  <div className='font-semibold text-sm mt-1'>{String(value)}</div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Filters */}
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>فلترة السجل</h2>
          </div>
          <div className='p-4 grid gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6'>
            <div className='lg:col-span-2'>
              <Label className='text-xs mb-1.5 block'>بحث نصي</Label>
              <Input value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
                placeholder='إجراء، كيان، منفذ، IP...'
                className='h-8 rounded-sm text-xs' />
            </div>
            <div>
              <Label className='text-xs mb-1.5 block'>الإجراء</Label>
              <select value={actionFilter}
                onChange={(e) => { setActionFilter(e.target.value); refresh(); }}
                className='h-8 w-full rounded-sm border border-border bg-background px-2 text-xs'>
                <option value=''>الكل</option>
                {uniqueActions.map((a) => <option key={a} value={a}>{a}</option>)}
              </select>
            </div>
            <div>
              <Label className='text-xs mb-1.5 block'>نوع الكيان</Label>
              <select value={entityTypeFilter}
                onChange={(e) => { setEntityTypeFilter(e.target.value); refresh(); }}
                className='h-8 w-full rounded-sm border border-border bg-background px-2 text-xs'>
                <option value=''>الكل</option>
                {uniqueEntityTypes.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <Label className='text-xs mb-1.5 block'>من تاريخ</Label>
              <Input type='date' value={dateFrom}
                onChange={(e) => { setDateFrom(e.target.value); refresh(); }}
                className='h-8 rounded-sm text-xs' />
            </div>
            <div>
              <Label className='text-xs mb-1.5 block'>إلى تاريخ</Label>
              <Input type='date' value={dateTo}
                onChange={(e) => { setDateTo(e.target.value); refresh(); }}
                className='h-8 rounded-sm text-xs' />
            </div>
          </div>
          <div className='px-4 pb-3 flex gap-2'>
            <Input value={performerFilter} onChange={(e) => { setPerformerFilter(e.target.value); refresh(); }}
              placeholder='فلتر بالمنفذ...' className='h-8 rounded-sm text-xs w-48' />
            <Button size='sm' variant='ghost' className='h-8 text-xs'
              onClick={() => {
                setSearchQuery(''); setActionFilter(''); setEntityTypeFilter('');
                setPerformerFilter(''); setDateFrom(''); setDateTo('');
                refresh();
              }}>
              مسح الكل
            </Button>
            <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
              <Icons.spinner className='ml-1 size-3.5' />
              تحديث
            </Button>
          </div>
        </div>

        {/* Table */}
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <div>
              <h2 className='text-sm font-semibold'>سجلات الأنشطة</h2>
              <p className='text-muted-foreground text-xs'>{filteredLogs.length} سجل</p>
            </div>
            <div className='flex items-center gap-2'>
              {logs.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
              <Button size='sm' variant='outline' className='h-7 text-xs' onClick={exportCsv}
                disabled={!filteredLogs.length}>
                <Icons.download className='ml-1 size-3.5' />
                تصدير CSV
              </Button>
            </div>
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>الإجراء</th>
                  <th>نوع الكيان</th>
                  <th>معرف الكيان</th>
                  <th>المنفذ</th>
                  <th>IP</th>
                  <th>التاريخ</th>
                  <th>تفاصيل</th>
                </tr>
              </thead>
              <tbody>
                {filteredLogs.map((log) => (
                  <>
                    <tr key={log.id}>
                      <td className='text-muted-foreground'>{log.id}</td>
                      <td className='font-medium text-xs'>{log.action ?? '-'}</td>
                      <td className='text-xs'>{log.entityType ?? '-'}</td>
                      <td className='text-xs font-mono'>{log.entityId ?? '-'}</td>
                      <td className='text-xs'>{log.performedBy ?? '-'}</td>
                      <td className='text-xs text-muted-foreground font-mono'>{log.ipAddress ?? '-'}</td>
                      <td className='text-xs text-muted-foreground'>{log.createdAt ?? '-'}</td>
                      <td>
                        {log.details && (
                          <Button size='sm' variant='ghost' className='h-6 text-xs px-2'
                            onClick={() => setExpandedId(expandedId === log.id ? null : log.id)}>
                            {expandedId === log.id ? 'إخفاء' : 'عرض'}
                          </Button>
                        )}
                      </td>
                    </tr>
                    {expandedId === log.id && log.details && (
                      <tr key={`${log.id}-detail`}>
                        <td colSpan={8} className='bg-muted/40 px-4 py-2'>
                          <pre className='text-xs font-mono whitespace-pre-wrap break-all'>
                            {log.details}
                          </pre>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
                {!logs.loading && !filteredLogs.length && (
                  <tr>
                    <td colSpan={8} className='text-muted-foreground text-sm py-8 text-center'>
                      لا توجد سجلات تطابق معايير البحث.
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

// ─── Branch Analytics ─────────────────────────────────────────────────────────

export function AnalyticsPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);
  const [branchId, setBranchId] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [currencyFilter, setCurrencyFilter] = useState('');

  const dailyQuery = [
    branchId && `branchId=${branchId}`,
    dateFrom && `from=${dateFrom}`,
    dateTo && `to=${dateTo}`,
    `_t=${tick}`,
  ].filter(Boolean).join('&');

  const daily = useBackend<BranchDailyStat[]>(`/analytics/my-branch/daily?${dailyQuery}`);
  const hourly = useBackend<BranchHourlyStat[]>(`/analytics/my-branch/peak-hours?_t=${tick}`);
  const platform = useBackend<Record<string, unknown>>(`/analytics/platform?_t=${tick}`);
  const topBranches = useBackend<Array<{
    branchId?: number; branchName?: string; transactionCount?: number;
    transactionVolume?: number; currency?: string;
  }>>(`/analytics/top-branches?_t=${tick}`);

  const maxHourly = Math.max(...(hourly.data ?? []).map((h) => h.transactionCount ?? 0), 1);

  const dailyData = (daily.data ?? []);
  const filteredDaily = currencyFilter
    ? dailyData.filter((d) => (d.currency ?? 'USD') === currencyFilter)
    : dailyData;

  const uniqueCurrencies = [...new Set(dailyData.map((d) => d.currency ?? 'USD').filter(Boolean))];

  // Summary stats derived from daily data
  const totalTx = filteredDaily.reduce((s, d) => s + (d.transactionCount ?? 0), 0);
  const totalVol = filteredDaily.reduce((s, d) => s + (d.transactionVolume ?? 0), 0);
  const totalNewCustomers = filteredDaily.reduce((s, d) => s + (d.newCustomers ?? 0), 0);
  const avgTxPerDay = filteredDaily.length > 0 ? Math.round(totalTx / filteredDaily.length) : 0;

  // Peak hour detection
  const peakHour = (hourly.data ?? []).reduce<{ hour: number; count: number } | null>(
    (max, h) => (!max || (h.transactionCount ?? 0) > max.count)
      ? { hour: h.hour ?? 0, count: h.transactionCount ?? 0 }
      : max,
    null
  );

  return (
    <PageContainer
      pageTitle='تحليلات الفروع'
      pageDescription='إحصاءات الأداء اليومية والساعية للمنصة مع فلترة متقدمة.'
    >
      <div className='space-y-4'>
        {/* Filters */}
        <div className='flex flex-wrap items-end gap-3'>
          <div>
            <Label className='text-xs mb-1 block'>الفرع (اختياري)</Label>
            <Input value={branchId} onChange={(e) => { setBranchId(e.target.value); refresh(); }}
              className='h-8 w-24 rounded-sm text-xs' type='number' placeholder='الكل' />
          </div>
          <div>
            <Label className='text-xs mb-1 block'>من تاريخ</Label>
            <Input type='date' value={dateFrom}
              onChange={(e) => { setDateFrom(e.target.value); refresh(); }}
              className='h-8 rounded-sm text-xs' />
          </div>
          <div>
            <Label className='text-xs mb-1 block'>إلى تاريخ</Label>
            <Input type='date' value={dateTo}
              onChange={(e) => { setDateTo(e.target.value); refresh(); }}
              className='h-8 rounded-sm text-xs' />
          </div>
          {uniqueCurrencies.length > 1 && (
            <div>
              <Label className='text-xs mb-1 block'>العملة</Label>
              <select value={currencyFilter} onChange={(e) => setCurrencyFilter(e.target.value)}
                className='h-8 rounded-sm border border-border bg-background px-2 text-xs'>
                <option value=''>الكل</option>
                {uniqueCurrencies.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
          )}
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
        </div>

        {/* Platform Stats */}
        {platform.data && (
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>إحصاءات المنصة الإجمالية</h2>
            </div>
            <div className='grid gap-3 p-4 grid-cols-2 md:grid-cols-4'>
              {Object.entries(platform.data).slice(0, 8).map(([key, value]) => (
                <div key={key} className='border-border rounded-sm border p-3'>
                  <div className='text-xs text-muted-foreground'>{key}</div>
                  <div className='font-semibold mt-1 tabular-nums'>{String(value)}</div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Period Summary Cards */}
        {filteredDaily.length > 0 && (
          <div className='grid grid-cols-2 gap-3 md:grid-cols-4'>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>إجمالي المعاملات</div>
              <div className='text-2xl font-bold tabular-nums mt-1'>{fmt(totalTx)}</div>
              <div className='text-xs text-muted-foreground'>{filteredDaily.length} يوم</div>
            </div>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>إجمالي الحجم</div>
              <div className='text-xl font-bold tabular-nums mt-1'>
                {fmt(totalVol)} {currencyFilter || ''}
              </div>
            </div>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>متوسط يومي</div>
              <div className='text-xl font-bold tabular-nums mt-1'>{fmt(avgTxPerDay)}</div>
              <div className='text-xs text-muted-foreground'>معاملة/يوم</div>
            </div>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>عملاء جدد</div>
              <div className='text-2xl font-bold tabular-nums mt-1'>{fmt(totalNewCustomers)}</div>
              {peakHour && (
                <div className='text-xs text-muted-foreground'>ذروة: {peakHour.hour}:00</div>
              )}
            </div>
          </div>
        )}

        <div className='grid gap-4 xl:grid-cols-2'>
          {/* Daily Stats Table */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>الإحصاءات اليومية</h2>
              {daily.loading && <Icons.spinner className='size-4 animate-spin' />}
            </div>
            <div className='overflow-x-auto'>
              <table className='gov-table'>
                <thead>
                  <tr>
                    <th>التاريخ</th>
                    <th>المعاملات</th>
                    <th>الحجم</th>
                    <th>العملة</th>
                    <th>عملاء جدد</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredDaily.map((d, i) => {
                    const prevVol = i > 0 ? (filteredDaily[i - 1]?.transactionVolume ?? 0) : null;
                    const trend = prevVol !== null
                      ? (d.transactionVolume ?? 0) > prevVol ? 'up'
                        : (d.transactionVolume ?? 0) < prevVol ? 'down' : 'flat'
                      : null;
                    return (
                      <tr key={i}>
                        <td className='font-mono text-xs'>{d.date ?? '-'}</td>
                        <td className='tabular-nums'>{fmt(d.transactionCount)}</td>
                        <td className='tabular-nums'>
                          <span>{fmt(d.transactionVolume)}</span>
                          {trend === 'up' && <span className='text-emerald-600 mr-1 text-xs'>↑</span>}
                          {trend === 'down' && <span className='text-destructive mr-1 text-xs'>↓</span>}
                        </td>
                        <td className='text-xs text-muted-foreground'>{d.currency ?? '-'}</td>
                        <td className='tabular-nums'>{d.newCustomers ?? 0}</td>
                      </tr>
                    );
                  })}
                  {!daily.loading && !filteredDaily.length && (
                    <tr>
                      <td colSpan={5} className='text-muted-foreground text-sm py-6 text-center'>
                        لا توجد بيانات للفترة المحددة.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className='space-y-4'>
            {/* Peak Hours Chart */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>أوقات الذروة</h2>
                {peakHour && (
                  <p className='text-muted-foreground text-xs'>
                    الذروة: {peakHour.hour}:00 — {fmt(peakHour.count)} معاملة
                  </p>
                )}
              </div>
              <div className='p-4 space-y-2'>
                {(hourly.data ?? []).map((h) => {
                  const pct = ((h.transactionCount ?? 0) / maxHourly) * 100;
                  const isPeak = h.hour === peakHour?.hour;
                  return (
                    <div key={h.hour} className='flex items-center gap-3'>
                      <span className='text-xs w-10 text-muted-foreground text-left dir-ltr font-mono'>
                        {String(h.hour ?? 0).padStart(2, '0')}:00
                      </span>
                      <div className='flex-1 bg-muted rounded-full h-2.5 overflow-hidden'>
                        <div
                          className={`h-full rounded-full transition-all ${isPeak ? 'bg-amber-500' : 'bg-primary'}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <span className={`text-xs w-10 tabular-nums text-right ${isPeak ? 'font-bold text-amber-600' : ''}`}>
                        {h.transactionCount ?? 0}
                      </span>
                      {h.avgAmount != null && (
                        <span className='text-xs text-muted-foreground w-20 text-left'>
                          avg: {fmt(h.avgAmount)}
                        </span>
                      )}
                    </div>
                  );
                })}
                {!hourly.loading && !hourly.data?.length && (
                  <p className='text-muted-foreground text-sm'>لا توجد بيانات.</p>
                )}
              </div>
            </div>

            {/* Top Branches */}
            {(topBranches.data ?? []).length > 0 && (
              <div className='gov-panel rounded-md'>
                <div className='gov-panel-header rounded-t-md px-4 py-3'>
                  <h2 className='text-sm font-semibold'>أعلى الفروع أداءً</h2>
                </div>
                <div className='overflow-x-auto'>
                  <table className='gov-table'>
                    <thead>
                      <tr>
                        <th>الفرع</th>
                        <th>المعاملات</th>
                        <th>الحجم</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(topBranches.data ?? []).map((b, i) => (
                        <tr key={i}>
                          <td className='font-medium'>
                            {b.branchName ?? `فرع #${b.branchId}`}
                          </td>
                          <td className='tabular-nums'>{fmt(b.transactionCount)}</td>
                          <td className='tabular-nums'>
                            {fmt(b.transactionVolume)} {b.currency ?? ''}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </PageContainer>
  );
}

// ─── QR Code ─────────────────────────────────────────────────────────────────

function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

type QrGenerationResult = {
  qrImageBase64?: string;
  tokenHash?: string;
  expiresAt?: string;
  transactionId?: number;
};

type ScanResult = {
  success?: boolean;
  transactionId?: number;
  receiverName?: string;
  amount?: number;
  message?: string;
};

type QrStatusResult = {
  tokenHash?: string;
  transactionId?: number;
  status?: string;
  usedAt?: string;
  expiresAt?: string;
  valid?: boolean;
  message?: string;
};

type MerchantQrResult = {
  qrImageBase64?: string;
  paymentUrl?: string;
  merchantId?: number;
  expiresAt?: string;
};

export function QrPage() {
  const [activeTab, setActiveTab] = useState<'transaction' | 'merchant' | 'status'>('transaction');

  // Transaction QR
  const [genTxId, setGenTxId] = useState('');
  const [genResult, setGenResult] = useState<QrGenerationResult | null>(null);
  const [generating, setGenerating] = useState(false);

  // Merchant QR
  const [merchantId, setMerchantId] = useState('');
  const [merchantQrResult, setMerchantQrResult] = useState<MerchantQrResult | null>(null);
  const [generatingMerchant, setGeneratingMerchant] = useState(false);

  // Status check
  const [statusTokenHash, setStatusTokenHash] = useState('');
  const [qrStatus, setQrStatus] = useState<QrStatusResult | null>(null);
  const [checkingStatus, setCheckingStatus] = useState(false);

  // Scan form
  const [scanForm, setScanForm] = useState({ encryptedPayload: '', tokenHash: '', totpCode: '' });
  const [scanning, setScanning] = useState(false);
  const [scanResult, setScanResult] = useState<ScanResult | null>(null);

  async function generateQr() {
    if (!genTxId) { toast.error('أدخل معرف المعاملة'); return; }
    setGenerating(true);
    setGenResult(null);
    try {
      const result = await apiClient<QrGenerationResult>(`/qr/generate/${genTxId}`, { method: 'POST' });
      setGenResult(result);
      if (result.tokenHash) {
        setScanForm((c) => ({ ...c, tokenHash: result.tokenHash! }));
        setStatusTokenHash(result.tokenHash);
      }
      toast.success('تم إنشاء رمز QR');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إنشاء رمز QR');
    } finally {
      setGenerating(false);
    }
  }

  async function generateMerchantQr() {
    if (!merchantId) { toast.error('أدخل معرف التاجر'); return; }
    setGeneratingMerchant(true);
    setMerchantQrResult(null);
    try {
      const result = await apiClient<MerchantQrResult>(`/qr/merchant/${merchantId}`, { method: 'POST' });
      setMerchantQrResult(result);
      toast.success('تم إنشاء رمز QR للتاجر');
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إنشاء رمز QR للتاجر');
    } finally {
      setGeneratingMerchant(false);
    }
  }

  async function checkQrStatus() {
    if (!statusTokenHash) { toast.error('أدخل Token Hash'); return; }
    setCheckingStatus(true);
    setQrStatus(null);
    try {
      const result = await apiClient<QrStatusResult>(`/qr/status/${encodeURIComponent(statusTokenHash)}`);
      setQrStatus(result);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التحقق من الحالة');
    } finally {
      setCheckingStatus(false);
    }
  }

  async function scanQr() {
    if (!scanForm.encryptedPayload || !scanForm.tokenHash || !scanForm.totpCode) {
      toast.error('يرجى إدخال جميع حقول المسح');
      return;
    }
    if (scanForm.totpCode.length !== 6) {
      toast.error('رمز TOTP يجب أن يكون 6 أرقام');
      return;
    }
    setScanning(true);
    setScanResult(null);
    try {
      const result = await apiClient<ScanResult>('/qr/scan', {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey() },
        body: JSON.stringify({
          encryptedPayload: scanForm.encryptedPayload,
          tokenHash: scanForm.tokenHash,
          totpCode: scanForm.totpCode,
        })
      });
      setScanResult(result);
      if (result.success) {
        toast.success(`تم المسح بنجاح — المعاملة #${result.transactionId}`);
      } else {
        toast.error(result.message ?? 'فشل التحقق من الرمز');
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل مسح الرمز');
    } finally {
      setScanning(false);
    }
  }

  function downloadQrImage(base64: string, name: string) {
    const link = document.createElement('a');
    link.href = `data:image/png;base64,${base64}`;
    link.download = `${name}.png`;
    link.click();
  }

  return (
    <PageContainer
      pageTitle='رموز QR'
      pageDescription='توليد رموز QR للمعاملات والتجار، مسح الرموز، والتحقق من حالتها.'
    >
      <div className='space-y-4'>
        {/* Tabs */}
        <div className='flex gap-1 border-b border-border pb-0'>
          {([
            ['transaction', 'QR معاملة'],
            ['merchant', 'QR تاجر'],
            ['status', 'حالة الرمز'],
          ] as const).map(([tab, label]) => (
            <button key={tab}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
                activeTab === tab
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => setActiveTab(tab)}>
              {label}
            </button>
          ))}
        </div>

        {activeTab === 'transaction' && (
          <div className='grid gap-4 xl:grid-cols-2'>
            {/* Generate Panel */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>توليد رمز QR للمعاملة</h2>
                <p className='text-muted-foreground text-xs'>يولّد رمز QR مشفر لمعاملة مُعلقة</p>
              </div>
              <div className='p-4 space-y-3'>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>معرف المعاملة <span className='text-destructive'>*</span></Label>
                  <Input value={genTxId} onChange={(e) => setGenTxId(e.target.value)}
                    type='number' className='h-9 rounded-sm' placeholder='مثال: 123' />
                </div>
                <Button className='w-full h-9 rounded-sm' onClick={() => { void generateQr(); }} disabled={generating}>
                  {generating
                    ? <Icons.spinner className='ml-2 size-4 animate-spin' />
                    : <Icons.qr className='ml-2 size-4' />}
                  توليد QR
                </Button>

                {genResult && (
                  <div className='space-y-3'>
                    {genResult.qrImageBase64 && (
                      <div className='bg-white rounded-sm p-3 flex flex-col items-center gap-2'>
                        <img
                          src={`data:image/png;base64,${genResult.qrImageBase64}`}
                          alt='رمز QR'
                          className='w-40 h-40 object-contain'
                        />
                        <Button size='sm' variant='outline' className='h-7 text-xs'
                          onClick={() => downloadQrImage(genResult.qrImageBase64!, `qr-tx-${genResult.transactionId}`)}>
                          <Icons.download className='ml-1 size-3.5' />
                          تنزيل الصورة
                        </Button>
                      </div>
                    )}
                    <div className='bg-muted/60 rounded-sm p-3 space-y-2 text-xs'>
                      {genResult.tokenHash && (
                        <div>
                          <div className='text-muted-foreground mb-0.5'>Token Hash:</div>
                          <div className='flex items-center gap-1.5'>
                            <code className='break-all flex-1' dir='ltr'>{genResult.tokenHash}</code>
                            <Button size='sm' variant='ghost' className='h-6 px-1.5 text-xs shrink-0'
                              onClick={() => {
                                void navigator.clipboard.writeText(genResult.tokenHash!);
                                toast.success('تم النسخ');
                              }}>
                              نسخ
                            </Button>
                          </div>
                        </div>
                      )}
                      {genResult.expiresAt && (
                        <div className='flex justify-between'>
                          <span className='text-muted-foreground'>ينتهي في:</span>
                          <span>{genResult.expiresAt}</span>
                        </div>
                      )}
                      {genResult.transactionId && (
                        <div className='flex justify-between'>
                          <span className='text-muted-foreground'>رقم المعاملة:</span>
                          <span className='font-medium'>#{genResult.transactionId}</span>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Scan Panel */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>مسح رمز QR</h2>
                <p className='text-muted-foreground text-xs'>يتطلب البيانات المشفرة من الرمز + رمز TOTP</p>
              </div>
              <div className='p-4 space-y-3'>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>البيانات المشفرة (Encrypted Payload) <span className='text-destructive'>*</span></Label>
                  <textarea
                    value={scanForm.encryptedPayload}
                    onChange={(e) => setScanForm((c) => ({ ...c, encryptedPayload: e.target.value }))}
                    rows={3}
                    dir='ltr'
                    className='w-full rounded-sm border border-border bg-background px-3 py-2 text-xs font-mono resize-none'
                    placeholder='البيانات المشفرة من QR...' />
                </div>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>Token Hash <span className='text-destructive'>*</span></Label>
                  <Input value={scanForm.tokenHash}
                    onChange={(e) => setScanForm((c) => ({ ...c, tokenHash: e.target.value }))}
                    className='h-9 rounded-sm font-mono text-xs' dir='ltr'
                    placeholder='مملوء تلقائياً عند التوليد...' />
                </div>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>رمز TOTP (6 أرقام) <span className='text-destructive'>*</span></Label>
                  <Input value={scanForm.totpCode}
                    onChange={(e) => setScanForm((c) => ({ ...c, totpCode: e.target.value }))}
                    className='h-9 rounded-sm font-mono tracking-widest text-center text-lg'
                    maxLength={6} placeholder='000000' />
                </div>
                <Button className='w-full h-9 rounded-sm' onClick={() => { void scanQr(); }} disabled={scanning}>
                  {scanning && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                  مسح الرمز والتحقق
                </Button>

                {scanResult && (
                  <div className={`rounded-sm border p-3 space-y-2 text-sm ${
                    scanResult.success ? 'border-emerald-200 bg-emerald-50 dark:bg-emerald-950/20' : 'border-destructive/20 bg-destructive/5'
                  }`}>
                    <div className='flex items-center gap-2 font-medium'>
                      {scanResult.success
                        ? <Icons.check className='size-4 text-emerald-600' />
                        : <Icons.close className='size-4 text-destructive' />}
                      {scanResult.success ? 'تم التحقق بنجاح' : 'فشل التحقق'}
                    </div>
                    {scanResult.receiverName && (
                      <div className='flex justify-between text-xs'>
                        <span className='text-muted-foreground'>المستفيد:</span>
                        <span className='font-medium'>{scanResult.receiverName}</span>
                      </div>
                    )}
                    {scanResult.amount != null && (
                      <div className='flex justify-between text-xs'>
                        <span className='text-muted-foreground'>المبلغ:</span>
                        <span className='font-medium tabular-nums'>{scanResult.amount}</span>
                      </div>
                    )}
                    {scanResult.transactionId && (
                      <div className='flex justify-between text-xs'>
                        <span className='text-muted-foreground'>رقم المعاملة:</span>
                        <span className='font-medium'>#{scanResult.transactionId}</span>
                      </div>
                    )}
                    {scanResult.message && (
                      <div className='text-xs text-muted-foreground'>{scanResult.message}</div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'merchant' && (
          <div className='max-w-md'>
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>رمز QR للتاجر</h2>
                <p className='text-muted-foreground text-xs'>توليد رمز دفع ثابت للتاجر</p>
              </div>
              <div className='p-4 space-y-3'>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>معرف التاجر <span className='text-destructive'>*</span></Label>
                  <Input value={merchantId} onChange={(e) => setMerchantId(e.target.value)}
                    type='number' className='h-9 rounded-sm' placeholder='مثال: 42' />
                </div>
                <Button className='w-full h-9 rounded-sm'
                  onClick={() => { void generateMerchantQr(); }} disabled={generatingMerchant}>
                  {generatingMerchant
                    ? <Icons.spinner className='ml-2 size-4 animate-spin' />
                    : <Icons.qr className='ml-2 size-4' />}
                  توليد QR للتاجر
                </Button>
                {merchantQrResult && (
                  <div className='space-y-3'>
                    {merchantQrResult.qrImageBase64 && (
                      <div className='bg-white rounded-sm p-4 flex flex-col items-center gap-3'>
                        <img
                          src={`data:image/png;base64,${merchantQrResult.qrImageBase64}`}
                          alt='رمز QR التاجر'
                          className='w-48 h-48 object-contain'
                        />
                        <Button size='sm' variant='outline' className='h-7 text-xs'
                          onClick={() => downloadQrImage(merchantQrResult.qrImageBase64!, `merchant-qr-${merchantId}`)}>
                          <Icons.download className='ml-1 size-3.5' />
                          تنزيل الصورة
                        </Button>
                      </div>
                    )}
                    <div className='bg-muted/60 rounded-sm p-3 text-xs space-y-2'>
                      <div className='flex justify-between'>
                        <span className='text-muted-foreground'>التاجر:</span>
                        <span>#{merchantQrResult.merchantId ?? merchantId}</span>
                      </div>
                      {merchantQrResult.paymentUrl && (
                        <div>
                          <div className='text-muted-foreground mb-0.5'>رابط الدفع:</div>
                          <code className='break-all text-xs' dir='ltr'>{merchantQrResult.paymentUrl}</code>
                        </div>
                      )}
                      {merchantQrResult.expiresAt && (
                        <div className='flex justify-between'>
                          <span className='text-muted-foreground'>ينتهي في:</span>
                          <span>{merchantQrResult.expiresAt}</span>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'status' && (
          <div className='max-w-md'>
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>التحقق من حالة رمز QR</h2>
                <p className='text-muted-foreground text-xs'>تحقق إذا تم استخدام الرمز أو انتهت صلاحيته</p>
              </div>
              <div className='p-4 space-y-3'>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>Token Hash <span className='text-destructive'>*</span></Label>
                  <Input value={statusTokenHash} onChange={(e) => setStatusTokenHash(e.target.value)}
                    className='h-9 rounded-sm font-mono text-xs' dir='ltr'
                    placeholder='أدخل Token Hash...' />
                </div>
                <Button variant='outline' className='w-full h-9 rounded-sm'
                  onClick={() => { void checkQrStatus(); }} disabled={checkingStatus}>
                  {checkingStatus && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                  التحقق من الحالة
                </Button>

                {qrStatus && (
                  <div className={`rounded-sm border p-3 space-y-2 text-sm ${
                    qrStatus.valid === false
                      ? 'border-destructive/20 bg-destructive/5'
                      : 'border-border bg-muted/40'
                  }`}>
                    <div className='flex justify-between items-center'>
                      <span className='text-muted-foreground text-xs'>الحالة:</span>
                      <Badge variant={qrStatus.status === 'USED' ? 'secondary' : qrStatus.valid === false ? 'destructive' : 'default'}>
                        {qrStatus.status === 'USED' ? 'مستخدم'
                          : qrStatus.status === 'EXPIRED' ? 'منتهي الصلاحية'
                          : qrStatus.status === 'ACTIVE' ? 'نشط'
                          : qrStatus.status ?? '-'}
                      </Badge>
                    </div>
                    {qrStatus.transactionId && (
                      <div className='flex justify-between text-xs'>
                        <span className='text-muted-foreground'>المعاملة:</span>
                        <span className='font-medium'>#{qrStatus.transactionId}</span>
                      </div>
                    )}
                    {qrStatus.usedAt && (
                      <div className='flex justify-between text-xs'>
                        <span className='text-muted-foreground'>تاريخ الاستخدام:</span>
                        <span>{qrStatus.usedAt}</span>
                      </div>
                    )}
                    {qrStatus.expiresAt && (
                      <div className='flex justify-between text-xs'>
                        <span className='text-muted-foreground'>ينتهي في:</span>
                        <span>{qrStatus.expiresAt}</span>
                      </div>
                    )}
                    {qrStatus.message && (
                      <div className='text-xs text-muted-foreground'>{qrStatus.message}</div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </PageContainer>
  );
}

// ─── Sync Queue ─────────────────────────────────────────────────────────────────

export function SyncQueuePage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [selectedDevice, setSelectedDevice] = useState<string | null>(null);
  const [showConflicts, setShowConflicts] = useState(false);

  const devices = useBackend<SyncDevice[] | { content?: SyncDevice[] }>(`/sync/devices?_t=${tick}`);
  const deviceList = toArray(devices.data as SyncDevice[] | { content?: SyncDevice[] } | null);

  const deviceStatus = useBackend<SyncStatus>(
    selectedDevice ? `/sync/status/${encodeURIComponent(selectedDevice)}?_t=${tick}` : null
  );
  const conflicts = useBackend<SyncConflict[]>(
    selectedDevice && showConflicts ? `/sync/conflicts/${encodeURIComponent(selectedDevice)}?_t=${tick}` : null
  );

  useEffect(() => {
    if (!autoRefresh) return;
    const id = setInterval(refresh, 15_000);
    return () => clearInterval(id);
  }, [autoRefresh, refresh]);

  // Stats
  const totalPending = deviceList.reduce((s, d) => s + (d.pendingItems ?? 0), 0);
  const offlineDevices = deviceList.filter((d) => d.status === 'OFFLINE').length;

  async function processQueue(deviceId: string) {
    try {
      await apiClient(`/sync/process/${encodeURIComponent(deviceId)}`, { method: 'POST', body: JSON.stringify({}) });
      toast.success('تمت معالجة طابور المزامنة');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل المعالجة');
    }
  }

  async function retryFailed(deviceId: string) {
    try {
      await apiClient(`/sync/retry/${encodeURIComponent(deviceId)}`, { method: 'POST' });
      toast.success('تتم إعادة المحاولة');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشلت إعادة المحاولة');
    }
  }

  async function forceSync(deviceId: string) {
    try {
      await apiClient(`/sync/force/${encodeURIComponent(deviceId)}`, { method: 'POST' });
      toast.success('تمت المزامنة الكاملة');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشلت المزامنة الكاملة');
    }
  }

  async function resolveConflict(conflictId: string | number, strategy: 'LOCAL' | 'REMOTE' | 'MANUAL') {
    try {
      await apiClient(`/sync/conflicts/${conflictId}/resolve`, {
        method: 'POST',
        body: JSON.stringify({ strategy }),
      });
      toast.success('تم حل التعارض');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل حل التعارض');
    }
  }

  return (
    <PageContainer
      pageTitle='طابور المزامنة'
      pageDescription='إدارة مزامنة الأجهزة غير المتصلة بالإنترنت، حل التعارضات، ومراقبة حالة الطابور.'
    >
      <div className='space-y-4'>
        {/* Stats */}
        <div className='grid grid-cols-2 gap-3 md:grid-cols-4'>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>إجمالي الأجهزة</div>
            <div className='text-2xl font-bold tabular-nums mt-1'>{deviceList.length}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>إجمالي العناصر المعلقة</div>
            <div className='text-2xl font-bold text-amber-600 tabular-nums mt-1'>{totalPending}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>أجهزة غير متصلة</div>
            <div className='text-2xl font-bold text-destructive tabular-nums mt-1'>{offlineDevices}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>تحديث تلقائي</div>
            <div className='mt-2'>
              <Button
                size='sm'
                variant={autoRefresh ? 'default' : 'outline'}
                className='h-7 rounded-sm text-xs'
                onClick={() => setAutoRefresh((v) => !v)}
              >
                {autoRefresh ? 'إيقاف (15 ث)' : 'تشغيل'}
              </Button>
            </div>
          </div>
        </div>

        {/* Toolbar */}
        <div className='flex gap-2'>
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
        </div>

        {/* Devices Table */}
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>الأجهزة المسجلة</h2>
            {devices.loading && <Icons.spinner className='size-4 animate-spin' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>معرف الجهاز</th>
                  <th>الاسم</th>
                  <th>آخر ظهور</th>
                  <th>معلق</th>
                  <th>الحالة</th>
                  <th>إجراءات</th>
                </tr>
              </thead>
              <tbody>
                {deviceList.map((d) => (
                  <>
                    <tr key={d.deviceId}>
                      <td className='font-mono text-xs'>{d.deviceId ?? '-'}</td>
                      <td className='font-medium text-sm'>{d.deviceName ?? '-'}</td>
                      <td className='text-xs text-muted-foreground'>{d.lastSeen ?? '-'}</td>
                      <td>
                        {(d.pendingItems ?? 0) > 0 ? (
                          <span className='rounded-full bg-amber-500/15 px-2 py-0.5 text-xs font-medium text-amber-700'>
                            {d.pendingItems}
                          </span>
                        ) : (
                          <span className='text-xs text-muted-foreground'>—</span>
                        )}
                      </td>
                      <td>
                        <Badge
                          variant={d.status === 'ONLINE' ? 'default' : d.status === 'OFFLINE' ? 'destructive' : 'secondary'}
                          className='text-xs'
                        >
                          {d.status === 'ONLINE' ? 'متصل' : d.status === 'OFFLINE' ? 'غير متصل' : d.status ?? '-'}
                        </Badge>
                      </td>
                      <td>
                        <div className='flex items-center gap-1'>
                          <Button
                            size='sm' variant='ghost' className='h-7 text-xs px-2'
                            onClick={() => {
                              setSelectedDevice(selectedDevice === d.deviceId ? null : (d.deviceId ?? null));
                              setShowConflicts(false);
                            }}
                            title='عرض الحالة'
                          >
                            <Icons.info className='size-3.5' />
                          </Button>
                          <Button
                            size='sm' variant='outline' className='h-7 text-xs'
                            onClick={() => { void processQueue(d.deviceId ?? ''); }}
                          >
                            معالجة
                          </Button>
                          <Button
                            size='sm' variant='ghost' className='h-7 text-xs'
                            onClick={() => { void retryFailed(d.deviceId ?? ''); }}
                            title='إعادة المحاولة'
                          >
                            <Icons.refresh className='size-3.5' />
                          </Button>
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button size='sm' variant='ghost' className='h-7 text-xs text-amber-600'>
                                مزامنة كاملة
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>مزامنة كاملة</AlertDialogTitle>
                                <AlertDialogDescription>
                                  سيتم فرض مزامنة كاملة للجهاز &quot;{d.deviceName ?? d.deviceId}&quot;. هل أنت متأكد؟
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>إلغاء</AlertDialogCancel>
                                <AlertDialogAction onClick={() => { void forceSync(d.deviceId ?? ''); }}>
                                  تأكيد
                                </AlertDialogAction>
                              </AlertDialogFooter>
                            </AlertDialogContent>
                          </AlertDialog>
                        </div>
                      </td>
                    </tr>
                    {selectedDevice === d.deviceId && (
                      <tr key={`${d.deviceId}-detail`}>
                        <td colSpan={6} className='bg-muted/30 px-4 py-3 space-y-3'>
                          {/* Status Cards */}
                          {deviceStatus.loading && <Icons.spinner className='size-4 animate-spin' />}
                          {deviceStatus.data && (
                            <div className='grid grid-cols-2 gap-2 sm:grid-cols-4'>
                              <div className='rounded-sm border border-border bg-background p-3'>
                                <div className='text-xs text-muted-foreground'>معلقة</div>
                                <div className='text-xl font-bold text-amber-600 tabular-nums mt-1'>
                                  {deviceStatus.data.pendingCount ?? 0}
                                </div>
                              </div>
                              <div className='rounded-sm border border-border bg-background p-3'>
                                <div className='text-xs text-muted-foreground'>فشل</div>
                                <div className='text-xl font-bold text-destructive tabular-nums mt-1'>
                                  {deviceStatus.data.failedCount ?? 0}
                                </div>
                              </div>
                              <div className='rounded-sm border border-border bg-background p-3 col-span-2'>
                                <div className='text-xs text-muted-foreground'>آخر مزامنة</div>
                                <div className='text-sm font-medium mt-1'>{deviceStatus.data.lastSyncAt ?? '-'}</div>
                              </div>
                            </div>
                          )}

                          {/* Conflicts Panel */}
                          <div className='flex items-center gap-2'>
                            <Button
                              size='sm' variant={showConflicts ? 'default' : 'outline'}
                              className='h-7 rounded-sm text-xs'
                              onClick={() => setShowConflicts((v) => !v)}
                            >
                              <Icons.warning className='me-1.5 size-3.5' />
                              التعارضات
                            </Button>
                          </div>

                          {showConflicts && (
                            <div>
                              {conflicts.loading && <Icons.spinner className='size-4 animate-spin' />}
                              {(conflicts.data ?? []).length > 0 ? (
                                <table className='gov-table text-xs'>
                                  <thead>
                                    <tr>
                                      <th>#</th>
                                      <th>نوع الكيان</th>
                                      <th>التاريخ</th>
                                      <th>حل</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {(conflicts.data ?? []).map((c) => (
                                      <tr key={c.id}>
                                        <td className='text-muted-foreground'>{c.id}</td>
                                        <td>{c.entityType ?? '-'}</td>
                                        <td>{c.createdAt ?? '-'}</td>
                                        <td>
                                          <div className='flex gap-1'>
                                            <Button size='sm' variant='outline' className='h-6 text-xs px-2'
                                              onClick={() => { void resolveConflict(c.conflictId ?? c.id, 'LOCAL'); }}>
                                              محلي
                                            </Button>
                                            <Button size='sm' variant='outline' className='h-6 text-xs px-2'
                                              onClick={() => { void resolveConflict(c.conflictId ?? c.id, 'REMOTE'); }}>
                                              خادم
                                            </Button>
                                          </div>
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              ) : !conflicts.loading ? (
                                <p className='text-xs text-muted-foreground'>لا توجد تعارضات.</p>
                              ) : null}
                            </div>
                          )}
                        </td>
                      </tr>
                    )}
                  </>
                ))}
                {!devices.loading && !deviceList.length && (
                  <tr>
                    <td colSpan={6} className='py-12 text-center'>
                      <div className='flex flex-col items-center gap-3'>
                        <Icons.laptop className='size-10 text-muted-foreground/40' />
                        <div className='text-sm text-muted-foreground'>لا توجد أجهزة مسجلة</div>
                      </div>
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

// ─── Fee Zones ─────────────────────────────────────────────────────────────────

export function FeeZonesPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);

  const zones = useBackend<FeeZone[] | { content?: FeeZone[] }>(`/fee-zones?_t=${tick}`);
  const zoneList = toArray(zones.data as FeeZone[] | { content?: FeeZone[] } | null);

  const [selectedZoneId, setSelectedZoneId] = useState<number | null>(null);
  const rules = useBackend<FeeRule[]>(
    selectedZoneId ? `/fee-zones/${selectedZoneId}/rules?_t=${tick}` : null
  );

  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ name: '', description: '', isDefault: false });
  const [creating, setCreating] = useState(false);

  const [editingZone, setEditingZone] = useState<{ id: number; name: string; description: string } | null>(null);
  const [saving, setSaving] = useState(false);

  const [addingRule, setAddingRule] = useState(false);
  const [ruleForm, setRuleForm] = useState({ minAmount: '', maxAmount: '', feeType: 'FLAT', feeValue: '', currency: 'SYP' });

  async function createZone() {
    if (!createForm.name.trim()) { toast.error('اسم المنطقة مطلوب'); return; }
    setCreating(true);
    try {
      await apiClient('/fee-zones', { method: 'POST', body: JSON.stringify(createForm) });
      toast.success('تم إنشاء منطقة الرسوم');
      setShowCreate(false);
      setCreateForm({ name: '', description: '', isDefault: false });
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإنشاء');
    } finally {
      setCreating(false);
    }
  }

  async function updateZone() {
    if (!editingZone) return;
    setSaving(true);
    try {
      await apiClient(`/fee-zones/${editingZone.id}`, {
        method: 'PUT',
        body: JSON.stringify({ name: editingZone.name, description: editingZone.description }),
      });
      toast.success('تم تحديث منطقة الرسوم');
      setEditingZone(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التحديث');
    } finally {
      setSaving(false);
    }
  }

  async function deleteZone(id: number) {
    try {
      await apiClient(`/fee-zones/${id}`, { method: 'DELETE' });
      toast.success('تم حذف منطقة الرسوم');
      if (selectedZoneId === id) setSelectedZoneId(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الحذف');
    }
  }

  async function setDefaultZone(id: number) {
    try {
      await apiClient(`/fee-zones/${id}/default`, { method: 'PUT' });
      toast.success('تم تعيين المنطقة الافتراضية');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التعيين');
    }
  }

  async function addRule(zoneId: number) {
    if (!ruleForm.feeValue) { toast.error('قيمة الرسم مطلوبة'); return; }
    try {
      await apiClient(`/fee-zones/${zoneId}/rules`, {
        method: 'POST',
        body: JSON.stringify({
          ...ruleForm,
          minAmount: ruleForm.minAmount ? Number(ruleForm.minAmount) : undefined,
          maxAmount: ruleForm.maxAmount ? Number(ruleForm.maxAmount) : undefined,
          feeValue: Number(ruleForm.feeValue),
        }),
      });
      toast.success('تمت إضافة قاعدة الرسوم');
      setAddingRule(false);
      setRuleForm({ minAmount: '', maxAmount: '', feeType: 'FLAT', feeValue: '', currency: 'SYP' });
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإضافة');
    }
  }

  async function deleteRule(zoneId: number, ruleId: number) {
    try {
      await apiClient(`/fee-zones/${zoneId}/rules/${ruleId}`, { method: 'DELETE' });
      toast.success('تم حذف القاعدة');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الحذف');
    }
  }

  return (
    <PageContainer
      pageTitle='مناطق الرسوم'
      pageDescription='إنشاء وإدارة مناطق الرسوم والعمولات مع قواعد مفصلة لكل منطقة.'
    >
      <div className='space-y-4'>
        <div className='flex gap-2'>
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
          <Button size='sm' className='h-8 rounded-sm text-xs' onClick={() => setShowCreate(!showCreate)}>
            <Icons.add className='me-1.5 size-3.5' />
            منطقة جديدة
          </Button>
        </div>

        {showCreate && (
          <div className='gov-panel rounded-md border-primary/20 bg-primary/5 max-w-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>إنشاء منطقة رسوم</h2>
            </div>
            <div className='p-4 space-y-3'>
              <div className='space-y-1.5'>
                <Label className='text-xs'>الاسم <span className='text-destructive'>*</span></Label>
                <Input value={createForm.name}
                  onChange={(e) => setCreateForm((f) => ({ ...f, name: e.target.value }))}
                  className='h-8 rounded-sm text-xs' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>الوصف</Label>
                <Input value={createForm.description}
                  onChange={(e) => setCreateForm((f) => ({ ...f, description: e.target.value }))}
                  className='h-8 rounded-sm text-xs' />
              </div>
              <div className='flex items-center gap-2'>
                <input type='checkbox' id='createIsDefault' checked={createForm.isDefault}
                  onChange={(e) => setCreateForm((f) => ({ ...f, isDefault: e.target.checked }))}
                  className='rounded' />
                <label htmlFor='createIsDefault' className='text-xs'>منطقة افتراضية</label>
              </div>
            </div>
            <div className='px-4 pb-4 flex gap-2'>
              <Button size='sm' className='h-8 rounded-sm text-xs'
                onClick={() => { void createZone(); }} disabled={creating}>
                {creating && <Icons.spinner className='me-1.5 size-3.5 animate-spin' />}
                إنشاء
              </Button>
              <Button size='sm' variant='outline' className='h-8 rounded-sm text-xs'
                onClick={() => setShowCreate(false)}>
                إلغاء
              </Button>
            </div>
          </div>
        )}

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>مناطق الرسوم ({zoneList.length})</h2>
            {zones.loading && <Icons.spinner className='size-4 animate-spin' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>الاسم</th>
                  <th>الوصف</th>
                  <th>افتراضي</th>
                  <th>إجراءات</th>
                </tr>
              </thead>
              <tbody>
                {zoneList.map((z) => (
                  <>
                    <tr key={z.id}>
                      <td className='text-muted-foreground'>{z.id}</td>
                      <td>
                        {editingZone?.id === z.id ? (
                          <Input value={editingZone.name}
                            onChange={(e) => setEditingZone((f) => f ? { ...f, name: e.target.value } : f)}
                            className='h-7 rounded-sm text-xs w-40' />
                        ) : (
                          <span className='font-medium'>{z.name ?? '-'}</span>
                        )}
                      </td>
                      <td>
                        {editingZone?.id === z.id ? (
                          <Input value={editingZone.description}
                            onChange={(e) => setEditingZone((f) => f ? { ...f, description: e.target.value } : f)}
                            className='h-7 rounded-sm text-xs w-44' />
                        ) : (
                          <span className='text-xs text-muted-foreground'>{z.description ?? '-'}</span>
                        )}
                      </td>
                      <td>
                        {z.isDefault
                          ? <Badge variant='default' className='text-xs'>افتراضي</Badge>
                          : <Badge variant='outline' className='text-xs'>لا</Badge>}
                      </td>
                      <td>
                        <div className='flex items-center gap-1'>
                          {editingZone?.id === z.id ? (
                            <>
                              <Button size='sm' variant='outline' className='h-7 text-xs px-2'
                                onClick={() => { void updateZone(); }} disabled={saving}>
                                حفظ
                              </Button>
                              <Button size='sm' variant='ghost' className='h-7 text-xs px-1'
                                onClick={() => setEditingZone(null)}>
                                <Icons.close className='size-3' />
                              </Button>
                            </>
                          ) : (
                            <>
                              <Button size='sm' variant='ghost' className='h-7 text-xs px-2'
                                onClick={() => setEditingZone({ id: z.id, name: z.name ?? '', description: z.description ?? '' })}>
                                <Icons.edit className='size-3.5' />
                              </Button>
                              <Button size='sm' variant='ghost' className='h-7 text-xs px-2'
                                onClick={() => setSelectedZoneId(selectedZoneId === z.id ? null : z.id)}
                                title='القواعد'>
                                <Icons.adjustments className='size-3.5' />
                              </Button>
                              {!z.isDefault && (
                                <Button size='sm' variant='ghost' className='h-7 text-xs px-2 text-primary'
                                  onClick={() => { void setDefaultZone(z.id); }}
                                  title='تعيين كافتراضي'>
                                  <Icons.check className='size-3.5' />
                                </Button>
                              )}
                              <AlertDialog>
                                <AlertDialogTrigger asChild>
                                  <Button size='sm' variant='ghost'
                                    className='h-7 text-xs px-2 text-destructive hover:text-destructive'>
                                    <Icons.trash className='size-3.5' />
                                  </Button>
                                </AlertDialogTrigger>
                                <AlertDialogContent>
                                  <AlertDialogHeader>
                                    <AlertDialogTitle>حذف منطقة الرسوم</AlertDialogTitle>
                                    <AlertDialogDescription>
                                      هل أنت متأكد من حذف &quot;{z.name}&quot;؟ سيتم حذف جميع قواعدها أيضاً.
                                    </AlertDialogDescription>
                                  </AlertDialogHeader>
                                  <AlertDialogFooter>
                                    <AlertDialogCancel>إلغاء</AlertDialogCancel>
                                    <AlertDialogAction
                                      className='bg-destructive text-destructive-foreground hover:bg-destructive/90'
                                      onClick={() => { void deleteZone(z.id); }}>
                                      حذف نهائي
                                    </AlertDialogAction>
                                  </AlertDialogFooter>
                                </AlertDialogContent>
                              </AlertDialog>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                    {selectedZoneId === z.id && (
                      <tr key={`${z.id}-rules`}>
                        <td colSpan={5} className='bg-muted/30 px-4 py-3'>
                          <div className='flex items-center justify-between mb-2'>
                            <div className='text-xs font-medium'>قواعد الرسوم — {z.name}</div>
                            <Button size='sm' className='h-7 text-xs rounded-sm'
                              onClick={() => setAddingRule(!addingRule)}>
                              <Icons.add className='me-1 size-3.5' />
                              إضافة قاعدة
                            </Button>
                          </div>

                          {addingRule && (
                            <div className='mb-3 grid gap-2 grid-cols-2 sm:grid-cols-5 bg-background rounded-sm border border-border p-3'>
                              <div className='space-y-1'>
                                <label className='text-xs text-muted-foreground'>من مبلغ</label>
                                <Input value={ruleForm.minAmount} type='number'
                                  onChange={(e) => setRuleForm((f) => ({ ...f, minAmount: e.target.value }))}
                                  className='h-7 rounded-sm text-xs' placeholder='0' />
                              </div>
                              <div className='space-y-1'>
                                <label className='text-xs text-muted-foreground'>إلى مبلغ</label>
                                <Input value={ruleForm.maxAmount} type='number'
                                  onChange={(e) => setRuleForm((f) => ({ ...f, maxAmount: e.target.value }))}
                                  className='h-7 rounded-sm text-xs' placeholder='∞' />
                              </div>
                              <div className='space-y-1'>
                                <label className='text-xs text-muted-foreground'>نوع الرسم</label>
                                <select value={ruleForm.feeType}
                                  onChange={(e) => setRuleForm((f) => ({ ...f, feeType: e.target.value }))}
                                  className='h-7 w-full rounded-sm border border-border bg-background px-2 text-xs'>
                                  <option value='FLAT'>ثابت</option>
                                  <option value='PERCENT'>نسبة مئوية</option>
                                </select>
                              </div>
                              <div className='space-y-1'>
                                <label className='text-xs text-muted-foreground'>القيمة <span className='text-destructive'>*</span></label>
                                <Input value={ruleForm.feeValue} type='number'
                                  onChange={(e) => setRuleForm((f) => ({ ...f, feeValue: e.target.value }))}
                                  className='h-7 rounded-sm text-xs' />
                              </div>
                              <div className='space-y-1'>
                                <label className='text-xs text-muted-foreground'>العملة</label>
                                <Input value={ruleForm.currency}
                                  onChange={(e) => setRuleForm((f) => ({ ...f, currency: e.target.value }))}
                                  className='h-7 rounded-sm text-xs' dir='ltr' />
                              </div>
                              <div className='col-span-2 sm:col-span-5 flex gap-2 pt-1'>
                                <Button size='sm' className='h-7 text-xs rounded-sm'
                                  onClick={() => { void addRule(z.id); }}>
                                  إضافة
                                </Button>
                                <Button size='sm' variant='outline' className='h-7 text-xs rounded-sm'
                                  onClick={() => setAddingRule(false)}>
                                  إلغاء
                                </Button>
                              </div>
                            </div>
                          )}

                          <table className='gov-table text-xs'>
                            <thead>
                              <tr>
                                <th>#</th>
                                <th>من</th>
                                <th>إلى</th>
                                <th>النوع</th>
                                <th>القيمة</th>
                                <th>العملة</th>
                                <th>حذف</th>
                              </tr>
                            </thead>
                            <tbody>
                              {rules.loading && (
                                <tr>
                                  <td colSpan={7} className='text-center py-3'>
                                    <Icons.spinner className='size-4 animate-spin mx-auto' />
                                  </td>
                                </tr>
                              )}
                              {(rules.data ?? []).map((r) => (
                                <tr key={r.id}>
                                  <td>{r.id}</td>
                                  <td className='tabular-nums'>{r.minAmount != null ? fmt(r.minAmount) : '—'}</td>
                                  <td className='tabular-nums'>{r.maxAmount != null ? fmt(r.maxAmount) : '∞'}</td>
                                  <td>{r.feeType === 'FLAT' ? 'ثابت' : 'نسبة'}</td>
                                  <td className='tabular-nums font-medium'>
                                    {fmt(r.feeValue)}{r.feeType === 'PERCENT' ? '%' : ''}
                                  </td>
                                  <td>{r.currency ?? '-'}</td>
                                  <td>
                                    <AlertDialog>
                                      <AlertDialogTrigger asChild>
                                        <Button size='sm' variant='ghost'
                                          className='h-6 text-xs px-1.5 text-destructive hover:text-destructive'>
                                          <Icons.trash className='size-3' />
                                        </Button>
                                      </AlertDialogTrigger>
                                      <AlertDialogContent>
                                        <AlertDialogHeader>
                                          <AlertDialogTitle>حذف القاعدة</AlertDialogTitle>
                                          <AlertDialogDescription>
                                            هل أنت متأكد من حذف هذه القاعدة؟
                                          </AlertDialogDescription>
                                        </AlertDialogHeader>
                                        <AlertDialogFooter>
                                          <AlertDialogCancel>إلغاء</AlertDialogCancel>
                                          <AlertDialogAction
                                            className='bg-destructive text-destructive-foreground hover:bg-destructive/90'
                                            onClick={() => { void deleteRule(z.id, r.id); }}>
                                            حذف
                                          </AlertDialogAction>
                                        </AlertDialogFooter>
                                      </AlertDialogContent>
                                    </AlertDialog>
                                  </td>
                                </tr>
                              ))}
                              {!rules.loading && !(rules.data ?? []).length && (
                                <tr>
                                  <td colSpan={7} className='text-center py-4 text-muted-foreground'>
                                    لا توجد قواعد. أضف قاعدة أولى.
                                  </td>
                                </tr>
                              )}
                            </tbody>
                          </table>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
                {!zones.loading && !zoneList.length && (
                  <tr>
                    <td colSpan={5} className='py-10 text-center text-muted-foreground text-sm'>
                      لا توجد مناطق رسوم. أنشئ منطقة جديدة للبدء.
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

// ─── Tenants ─────────────────────────────────────────────────────────────────

const EMPTY_TENANT = { name: '', domain: '', status: 'ACTIVE', plan: '', contactEmail: '' };

type TenantForm = typeof EMPTY_TENANT;

function TenantDialog({
  open,
  onClose,
  initial,
  onSaved,
}: {
  open: boolean;
  onClose: () => void;
  initial: (TenantForm & { id?: number }) | null;
  onSaved: () => void;
}) {
  const isEdit = Boolean(initial?.id);
  const [form, setForm] = useState<TenantForm>(initial ?? EMPTY_TENANT);
  const [saving, setSaving] = useState(false);

  const set = useCallback((field: keyof TenantForm, value: string) =>
    setForm((p) => ({ ...p, [field]: value })), []);

  // Reset form when dialog opens
  useState(() => { setForm(initial ?? EMPTY_TENANT); });

  async function save() {
    if (!form.name.trim()) { toast.error('اسم المستأجر مطلوب'); return; }
    setSaving(true);
    try {
      if (isEdit && initial?.id) {
        await apiClient(`/platform/tenants/${initial.id}`, {
          method: 'PUT',
          body: JSON.stringify(form),
        });
        toast.success('تم تحديث بيانات المستأجر');
      } else {
        await apiClient('/platform/tenants', {
          method: 'POST',
          body: JSON.stringify(form),
        });
        toast.success('تم إنشاء المستأجر بنجاح');
      }
      onSaved();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الحفظ');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className='sm:max-w-md'>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'تعديل المستأجر' : 'إضافة مستأجر جديد'}</DialogTitle>
          <DialogDescription>
            {isEdit ? 'عدّل بيانات المستأجر ثم احفظ.' : 'أدخل بيانات المستأجر الجديد للمنصة.'}
          </DialogDescription>
        </DialogHeader>

        <div className='grid gap-4 py-2'>
          <div className='grid gap-1.5'>
            <Label className='text-xs'>اسم المستأجر *</Label>
            <Input value={form.name} onChange={(e) => set('name', e.target.value)}
              placeholder='مثال: شركة الأمل للصرافة' className='h-9 rounded-sm' />
          </div>
          <div className='grid gap-1.5'>
            <Label className='text-xs'>النطاق (Domain)</Label>
            <Input value={form.domain} onChange={(e) => set('domain', e.target.value)}
              placeholder='مثال: alamal.almukhtar.app' className='h-9 rounded-sm' dir='ltr' />
          </div>
          <div className='grid gap-1.5'>
            <Label className='text-xs'>البريد الإلكتروني للتواصل</Label>
            <Input value={form.contactEmail} onChange={(e) => set('contactEmail', e.target.value)}
              type='email' placeholder='contact@tenant.com' className='h-9 rounded-sm' dir='ltr' />
          </div>
          <div className='grid grid-cols-2 gap-3'>
            <div className='grid gap-1.5'>
              <Label className='text-xs'>الخطة</Label>
              <Select value={form.plan || 'BASIC'} onValueChange={(v) => set('plan', v)}>
                <SelectTrigger className='h-9 rounded-sm text-sm'><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value='BASIC'>أساسي</SelectItem>
                  <SelectItem value='STANDARD'>قياسي</SelectItem>
                  <SelectItem value='PREMIUM'>مميز</SelectItem>
                  <SelectItem value='ENTERPRISE'>مؤسسي</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className='grid gap-1.5'>
              <Label className='text-xs'>الحالة</Label>
              <Select value={form.status} onValueChange={(v) => set('status', v)}>
                <SelectTrigger className='h-9 rounded-sm text-sm'><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value='ACTIVE'>نشط</SelectItem>
                  <SelectItem value='SUSPENDED'>موقوف</SelectItem>
                  <SelectItem value='INACTIVE'>غير نشط</SelectItem>
                  <SelectItem value='TRIAL'>تجريبي</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant='outline' onClick={onClose} disabled={saving} className='rounded-sm h-9'>
            إلغاء
          </Button>
          <Button onClick={() => { void save(); }} disabled={saving} className='rounded-sm h-9'>
            {saving && <Icons.spinner className='me-2 size-4 animate-spin' />}
            {isEdit ? 'حفظ التعديلات' : 'إنشاء المستأجر'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: 'نشط',
  SUSPENDED: 'موقوف',
  INACTIVE: 'غير نشط',
  TRIAL: 'تجريبي',
};

const PLAN_LABEL: Record<string, string> = {
  BASIC: 'أساسي',
  STANDARD: 'قياسي',
  PREMIUM: 'مميز',
  ENTERPRISE: 'مؤسسي',
};

export function TenantsPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);

  const tenants = useBackend<Tenant[] | { content?: Tenant[] }>(`/platform/tenants?_t=${tick}`);
  const tenantList = toArray(tenants.data as Tenant[] | { content?: Tenant[] } | null);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<(TenantForm & { id?: number }) | null>(null);

  const [search, setSearch] = useState('');
  const [planTab, setPlanTab] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [branchPanelId, setBranchPanelId] = useState<number | null>(null);
  const [provisioningId, setProvisioningId] = useState<number | null>(null);

  const tenantStats = useBackend<{ branchCount?: number; userCount?: number; transactionCount?: number }>(
    expandedId ? `/platform/tenants/${expandedId}/stats?_t=${tick}` : null
  );
  const tenantBranches = useBackend<BranchSummary[]>(
    branchPanelId ? `/platform/tenants/${branchPanelId}/branches?_t=${tick}` : null
  );

  // Derived filter
  const filtered = tenantList.filter((t) => {
    if (planTab && t.plan !== planTab) return false;
    if (search) {
      const q = search.toLowerCase();
      return (
        (t.name ?? '').toLowerCase().includes(q) ||
        (t.domain ?? '').toLowerCase().includes(q) ||
        (t.contactEmail ?? '').toLowerCase().includes(q)
      );
    }
    return true;
  });

  // Stats
  const totalActive = tenantList.filter((t) => t.status === 'ACTIVE').length;
  const totalSuspended = tenantList.filter((t) => t.status === 'SUSPENDED').length;
  const totalTrial = tenantList.filter((t) => t.status === 'TRIAL').length;

  function openCreate() {
    setEditing(null);
    setDialogOpen(true);
  }

  function openEdit(t: Tenant) {
    setEditing({
      id: t.id,
      name: t.name ?? '',
      domain: t.domain ?? '',
      status: t.status ?? 'ACTIVE',
      plan: t.plan ?? 'BASIC',
      contactEmail: t.contactEmail ?? '',
    });
    setDialogOpen(true);
  }

  async function deleteTenant(id: number) {
    try {
      await apiClient(`/platform/tenants/${id}`, { method: 'DELETE' });
      toast.success('تم حذف المستأجر');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الحذف');
    }
  }

  async function toggleStatus(t: Tenant) {
    const next = t.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
      await apiClient(`/platform/tenants/${t.id}`, {
        method: 'PUT',
        body: JSON.stringify({ ...t, status: next }),
      });
      toast.success(`تم ${next === 'ACTIVE' ? 'تفعيل' : 'إيقاف'} المستأجر`);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التغيير');
    }
  }

  async function provisionTenant(id: number) {
    setProvisioningId(id);
    try {
      await apiClient(`/platform/tenants/${id}/provision`, { method: 'POST' });
      toast.success('تم تهيئة المستأجر — تم إنشاء الفرع الافتراضي والمشرف');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التهيئة');
    } finally {
      setProvisioningId(null);
    }
  }

  return (
    <PageContainer
      pageTitle='إدارة المستأجرين'
      pageDescription='إنشاء وإدارة مستأجري المنصة (Multi-Tenant). لكل مستأجر فروعه وموظفوه وعملياته المستقلة.'
    >
      <TenantDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        initial={editing}
        onSaved={refresh}
      />

      <div className='space-y-4'>
        {/* Stats Cards */}
        <div className='grid grid-cols-2 gap-3 md:grid-cols-4'>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>إجمالي</div>
            <div className='text-2xl font-bold tabular-nums mt-1'>{tenantList.length}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>نشط</div>
            <div className='text-2xl font-bold text-emerald-600 tabular-nums mt-1'>{totalActive}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>موقوف</div>
            <div className='text-2xl font-bold text-amber-600 tabular-nums mt-1'>{totalSuspended}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>تجريبي</div>
            <div className='text-2xl font-bold text-blue-600 tabular-nums mt-1'>{totalTrial}</div>
          </div>
        </div>

        {/* Filters */}
        <div className='flex flex-wrap items-end gap-3'>
          <div>
            <label className='text-xs mb-1 block text-muted-foreground'>بحث</label>
            <Input value={search} onChange={(e) => setSearch(e.target.value)}
              placeholder='الاسم، النطاق، البريد...' className='h-8 rounded-sm text-xs w-52' />
          </div>
          <Button variant='outline' size='sm' className='h-8 rounded-sm text-xs' onClick={refresh}>
            <Icons.spinner className='me-1.5 size-3.5' />
            تحديث
          </Button>
          <Button size='sm' className='h-8 rounded-sm text-xs mr-auto' onClick={openCreate}>
            <Icons.add className='me-1.5 size-3.5' />
            إضافة مستأجر
          </Button>
        </div>

        {/* Plan Tabs */}
        <div className='flex gap-1 border-b border-border pb-0'>
          {([
            ['', 'الكل'],
            ['BASIC', 'أساسي'],
            ['STANDARD', 'قياسي'],
            ['PREMIUM', 'مميز'],
            ['ENTERPRISE', 'مؤسسي'],
          ] as [string, string][]).map(([tab, label]) => (
            <button key={tab}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
                planTab === tab
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => setPlanTab(tab)}>
              {label}
            </button>
          ))}
        </div>

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <div className='flex items-center gap-3'>
              <h2 className='text-sm font-semibold'>
                المستأجرون
                <span className='text-muted-foreground font-normal mr-1'>({filtered.length})</span>
              </h2>
              {tenants.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
            </div>
          </div>

          {tenants.error && (
            <div className='border-b border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive'>
              تعذر تحميل البيانات: {tenants.error}
            </div>
          )}

          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>المستأجر</th>
                  <th>النطاق</th>
                  <th>الخطة</th>
                  <th>الحالة</th>
                  <th>تاريخ الإنشاء</th>
                  <th>إجراءات</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((t) => (
                  <>
                    <tr key={t.id}>
                      <td className='text-muted-foreground text-xs'>{t.id}</td>
                      <td>
                        <div className='font-medium text-sm'>{t.name ?? '-'}</div>
                        {t.contactEmail && (
                          <div className='text-muted-foreground text-xs'>{t.contactEmail}</div>
                        )}
                      </td>
                      <td className='text-muted-foreground text-xs font-mono'>{t.domain ?? '-'}</td>
                      <td>
                        {t.plan ? (
                          <Badge variant='outline' className='text-xs'>
                            {PLAN_LABEL[t.plan] ?? t.plan}
                          </Badge>
                        ) : '-'}
                      </td>
                      <td>
                        <Badge
                          variant={
                            t.status === 'ACTIVE' ? 'default'
                              : t.status === 'SUSPENDED' ? 'destructive'
                              : t.status === 'TRIAL' ? 'secondary'
                              : 'outline'
                          }
                          className='text-xs'
                        >
                          {STATUS_LABEL[t.status ?? ''] ?? t.status ?? '-'}
                        </Badge>
                      </td>
                      <td className='text-xs text-muted-foreground'>{t.createdAt ?? '-'}</td>
                      <td>
                        <div className='flex items-center gap-1'>
                          <Button variant='ghost' size='sm' className='h-7 rounded-sm text-xs'
                            onClick={() => {
                              setExpandedId(expandedId === t.id ? null : t.id);
                              setBranchPanelId(null);
                            }}
                            title='تفاصيل'>
                            <Icons.info className='size-3.5' />
                          </Button>
                          <Button variant='ghost' size='sm' className='h-7 rounded-sm text-xs'
                            onClick={() => openEdit(t)}>
                            <Icons.edit className='size-3.5' />
                          </Button>
                          <Button variant='ghost' size='sm' className='h-7 rounded-sm text-xs'
                            onClick={() => { void toggleStatus(t); }}
                            title={t.status === 'ACTIVE' ? 'إيقاف' : 'تفعيل'}>
                            {t.status === 'ACTIVE'
                              ? <Icons.close className='size-3.5 text-amber-600' />
                              : <Icons.check className='size-3.5 text-emerald-600' />}
                          </Button>
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button variant='ghost' size='sm'
                                className='h-7 rounded-sm text-xs text-destructive hover:text-destructive'>
                                <Icons.trash className='size-3.5' />
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>حذف المستأجر</AlertDialogTitle>
                                <AlertDialogDescription>
                                  هل أنت متأكد من حذف &quot;{t.name}&quot;؟ لا يمكن التراجع عن هذا الإجراء.
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>إلغاء</AlertDialogCancel>
                                <AlertDialogAction
                                  className='bg-destructive text-destructive-foreground hover:bg-destructive/90'
                                  onClick={() => { void deleteTenant(t.id); }}>
                                  حذف نهائي
                                </AlertDialogAction>
                              </AlertDialogFooter>
                            </AlertDialogContent>
                          </AlertDialog>
                        </div>
                      </td>
                    </tr>
                    {expandedId === t.id && (
                      <tr key={`${t.id}-detail`}>
                        <td colSpan={7} className='bg-muted/30 px-4 py-3 space-y-3'>
                          {/* Stats */}
                          {tenantStats.loading && <Icons.spinner className='size-4 animate-spin' />}
                          {tenantStats.data && (
                            <div className='grid grid-cols-3 gap-2 max-w-sm'>
                              <div className='rounded-sm border border-border bg-background p-2.5'>
                                <div className='text-xs text-muted-foreground'>الفروع</div>
                                <div className='text-lg font-bold tabular-nums'>{tenantStats.data.branchCount ?? 0}</div>
                              </div>
                              <div className='rounded-sm border border-border bg-background p-2.5'>
                                <div className='text-xs text-muted-foreground'>المستخدمون</div>
                                <div className='text-lg font-bold tabular-nums'>{tenantStats.data.userCount ?? 0}</div>
                              </div>
                              <div className='rounded-sm border border-border bg-background p-2.5'>
                                <div className='text-xs text-muted-foreground'>المعاملات</div>
                                <div className='text-lg font-bold tabular-nums'>{tenantStats.data.transactionCount ?? 0}</div>
                              </div>
                            </div>
                          )}

                          <div className='flex gap-2 flex-wrap'>
                            <Button size='sm' variant={branchPanelId === t.id ? 'default' : 'outline'}
                              className='h-7 rounded-sm text-xs'
                              onClick={() => setBranchPanelId(branchPanelId === t.id ? null : t.id)}>
                              <Icons.workspace className='me-1.5 size-3.5' />
                              الفروع
                            </Button>
                            <AlertDialog>
                              <AlertDialogTrigger asChild>
                                <Button size='sm' variant='outline' className='h-7 rounded-sm text-xs'
                                  disabled={provisioningId === t.id}>
                                  {provisioningId === t.id
                                    ? <Icons.spinner className='me-1.5 size-3.5 animate-spin' />
                                    : <Icons.add className='me-1.5 size-3.5' />}
                                  تهيئة المستأجر
                                </Button>
                              </AlertDialogTrigger>
                              <AlertDialogContent>
                                <AlertDialogHeader>
                                  <AlertDialogTitle>تهيئة المستأجر</AlertDialogTitle>
                                  <AlertDialogDescription>
                                    سيتم إنشاء فرع افتراضي ومشرف للمستأجر &quot;{t.name}&quot;. هل أنت متأكد؟
                                  </AlertDialogDescription>
                                </AlertDialogHeader>
                                <AlertDialogFooter>
                                  <AlertDialogCancel>إلغاء</AlertDialogCancel>
                                  <AlertDialogAction onClick={() => { void provisionTenant(t.id); }}>
                                    تهيئة
                                  </AlertDialogAction>
                                </AlertDialogFooter>
                              </AlertDialogContent>
                            </AlertDialog>
                          </div>

                          {/* Branches Panel */}
                          {branchPanelId === t.id && (
                            <div>
                              {tenantBranches.loading && <Icons.spinner className='size-4 animate-spin' />}
                              {(tenantBranches.data ?? []).length > 0 ? (
                                <table className='gov-table text-xs'>
                                  <thead>
                                    <tr><th>#</th><th>الفرع</th><th>المدينة</th><th>الحالة</th></tr>
                                  </thead>
                                  <tbody>
                                    {(tenantBranches.data ?? []).map((b) => (
                                      <tr key={b.id}>
                                        <td>{b.id}</td>
                                        <td className='font-medium'>{b.name}</td>
                                        <td>{b.city ?? '-'}</td>
                                        <td>
                                          <Badge variant={b.status === 'ACTIVE' ? 'default' : 'secondary'} className='text-xs'>
                                            {b.status ?? '-'}
                                          </Badge>
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              ) : !tenantBranches.loading ? (
                                <p className='text-xs text-muted-foreground'>لا توجد فروع لهذا المستأجر.</p>
                              ) : null}
                            </div>
                          )}
                        </td>
                      </tr>
                    )}
                  </>
                ))}
                {!tenants.loading && !filtered.length && (
                  <tr>
                    <td colSpan={7} className='py-12 text-center'>
                      <div className='flex flex-col items-center gap-3'>
                        <Icons.workspace className='size-10 text-muted-foreground/40' />
                        <div>
                          <div className='text-sm font-medium'>
                            {search || planTab ? 'لا توجد نتائج تطابق البحث' : 'لا توجد مستأجرون حتى الآن'}
                          </div>
                          <div className='text-muted-foreground text-xs mt-1'>
                            {!search && !planTab && 'ابدأ بإضافة أول مستأجر للمنصة'}
                          </div>
                        </div>
                        {!search && !planTab && (
                          <Button size='sm' className='h-8 rounded-sm text-xs mt-2' onClick={openCreate}>
                            <Icons.add className='me-1.5 size-3.5' />
                            إضافة مستأجر
                          </Button>
                        )}
                      </div>
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

// ─── Corporate Accounts ────────────────────────────────────────────────────────

export function CorporatePage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);

  const accounts = useBackend<CorporateAccount[] | { content?: CorporateAccount[] }>(
    `/corporate/accounts?_t=${tick}`
  );
  const accountList = toArray(accounts.data as CorporateAccount[] | { content?: CorporateAccount[] } | null);

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [activePanel, setActivePanel] = useState<'sub' | 'statement' | null>(null);

  const subAccounts = useBackend<CorporateSubAccount[]>(
    selectedUserId && activePanel === 'sub' ? `/corporate/${selectedUserId}/sub-accounts?_t=${tick}` : null
  );
  const statement = useBackend<CorporateStatement[]>(
    selectedUserId && activePanel === 'statement' ? `/corporate/${selectedUserId}/statement?_t=${tick}` : null
  );

  const [adjustingId, setAdjustingId] = useState<number | null>(null);
  const [creditLimitValue, setCreditLimitValue] = useState('');

  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({
    parentUserId: '',
    companyName: '',
    registrationNumber: '',
    creditLimit: '',
    currency: 'SYP',
  });
  const [creating, setCreating] = useState(false);

  // Stats
  const totalCreditLimit = accountList.reduce((s, a) => s + Number(a.creditLimit ?? 0), 0);
  const totalUsedCredit = accountList.reduce((s, a) => s + Number(a.usedCredit ?? 0), 0);
  const activeCount = accountList.filter((a) => a.status === 'ACTIVE').length;

  async function createAccount() {
    if (!createForm.parentUserId || !createForm.companyName) {
      toast.error('معرف المستخدم واسم الشركة مطلوبان');
      return;
    }
    setCreating(true);
    try {
      await apiClient('/corporate/accounts', {
        method: 'POST',
        body: JSON.stringify({
          ...createForm,
          parentUserId: Number(createForm.parentUserId),
          creditLimit: Number(createForm.creditLimit || 0),
        }),
      });
      toast.success('تم إنشاء الحساب المؤسسي');
      setShowCreate(false);
      setCreateForm({ parentUserId: '', companyName: '', registrationNumber: '', creditLimit: '', currency: 'SYP' });
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإنشاء');
    } finally {
      setCreating(false);
    }
  }

  async function adjustCreditLimit(id: number) {
    if (!creditLimitValue) { toast.error('أدخل الحد الائتماني الجديد'); return; }
    try {
      await apiClient(`/corporate/${id}/credit-limit?limit=${creditLimitValue}`, { method: 'PUT' });
      toast.success('تم تعديل الحد الائتماني');
      setAdjustingId(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التعديل');
    }
  }

  async function toggleAccountStatus(a: CorporateAccount) {
    const endpoint = a.status === 'ACTIVE' ? 'suspend' : 'reactivate';
    try {
      await apiClient(`/corporate/${a.id}/${endpoint}`, { method: 'POST' });
      toast.success(a.status === 'ACTIVE' ? 'تم إيقاف الحساب' : 'تم تفعيل الحساب');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تغيير الحالة');
    }
  }

  function selectPanel(a: CorporateAccount, panel: 'sub' | 'statement') {
    if (selectedId === a.id && activePanel === panel) {
      setSelectedId(null);
      setActivePanel(null);
    } else {
      setSelectedId(a.id);
      setSelectedUserId(a.parentUserId ?? null);
      setActivePanel(panel);
    }
  }

  return (
    <PageContainer
      pageTitle='الحسابات المؤسسية'
      pageDescription='إدارة حسابات الشركات والحدود الائتمانية والحسابات الفرعية وكشوف الحساب.'
    >
      <div className='space-y-4'>
        {/* Stats */}
        <div className='grid grid-cols-2 gap-3 md:grid-cols-4'>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>إجمالي الحسابات</div>
            <div className='text-2xl font-bold tabular-nums mt-1'>{accountList.length}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>نشط</div>
            <div className='text-2xl font-bold text-emerald-600 tabular-nums mt-1'>{activeCount}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>إجمالي الحد الائتماني</div>
            <div className='text-lg font-bold tabular-nums mt-1'>{fmt(totalCreditLimit)}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>الائتمان المستخدم</div>
            <div className='text-lg font-bold text-amber-600 tabular-nums mt-1'>{fmt(totalUsedCredit)}</div>
          </div>
        </div>

        <div className='flex gap-2'>
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
          <Button size='sm' className='h-8 rounded-sm text-xs' onClick={() => setShowCreate(!showCreate)}>
            <Icons.add className='me-1.5 size-3.5' />
            حساب مؤسسي جديد
          </Button>
        </div>

        {showCreate && (
          <div className='gov-panel rounded-md border-primary/20 bg-primary/5 max-w-lg'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>إنشاء حساب مؤسسي</h2>
            </div>
            <div className='p-4 grid gap-3 sm:grid-cols-2'>
              <div className='space-y-1.5'>
                <Label className='text-xs'>معرف المستخدم الرئيسي <span className='text-destructive'>*</span></Label>
                <Input value={createForm.parentUserId} type='number'
                  onChange={(e) => setCreateForm((f) => ({ ...f, parentUserId: e.target.value }))}
                  className='h-8 rounded-sm text-xs' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>اسم الشركة <span className='text-destructive'>*</span></Label>
                <Input value={createForm.companyName}
                  onChange={(e) => setCreateForm((f) => ({ ...f, companyName: e.target.value }))}
                  className='h-8 rounded-sm text-xs' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>رقم التسجيل</Label>
                <Input value={createForm.registrationNumber}
                  onChange={(e) => setCreateForm((f) => ({ ...f, registrationNumber: e.target.value }))}
                  className='h-8 rounded-sm text-xs' dir='ltr' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>الحد الائتماني</Label>
                <Input value={createForm.creditLimit} type='number'
                  onChange={(e) => setCreateForm((f) => ({ ...f, creditLimit: e.target.value }))}
                  className='h-8 rounded-sm text-xs' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>العملة</Label>
                <select value={createForm.currency}
                  onChange={(e) => setCreateForm((f) => ({ ...f, currency: e.target.value }))}
                  className='h-8 w-full rounded-sm border border-border bg-background px-2 text-xs'>
                  <option value='SYP'>SYP</option>
                  <option value='USD'>USD</option>
                  <option value='EUR'>EUR</option>
                </select>
              </div>
            </div>
            <div className='px-4 pb-4 flex gap-2'>
              <Button size='sm' className='h-8 rounded-sm text-xs'
                onClick={() => { void createAccount(); }} disabled={creating}>
                {creating && <Icons.spinner className='me-1.5 size-3.5 animate-spin' />}
                إنشاء
              </Button>
              <Button size='sm' variant='outline' className='h-8 rounded-sm text-xs'
                onClick={() => setShowCreate(false)}>
                إلغاء
              </Button>
            </div>
          </div>
        )}

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>الحسابات المؤسسية ({accountList.length})</h2>
            {accounts.loading && <Icons.spinner className='size-4 animate-spin' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>الشركة</th>
                  <th>رقم التسجيل</th>
                  <th>الحد الائتماني</th>
                  <th>المستخدم</th>
                  <th>الحالة</th>
                  <th>إجراءات</th>
                </tr>
              </thead>
              <tbody>
                {accountList.map((a) => (
                  <>
                    <tr key={a.id}>
                      <td className='text-muted-foreground text-xs'>{a.id}</td>
                      <td>
                        <div className='font-medium text-sm'>{a.companyName ?? '-'}</div>
                        <div className='text-xs text-muted-foreground'>{a.createdAt ?? ''}</div>
                      </td>
                      <td className='text-xs font-mono'>{a.registrationNumber ?? '-'}</td>
                      <td>
                        {adjustingId === a.id ? (
                          <div className='flex items-center gap-1'>
                            <Input value={creditLimitValue} type='number'
                              onChange={(e) => setCreditLimitValue(e.target.value)}
                              className='h-7 w-28 rounded-sm text-xs' />
                            <Button size='sm' variant='outline' className='h-7 text-xs px-2'
                              onClick={() => { void adjustCreditLimit(a.id); }}>
                              حفظ
                            </Button>
                            <Button size='sm' variant='ghost' className='h-7 text-xs px-1'
                              onClick={() => setAdjustingId(null)}>
                              <Icons.close className='size-3' />
                            </Button>
                          </div>
                        ) : (
                          <div className='flex items-center gap-1.5 text-sm'>
                            <span className='tabular-nums'>{fmt(a.creditLimit)}</span>
                            {a.usedCredit != null && (
                              <span className='text-xs text-muted-foreground'>
                                ({fmt(a.usedCredit)} مستخدم)
                              </span>
                            )}
                            <Button size='sm' variant='ghost' className='h-6 text-xs px-1'
                              onClick={() => { setAdjustingId(a.id); setCreditLimitValue(String(a.creditLimit ?? '')); }}>
                              <Icons.edit className='size-3' />
                            </Button>
                          </div>
                        )}
                      </td>
                      <td className='text-xs text-muted-foreground'>{a.parentUserId ?? '-'}</td>
                      <td>
                        <Badge variant={a.status === 'ACTIVE' ? 'default' : 'destructive'} className='text-xs'>
                          {a.status === 'ACTIVE' ? 'نشط' : a.status === 'SUSPENDED' ? 'موقوف' : a.status ?? '-'}
                        </Badge>
                      </td>
                      <td>
                        <div className='flex items-center gap-1'>
                          <Button size='sm' variant='ghost' className='h-7 text-xs px-2'
                            onClick={() => selectPanel(a, 'sub')} title='الحسابات الفرعية'>
                            <Icons.teams className='size-3.5' />
                          </Button>
                          <Button size='sm' variant='ghost' className='h-7 text-xs px-2'
                            onClick={() => selectPanel(a, 'statement')} title='كشف الحساب'>
                            <Icons.fileTypeXls className='size-3.5' />
                          </Button>
                          <Button size='sm' variant='ghost'
                            className={`h-7 text-xs px-2 ${a.status === 'ACTIVE' ? 'text-amber-600' : 'text-emerald-600'}`}
                            onClick={() => { void toggleAccountStatus(a); }}>
                            {a.status === 'ACTIVE'
                              ? <Icons.lock className='size-3.5' />
                              : <Icons.unlock className='size-3.5' />}
                          </Button>
                        </div>
                      </td>
                    </tr>

                    {selectedId === a.id && activePanel === 'sub' && (
                      <tr key={`${a.id}-sub`}>
                        <td colSpan={7} className='bg-muted/30 px-4 py-3'>
                          <div className='text-xs font-medium mb-2'>الحسابات الفرعية</div>
                          {subAccounts.loading && <Icons.spinner className='size-4 animate-spin' />}
                          <table className='gov-table text-xs'>
                            <thead>
                              <tr><th>#</th><th>الحساب</th><th>الرصيد</th><th>الحالة</th></tr>
                            </thead>
                            <tbody>
                              {toArray(subAccounts.data).map((s) => (
                                <tr key={s.id}>
                                  <td>{s.id}</td>
                                  <td>{s.label ?? s.accountNumber ?? '-'}</td>
                                  <td className='tabular-nums'>{fmt(s.balance)} {s.currency ?? ''}</td>
                                  <td>
                                    <Badge variant={s.status === 'ACTIVE' ? 'default' : 'secondary'} className='text-xs'>
                                      {s.status ?? '-'}
                                    </Badge>
                                  </td>
                                </tr>
                              ))}
                              {!subAccounts.loading && !toArray(subAccounts.data).length && (
                                <tr>
                                  <td colSpan={4} className='text-muted-foreground text-center py-4'>
                                    لا توجد حسابات فرعية.
                                  </td>
                                </tr>
                              )}
                            </tbody>
                          </table>
                        </td>
                      </tr>
                    )}

                    {selectedId === a.id && activePanel === 'statement' && (
                      <tr key={`${a.id}-stmt`}>
                        <td colSpan={7} className='bg-muted/30 px-4 py-3'>
                          <div className='text-xs font-medium mb-2'>كشف الحساب</div>
                          {statement.loading && <Icons.spinner className='size-4 animate-spin' />}
                          <table className='gov-table text-xs'>
                            <thead>
                              <tr><th>#</th><th>التاريخ</th><th>الوصف</th><th>المبلغ</th><th>الرصيد</th></tr>
                            </thead>
                            <tbody>
                              {toArray(statement.data).map((s) => (
                                <tr key={s.id}>
                                  <td>{s.id}</td>
                                  <td className='font-mono'>{s.date ?? '-'}</td>
                                  <td>{s.description ?? '-'}</td>
                                  <td className={`tabular-nums font-medium ${Number(s.amount ?? 0) >= 0 ? 'text-emerald-600' : 'text-destructive'}`}>
                                    {fmt(s.amount)}
                                  </td>
                                  <td className='tabular-nums'>{fmt(s.balance)}</td>
                                </tr>
                              ))}
                              {!statement.loading && !toArray(statement.data).length && (
                                <tr>
                                  <td colSpan={5} className='text-muted-foreground text-center py-4'>
                                    لا توجد حركات.
                                  </td>
                                </tr>
                              )}
                            </tbody>
                          </table>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
                {!accounts.loading && !accountList.length && (
                  <tr>
                    <td colSpan={7} className='py-12 text-center'>
                      <div className='flex flex-col items-center gap-3'>
                        <Icons.workspace className='size-10 text-muted-foreground/40' />
                        <div className='text-sm text-muted-foreground'>لا توجد حسابات مؤسسية حتى الآن</div>
                      </div>
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

// ─── Wallet Top-Up (Admin) ────────────────────────────────────────────────────

type TopupRow = {
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

export function WalletTopUpAdminPage() {
  const [branchId, setBranchId] = useState('1');
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);
  const [statusTab, setStatusTab] = useState<'PENDING' | 'COMPLETED' | 'REJECTED' | ''>('PENDING');

  const pending = useBackend<TopupRow[]>(
    branchId ? `/wallet/topup/pending?branchId=${branchId}&_t=${tick}` : null
  );
  const allRequests = useBackend<TopupRow[]>(
    branchId && statusTab !== 'PENDING' ? `/wallet/topup?branchId=${branchId}&status=${statusTab}&_t=${tick}` : null
  );

  const displayedRequests = statusTab === 'PENDING' ? (pending.data ?? []) : (allRequests.data ?? []);
  const isLoading = statusTab === 'PENDING' ? pending.loading : allRequests.loading;

  const [rejectForm, setRejectForm] = useState<{ id: number; note: string } | null>(null);
  const moneyFmt2 = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });

  // Stats from pending
  const totalPendingAmount = (pending.data ?? []).reduce((sum, r) => sum + Number(r.amount ?? 0), 0);

  async function complete(id: number) {
    try {
      await apiClient(`/wallet/topup/${id}/complete`, {
        method: 'PUT',
        headers: { 'Idempotency-Key': idempotencyKey() },
      });
      toast.success('تمت الموافقة على طلب التعبئة');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإتمام');
    }
  }

  async function reject(id: number, note: string) {
    try {
      await apiClient(`/wallet/topup/${id}/reject`, {
        method: 'PUT',
        body: JSON.stringify({ note: note || undefined }),
        headers: { 'Idempotency-Key': idempotencyKey() },
      });
      toast.success('تم رفض طلب التعبئة');
      setRejectForm(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الرفض');
    }
  }

  return (
    <PageContainer
      pageTitle='طلبات التعبئة (إدارة)'
      pageDescription='مراجعة طلبات تعبئة المحافظ الواردة للفرع والموافقة عليها أو رفضها.'
    >
      <div className='space-y-4'>
        <div className='flex items-center gap-3 flex-wrap'>
          <Label className='text-xs whitespace-nowrap'>
            الفرع <span className='text-destructive'>*</span>
          </Label>
          <Input value={branchId} onChange={(e) => { setBranchId(e.target.value); refresh(); }}
            className='h-8 w-28 rounded-sm' type='number' />
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
        </div>

        {/* Stats Cards */}
        {(pending.data ?? []).length > 0 && (
          <div className='grid grid-cols-2 gap-3 md:grid-cols-4'>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>معلقة</div>
              <div className='text-2xl font-bold text-amber-600 tabular-nums'>{pending.data?.length ?? 0}</div>
            </div>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>إجمالي المبالغ المعلقة</div>
              <div className='text-xl font-bold tabular-nums'>{moneyFmt2.format(totalPendingAmount)}</div>
            </div>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>متوسط الطلب</div>
              <div className='text-xl font-bold tabular-nums'>
                {(pending.data?.length ?? 0) > 0
                  ? moneyFmt2.format(totalPendingAmount / (pending.data?.length ?? 1))
                  : '-'}
              </div>
            </div>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>أكبر طلب</div>
              <div className='text-xl font-bold tabular-nums'>
                {moneyFmt2.format(Math.max(...(pending.data ?? []).map((r) => Number(r.amount ?? 0)), 0))}
              </div>
            </div>
          </div>
        )}

        {/* Status Tabs */}
        <div className='flex gap-1 border-b border-border pb-0'>
          {([
            ['PENDING', 'معلقة'],
            ['COMPLETED', 'مكتملة'],
            ['REJECTED', 'مرفوضة'],
          ] as const).map(([tab, label]) => (
            <button key={tab}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
                statusTab === tab
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => setStatusTab(tab)}>
              {label}
              {tab === 'PENDING' && (pending.data?.length ?? 0) > 0 && (
                <span className='mr-1.5 rounded-full bg-amber-500/20 px-1.5 py-0.5 text-xs text-amber-700'>
                  {pending.data?.length}
                </span>
              )}
            </button>
          ))}
        </div>

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>
              {statusTab === 'PENDING' ? 'طلبات التعبئة المعلقة'
                : statusTab === 'COMPLETED' ? 'الطلبات المكتملة'
                : 'الطلبات المرفوضة'}
            </h2>
            {isLoading && <Icons.spinner className='size-4 animate-spin' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>المستخدم</th>
                  <th>رقم المحفظة</th>
                  <th>المبلغ</th>
                  <th>العملة</th>
                  <th>الفرع</th>
                  <th>وقت الطلب</th>
                  <th>إجراء</th>
                </tr>
              </thead>
              <tbody>
                {displayedRequests.map((req) => (
                  <tr key={req.id}>
                    <td className='text-muted-foreground'>{req.id}</td>
                    <td>
                      <div className='font-medium text-sm'>{req.username ?? '-'}</div>
                      {req.userId && <div className='text-xs text-muted-foreground'>#{req.userId}</div>}
                    </td>
                    <td className='text-xs text-muted-foreground'>{req.walletId ?? '-'}</td>
                    <td className='tabular-nums font-semibold'>{moneyFmt2.format(Number(req.amount ?? 0))}</td>
                    <td>{req.currency ?? '-'}</td>
                    <td>{req.branchId ?? '-'}</td>
                    <td className='text-xs text-muted-foreground'>{req.createdAt ?? '-'}</td>
                    <td>
                      {statusTab === 'PENDING' && (
                        <div className='flex items-center gap-1'>
                          <Button size='sm' variant='outline'
                            className='h-7 text-xs text-emerald-600 border-emerald-200'
                            onClick={() => { void complete(req.id); }}>
                            <Icons.check className='ml-1 size-3' />
                            إتمام
                          </Button>
                          <Button size='sm' variant='ghost'
                            className='h-7 text-xs text-destructive hover:text-destructive'
                            onClick={() => setRejectForm({ id: req.id, note: '' })}>
                            رفض
                          </Button>
                        </div>
                      )}
                      {statusTab !== 'PENDING' && (
                        <Badge variant={req.status === 'COMPLETED' ? 'default' : 'destructive'} className='text-xs'>
                          {req.status === 'COMPLETED' ? 'مكتمل' : 'مرفوض'}
                        </Badge>
                      )}
                    </td>
                  </tr>
                ))}
                {!isLoading && !displayedRequests.length && (
                  <tr>
                    <td colSpan={8} className='py-10 text-center text-muted-foreground text-sm'>
                      لا توجد طلبات {statusTab === 'PENDING' ? 'معلقة' : statusTab === 'COMPLETED' ? 'مكتملة' : 'مرفوضة'} لهذا الفرع.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Reject Form */}
        {rejectForm && (
          <div className='gov-panel rounded-md border-destructive/20 bg-destructive/5 max-w-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold text-destructive'>رفض طلب التعبئة #{rejectForm.id}</h2>
            </div>
            <div className='p-4 space-y-3'>
              <div className='space-y-1.5'>
                <Label className='text-xs'>ملاحظة الرفض (اختياري)</Label>
                <Input value={rejectForm.note}
                  onChange={(e) => setRejectForm((r) => r ? { ...r, note: e.target.value } : r)}
                  placeholder='سبب الرفض...' className='h-9 rounded-sm' />
              </div>
              <div className='flex gap-2'>
                <Button size='sm' variant='destructive' className='h-8 text-xs rounded-sm'
                  onClick={() => { void reject(rejectForm.id, rejectForm.note); }}>
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
      </div>
    </PageContainer>
  );
}

// ─── Cashout Admin ─────────────────────────────────────────────────────────────

type CashoutRow = {
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

export function CashoutAdminPage() {
  const [branchId, setBranchId] = useState('1');
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);
  const [statusTab, setStatusTab] = useState<'PENDING' | 'COMPLETED' | 'REJECTED'>('PENDING');

  const pending = useBackend<CashoutRow[]>(
    branchId ? `/wallet/cashout/pending?branchId=${branchId}&_t=${tick}` : null
  );
  const history = useBackend<CashoutRow[]>(
    branchId && statusTab !== 'PENDING'
      ? `/wallet/cashout?branchId=${branchId}&status=${statusTab}&_t=${tick}`
      : null
  );

  const displayedRequests = statusTab === 'PENDING' ? (pending.data ?? []) : (history.data ?? []);
  const isLoading = statusTab === 'PENDING' ? pending.loading : history.loading;

  const [rejectForm, setRejectForm] = useState<{ id: number; note: string } | null>(null);
  const moneyFmt2 = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });

  const totalPendingAmount = (pending.data ?? []).reduce((sum, r) => sum + Number(r.amount ?? 0), 0);

  async function complete(id: number) {
    try {
      await apiClient(`/wallet/cashout/${id}/complete`, {
        method: 'PUT',
        headers: { 'Idempotency-Key': idempotencyKey() },
      });
      toast.success('تم إتمام طلب السحب بنجاح');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإتمام');
    }
  }

  async function reject(id: number, note: string) {
    try {
      await apiClient(`/wallet/cashout/${id}/reject`, {
        method: 'PUT',
        body: JSON.stringify({ note: note || undefined }),
        headers: { 'Idempotency-Key': idempotencyKey() },
      });
      toast.success('تم رفض طلب السحب');
      setRejectForm(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الرفض');
    }
  }

  return (
    <PageContainer
      pageTitle='طلبات السحب (إدارة)'
      pageDescription='مراجعة طلبات السحب النقدي وإتمامها بعد التحقق من الرمز أو رفضها.'
    >
      <div className='space-y-4'>
        <div className='flex items-center gap-3 flex-wrap'>
          <Label className='text-xs whitespace-nowrap'>
            الفرع <span className='text-destructive'>*</span>
          </Label>
          <Input value={branchId} onChange={(e) => { setBranchId(e.target.value); refresh(); }}
            className='h-8 w-28 rounded-sm' type='number' />
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
        </div>

        {/* Stats Cards */}
        {(pending.data ?? []).length > 0 && (
          <div className='grid grid-cols-2 gap-3 md:grid-cols-4'>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>طلبات معلقة</div>
              <div className='text-2xl font-bold text-amber-600 tabular-nums'>{pending.data?.length ?? 0}</div>
            </div>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>إجمالي مبالغ معلقة</div>
              <div className='text-xl font-bold tabular-nums'>{moneyFmt2.format(totalPendingAmount)}</div>
            </div>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>متوسط مبلغ السحب</div>
              <div className='text-xl font-bold tabular-nums'>
                {(pending.data?.length ?? 0) > 0
                  ? moneyFmt2.format(totalPendingAmount / (pending.data?.length ?? 1))
                  : '—'}
              </div>
            </div>
            <div className='gov-panel rounded-md p-3'>
              <div className='text-xs text-muted-foreground'>أكبر سحب</div>
              <div className='text-xl font-bold tabular-nums'>
                {moneyFmt2.format(Math.max(...(pending.data ?? []).map((r) => Number(r.amount ?? 0)), 0))}
              </div>
            </div>
          </div>
        )}

        {/* Status Tabs */}
        <div className='flex gap-1 border-b border-border pb-0'>
          {([
            ['PENDING', 'معلقة'],
            ['COMPLETED', 'مكتملة'],
            ['REJECTED', 'مرفوضة'],
          ] as const).map(([tab, label]) => (
            <button key={tab}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
                statusTab === tab
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => setStatusTab(tab)}>
              {label}
              {tab === 'PENDING' && (pending.data?.length ?? 0) > 0 && (
                <span className='mr-1.5 rounded-full bg-amber-500/20 px-1.5 py-0.5 text-xs text-amber-700'>
                  {pending.data?.length}
                </span>
              )}
            </button>
          ))}
        </div>

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>
              {statusTab === 'PENDING' ? 'طلبات السحب المعلقة'
                : statusTab === 'COMPLETED' ? 'السحوبات المكتملة'
                : 'السحوبات المرفوضة'}
            </h2>
            {isLoading && <Icons.spinner className='size-4 animate-spin' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>المستخدم</th>
                  <th>المبلغ</th>
                  <th>العملة</th>
                  <th>الفرع</th>
                  <th>رمز الصرف</th>
                  <th>وقت الطلب</th>
                  <th>إجراء</th>
                </tr>
              </thead>
              <tbody>
                {displayedRequests.map((req) => (
                  <tr key={req.id}>
                    <td className='text-muted-foreground'>{req.id}</td>
                    <td>
                      <div className='font-medium text-sm'>{req.username ?? '-'}</div>
                      {req.userId && <div className='text-xs text-muted-foreground'>#{req.userId}</div>}
                    </td>
                    <td className='tabular-nums font-semibold'>{moneyFmt2.format(Number(req.amount ?? 0))}</td>
                    <td>{req.currency ?? '-'}</td>
                    <td>{req.branchId ?? '-'}</td>
                    <td>
                      {req.passcode ? (
                        <code className='bg-muted rounded px-1.5 py-0.5 text-xs font-mono tracking-widest'>
                          {req.passcode}
                        </code>
                      ) : (
                        <span className='text-muted-foreground text-xs'>—</span>
                      )}
                    </td>
                    <td className='text-xs text-muted-foreground'>{req.createdAt ?? '-'}</td>
                    <td>
                      {statusTab === 'PENDING' && (
                        <div className='flex items-center gap-1'>
                          <Button size='sm' className='h-7 text-xs'
                            onClick={() => { void complete(req.id); }}>
                            <Icons.check className='ml-1 size-3' />
                            إتمام
                          </Button>
                          <Button size='sm' variant='ghost'
                            className='h-7 text-xs text-destructive hover:text-destructive'
                            onClick={() => setRejectForm({ id: req.id, note: '' })}>
                            رفض
                          </Button>
                        </div>
                      )}
                      {statusTab !== 'PENDING' && (
                        <Badge variant={req.status === 'COMPLETED' ? 'default' : 'destructive'} className='text-xs'>
                          {req.status === 'COMPLETED' ? 'مكتمل' : 'مرفوض'}
                        </Badge>
                      )}
                    </td>
                  </tr>
                ))}
                {!isLoading && !displayedRequests.length && (
                  <tr>
                    <td colSpan={8} className='py-10 text-center text-muted-foreground text-sm'>
                      لا توجد طلبات سحب {statusTab === 'PENDING' ? 'معلقة' : statusTab === 'COMPLETED' ? 'مكتملة' : 'مرفوضة'} لهذا الفرع.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Reject Form */}
        {rejectForm && (
          <div className='gov-panel rounded-md border-destructive/20 bg-destructive/5 max-w-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold text-destructive'>رفض طلب السحب #{rejectForm.id}</h2>
            </div>
            <div className='p-4 space-y-3'>
              <div className='space-y-1.5'>
                <Label className='text-xs'>ملاحظة الرفض (اختياري)</Label>
                <Input value={rejectForm.note}
                  onChange={(e) => setRejectForm((r) => r ? { ...r, note: e.target.value } : r)}
                  placeholder='سبب الرفض...' className='h-9 rounded-sm' />
              </div>
              <div className='flex gap-2'>
                <Button size='sm' variant='destructive' className='h-8 text-xs rounded-sm'
                  onClick={() => { void reject(rejectForm.id, rejectForm.note); }}>
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
      </div>
    </PageContainer>
  );
}

// ─── Users ─────────────────────────────────────────────────────────────────────

const USER_ROLE_LABEL: Record<string, string> = {
  CASHIER: 'كاشير',
  MANAGER: 'مدير',
  ADMIN: 'مشرف',
  CUSTOMER: 'عميل',
};

const USER_STATUS_LABEL: Record<string, string> = {
  ACTIVE: 'نشط',
  SUSPENDED: 'موقوف',
};

const EMPTY_USER_FORM = {
  username: '',
  fullName: '',
  phone: '',
  email: '',
  password: '',
  role: 'CASHIER',
  branchId: '',
};

export function UsersPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);

  const users = useBackend<User[] | { content?: User[] }>(`/users?_t=${tick}`);
  const userList = toArray(users.data as User[] | { content?: User[] } | null);

  const [search, setSearch] = useState('');
  const [roleTab, setRoleTab] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState(EMPTY_USER_FORM);
  const [creating, setCreating] = useState(false);

  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [changingRoleId, setChangingRoleId] = useState<number | null>(null);
  const [roleValue, setRoleValue] = useState('');
  const [resettingId, setResettingId] = useState<number | null>(null);

  const filtered = userList.filter((u) => {
    if (roleTab && u.role !== roleTab) return false;
    if (statusFilter && u.status !== statusFilter) return false;
    if (search) {
      const q = search.toLowerCase();
      return (
        (u.username ?? '').toLowerCase().includes(q) ||
        (u.fullName ?? '').toLowerCase().includes(q) ||
        (u.phone ?? '').includes(q)
      );
    }
    return true;
  });

  const total = userList.length;
  const active = userList.filter((u) => u.status === 'ACTIVE').length;
  const suspended = userList.filter((u) => u.status === 'SUSPENDED').length;
  const byRole = (role: string) => userList.filter((u) => u.role === role).length;

  async function suspendUser(id: number) {
    try {
      await apiClient(`/users/${id}/suspend`, { method: 'POST' });
      toast.success('تم إيقاف المستخدم');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإيقاف');
    }
  }

  async function reactivateUser(id: number) {
    try {
      await apiClient(`/users/${id}/reactivate`, { method: 'POST' });
      toast.success('تم تفعيل المستخدم');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التفعيل');
    }
  }

  async function resetPassword(id: number) {
    setResettingId(id);
    try {
      const res = await apiClient<{ temporaryPassword?: string; password?: string }>(
        `/users/${id}/reset-password`, { method: 'POST' }
      );
      const tempPwd = res.temporaryPassword ?? res.password ?? 'راجع البريد الإلكتروني';
      toast.success(`كلمة المرور المؤقتة: ${tempPwd}`, { duration: 10000 });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إعادة تعيين كلمة المرور');
    } finally {
      setResettingId(null);
    }
  }

  async function changeRole(id: number, role: string) {
    try {
      await apiClient(`/users/${id}/role`, { method: 'PUT', body: JSON.stringify({ role }) });
      toast.success('تم تغيير الدور');
      setChangingRoleId(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تغيير الدور');
    }
  }

  async function createUser() {
    if (!createForm.username.trim() || !createForm.password.trim()) {
      toast.error('اسم المستخدم وكلمة المرور مطلوبان');
      return;
    }
    setCreating(true);
    try {
      await apiClient('/users', {
        method: 'POST',
        body: JSON.stringify({
          ...createForm,
          branchId: createForm.branchId ? Number(createForm.branchId) : undefined,
        }),
      });
      toast.success('تم إنشاء المستخدم بنجاح');
      setShowCreate(false);
      setCreateForm(EMPTY_USER_FORM);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إنشاء المستخدم');
    } finally {
      setCreating(false);
    }
  }

  return (
    <PageContainer
      pageTitle='المستخدمون'
      pageDescription='إدارة حسابات المستخدمين — الكاشيرون، المديرون، المشرفون، والعملاء.'
    >
      <div className='space-y-4'>
        {/* Stats */}
        <div className='grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-7'>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>إجمالي</div>
            <div className='text-2xl font-bold tabular-nums mt-1'>{total}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>نشط</div>
            <div className='text-2xl font-bold text-emerald-600 tabular-nums mt-1'>{active}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>موقوف</div>
            <div className='text-2xl font-bold text-amber-600 tabular-nums mt-1'>{suspended}</div>
          </div>
          {(['CASHIER', 'MANAGER', 'ADMIN', 'CUSTOMER'] as const).map((r) => (
            <div key={r}
              className={`gov-panel rounded-md p-3 cursor-pointer transition-all hover:border-primary ${roleTab === r ? 'border-primary ring-1 ring-primary' : ''}`}
              onClick={() => setRoleTab(roleTab === r ? '' : r)}>
              <div className='text-xs text-muted-foreground'>{USER_ROLE_LABEL[r]}</div>
              <div className='text-xl font-bold tabular-nums mt-1'>{byRole(r)}</div>
            </div>
          ))}
        </div>

        {/* Filters */}
        <div className='flex flex-wrap items-end gap-3'>
          <div>
            <label className='text-xs mb-1 block text-muted-foreground'>بحث</label>
            <Input value={search} onChange={(e) => setSearch(e.target.value)}
              placeholder='اسم، هاتف، اسم مستخدم...' className='h-8 rounded-sm text-xs w-52' />
          </div>
          <div>
            <label className='text-xs mb-1 block text-muted-foreground'>الحالة</label>
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}
              className='h-8 rounded-sm border border-border bg-background px-2 text-xs'>
              <option value=''>الكل</option>
              <option value='ACTIVE'>نشط</option>
              <option value='SUSPENDED'>موقوف</option>
            </select>
          </div>
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
          <Button size='sm' className='h-8 rounded-sm text-xs mr-auto'
            onClick={() => setShowCreate(!showCreate)}>
            <Icons.add className='me-1.5 size-3.5' />
            مستخدم جديد
          </Button>
        </div>

        {/* Role Tabs */}
        <div className='flex gap-1 border-b border-border pb-0'>
          {([
            ['', 'الكل'],
            ['CASHIER', 'كاشير'],
            ['MANAGER', 'مدير'],
            ['ADMIN', 'مشرف'],
            ['CUSTOMER', 'عميل'],
          ] as [string, string][]).map(([tab, label]) => (
            <button key={tab}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
                roleTab === tab
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => setRoleTab(tab)}>
              {label}
            </button>
          ))}
        </div>

        {/* Create Form */}
        {showCreate && (
          <div className='gov-panel rounded-md border-primary/20 bg-primary/5 max-w-2xl'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>إنشاء مستخدم جديد</h2>
            </div>
            <div className='p-4 grid gap-3 sm:grid-cols-2'>
              <div className='space-y-1.5'>
                <Label className='text-xs'>اسم المستخدم <span className='text-destructive'>*</span></Label>
                <Input value={createForm.username}
                  onChange={(e) => setCreateForm((f) => ({ ...f, username: e.target.value }))}
                  className='h-8 rounded-sm text-xs' dir='ltr' placeholder='username' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>الاسم الكامل</Label>
                <Input value={createForm.fullName}
                  onChange={(e) => setCreateForm((f) => ({ ...f, fullName: e.target.value }))}
                  className='h-8 rounded-sm text-xs' placeholder='الاسم الكامل' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>رقم الهاتف</Label>
                <Input value={createForm.phone}
                  onChange={(e) => setCreateForm((f) => ({ ...f, phone: e.target.value }))}
                  className='h-8 rounded-sm text-xs' dir='ltr' placeholder='+963...' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>البريد الإلكتروني</Label>
                <Input value={createForm.email} type='email'
                  onChange={(e) => setCreateForm((f) => ({ ...f, email: e.target.value }))}
                  className='h-8 rounded-sm text-xs' dir='ltr' placeholder='email@example.com' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>كلمة المرور <span className='text-destructive'>*</span></Label>
                <Input value={createForm.password} type='password'
                  onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
                  className='h-8 rounded-sm text-xs' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>الدور</Label>
                <Select value={createForm.role} onValueChange={(v) => setCreateForm((f) => ({ ...f, role: v }))}>
                  <SelectTrigger className='h-8 rounded-sm text-xs'><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value='CASHIER'>كاشير</SelectItem>
                    <SelectItem value='MANAGER'>مدير</SelectItem>
                    <SelectItem value='ADMIN'>مشرف</SelectItem>
                    <SelectItem value='CUSTOMER'>عميل</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>معرف الفرع</Label>
                <Input value={createForm.branchId} type='number'
                  onChange={(e) => setCreateForm((f) => ({ ...f, branchId: e.target.value }))}
                  className='h-8 rounded-sm text-xs' placeholder='اختياري' />
              </div>
            </div>
            <div className='px-4 pb-4 flex gap-2'>
              <Button size='sm' className='h-8 rounded-sm text-xs'
                onClick={() => { void createUser(); }} disabled={creating}>
                {creating && <Icons.spinner className='me-1.5 size-3.5 animate-spin' />}
                إنشاء المستخدم
              </Button>
              <Button size='sm' variant='outline' className='h-8 rounded-sm text-xs'
                onClick={() => { setShowCreate(false); setCreateForm(EMPTY_USER_FORM); }}>
                إلغاء
              </Button>
            </div>
          </div>
        )}

        {/* Table */}
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <div>
              <h2 className='text-sm font-semibold'>المستخدمون</h2>
              <p className='text-muted-foreground text-xs'>{filtered.length} مستخدم</p>
            </div>
            {users.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
          </div>

          {users.error && (
            <div className='border-b border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive'>
              تعذر تحميل البيانات: {users.error}
            </div>
          )}

          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>المستخدم</th>
                  <th>الهاتف</th>
                  <th>الدور</th>
                  <th>الحالة</th>
                  <th>الفرع</th>
                  <th>تاريخ الإنشاء</th>
                  <th>إجراءات</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((u) => (
                  <>
                    <tr key={u.id}>
                      <td className='text-muted-foreground text-xs'>{u.id}</td>
                      <td>
                        <div className='font-medium text-sm'>{u.fullName ?? u.username ?? '-'}</div>
                        <div className='text-xs text-muted-foreground'>{u.username ?? ''}</div>
                      </td>
                      <td className='text-xs font-mono'>{u.phone ?? '-'}</td>
                      <td>
                        {changingRoleId === u.id ? (
                          <div className='flex items-center gap-1'>
                            <Select value={roleValue} onValueChange={setRoleValue}>
                              <SelectTrigger className='h-7 rounded-sm text-xs w-24'>
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value='CASHIER'>كاشير</SelectItem>
                                <SelectItem value='MANAGER'>مدير</SelectItem>
                                <SelectItem value='ADMIN'>مشرف</SelectItem>
                                <SelectItem value='CUSTOMER'>عميل</SelectItem>
                              </SelectContent>
                            </Select>
                            <Button size='sm' variant='outline' className='h-7 text-xs px-2'
                              onClick={() => { void changeRole(u.id, roleValue); }}>
                              حفظ
                            </Button>
                            <Button size='sm' variant='ghost' className='h-7 text-xs px-1'
                              onClick={() => setChangingRoleId(null)}>
                              <Icons.close className='size-3' />
                            </Button>
                          </div>
                        ) : (
                          <Badge variant='outline' className='text-xs cursor-pointer'
                            onClick={() => { setChangingRoleId(u.id); setRoleValue(u.role ?? 'CASHIER'); }}>
                            {USER_ROLE_LABEL[u.role ?? ''] ?? u.role ?? '-'}
                          </Badge>
                        )}
                      </td>
                      <td>
                        <Badge
                          variant={u.status === 'ACTIVE' ? 'default' : 'destructive'}
                          className='text-xs'>
                          {USER_STATUS_LABEL[u.status ?? ''] ?? u.status ?? '-'}
                        </Badge>
                      </td>
                      <td className='text-xs text-muted-foreground'>{u.branchId ?? '-'}</td>
                      <td className='text-xs text-muted-foreground'>{u.createdAt ?? '-'}</td>
                      <td>
                        <div className='flex items-center gap-1'>
                          <Button size='sm' variant='ghost' className='h-7 text-xs px-2'
                            onClick={() => setExpandedId(expandedId === u.id ? null : u.id)}
                            title='تفاصيل'>
                            <Icons.info className='size-3.5' />
                          </Button>
                          {u.status === 'ACTIVE' ? (
                            <Button size='sm' variant='ghost' className='h-7 text-xs px-2 text-amber-600'
                              onClick={() => { void suspendUser(u.id); }} title='إيقاف'>
                              <Icons.lock className='size-3.5' />
                            </Button>
                          ) : (
                            <Button size='sm' variant='ghost' className='h-7 text-xs px-2 text-emerald-600'
                              onClick={() => { void reactivateUser(u.id); }} title='تفعيل'>
                              <Icons.unlock className='size-3.5' />
                            </Button>
                          )}
                          <Button size='sm' variant='ghost' className='h-7 text-xs px-2'
                            disabled={resettingId === u.id}
                            onClick={() => { void resetPassword(u.id); }} title='إعادة تعيين كلمة المرور'>
                            {resettingId === u.id
                              ? <Icons.spinner className='size-3.5 animate-spin' />
                              : <Icons.settings className='size-3.5 text-muted-foreground' />}
                          </Button>
                        </div>
                      </td>
                    </tr>
                    {expandedId === u.id && (
                      <tr key={`${u.id}-detail`}>
                        <td colSpan={8} className='bg-muted/40 px-4 py-3'>
                          <div className='grid grid-cols-2 gap-2 text-xs sm:grid-cols-4'>
                            <div><span className='text-muted-foreground'>المعرف: </span><span className='font-mono'>{u.id}</span></div>
                            <div><span className='text-muted-foreground'>اسم المستخدم: </span><span dir='ltr'>{u.username ?? '-'}</span></div>
                            <div><span className='text-muted-foreground'>الاسم الكامل: </span>{u.fullName ?? '-'}</div>
                            <div><span className='text-muted-foreground'>الهاتف: </span><span dir='ltr'>{u.phone ?? '-'}</span></div>
                            <div><span className='text-muted-foreground'>البريد: </span><span dir='ltr'>{u.email ?? '-'}</span></div>
                            <div><span className='text-muted-foreground'>الفرع: </span>{u.branchId ?? '-'}</div>
                            <div><span className='text-muted-foreground'>رقم المحفظة: </span>{u.walletId ?? '-'}</div>
                            <div><span className='text-muted-foreground'>تاريخ الإنشاء: </span>{u.createdAt ?? '-'}</div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
                {!users.loading && !filtered.length && (
                  <tr>
                    <td colSpan={8} className='py-12 text-center'>
                      <div className='flex flex-col items-center gap-3'>
                        <Icons.teams className='size-10 text-muted-foreground/40' />
                        <div className='text-sm text-muted-foreground'>
                          {search || roleTab || statusFilter
                            ? 'لا توجد نتائج تطابق معايير البحث'
                            : 'لا يوجد مستخدمون حتى الآن'}
                        </div>
                      </div>
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

// ─── Branches ───────────────────────────────────────────────────────────────────

const EMPTY_BRANCH_FORM = {
  name: '',
  city: '',
  country: '',
  addressLine: '',
  phone: '',
  latitude: '',
  longitude: '',
  opensAt: '',
  closesAt: '',
  services: '',
};

type BranchFormState = typeof EMPTY_BRANCH_FORM;

export function BranchesPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);

  const branches = useBackend<Branch[] | { content?: Branch[] }>(`/admin/branches?_t=${tick}`);
  const branchList = toArray(branches.data as Branch[] | { content?: Branch[] } | null);

  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [form, setForm] = useState<BranchFormState>(EMPTY_BRANCH_FORM);
  const [saving, setSaving] = useState(false);

  const setField = (k: keyof BranchFormState, v: string) =>
    setForm((f) => ({ ...f, [k]: v }));

  const filtered = branchList.filter((b) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      (b.name ?? '').toLowerCase().includes(q) ||
      (b.city ?? '').toLowerCase().includes(q) ||
      (b.country ?? '').toLowerCase().includes(q)
    );
  });

  const total = branchList.length;
  const cities = new Set(branchList.map((b) => b.city).filter(Boolean)).size;
  const countries = new Set(branchList.map((b) => b.country).filter(Boolean)).size;

  function openCreate() {
    setEditId(null);
    setForm(EMPTY_BRANCH_FORM);
    setShowForm(true);
  }

  function openEdit(b: Branch) {
    setEditId(b.id);
    setForm({
      name: b.name ?? '',
      city: b.city ?? '',
      country: b.country ?? '',
      addressLine: b.addressLine ?? '',
      phone: b.phone ?? '',
      latitude: b.latitude != null ? String(b.latitude) : '',
      longitude: b.longitude != null ? String(b.longitude) : '',
      opensAt: b.opensAt ?? '',
      closesAt: b.closesAt ?? '',
      services: b.services ?? '',
    });
    setShowForm(true);
  }

  async function save() {
    if (!form.name.trim()) {
      toast.error('اسم الفرع مطلوب');
      return;
    }
    setSaving(true);
    const payload = {
      name: form.name.trim(),
      city: form.city || undefined,
      country: form.country || undefined,
      addressLine: form.addressLine || undefined,
      phone: form.phone || undefined,
      latitude: form.latitude ? Number(form.latitude) : undefined,
      longitude: form.longitude ? Number(form.longitude) : undefined,
      opensAt: form.opensAt || undefined,
      closesAt: form.closesAt || undefined,
      services: form.services || undefined,
    };
    try {
      if (editId == null) {
        await apiClient('/admin/branches', { method: 'POST', body: JSON.stringify(payload) });
        toast.success('تم إنشاء الفرع وتهيئة رسومه الافتراضية');
      } else {
        await apiClient(`/admin/branches/${editId}`, { method: 'PUT', body: JSON.stringify(payload) });
        toast.success('تم تحديث الفرع');
      }
      setShowForm(false);
      setForm(EMPTY_BRANCH_FORM);
      setEditId(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل حفظ الفرع');
    } finally {
      setSaving(false);
    }
  }

  async function remove(id: number) {
    try {
      await apiClient(`/admin/branches/${id}`, { method: 'DELETE' });
      toast.success('تم حذف الفرع');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل حذف الفرع');
    }
  }

  return (
    <PageContainer
      pageTitle='الفروع'
      pageDescription='إدارة فروع المنصة — الإنشاء والتعديل والحذف. كل فرع جديد يُهيَّأ برسوم افتراضية قابلة للتعديل.'
    >
      <div className='space-y-4'>
        {/* Stats */}
        <div className='grid grid-cols-3 gap-3'>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>إجمالي الفروع</div>
            <div className='text-2xl font-bold tabular-nums mt-1'>{total}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>المدن</div>
            <div className='text-2xl font-bold tabular-nums mt-1'>{cities}</div>
          </div>
          <div className='gov-panel rounded-md p-3'>
            <div className='text-xs text-muted-foreground'>الدول</div>
            <div className='text-2xl font-bold tabular-nums mt-1'>{countries}</div>
          </div>
        </div>

        {/* Toolbar */}
        <div className='flex flex-wrap items-end gap-3'>
          <div>
            <label className='text-xs mb-1 block text-muted-foreground'>بحث</label>
            <Input value={search} onChange={(e) => setSearch(e.target.value)}
              placeholder='اسم، مدينة، دولة...' className='h-8 rounded-sm text-xs w-56' />
          </div>
          <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
            <Icons.spinner className='ml-1 size-3.5' />
            تحديث
          </Button>
          <Button size='sm' className='h-8 rounded-sm text-xs mr-auto' onClick={openCreate}>
            <Icons.add className='me-1.5 size-3.5' />
            فرع جديد
          </Button>
        </div>

        {/* Create / Edit form */}
        {showForm && (
          <div className='gov-panel rounded-md border-primary/20 bg-primary/5 max-w-3xl'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>
                {editId == null ? 'إنشاء فرع جديد' : `تعديل الفرع #${editId}`}
              </h2>
            </div>
            <div className='p-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3'>
              <div className='space-y-1.5'>
                <Label className='text-xs'>اسم الفرع <span className='text-destructive'>*</span></Label>
                <Input value={form.name} onChange={(e) => setField('name', e.target.value)}
                  className='h-8 rounded-sm text-xs' placeholder='مثال: فرع دمشق المركزي' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>المدينة</Label>
                <Input value={form.city} onChange={(e) => setField('city', e.target.value)}
                  className='h-8 rounded-sm text-xs' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>الدولة</Label>
                <Input value={form.country} onChange={(e) => setField('country', e.target.value)}
                  className='h-8 rounded-sm text-xs' />
              </div>
              <div className='space-y-1.5 sm:col-span-2 lg:col-span-3'>
                <Label className='text-xs'>العنوان</Label>
                <Input value={form.addressLine} onChange={(e) => setField('addressLine', e.target.value)}
                  className='h-8 rounded-sm text-xs' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>الهاتف</Label>
                <Input value={form.phone} onChange={(e) => setField('phone', e.target.value)}
                  className='h-8 rounded-sm text-xs' dir='ltr' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>خط العرض (latitude)</Label>
                <Input value={form.latitude} type='number' onChange={(e) => setField('latitude', e.target.value)}
                  className='h-8 rounded-sm text-xs' dir='ltr' placeholder='33.51' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>خط الطول (longitude)</Label>
                <Input value={form.longitude} type='number' onChange={(e) => setField('longitude', e.target.value)}
                  className='h-8 rounded-sm text-xs' dir='ltr' placeholder='36.29' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>وقت الفتح</Label>
                <Input value={form.opensAt} type='time' onChange={(e) => setField('opensAt', e.target.value)}
                  className='h-8 rounded-sm text-xs' dir='ltr' />
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>وقت الإغلاق</Label>
                <Input value={form.closesAt} type='time' onChange={(e) => setField('closesAt', e.target.value)}
                  className='h-8 rounded-sm text-xs' dir='ltr' />
              </div>
              <div className='space-y-1.5 sm:col-span-2 lg:col-span-3'>
                <Label className='text-xs'>الخدمات (JSON)</Label>
                <Input value={form.services} onChange={(e) => setField('services', e.target.value)}
                  className='h-8 rounded-sm text-xs' dir='ltr'
                  placeholder='["TOPUP","TRANSFER","QR_RELEASE","EXCHANGE"]' />
              </div>
            </div>
            <div className='px-4 pb-4 flex gap-2'>
              <Button size='sm' className='h-8 rounded-sm text-xs'
                onClick={() => { void save(); }} disabled={saving}>
                {saving && <Icons.spinner className='me-1.5 size-3.5 animate-spin' />}
                {editId == null ? 'إنشاء الفرع' : 'حفظ التعديلات'}
              </Button>
              <Button size='sm' variant='outline' className='h-8 rounded-sm text-xs'
                onClick={() => { setShowForm(false); setForm(EMPTY_BRANCH_FORM); setEditId(null); }}>
                إلغاء
              </Button>
            </div>
          </div>
        )}

        {/* Table */}
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <div>
              <h2 className='text-sm font-semibold'>الفروع</h2>
              <p className='text-muted-foreground text-xs'>{filtered.length} فرع</p>
            </div>
            {branches.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
          </div>

          {branches.error && (
            <div className='border-b border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive'>
              تعذر تحميل الفروع: {branches.error}
            </div>
          )}

          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>الاسم</th>
                  <th>المدينة</th>
                  <th>الدولة</th>
                  <th>الهاتف</th>
                  <th>ساعات العمل</th>
                  <th>إجراءات</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((b) => (
                  <tr key={b.id}>
                    <td className='text-muted-foreground text-xs'>{b.id}</td>
                    <td className='font-medium text-sm'>{b.name ?? '-'}</td>
                    <td className='text-xs'>{b.city ?? '-'}</td>
                    <td className='text-xs'>{b.country ?? '-'}</td>
                    <td className='text-xs font-mono'>{b.phone ?? '-'}</td>
                    <td className='text-xs text-muted-foreground'>
                      {b.opensAt && b.closesAt ? `${b.opensAt} - ${b.closesAt}` : '-'}
                    </td>
                    <td>
                      <div className='flex items-center gap-1'>
                        <Button size='sm' variant='ghost' className='h-7 text-xs px-2'
                          onClick={() => openEdit(b)} title='تعديل'>
                          <Icons.edit className='size-3.5' />
                        </Button>
                        <AlertDialog>
                          <AlertDialogTrigger asChild>
                            <Button size='sm' variant='ghost'
                              className='h-7 text-xs px-2 text-destructive hover:text-destructive' title='حذف'>
                              <Icons.trash className='size-3.5' />
                            </Button>
                          </AlertDialogTrigger>
                          <AlertDialogContent>
                            <AlertDialogHeader>
                              <AlertDialogTitle>حذف الفرع</AlertDialogTitle>
                              <AlertDialogDescription>
                                هل أنت متأكد من حذف الفرع &quot;{b.name}&quot;؟ لا يمكن التراجع عن هذا الإجراء.
                              </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                              <AlertDialogCancel>إلغاء</AlertDialogCancel>
                              <AlertDialogAction onClick={() => { void remove(b.id); }}>
                                حذف
                              </AlertDialogAction>
                            </AlertDialogFooter>
                          </AlertDialogContent>
                        </AlertDialog>
                      </div>
                    </td>
                  </tr>
                ))}
                {!branches.loading && !filtered.length && (
                  <tr>
                    <td colSpan={7} className='py-12 text-center'>
                      <div className='flex flex-col items-center gap-3'>
                        <Icons.workspace className='size-10 text-muted-foreground/40' />
                        <div className='text-sm text-muted-foreground'>
                          {search ? 'لا توجد نتائج تطابق البحث' : 'لا توجد فروع — أنشئ أول فرع'}
                        </div>
                      </div>
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

// ─── Fee Configuration ──────────────────────────────────────────────────────────

const SCOPE_META: Record<CommissionScope, { label: string; hint: string; group: 'platform' | 'branch' }> = {
  PLATFORM_BASE_FEE: {
    label: 'رسوم المنصة الأساسية',
    hint: 'رسم إلزامي لكل 1000 دولار',
    group: 'platform',
  },
  PLATFORM_EXCHANGE_PROFIT: {
    label: 'ربح المنصة من الصرف',
    hint: 'رسم على العملات غير المتطابقة لكل 1000 دولار',
    group: 'platform',
  },
  WALLET_EXCHANGE: {
    label: 'رسم صرف المحفظة',
    hint: 'رسم المنصة على صرف عملة المحفظة',
    group: 'platform',
  },
  SENDING_BRANCH_FEE: {
    label: 'رسم الفرع المُرسِل',
    hint: 'رسم الإرسال القابل للتعديل من الفرع',
    group: 'branch',
  },
  RECEIVING_BRANCH_FEE: {
    label: 'رسم الفرع المُستقبِل',
    hint: 'رسم الاستلام القابل للتعديل من الفرع',
    group: 'branch',
  },
};

const ALL_SCOPES: CommissionScope[] = [
  'PLATFORM_BASE_FEE',
  'PLATFORM_EXCHANGE_PROFIT',
  'WALLET_EXCHANGE',
  'SENDING_BRANCH_FEE',
  'RECEIVING_BRANCH_FEE',
];

export function FeeConfigPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);

  const branches = useBackend<Branch[] | { content?: Branch[] }>('/admin/branches');
  const branchList = toArray(branches.data as Branch[] | { content?: Branch[] } | null);

  const [branchId, setBranchId] = useState<string>('');

  const rates = useBackend<CommissionRate[] | { content?: CommissionRate[] }>(
    branchId ? `/admin/fees/${branchId}?_t=${tick}` : null
  );
  const rateList = toArray(rates.data as CommissionRate[] | { content?: CommissionRate[] } | null);

  const rateByScope = new Map<string, CommissionRate>();
  for (const r of rateList) {
    if (r.commissionScope) rateByScope.set(r.commissionScope, r);
  }

  const [editScope, setEditScope] = useState<CommissionScope | null>(null);
  const [editValue, setEditValue] = useState('');
  const [savingScope, setSavingScope] = useState(false);
  const [initializing, setInitializing] = useState(false);

  function startEdit(scope: CommissionScope, current?: number) {
    setEditScope(scope);
    setEditValue(current != null ? String(current) : '');
  }

  async function saveRate(scope: CommissionScope) {
    if (!branchId) return;
    const val = Number(editValue);
    if (Number.isNaN(val) || val < 0) {
      toast.error('القيمة يجب أن تكون رقماً غير سالب');
      return;
    }
    setSavingScope(true);
    try {
      await apiClient(`/admin/fees/${branchId}/${scope}`, {
        method: 'PUT',
        body: JSON.stringify({ newRate: val }),
      });
      toast.success('تم تحديث الرسم');
      setEditScope(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تحديث الرسم');
    } finally {
      setSavingScope(false);
    }
  }

  async function initializeDefaults() {
    if (!branchId) return;
    setInitializing(true);
    try {
      await apiClient(`/admin/fees/${branchId}/defaults`, { method: 'POST' });
      toast.success('تمت تهيئة الرسوم الافتراضية المفقودة');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تهيئة الرسوم');
    } finally {
      setInitializing(false);
    }
  }

  const selectedBranch = branchList.find((b) => String(b.id) === branchId);
  const missingScopes = branchId ? ALL_SCOPES.filter((s) => !rateByScope.has(s)) : [];

  function renderScopeCard(scope: CommissionScope) {
    const meta = SCOPE_META[scope];
    const rate = rateByScope.get(scope);
    const isEditing = editScope === scope;
    return (
      <div key={scope} className='gov-panel rounded-md p-4'>
        <div className='flex items-start justify-between gap-2'>
          <div className='min-w-0'>
            <div className='text-sm font-medium'>{meta.label}</div>
            <div className='text-xs text-muted-foreground mt-0.5'>{meta.hint}</div>
            <div className='text-[10px] font-mono text-muted-foreground/70 mt-1' dir='ltr'>{scope}</div>
          </div>
          <Badge variant={meta.group === 'platform' ? 'default' : 'secondary'} className='text-xs shrink-0'>
            {meta.group === 'platform' ? 'المنصة' : 'الفرع'}
          </Badge>
        </div>

        <div className='mt-3 border-t border-border pt-3'>
          {isEditing ? (
            <div className='flex items-center gap-2'>
              <Input value={editValue} type='number' dir='ltr'
                onChange={(e) => setEditValue(e.target.value)}
                className='h-8 rounded-sm text-xs w-28' placeholder='0.00' />
              <Button size='sm' className='h-8 text-xs px-3'
                onClick={() => { void saveRate(scope); }} disabled={savingScope}>
                {savingScope ? <Icons.spinner className='size-3.5 animate-spin' /> : 'حفظ'}
              </Button>
              <Button size='sm' variant='ghost' className='h-8 text-xs px-2'
                onClick={() => setEditScope(null)}>
                <Icons.close className='size-3.5' />
              </Button>
            </div>
          ) : (
            <div className='flex items-center justify-between'>
              <div>
                {rate ? (
                  <span className='text-lg font-bold tabular-nums' dir='ltr'>{fmt(rate.rateValue)}</span>
                ) : (
                  <span className='text-xs text-amber-600'>غير محدد</span>
                )}
                <span className='text-xs text-muted-foreground mr-1'>/ 1000$</span>
              </div>
              <Button size='sm' variant='outline' className='h-7 text-xs px-3'
                disabled={!rate}
                onClick={() => startEdit(scope, rate?.rateValue)}>
                <Icons.edit className='me-1 size-3' />
                تعديل
              </Button>
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <PageContainer
      pageTitle='إعداد الرسوم'
      pageDescription='التحكم في رسوم المنصة ورسوم الفروع (نِسَب العمولة) لكل فرع على حدة.'
    >
      <div className='space-y-4'>
        {/* Branch selector */}
        <div className='gov-panel rounded-md p-4'>
          <div className='flex flex-wrap items-end gap-3'>
            <div className='min-w-[260px]'>
              <Label className='text-xs mb-1 block text-muted-foreground'>اختر الفرع</Label>
              <Select value={branchId} onValueChange={setBranchId}>
                <SelectTrigger className='h-9 rounded-sm text-sm'>
                  <SelectValue placeholder={branches.loading ? 'جارٍ التحميل...' : 'اختر فرعاً لعرض رسومه'} />
                </SelectTrigger>
                <SelectContent>
                  {branchList.map((b) => (
                    <SelectItem key={b.id} value={String(b.id)}>
                      {b.name ?? `فرع #${b.id}`}{b.city ? ` — ${b.city}` : ''}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            {branchId && (
              <>
                <Button size='sm' variant='ghost' className='h-9 text-xs' onClick={refresh}>
                  <Icons.spinner className='ml-1 size-3.5' />
                  تحديث
                </Button>
                {missingScopes.length > 0 && (
                  <Button size='sm' variant='outline' className='h-9 rounded-sm text-xs'
                    onClick={() => { void initializeDefaults(); }} disabled={initializing}>
                    {initializing && <Icons.spinner className='me-1.5 size-3.5 animate-spin' />}
                    تهيئة الرسوم الافتراضية ({missingScopes.length})
                  </Button>
                )}
              </>
            )}
          </div>
          {branches.error && (
            <div className='mt-2 text-xs text-destructive'>تعذر تحميل الفروع: {branches.error}</div>
          )}
        </div>

        {!branchId && (
          <div className='gov-panel rounded-md py-16 text-center'>
            <Icons.billing className='mx-auto mb-3 size-12 text-muted-foreground/30' />
            <div className='text-sm font-medium text-muted-foreground'>اختر فرعاً للبدء</div>
            <div className='text-xs text-muted-foreground mt-1'>
              ستظهر هنا رسوم المنصة ورسوم الفرع القابلة للتعديل
            </div>
          </div>
        )}

        {branchId && rates.error && (
          <div className='gov-panel rounded-md border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive'>
            تعذر تحميل الرسوم: {rates.error}
          </div>
        )}

        {branchId && (
          <>
            <div className='flex items-center gap-2'>
              <h2 className='text-sm font-semibold'>
                رسوم: {selectedBranch?.name ?? `فرع #${branchId}`}
              </h2>
              {rates.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
            </div>

            {/* Platform-level fees */}
            <div>
              <div className='text-xs font-medium text-muted-foreground mb-2'>رسوم المنصة</div>
              <div className='grid gap-3 sm:grid-cols-2 lg:grid-cols-3'>
                {ALL_SCOPES.filter((s) => SCOPE_META[s].group === 'platform').map(renderScopeCard)}
              </div>
            </div>

            {/* Branch-level fees */}
            <div>
              <div className='text-xs font-medium text-muted-foreground mb-2'>رسوم الفرع</div>
              <div className='grid gap-3 sm:grid-cols-2 lg:grid-cols-3'>
                {ALL_SCOPES.filter((s) => SCOPE_META[s].group === 'branch').map(renderScopeCard)}
              </div>
            </div>
          </>
        )}
      </div>
    </PageContainer>
  );
}
