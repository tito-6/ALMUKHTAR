'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger
} from '@/components/ui/alert-dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Separator } from '@/components/ui/separator';
import { apiClient } from '@/lib/api-client';
import { currencyFlag, currencyFlagUrl, currencyName } from '@/lib/currencies';
import { CurrencyFlag } from '@/components/currency-flag';
import {
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getSortedRowModel,
  useReactTable,
  type ColumnDef,
  type SortingState
} from '@tanstack/react-table';
import { useMemo, useState } from 'react';
import { toast } from 'sonner';
import type { Fund, PageResponse, Transaction } from './types';
import { useBackend } from './use-backend';
import { useSession } from './session';

const NUM_FMT = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });

function fmt(n?: number | string, c = 'USD') {
  return `${currencyFlag(c)} ${NUM_FMT.format(Number(n ?? 0))} ${c}`;
}

function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

const CURRENCIES = ['USD', 'EUR', 'GBP', 'TRY', 'SYP', 'SAR', 'AED', 'JOD'];

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
      <Label htmlFor={id} className='text-xs font-medium'>
        {label}
      </Label>
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

function statusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
  switch (status?.toUpperCase()) {
    case 'COMPLETED': return 'default';
    case 'RELEASED': return 'default';
    case 'PENDING': return 'secondary';
    case 'READY_FOR_PICKUP': return 'secondary';
    case 'FAILED': return 'destructive';
    case 'CANCELLED': return 'destructive';
    default: return 'outline';
  }
}

const STATUS_LABEL: Record<string, string> = {
  COMPLETED: 'مكتملة',
  RELEASED: 'صُرفت من المرسل',
  PENDING: 'معلقة',
  READY_FOR_PICKUP: 'جاهزة للاستلام',
  FAILED: 'فاشلة',
  CANCELLED: 'ملغية',
  PROCESSING: 'قيد المعالجة'
};

const isCancellable = (status: string) =>
  !['COMPLETED', 'FAILED', 'CANCELLED'].includes(status?.toUpperCase());

type SendForm = {
  senderName: string;
  senderPhone: string;
  receiverName: string;
  receiverPhone: string;
  amount: string;
  currency: string;
  fundId: string;
  note: string;
};

type PickupForm = {
  passcode: string;
  qrToken: string;
  branchId: string;
};

