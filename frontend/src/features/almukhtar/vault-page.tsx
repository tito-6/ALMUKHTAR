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
import { apiClient } from '@/lib/api-client';
import { useRef, useState } from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';

type VaultDocument = {
  id: number;
  filename?: string;
  fileType?: string;
  documentType?: string;
  uploadedAt?: string;
  fileSize?: number;
  status?: string;
};

type DownloadUrlResponse = {
  url?: string;
  downloadUrl?: string;
  signedUrl?: string;
};

const DOC_TYPE_OPTIONS = [
  { value: 'IDENTITY', label: 'هوية وطنية' },
  { value: 'PASSPORT', label: 'جواز سفر' },
  { value: 'ADDRESS', label: 'إثبات عنوان' },
  { value: 'CONTRACT', label: 'عقد' },
  { value: 'INCOME', label: 'إثبات دخل' },
  { value: 'TRADE_LICENSE', label: 'سجل تجاري' },
  { value: 'OTHER', label: 'أخرى' }
];

const DOC_TYPE_LABEL: Record<string, string> = Object.fromEntries(
  DOC_TYPE_OPTIONS.map(({ value, label }) => [value, label])
);

const DOC_STATUS_LABEL: Record<string, string> = {
  ACTIVE: 'نشط', PENDING_REVIEW: 'قيد المراجعة',
  EXPIRED: 'منتهي الصلاحية', REJECTED: 'مرفوض'
};

function docTypeLabel(t?: string) {
  return DOC_TYPE_LABEL[t ?? ''] ?? t ?? 'أخرى';
}

function docStatusVariant(s?: string): 'default' | 'secondary' | 'destructive' | 'outline' {
  if (s === 'ACTIVE') return 'default';
  if (s === 'PENDING_REVIEW') return 'secondary';
  if (s === 'REJECTED' || s === 'EXPIRED') return 'destructive';
  return 'outline';
}

