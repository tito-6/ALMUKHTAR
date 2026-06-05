'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { apiClient } from '@/lib/api-client';
import { getStoredSession } from '@/lib/api-client';
import { useCallback, useEffect, useRef, useState } from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import type { BatchJob, BatchTemplate } from './types';

function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

const statusVariant = (s?: string): 'default' | 'destructive' | 'secondary' | 'outline' => {
  if (s === 'COMPLETED') return 'default';
  if (s === 'FAILED') return 'destructive';
  if (s === 'PROCESSING') return 'secondary';
  if (s === 'CANCELLED') return 'outline';
  return 'outline';
};

const STATUS_LABEL: Record<string, string> = {
  PENDING_APPROVAL: 'بانتظار الموافقة',
  APPROVED: 'موافق عليه',
  PROCESSING: 'قيد التنفيذ',
  COMPLETED: 'مكتمل',
  FAILED: 'فاشل',
  CANCELLED: 'ملغى',
};

type BatchProgress = {
  successCount: number;
  failureCount: number;
  totalCount: number;
  status: string;
};

type BatchReportRow = {
  rowNumber?: number;
  receiverIdentifier?: string;
  amount?: number;
  status?: string;
  errorMessage?: string;
  transactionId?: number;
};

type BatchReport = {
  jobId?: number;
  status?: string;
  totalRows?: number;
  successCount?: number;
  failedCount?: number;
  rows?: BatchReportRow[];
};

type ValidationPreview = {
  valid: boolean;
  rowCount?: number;
  errors?: Array<{ row: number; column?: string; message: string }>;
  preview?: Array<Record<string, string>>;
};