function CancelButton({ tx, onDone }: { tx: Transaction; onDone: () => void }) {
  const [cancelling, setCancelling] = useState(false);

  async function doCancel() {
    setCancelling(true);
    try {
      await apiClient(`/transactions/${tx.id}/cancel`, { method: 'PUT' });
      toast.success(`تم إلغاء الحوالة #${tx.id}`);
      onDone();
    } catch {
      try {
        await apiClient(`/transactions/${tx.id}`, {
          method: 'PATCH',
          body: JSON.stringify({ status: 'CANCELLED' })
        });
        toast.success(`تم إلغاء الحوالة #${tx.id}`);
        onDone();
      } catch (err2) {
        toast.error(err2 instanceof Error ? err2.message : 'تعذر إلغاء الحوالة');
      }
    } finally {
      setCancelling(false);
    }
  }

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant='destructive' size='sm' className='h-6 px-2 text-xs' disabled={cancelling}>
          {cancelling ? <Icons.spinner className='size-3 animate-spin' /> : <Icons.close className='size-3' />}
          <span className='ms-1'>إلغاء</span>
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>تأكيد إلغاء الحوالة #{tx.id}</AlertDialogTitle>
          <AlertDialogDescription>
            الحوالة من <strong>{tx.senderName ?? `#${tx.senderId}`}</strong> إلى{' '}
            <strong>{tx.receiverName ?? `#${tx.receiverId}`}</strong> بمبلغ{' '}
            <strong>{fmt(tx.amount, tx.currencyCode ?? 'USD')}</strong>.
            <br />
            الحالة الحالية: <strong>{STATUS_LABEL[tx.status] ?? tx.status}</strong>
            <br /><br />
            هل أنت متأكد من إلغاء هذه الحوالة؟ لا يمكن التراجع عن هذا الإجراء.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>تراجع</AlertDialogCancel>
          <AlertDialogAction
            className='bg-destructive text-destructive-foreground hover:bg-destructive/90'
            onClick={() => { void doCancel(); }}
          >
            نعم، إلغاء الحوالة
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

export function TransfersPage() {
  const { session } = useSession();
  const isAdmin = session?.role === 'SUPER_ADMIN' || session?.role === 'ADMIN' || session?.role === 'OWNER';

  const txResult = useBackend<PageResponse<Transaction> | Transaction[]>('/transactions?size=200');
  const fundsResult = useBackend<Fund[]>('/funds');

  const [mode, setMode] = useState<'send' | 'pickup'>('send');
  const [sending, setSending] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [refresh, setRefresh] = useState(0);

  const [send, setSend] = useState<SendForm>({
    senderName: '',
    senderPhone: '',
    receiverName: '',
    receiverPhone: '',
    amount: '',
    currency: 'USD',
    fundId: '',
    note: ''
  });

  const [pickup, setPickup] = useState<PickupForm>({
    passcode: '',
    qrToken: '',
    branchId: ''
  });

  const [sorting, setSorting] = useState<SortingState>([{ id: 'id', desc: true }]);
  const [globalFilter, setGlobalFilter] = useState('');

  const allTx: Transaction[] = useMemo(() => {
    const raw = txResult.data;
    if (!raw) return [];
    if (Array.isArray(raw)) return raw;
    return (raw as PageResponse<Transaction>).content ?? [];
  }, [txResult.data]);

  const filteredTx = useMemo(() => {
    if (statusFilter === 'ALL') return allTx;
    return allTx.filter((t) => t.status === statusFilter);
  }, [allTx, statusFilter]);

  const fundOptions = useMemo(() => fundsResult.data ?? [], [fundsResult.data]);

  const columns = useMemo<ColumnDef<Transaction>[]>(() => {
    const cols: ColumnDef<Transaction>[] = [
      {
        accessorKey: 'id',
        header: 'رقم الحوالة',
        cell: ({ row }) => <span className='font-mono text-xs'>#{row.original.id}</span>
      },
      {
        accessorKey: 'senderName',
        header: 'المرسل',
        cell: ({ row }) => (
          <div>
            <div className='font-medium text-sm'>{row.original.senderName ?? `—`}</div>
            <div className='text-muted-foreground text-xs'>{row.original.senderPhone ?? ''}</div>
          </div>
        )
      },
      {
        accessorKey: 'receiverName',
        header: 'المستلم',
        cell: ({ row }) => (
          <div>
            <div className='font-medium text-sm'>{row.original.receiverName ?? `—`}</div>
            <div className='text-muted-foreground text-xs'>{row.original.receiverPhone ?? ''}</div>
          </div>
        )
      },
      {
        accessorKey: 'amount',
        header: 'المبلغ',
        cell: ({ row }) => (
          <span className='inline-flex items-center gap-1 tabular-nums font-medium'>
            <CurrencyFlag code={row.original.currencyCode ?? 'USD'} size={16} />
            {NUM_FMT.format(Number(row.original.amount ?? 0))}{' '}
            <span className='text-muted-foreground text-xs'>{row.original.currencyCode ?? 'USD'}</span>
          </span>
        )
      },
      {
        accessorKey: 'status',
        header: 'الحالة',
        cell: ({ row }) => (
          <Badge variant={statusVariant(row.original.status)} className='text-xs'>
            {STATUS_LABEL[row.original.status] ?? row.original.status}
          </Badge>
        )
      },
      {
        accessorKey: 'createdAt',
        header: 'التاريخ',
        cell: ({ row }) => (
          <span className='text-muted-foreground text-xs'>{row.original.createdAt ?? '—'}</span>
        )
      }
    ];

    if (isAdmin) {
      cols.push({
        id: 'actions',
        header: 'إجراء',
        cell: ({ row }) =>
          isCancellable(row.original.status) ? (
            <CancelButton
              tx={row.original}
              onDone={() => setRefresh((n) => n + 1)}
            />
          ) : (
            <span className='text-muted-foreground text-xs'>—</span>
          )
      });
    }

    return cols;
  }, [isAdmin]);

  const table = useReactTable({
    data: filteredTx,
    columns,
    state: { sorting, globalFilter },
    onSortingChange: setSorting,
    onGlobalFilterChange: setGlobalFilter,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel()
  });

  async function submitSend(e: React.FormEvent) {
    e.preventDefault();
    setSending(true);
    try {
      const result = await apiClient<Transaction>('/transactions/transfer', {
        method: 'POST',
        headers: { 'X-Idempotency-Key': idempotencyKey() },
        body: JSON.stringify({
          senderName: send.senderName,
          senderPhone: send.senderPhone,
          receiverName: send.receiverName,
          receiverPhone: send.receiverPhone,
          fundId: Number(send.fundId) || undefined,
          amount: Number(send.amount),
          currency: send.currency,
          note: send.note || undefined
        })
      });
      toast.success(`تم تثبيت الحوالة رقم ${result?.id ?? '—'}`);
      setSend({ senderName: '', senderPhone: '', receiverName: '', receiverPhone: '', amount: '', currency: 'USD', fundId: '', note: '' });
      setRefresh((n) => n + 1);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إرسال الحوالة');
    } finally {
      setSending(false);
    }
  }

  async function submitPickup(e: React.FormEvent) {
    e.preventDefault();
    setSending(true);
    try {
      await apiClient('/wallet/cashout/pickup', {
        method: 'POST',
        headers: { 'X-Idempotency-Key': idempotencyKey() },
        body: JSON.stringify({
          passcode: pickup.passcode,
          qrToken: pickup.qrToken || undefined,
          branchId: Number(pickup.branchId) || undefined
        })
      });
      toast.success('تم صرف الحوالة بنجاح');
      setPickup({ passcode: '', qrToken: '', branchId: '' });
      setRefresh((n) => n + 1);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشلت عملية الصرف');
    } finally {
      setSending(false);
    }
  }

  const statusCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const tx of allTx) {
      counts[tx.status] = (counts[tx.status] ?? 0) + 1;
    }
    return counts;
  }, [allTx]);

  void refresh;

  return (
    <PageContainer
      pageTitle='مكتب الحوالات'
      pageDescription='إرسال حوالة، صرف طلب نقدي، ومراجعة السجل الكامل.'
    >
      <div className='grid gap-4 xl:grid-cols-[400px_1fr]'>
        {/* ── Wizard Panel ── */}
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header rounded-t-md px-4 py-2'>
            <div className='flex gap-1'>
              <button
                type='button'
                onClick={() => setMode('send')}
                className={`rounded-sm px-3 py-1.5 text-xs font-semibold transition-colors ${
                  mode === 'send'
                    ? 'bg-foreground text-background'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <Icons.send className='me-1.5 inline size-3.5' />
                إرسال حوالة
              </button>
              <button
                type='button'
                onClick={() => setMode('pickup')}
                className={`rounded-sm px-3 py-1.5 text-xs font-semibold transition-colors ${
                  mode === 'pickup'
                    ? 'bg-foreground text-background'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                <Icons.check className='me-1.5 inline size-3.5' />
                صرف حوالة
              </button>
            </div>
          </div>

          {mode === 'send' ? (
            <form className='grid gap-3 p-4' onSubmit={(e) => { void submitSend(e); }}>
              <div className='text-muted-foreground text-xs font-medium uppercase tracking-wider'>بيانات المرسل</div>
              <div className='grid grid-cols-2 gap-3'>
                <Field id='senderName' label='اسم المرسل' value={send.senderName} onChange={(v) => setSend((p) => ({ ...p, senderName: v }))} placeholder='الاسم الكامل' />
                <Field id='senderPhone' label='رقم هاتفه' value={send.senderPhone} onChange={(v) => setSend((p) => ({ ...p, senderPhone: v }))} placeholder='+9639xxxxxxxx' />
              </div>

              <Separator className='my-0.5' />
              <div className='text-muted-foreground text-xs font-medium uppercase tracking-wider'>بيانات المستلم</div>
              <div className='grid grid-cols-2 gap-3'>
                <Field id='receiverName' label='اسم المستلم' value={send.receiverName} onChange={(v) => setSend((p) => ({ ...p, receiverName: v }))} placeholder='الاسم الكامل' />
                <Field id='receiverPhone' label='رقم هاتفه' value={send.receiverPhone} onChange={(v) => setSend((p) => ({ ...p, receiverPhone: v }))} placeholder='+9639xxxxxxxx' />
              </div>

              <Separator className='my-0.5' />
              <div className='grid grid-cols-2 gap-3'>
                <div className='space-y-1'>
                  <Label className='text-xs font-medium'>المبلغ</Label>
                  <Input
                    type='number'
                    value={send.amount}
                    onChange={(e) => setSend((p) => ({ ...p, amount: e.target.value }))}
                    className='h-8 rounded-sm text-sm'
                    min='1'
                    placeholder='0'
                    required
                  />
                </div>
                <div className='space-y-1'>
                  <Label className='text-xs font-medium'>العملة</Label>
                  <Select value={send.currency} onValueChange={(v) => setSend((p) => ({ ...p, currency: v }))}>
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

              {/* Fund picker */}
              <div className='space-y-1'>
                <Label className='text-xs font-medium'>الصندوق التشغيلي</Label>
                <Select value={send.fundId} onValueChange={(v) => setSend((p) => ({ ...p, fundId: v }))}>
                  <SelectTrigger className='h-8 rounded-sm text-sm'>
                    <SelectValue placeholder='اختر الصندوق' />
                  </SelectTrigger>
                  <SelectContent>
                    {fundOptions.map((f) => (
                      <SelectItem key={f.id} value={String(f.id)}>
                        {f.name} ({f.balance ?? 0} {f.currency ?? ''})
                      </SelectItem>
                    ))}
                    {!fundOptions.length && (
                      <SelectItem value='__none' disabled>لا توجد صناديق متاحة</SelectItem>
                    )}
                  </SelectContent>
                </Select>
              </div>

              <div className='space-y-1'>
                <Label className='text-xs font-medium'>ملاحظة (اختياري)</Label>
                <Input
                  value={send.note}
                  onChange={(e) => setSend((p) => ({ ...p, note: e.target.value }))}
                  className='h-8 rounded-sm text-sm'
                  placeholder='تفاصيل إضافية...'
                />
              </div>

              <div className='bg-muted/40 rounded-sm border border-border p-2.5 mt-1 text-xs text-muted-foreground'>
                <Icons.info className='inline me-1 size-3' />
                الرسوم تُحسب تلقائياً من منطقة الرسوم المرتبطة بالصندوق والعملة عند التثبيت.
              </div>

              <Button type='submit' className='h-9 rounded-sm mt-1' disabled={sending}>
                {sending ? <Icons.spinner className='me-2 size-4 animate-spin' /> : <Icons.send className='me-2 size-4' />}
                تثبيت الحوالة
              </Button>
            </form>
          ) : (
            <form className='grid gap-3 p-4' onSubmit={(e) => { void submitPickup(e); }}>
              <div className='text-muted-foreground text-xs mb-1'>أدخل رمز الصرف وعنوان QR لإتمام الاستلام.</div>
              <Field id='passcode' label='رمز الصرف السداسي' value={pickup.passcode} onChange={(v) => setPickup((p) => ({ ...p, passcode: v }))} placeholder='123456' />
              <Field id='qrToken' label='رمز QR Token (اختياري)' value={pickup.qrToken} onChange={(v) => setPickup((p) => ({ ...p, qrToken: v }))} placeholder='xxxxxxxx-xxxx-xxxx' />
              <Field id='pickupBranch' label='معرف الفرع (اختياري)' value={pickup.branchId} onChange={(v) => setPickup((p) => ({ ...p, branchId: v }))} type='number' placeholder='رقم الفرع' />
              <Button type='submit' className='h-9 rounded-sm mt-1' disabled={sending}>
                {sending ? <Icons.spinner className='me-2 size-4 animate-spin' /> : <Icons.check className='me-2 size-4' />}
                تأكيد الصرف النقدي
              </Button>
            </form>
          )}
        </div>

        {/* ── Transactions Table ── */}
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex flex-wrap items-center justify-between gap-3 rounded-t-md px-4 py-2'>
            <div>
              <h2 className='text-sm font-semibold'>
                سجل الحوالات
                {isAdmin && (
                  <Badge variant='secondary' className='ms-2 text-xs'>صلاحية كاملة</Badge>
                )}
              </h2>
              <p className='text-muted-foreground text-xs'>
                {NUM_FMT.format(allTx.length)} حوالة إجمالاً | {NUM_FMT.format(filteredTx.length)} معروضة
              </p>
            </div>
            <div className='flex items-center gap-2'>
              {/* Status filter chips */}
              <div className='flex flex-wrap gap-1'>
                {['ALL', 'PENDING', 'RELEASED', 'READY_FOR_PICKUP', 'COMPLETED', 'CANCELLED'].map((s) => (
                  <button
                    key={s}
                    type='button'
                    onClick={() => setStatusFilter(s)}
                    className={`rounded-full px-2 py-0.5 text-xs font-medium transition-colors border ${
                      statusFilter === s
                        ? 'bg-foreground text-background border-foreground'
                        : 'text-muted-foreground border-border hover:border-foreground/50'
                    }`}
                  >
                    {s === 'ALL' ? 'الكل' : (STATUS_LABEL[s] ?? s)}
                    {s !== 'ALL' && statusCounts[s] ? ` (${statusCounts[s]})` : ''}
                  </button>
                ))}
              </div>
              <Input
                placeholder='بحث...'
                value={globalFilter}
                onChange={(e) => setGlobalFilter(e.target.value)}
                className='h-7 w-36 rounded-sm text-xs'
              />
            </div>
          </div>

          {isAdmin && (
            <div className='px-4 py-2 bg-amber-500/5 border-b border-amber-500/20 text-xs text-amber-700 dark:text-amber-400 flex items-center gap-1.5'>
              <Icons.warning className='size-3.5' />
              أنت تعرض جميع الحوالات بصلاحيات المالك. يمكنك إلغاء أي حوالة لم تُسلَّم بعد للمستلم.
            </div>
          )}

          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                {table.getHeaderGroups().map((hg) => (
                  <tr key={hg.id}>
                    {hg.headers.map((header) => (
                      <th
                        key={header.id}
                        onClick={header.column.getToggleSortingHandler()}
                        className={header.column.getCanSort() ? 'cursor-pointer select-none' : ''}
                      >
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {header.column.getIsSorted() === 'asc' && ' ↑'}
                        {header.column.getIsSorted() === 'desc' && ' ↓'}
                      </th>
                    ))}
                  </tr>
                ))}
              </thead>
              <tbody>
                {table.getRowModel().rows.map((row) => (
                  <tr
                    key={row.id}
                    className={`hover:bg-muted/30 transition-colors ${
                      row.original.status === 'RELEASED' ? 'bg-amber-500/5' : ''
                    }`}
                  >
                    {row.getVisibleCells().map((cell) => (
                      <td key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</td>
                    ))}
                  </tr>
                ))}
                {!txResult.loading && !filteredTx.length && (
                  <tr>
                    <td colSpan={columns.length} className='text-muted-foreground text-center text-sm py-6'>
                      {txResult.error ? 'تعذر تحميل الحوالات' : 'لا توجد حوالات بهذا الفلتر.'}
                    </td>
                  </tr>
                )}
                {txResult.loading && (
                  <tr>
                    <td colSpan={columns.length} className='text-muted-foreground text-center text-sm py-6'>
                      <Icons.spinner className='inline me-2 size-4 animate-spin' />
                      جارٍ التحميل...
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* RELEASED hawalas admin alert */}
          {isAdmin && statusCounts['RELEASED'] > 0 && (
            <div className='px-4 py-3 border-t border-amber-500/20 bg-amber-500/5'>
              <div className='text-xs font-semibold text-amber-700 dark:text-amber-400 mb-1'>
                ⚠️ {statusCounts['RELEASED']} حوالة صُرفت من الطرف المرسل ولم تُسلَّم بعد للمستلم
              </div>
              <p className='text-xs text-muted-foreground'>
                هذه الحوالات في حالة &quot;صُرفت من المرسل&quot; — يمكنك إلغاؤها إذا لم تُسلَّم للعميل في الفرع المستلِم بعد.
                <button
                  type='button'
                  className='ms-2 text-amber-700 dark:text-amber-400 underline underline-offset-2 font-medium'
                  onClick={() => setStatusFilter('RELEASED')}
                >
                  عرض فقط
                </button>
              </p>
            </div>
          )}
        </div>
      </div>
    </PageContainer>
  );
}