function sizeLabel(bytes?: number): string {
  if (!bytes) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function fileIcon(fileType?: string): string {
  const t = (fileType ?? '').toLowerCase();
  if (t.includes('pdf')) return '📄';
  if (t.includes('image') || t.includes('jpeg') || t.includes('png')) return '🖼️';
  if (t.includes('word') || t.includes('doc')) return '📝';
  return '📎';
}

function isImageType(fileType?: string): boolean {
  const t = (fileType ?? '').toLowerCase();
  return t.includes('image') || t.includes('jpeg') || t.includes('jpg') || t.includes('png') || t.includes('gif') || t.includes('webp');
}

// ─── Upload Panel ─────────────────────────────────────────────────────────────

function UploadPanel({ onSuccess }: { onSuccess: () => void }) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [docType, setDocType] = useState('IDENTITY');
  const [uploading, setUploading] = useState(false);
  const [fileName, setFileName] = useState<string | null>(null);

  async function handleUpload() {
    const file = fileRef.current?.files?.[0];
    if (!file) { toast.error('اختر ملفاً أولاً'); return; }

    const maxSize = 10 * 1024 * 1024; // 10 MB
    if (file.size > maxSize) {
      toast.error('حجم الملف يتجاوز 10 ميجابايت');
      return;
    }

    setUploading(true);
    try {
      const form = new FormData();
      form.append('file', file);
      form.append('documentType', docType);
      await apiClient('/vault/upload', { method: 'POST', body: form });
      toast.success(`✅ تم رفع "${file.name}" بنجاح`);
      if (fileRef.current) fileRef.current.value = '';
      setFileName(null);
      onSuccess();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل رفع الملف');
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className='gov-panel rounded-md'>
      <div className='gov-panel-header rounded-t-md px-4 py-3'>
        <h2 className='text-sm font-semibold'>رفع مستند جديد</h2>
        <p className='text-muted-foreground text-xs'>PDF أو صورة — الحد الأقصى 10 MB</p>
      </div>
      <div className='p-4 space-y-3'>
        <div className='space-y-1.5'>
          <label className='text-xs font-medium'>نوع المستند</label>
          <select
            value={docType}
            onChange={(e) => setDocType(e.target.value)}
            className='h-9 w-full rounded-sm border border-border bg-background px-2 text-sm'
          >
            {DOC_TYPE_OPTIONS.map(({ value, label }) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>

        <div className='space-y-1.5'>
          <label className='text-xs font-medium'>الملف</label>
          <div
            className='relative flex flex-col items-center justify-center gap-2 rounded-sm border-2 border-dashed border-border p-5 text-center transition-colors hover:border-primary/50 cursor-pointer'
            onClick={() => fileRef.current?.click()}
          >
            <Icons.upload className='size-6 text-muted-foreground' />
            <p className='text-xs text-muted-foreground'>
              {fileName ?? 'انقر لاختيار ملف أو اسحبه هنا'}
            </p>
            <input
              ref={fileRef}
              type='file'
              accept='image/*,.pdf,.doc,.docx'
              className='sr-only'
              onChange={(e) => setFileName(e.target.files?.[0]?.name ?? null)}
            />
          </div>
        </div>

        <Button
          className='w-full h-9 rounded-sm'
          onClick={() => { void handleUpload(); }}
          disabled={uploading || !fileName}
        >
          {uploading ? (
            <>
              <Icons.spinner className='me-2 size-4 animate-spin' />
              جاري الرفع...
            </>
          ) : (
            <>
              <Icons.upload className='me-2 size-4' />
              رفع المستند
            </>
          )}
        </Button>
      </div>
    </div>
  );
}

// ─── Main Page ─────────────────────────────────────────────────────────────────

export function VaultPage() {
  const docs = useBackend<VaultDocument[]>('/vault');
  const [deleting, setDeleting] = useState<number | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<VaultDocument | null>(null);
  const [previewDoc, setPreviewDoc] = useState<{ id: number; url: string; filename: string } | null>(null);
  const [downloading, setDownloading] = useState<number | null>(null);

  async function handleDelete(doc: VaultDocument) {
    setDeleting(doc.id);
    try {
      await apiClient(`/vault/${doc.id}`, { method: 'DELETE' });
      toast.success(`تم حذف "${doc.filename ?? `مستند-${doc.id}`}"`);
      docs.refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل حذف المستند');
    } finally {
      setDeleting(null);
      setConfirmDelete(null);
    }
  }

  /**
   * Backend GET /api/vault/{id}/download may return:
   *   a) JSON { url: "https://..." }  → redirect to that URL
   *   b) Binary blob                  → trigger direct download
   */
  async function handleDownload(doc: VaultDocument) {
    setDownloading(doc.id);
    try {
      const session =
        typeof window !== 'undefined'
          ? (JSON.parse(localStorage.getItem('almukhtar.session') ?? 'null') as { token?: string } | null)
          : null;

      const res = await fetch(`/api/backend/vault/${doc.id}/download`, {
        headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {}
      });

      if (!res.ok) {
        toast.error(`فشل التنزيل — ${res.status}`);
        return;
      }

      const contentType = res.headers.get('content-type') ?? '';

      if (contentType.includes('application/json')) {
        // Backend returned a signed URL
        const json = (await res.json()) as DownloadUrlResponse;
        const url = json.url ?? json.downloadUrl ?? json.signedUrl;
        if (url) {
          if (isImageType(doc.fileType) && previewDoc === null) {
            setPreviewDoc({ id: doc.id, url, filename: doc.filename ?? `مستند-${doc.id}` });
          } else {
            window.open(url, '_blank', 'noopener');
          }
        } else {
          toast.error('لم يتم العثور على رابط التنزيل');
        }
      } else {
        // Backend returned binary content
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.filename ?? `document-${doc.id}`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التنزيل');
    } finally {
      setDownloading(null);
    }
  }

  return (
    <PageContainer
      pageTitle='خزينة الوثائق'
      pageDescription='رفع، معاينة، تنزيل، وإدارة مستندات التحقق والهوية والعقود.'
    >
      <div className='grid gap-4 xl:grid-cols-[320px_1fr]'>
        <UploadPanel onSuccess={() => docs.refetch()} />

        {/* Documents Table */}
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <div>
              <h2 className='text-sm font-semibold'>مستنداتي</h2>
              <p className='text-muted-foreground text-xs'>
                {docs.data?.length ?? 0} مستند مرفوع
              </p>
            </div>
            <div className='flex items-center gap-2'>
              {docs.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
              <Button variant='ghost' size='sm' className='h-7 w-7 p-0' onClick={() => docs.refetch()}>
                <Icons.refresh className='size-3.5' />
              </Button>
            </div>
          </div>

          {docs.error && (
            <div className='mx-4 mt-3 rounded-sm border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-destructive'>
              {docs.error}
            </div>
          )}

          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th>
                  <th>اسم الملف</th>
                  <th>النوع</th>
                  <th>الحجم</th>
                  <th>الحالة</th>
                  <th>تاريخ الرفع</th>
                  <th>إجراءات</th>
                </tr>
              </thead>
              <tbody>
                {(docs.data ?? []).map((doc) => (
                  <tr key={doc.id} className='hover:bg-muted/20 transition-colors'>
                    <td className='font-mono text-xs'>#{doc.id}</td>
                    <td>
                      <div className='flex items-center gap-1.5 max-w-48'>
                        <span className='shrink-0'>{fileIcon(doc.fileType)}</span>
                        <span className='truncate text-xs'>{doc.filename ?? `مستند-${doc.id}`}</span>
                      </div>
                    </td>
                    <td>
                      <Badge variant='outline' className='text-xs'>
                        {docTypeLabel(doc.documentType)}
                      </Badge>
                    </td>
                    <td className='text-muted-foreground text-xs'>{sizeLabel(doc.fileSize)}</td>
                    <td>
                      <Badge variant={docStatusVariant(doc.status)} className='text-xs'>
                        {DOC_STATUS_LABEL[doc.status ?? ''] ?? doc.status ?? '—'}
                      </Badge>
                    </td>
                    <td className='text-muted-foreground text-xs'>
                      {doc.uploadedAt?.split('T')[0] ?? '—'}
                    </td>
                    <td>
                      <div className='flex gap-1'>
                        <Button
                          size='sm'
                          variant='outline'
                          className='h-7 w-7 p-0'
                          onClick={() => { void handleDownload(doc); }}
                          disabled={downloading === doc.id}
                          title='تنزيل'
                        >
                          {downloading === doc.id
                            ? <Icons.spinner className='size-3 animate-spin' />
                            : <Icons.download className='size-3' />}
                        </Button>
                        <Button
                          size='sm'
                          variant='ghost'
                          className='h-7 w-7 p-0 text-destructive hover:text-destructive'
                          onClick={() => setConfirmDelete(doc)}
                          disabled={deleting === doc.id}
                          title='حذف'
                        >
                          {deleting === doc.id
                            ? <Icons.spinner className='size-3 animate-spin' />
                            : <Icons.trash className='size-3' />}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!docs.loading && !docs.data?.length && !docs.error && (
                  <tr>
                    <td colSpan={7} className='text-center text-muted-foreground text-sm py-8'>
                      <div className='flex flex-col items-center gap-2'>
                        <span className='text-3xl'>📂</span>
                        <span>لا توجد مستندات مرفوعة بعد.</span>
                        <span className='text-xs'>ارفع مستنداتك من اللوحة على اليمين.</span>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      {confirmDelete && (
        <Dialog open onOpenChange={(v) => { if (!v) setConfirmDelete(null); }}>
          <DialogContent className='max-w-sm'>
            <DialogHeader>
              <DialogTitle>تأكيد الحذف</DialogTitle>
              <DialogDescription>
                هل أنت متأكد من حذف مستند{' '}
                <strong>"{confirmDelete.filename ?? `#${confirmDelete.id}`}"</strong>؟
                لا يمكن التراجع عن هذا الإجراء.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant='outline' onClick={() => setConfirmDelete(null)} className='h-8 text-xs'>
                إلغاء
              </Button>
              <Button
                variant='destructive'
                className='h-8 text-xs'
                onClick={() => { void handleDelete(confirmDelete); }}
                disabled={deleting === confirmDelete.id}
              >
                {deleting === confirmDelete.id && (
                  <Icons.spinner className='me-1.5 size-3 animate-spin' />
                )}
                حذف نهائياً
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}

      {/* Image Preview Dialog */}
      {previewDoc && (
        <Dialog open onOpenChange={(v) => { if (!v) setPreviewDoc(null); }}>
          <DialogContent className='max-w-2xl'>
            <DialogHeader>
              <DialogTitle className='text-sm'>{previewDoc.filename}</DialogTitle>
            </DialogHeader>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={previewDoc.url}
              alt={previewDoc.filename}
              className='w-full max-h-[60vh] object-contain rounded-sm border border-border'
            />
            <DialogFooter>
              <Button
                variant='outline'
                className='h-8 text-xs'
                onClick={() => {
                  window.open(previewDoc.url, '_blank', 'noopener');
                }}
              >
                <Icons.download className='me-1.5 size-3' />
                فتح في تبويب جديد
              </Button>
              <Button variant='ghost' className='h-8 text-xs' onClick={() => setPreviewDoc(null)}>
                إغلاق
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </PageContainer>
  );
}