export function BatchPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);
  const [statusFilter, setStatusFilter] = useState('');
  const jobs = useBackend<BatchJob[]>(`/batch?_t=${tick}`);
  const templates = useBackend<BatchTemplate[]>('/batch/templates');

  const fileRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [selectedJob, setSelectedJob] = useState<BatchJob | null>(null);

  // SSE-based progress
  const [progress, setProgress] = useState<BatchProgress | null>(null);
  const sseRef = useRef<EventSource | null>(null);

  // Report
  const [report, setReport] = useState<BatchReport | null>(null);
  const [loadingReport, setLoadingReport] = useState(false);

  // CSV validation preview
  const [validating, setValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<ValidationPreview | null>(null);
  const validateRef = useRef<HTMLInputElement>(null);

  // Retry failed rows
  const [retrying, setRetrying] = useState<number | null>(null);

  const connectSSE = useCallback((jobId: number) => {
    if (sseRef.current) {
      sseRef.current.close();
      sseRef.current = null;
    }
    setProgress(null);

    const session = getStoredSession();
    const token = session?.token ? `?token=${session.token}` : '';
    const url = `/api/backend/batch/${jobId}/progress${token}`;

    const es = new EventSource(url);
    es.onmessage = (ev) => {
      try {
        const data = JSON.parse(ev.data) as BatchProgress;
        setProgress(data);
        if (data.status === 'COMPLETED' || data.status === 'FAILED') {
          es.close();
          sseRef.current = null;
          refresh();
        }
      } catch {
        // ignore parse errors
      }
    };
    es.onerror = () => {
      es.close();
      sseRef.current = null;
    };
    sseRef.current = es;
  }, [refresh]);

  useEffect(() => {
    return () => { sseRef.current?.close(); };
  }, []);

  function selectJob(job: BatchJob) {
    setSelectedJob(job);
    setReport(null);
    if (job.status === 'PROCESSING') {
      connectSSE(job.id);
    }
  }

  async function loadReport(jobId: number) {
    setLoadingReport(true);
    try {
      const data = await apiClient<BatchReport>(`/batch/${jobId}/report`);
      setReport(data);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تحميل التقرير');
    } finally {
      setLoadingReport(false);
    }
  }

  async function uploadFile() {
    const file = fileRef.current?.files?.[0];
    if (!file) { toast.error('يرجى اختيار ملف CSV أولاً'); return; }
    setUploading(true);
    const form = new FormData();
    form.append('file', file);
    try {
      const result = await apiClient<BatchJob>('/batch/upload', { method: 'POST', body: form });
      toast.success(`تم رفع الملف — وظيفة رقم ${result.id}`);
      selectJob(result);
      if (fileRef.current) fileRef.current.value = '';
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل رفع الملف');
    } finally {
      setUploading(false);
    }
  }

  async function approveJob(id: number) {
    try {
      await apiClient(`/batch/${id}/approve`, { method: 'POST' });
      toast.success('تمت الموافقة على الدفعة');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإجراء');
    }
  }

  async function executeJob(id: number) {
    try {
      await apiClient(`/batch/${id}/execute`, {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey() },
      });
      toast.success('بدأ تنفيذ الدفعة');
      connectSSE(id);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التنفيذ');
    }
  }

  async function cancelJob(id: number) {
    try {
      await apiClient(`/batch/${id}/cancel`, { method: 'DELETE' });
      toast.success('تم إلغاء الدفعة');
      if (sseRef.current) { sseRef.current.close(); sseRef.current = null; }
      setSelectedJob(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإلغاء');
    }
  }

  async function retryFailed(id: number) {
    setRetrying(id);
    try {
      const result = await apiClient<BatchJob>(`/batch/${id}/retry-failed`, {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey() },
      });
      toast.success('جاري إعادة محاولة الصفوف الفاشلة');
      selectJob(result);
      if (result.status === 'PROCESSING') connectSSE(result.id);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشلت إعادة المحاولة');
    } finally {
      setRetrying(null);
    }
  }

  async function validateFile() {
    const file = validateRef.current?.files?.[0];
    if (!file) { toast.error('اختر ملفاً للتحقق منه'); return; }
    setValidating(true);
    setValidationResult(null);
    const form = new FormData();
    form.append('file', file);
    try {
      const result = await apiClient<ValidationPreview>('/batch/validate', { method: 'POST', body: form });
      setValidationResult(result);
      if (result.valid) {
        toast.success(`الملف صالح — ${result.rowCount ?? 0} صف`);
      } else {
        toast.error(`الملف يحتوي على ${result.errors?.length ?? 0} خطأ`);
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التحقق');
    } finally {
      setValidating(false);
    }
  }

  function downloadTemplate(templateId: number, name?: string) {
    const filename = `batch-template-${name?.toLowerCase().replace(/\s+/g, '-') ?? templateId}.csv`;
    const link = document.createElement('a');
    link.href = `/api/backend/batch/templates/${templateId}/download`;
    link.download = filename;
    link.click();
  }

  const filteredJobs = statusFilter
    ? (jobs.data ?? []).filter((j) => j.status === statusFilter)
    : (jobs.data ?? []);

  const statsMap = (jobs.data ?? []).reduce<Record<string, number>>((acc, j) => {
    const s = j.status ?? 'PENDING_APPROVAL';
    acc[s] = (acc[s] ?? 0) + 1;
    return acc;
  }, {});

  const progressPct = progress
    ? Math.round(
        (((progress.successCount ?? 0) + (progress.failureCount ?? 0)) /
          Math.max(1, progress.totalCount ?? 1)) *
          100
      )
    : 0;

  return (
    <PageContainer
      pageTitle='حوالات الدفعة'
      pageDescription='رفع ملفات CSV لتنفيذ تحويلات جماعية، مراجعة التقدم والموافقة.'
    >
      <div className='space-y-4'>
        {/* Stats Row */}
        {jobs.data && jobs.data.length > 0 && (
          <div className='grid grid-cols-3 gap-3 md:grid-cols-6'>
            {[
              ['PENDING_APPROVAL', 'بانتظار الموافقة', 'text-amber-600'],
              ['APPROVED', 'موافق عليه', 'text-blue-600'],
              ['PROCESSING', 'قيد التنفيذ', 'text-blue-600'],
              ['COMPLETED', 'مكتمل', 'text-emerald-600'],
              ['FAILED', 'فاشل', 'text-destructive'],
              ['CANCELLED', 'ملغى', 'text-muted-foreground'],
            ].map(([status, label, cls]) => (
              <button key={status}
                className={`gov-panel rounded-md p-3 text-center transition-all ${statusFilter === status ? 'ring-2 ring-primary/40' : ''}`}
                onClick={() => setStatusFilter((s) => s === status ? '' : status)}>
                <div className={`text-xl font-bold tabular-nums ${cls}`}>{statsMap[status] ?? 0}</div>
                <div className='text-xs text-muted-foreground mt-0.5'>{label}</div>
              </button>
            ))}
          </div>
        )}

        <div className='grid gap-4 xl:grid-cols-[400px_1fr]'>
          <div className='space-y-4'>
            {/* Upload */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>رفع ملف CSV</h2>
                <p className='text-muted-foreground text-xs'>اختر ملف بيانات الحوالات الجماعية</p>
              </div>
              <div className='p-4 space-y-3'>
                <input ref={fileRef} type='file' accept='.csv,.xlsx' className='text-sm w-full' />
                <Button className='w-full h-9 rounded-sm' onClick={() => { void uploadFile(); }} disabled={uploading}>
                  {uploading
                    ? <Icons.spinner className='ml-2 size-4 animate-spin' />
                    : <Icons.upload className='ml-2 size-4' />}
                  رفع الملف
                </Button>
              </div>
            </div>

            {/* Validation Preview */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>التحقق من الملف</h2>
                <p className='text-muted-foreground text-xs'>تحقق من صحة الملف قبل الرفع</p>
              </div>
              <div className='p-4 space-y-3'>
                <input ref={validateRef} type='file' accept='.csv,.xlsx' className='text-sm w-full' />
                <Button variant='outline' className='w-full h-9 rounded-sm'
                  onClick={() => { void validateFile(); }} disabled={validating}>
                  {validating && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                  فحص الملف
                </Button>
                {validationResult && (
                  <div className={`rounded-sm border p-3 text-xs space-y-2 ${
                    validationResult.valid
                      ? 'border-emerald-200 bg-emerald-50 dark:bg-emerald-950/20'
                      : 'border-destructive/20 bg-destructive/5'
                  }`}>
                    <div className='flex items-center gap-1.5 font-medium'>
                      {validationResult.valid
                        ? <><Icons.check className='size-3.5 text-emerald-600' /> الملف صالح — {validationResult.rowCount} صف</>
                        : <><Icons.close className='size-3.5 text-destructive' /> يحتوي على أخطاء</>}
                    </div>
                    {(validationResult.errors ?? []).slice(0, 5).map((e, i) => (
                      <div key={i} className='text-destructive'>
                        صف {e.row}{e.column ? ` · عمود "${e.column}"` : ''}: {e.message}
                      </div>
                    ))}
                    {(validationResult.errors?.length ?? 0) > 5 && (
                      <div className='text-muted-foreground'>
                        وأيضاً {(validationResult.errors?.length ?? 0) - 5} خطأ آخر...
                      </div>
                    )}
                    {validationResult.valid && validationResult.preview && (
                      <div className='overflow-x-auto mt-1'>
                        <table className='gov-table text-xs'>
                          <thead>
                            <tr>{Object.keys(validationResult.preview[0] ?? {}).map((k) => <th key={k}>{k}</th>)}</tr>
                          </thead>
                          <tbody>
                            {validationResult.preview.slice(0, 3).map((row, i) => (
                              <tr key={i}>{Object.values(row).map((v, j) => <td key={j}>{v}</td>)}</tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Templates */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>القوالب المتاحة</h2>
              </div>
              <div className='divide-y divide-border'>
                {(templates.data ?? []).map((t) => (
                  <div key={t.id} className='px-4 py-3'>
                    <div className='flex items-center justify-between gap-2'>
                      <div className='font-medium text-sm'>{t.name ?? `قالب #${t.id}`}</div>
                      <Button size='sm' variant='ghost' className='h-7 text-xs shrink-0'
                        onClick={() => downloadTemplate(t.id, t.name)}>
                        <Icons.download className='ml-1 size-3.5' />
                        تنزيل
                      </Button>
                    </div>
                    <div className='text-xs text-muted-foreground'>{t.description ?? ''}</div>
                    {t.columns?.length ? (
                      <div className='mt-1 flex flex-wrap gap-1'>
                        {t.columns.map((col) => (
                          <Badge key={col} variant='outline' className='text-xs'>{col}</Badge>
                        ))}
                      </div>
                    ) : null}
                  </div>
                ))}
                {!templates.loading && !templates.data?.length && (
                  <div className='px-4 py-3 text-sm text-muted-foreground'>لا توجد قوالب.</div>
                )}
              </div>
            </div>
          </div>

          <div className='space-y-4'>
            {/* Selected Job Detail */}
            {selectedJob && (
              <div className='gov-panel rounded-md'>
                <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                  <div>
                    <h2 className='text-sm font-semibold'>
                      وظيفة #{selectedJob.id}
                      {selectedJob.name ? ` — ${selectedJob.name}` : ''}
                    </h2>
                    <p className='text-muted-foreground text-xs'>{selectedJob.createdAt ?? ''}</p>
                  </div>
                  <Badge variant={statusVariant(selectedJob.status)}>
                    {STATUS_LABEL[selectedJob.status ?? ''] ?? selectedJob.status ?? '-'}
                  </Badge>
                </div>
                <div className='p-4 space-y-4'>
                  {/* SSE Progress */}
                  {(progress || selectedJob.status === 'PROCESSING') && (
                    <div className='space-y-3'>
                      <Progress value={progressPct} className='h-2' />
                      <div className='grid grid-cols-3 gap-3 text-center'>
                        <div className='border-border rounded-sm border p-2'>
                          <div className='text-lg font-semibold text-emerald-600'>
                            {progress?.successCount ?? selectedJob.successRows ?? 0}
                          </div>
                          <div className='text-xs text-muted-foreground'>ناجح</div>
                        </div>
                        <div className='border-border rounded-sm border p-2'>
                          <div className='text-lg font-semibold text-destructive'>
                            {progress?.failureCount ?? selectedJob.failedRows ?? 0}
                          </div>
                          <div className='text-xs text-muted-foreground'>فاشل</div>
                        </div>
                        <div className='border-border rounded-sm border p-2'>
                          <div className='text-lg font-semibold'>
                            {progress?.totalCount ?? selectedJob.totalRows ?? 0}
                          </div>
                          <div className='text-xs text-muted-foreground'>المجموع</div>
                        </div>
                      </div>
                      {sseRef.current && (
                        <p className='text-xs text-muted-foreground flex items-center gap-1'>
                          <Icons.spinner className='size-3 animate-spin' />
                          جاري التنفيذ...
                        </p>
                      )}
                    </div>
                  )}

                  {/* Actions */}
                  <div className='flex flex-wrap gap-2'>
                    {selectedJob.status === 'PENDING_APPROVAL' && (
                      <Button size='sm' className='h-8 text-xs'
                        onClick={() => { void approveJob(selectedJob.id); }}>
                        موافقة
                      </Button>
                    )}
                    {selectedJob.status === 'APPROVED' && (
                      <Button size='sm' variant='outline' className='h-8 text-xs'
                        onClick={() => { void executeJob(selectedJob.id); }}>
                        <Icons.send className='ml-1.5 size-3.5' />
                        تنفيذ
                      </Button>
                    )}
                    {(selectedJob.status === 'COMPLETED' || selectedJob.status === 'FAILED') && (
                      <>
                        <Button size='sm' variant='outline' className='h-8 text-xs'
                          onClick={() => { void loadReport(selectedJob.id); }} disabled={loadingReport}>
                          {loadingReport && <Icons.spinner className='ml-1.5 size-3.5 animate-spin' />}
                          عرض التقرير
                        </Button>
                        {(selectedJob.failedRows ?? 0) > 0 && (
                          <Button size='sm' variant='outline'
                            className='h-8 text-xs text-amber-600 border-amber-200'
                            onClick={() => { void retryFailed(selectedJob.id); }}
                            disabled={retrying === selectedJob.id}>
                            {retrying === selectedJob.id
                              ? <Icons.spinner className='ml-1.5 size-3.5 animate-spin' />
                              : null}
                            إعادة محاولة الفاشل ({selectedJob.failedRows})
                          </Button>
                        )}
                      </>
                    )}
                    {(selectedJob.status === 'PENDING_APPROVAL' || selectedJob.status === 'APPROVED') && (
                      <Button size='sm' variant='destructive' className='h-8 text-xs'
                        onClick={() => { void cancelJob(selectedJob.id); }}>
                        إلغاء
                      </Button>
                    )}
                  </div>

                  {/* Inline Report */}
                  {report && (
                    <div className='space-y-2'>
                      <div className='flex items-center justify-between'>
                        <div className='text-xs font-medium text-muted-foreground'>تقرير التنفيذ</div>
                        <div className='flex gap-3 text-xs'>
                          <span className='text-emerald-600'>ناجح: {report.successCount ?? 0}</span>
                          <span className='text-destructive'>فاشل: {report.failedCount ?? 0}</span>
                          <span>الإجمالي: {report.totalRows ?? 0}</span>
                        </div>
                      </div>
                      <div className='overflow-x-auto'>
                        <table className='gov-table'>
                          <thead>
                            <tr>
                              <th>#</th>
                              <th>المستفيد</th>
                              <th>المبلغ</th>
                              <th>الحالة</th>
                              <th>رقم المعاملة</th>
                              <th>ملاحظة</th>
                            </tr>
                          </thead>
                          <tbody>
                            {(report.rows ?? []).map((row, i) => (
                              <tr key={i}>
                                <td className='text-muted-foreground'>{row.rowNumber ?? i + 1}</td>
                                <td>{row.receiverIdentifier ?? '-'}</td>
                                <td className='tabular-nums'>{row.amount ?? '-'}</td>
                                <td>
                                  <Badge variant={row.status === 'SUCCESS' ? 'default' : 'destructive'} className='text-xs'>
                                    {row.status === 'SUCCESS' ? 'ناجح' : row.status ?? '-'}
                                  </Badge>
                                </td>
                                <td className='text-xs text-muted-foreground'>{row.transactionId ?? '-'}</td>
                                <td className='text-xs text-muted-foreground max-w-48 truncate'>
                                  {row.errorMessage ?? '-'}
                                </td>
                              </tr>
                            ))}
                            {!report.rows?.length && (
                              <tr><td colSpan={6} className='text-muted-foreground text-sm text-center py-3'>لا صفوف.</td></tr>
                            )}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Jobs List */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>
                  وظائف الدفعة
                  {statusFilter && (
                    <span className='text-muted-foreground font-normal mr-1 text-xs'>
                      ({STATUS_LABEL[statusFilter] ?? statusFilter})
                    </span>
                  )}
                </h2>
                <div className='flex items-center gap-2'>
                  {jobs.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
                  {statusFilter && (
                    <Button size='sm' variant='ghost' className='h-7 text-xs text-muted-foreground'
                      onClick={() => setStatusFilter('')}>
                      مسح الفلتر
                    </Button>
                  )}
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
                      <th>الاسم</th>
                      <th>الحالة</th>
                      <th>الإجمالي</th>
                      <th>ناجح</th>
                      <th>فاشل</th>
                      <th>التاريخ</th>
                      <th>إجراء</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredJobs.map((job) => (
                      <tr key={job.id} className={selectedJob?.id === job.id ? 'bg-muted/40' : ''}>
                        <td className='text-muted-foreground'>{job.id}</td>
                        <td>{job.name ?? '-'}</td>
                        <td>
                          <Badge variant={statusVariant(job.status)} className='text-xs'>
                            {STATUS_LABEL[job.status ?? ''] ?? job.status ?? '-'}
                          </Badge>
                        </td>
                        <td className='tabular-nums'>{job.totalRows ?? '-'}</td>
                        <td className='tabular-nums text-emerald-600'>{job.successRows ?? '-'}</td>
                        <td className='tabular-nums text-destructive'>{job.failedRows ?? '-'}</td>
                        <td className='text-xs text-muted-foreground'>{job.createdAt ?? '-'}</td>
                        <td>
                          <Button size='sm' variant='outline' className='h-7 text-xs'
                            onClick={() => selectJob(job)}>
                            تفاصيل
                          </Button>
                        </td>
                      </tr>
                    ))}
                    {!jobs.loading && !filteredJobs.length && (
                      <tr>
                        <td colSpan={8} className='py-8 text-center text-muted-foreground text-sm'>
                          {statusFilter ? `لا توجد وظائف بحالة "${STATUS_LABEL[statusFilter] ?? statusFilter}".` : 'لا توجد وظائف دفعة بعد.'}
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
