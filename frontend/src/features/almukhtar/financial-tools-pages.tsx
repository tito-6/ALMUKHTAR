'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { apiClient } from '@/lib/api-client';
import { FormEvent, useState } from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import type {
  SavingsGoal,
  RecurringTransfer,
  PayoutReservation
} from './types';

function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

const moneyFmt = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });
const fmt = (v?: number | string, cur = '') => `${moneyFmt.format(Number(v ?? 0))}${cur ? ' ' + cur : ''}`;

function Field({
  id, label, value, onChange, type = 'text', placeholder
}: {
  id: string; label: string; value: string; onChange: (v: string) => void;
  type?: string; placeholder?: string;
}) {
  return (
    <div className='space-y-1.5'>
      <Label htmlFor={id} className='text-xs'>{label}</Label>
      <Input id={id} type={type} value={value} placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)} className='h-9 rounded-sm' />
    </div>
  );
}

// ─── Savings Goals ───────────────────────────────────────────────────────────

export function SavingsPage() {
  const goals = useBackend<SavingsGoal[]>('/savings-goals/my');
  const [form, setForm] = useState({ name: '', targetAmount: '', currency: 'USD', targetDate: '' });
  const [depositForm, setDepositForm] = useState<{ goalId: number; amount: string } | null>(null);

  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    await apiClient('/savings-goals', {
      method: 'POST',
      body: JSON.stringify({
        name: form.name,
        targetAmount: Number(form.targetAmount),
        currency: form.currency,
        deadline: form.targetDate || undefined
      })
    });
    toast.success('تم إنشاء هدف الادخار');
    setForm({ name: '', targetAmount: '', currency: 'USD', targetDate: '' });
    goals.refetch();
  }

  async function deleteGoal(id: number) {
    await apiClient(`/savings-goals/${id}`, { method: 'DELETE' });
    toast.success('تم حذف هدف الادخار');
    goals.refetch();
  }

  async function deposit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!depositForm) return;
    await apiClient(`/savings-goals/${depositForm.goalId}/deposit`, {
      method: 'POST',
      body: JSON.stringify({ amount: Number(depositForm.amount) })
    });
    toast.success('تم إضافة مبلغ للهدف');
    setDepositForm(null);
    goals.refetch();
  }

  return (
    <PageContainer
      pageTitle='أهداف الادخار'
      pageDescription='إنشاء وإدارة أهداف الادخار الشخصية.'
    >
      <div className='grid gap-4 xl:grid-cols-[320px_1fr]'>
        <div className='space-y-4'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>هدف ادخار جديد</h2>
            </div>
            <form className='grid gap-3 p-4' onSubmit={(e) => { void create(e); }}>
              <Field id='sgName' label='اسم الهدف' value={form.name}
                onChange={(v) => setForm((c) => ({ ...c, name: v }))} placeholder='مثال: صندوق الطوارئ' />
              <div className='grid grid-cols-2 gap-2'>
                <Field id='sgAmount' label='المبلغ المستهدف' value={form.targetAmount}
                  onChange={(v) => setForm((c) => ({ ...c, targetAmount: v }))} type='number' />
                <Field id='sgCur' label='العملة' value={form.currency}
                  onChange={(v) => setForm((c) => ({ ...c, currency: v }))} />
              </div>
              <Field id='sgDate' label='تاريخ الهدف' value={form.targetDate}
                onChange={(v) => setForm((c) => ({ ...c, targetDate: v }))} type='date' />
              <Button type='submit' className='h-9 rounded-sm'>
                <Icons.check className='ml-2 size-4' />
                إنشاء الهدف
              </Button>
            </form>
          </div>

          {depositForm && (
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>إضافة مبلغ للهدف #{depositForm.goalId}</h2>
              </div>
              <form className='grid gap-3 p-4' onSubmit={(e) => { void deposit(e); }}>
                <Field id='depAmt' label='المبلغ' value={depositForm.amount}
                  onChange={(v) => setDepositForm((c) => c ? { ...c, amount: v } : null)} type='number' />
                <div className='flex gap-2'>
                  <Button type='submit' className='h-9 rounded-sm flex-1'>تأكيد الإضافة</Button>
                  <Button type='button' variant='outline' className='h-9 rounded-sm'
                    onClick={() => setDepositForm(null)}>إلغاء</Button>
                </div>
              </form>
            </div>
          )}
        </div>

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>أهدافي ({goals.data?.length ?? 0})</h2>
            {goals.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
          </div>
          <div className='grid gap-3 p-4 md:grid-cols-2'>
            {(goals.data ?? []).map((g) => {
              const saved = g.savedAmount ?? g.currentAmount ?? 0;
              const deadline = g.deadline ?? g.targetDate;
              const pct = Math.min(100, Math.round((saved / Math.max(1, g.targetAmount ?? 1)) * 100));
              return (
                <div key={g.id} className='border-border rounded-sm border p-4 space-y-2'>
                  <div className='flex items-start justify-between'>
                    <div>
                      <div className='font-medium text-sm'>{g.name}</div>
                      <div className='text-xs text-muted-foreground'>{deadline ?? 'بدون تاريخ'}</div>
                    </div>
                    <div className='flex gap-1 -mt-1 -mr-1'>
                      <Button size='sm' variant='outline' className='h-7 text-xs'
                        onClick={() => setDepositForm({ goalId: g.id, amount: '' })}>
                        إضافة
                      </Button>
                      <Button size='sm' variant='ghost' className='h-7 text-destructive text-xs'
                        onClick={() => { void deleteGoal(g.id); }}>
                        <Icons.trash className='size-3' />
                      </Button>
                    </div>
                  </div>
                  <div className='flex justify-between text-xs'>
                    <span className='text-muted-foreground'>المحقق</span>
                    <span className='font-medium'>{fmt(saved, g.currency)} / {fmt(g.targetAmount, g.currency)}</span>
                  </div>
                  <div className='h-1.5 rounded-full bg-muted overflow-hidden'>
                    <div className='h-full bg-primary rounded-full transition-all' style={{ width: `${pct}%` }} />
                  </div>
                  <div className='flex justify-between text-xs text-muted-foreground'>
                    <span>{pct}%</span>
                    {g.status && <Badge variant={g.status === 'COMPLETED' ? 'default' : 'outline'} className='text-xs h-4'>{g.status}</Badge>}
                  </div>
                </div>
              );
            })}
            {!goals.loading && !goals.data?.length && (
              <p className='text-muted-foreground text-sm col-span-full'>لا توجد أهداف ادخار.</p>
            )}
          </div>
        </div>
      </div>
    </PageContainer>
  );
}

// ─── Recurring Transfers ─────────────────────────────────────────────────────

export function RecurringPage() {
  const transfers = useBackend<RecurringTransfer[]>('/recurring-transfers/my');
  const [form, setForm] = useState({ receiverId: '', amount: '', currency: 'USD', frequency: 'MONTHLY' });

  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    await apiClient('/recurring-transfers', {
      method: 'POST',
      headers: { 'X-Idempotency-Key': idempotencyKey() },
      body: JSON.stringify({
        receiverId: Number(form.receiverId),
        amount: Number(form.amount),
        currency: form.currency,
        frequency: form.frequency
      })
    });
    toast.success('تم إنشاء التحويل الدوري');
    setForm({ receiverId: '', amount: '', currency: 'USD', frequency: 'MONTHLY' });
    transfers.refetch();
  }

  async function pauseTransfer(id: number) {
    await apiClient(`/recurring-transfers/${id}/pause`, { method: 'PATCH' });
    toast.success('تم إيقاف التحويل مؤقتاً');
    transfers.refetch();
  }

  async function resumeTransfer(id: number) {
    await apiClient(`/recurring-transfers/${id}/resume`, { method: 'PATCH' });
    toast.success('تم استئناف التحويل');
    transfers.refetch();
  }

  async function deleteTransfer(id: number) {
    await apiClient(`/recurring-transfers/${id}`, { method: 'DELETE' });
    toast.success('تم حذف التحويل الدوري');
    transfers.refetch();
  }

  const freqLabel = (f?: string) => ({ DAILY: 'يومي', WEEKLY: 'أسبوعي', MONTHLY: 'شهري', YEARLY: 'سنوي' }[f ?? ''] ?? f ?? '-');

  return (
    <PageContainer
      pageTitle='التحويلات الدورية'
      pageDescription='إعداد تحويلات تلقائية متكررة وإدارتها.'
    >
      <div className='grid gap-4 xl:grid-cols-[320px_1fr]'>
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>تحويل دوري جديد</h2>
          </div>
          <form className='grid gap-3 p-4' onSubmit={(e) => { void create(e); }}>
            <Field id='rtReceiver' label='معرف المستلم' value={form.receiverId}
              onChange={(v) => setForm((c) => ({ ...c, receiverId: v }))} type='number' />
            <div className='grid grid-cols-2 gap-2'>
              <Field id='rtAmount' label='المبلغ' value={form.amount}
                onChange={(v) => setForm((c) => ({ ...c, amount: v }))} type='number' />
              <Field id='rtCur' label='العملة' value={form.currency}
                onChange={(v) => setForm((c) => ({ ...c, currency: v }))} />
            </div>
            <div className='space-y-1.5'>
              <Label className='text-xs'>التكرار</Label>
              <select value={form.frequency} onChange={(e) => setForm((c) => ({ ...c, frequency: e.target.value }))}
                className='h-9 w-full rounded-sm border border-border bg-background px-2 text-sm'>
                <option value='DAILY'>يومي</option>
                <option value='WEEKLY'>أسبوعي</option>
                <option value='MONTHLY'>شهري</option>
                <option value='YEARLY'>سنوي</option>
              </select>
            </div>
            <Button type='submit' className='h-9 rounded-sm'>إنشاء</Button>
          </form>
        </div>

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>تحويلاتي الدورية ({transfers.data?.length ?? 0})</h2>
            {transfers.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead><tr><th>#</th><th>المستلم</th><th>المبلغ</th><th>التكرار</th><th>الحالة</th><th>القادم</th><th>إجراءات</th></tr></thead>
              <tbody>
                {(transfers.data ?? []).map((t) => (
                  <tr key={t.id}>
                    <td>{t.id}</td>
                    <td>{t.receiverId}</td>
                    <td className='tabular-nums'>{fmt(t.amount, t.currency ?? '')}</td>
                    <td>{freqLabel(t.frequency)}</td>
                    <td><Badge variant={t.status === 'ACTIVE' ? 'default' : 'secondary'}>{t.status}</Badge></td>
                    <td className='text-xs'>{t.nextRunDate ?? '-'}</td>
                    <td>
                      <div className='flex gap-1'>
                        {t.status === 'ACTIVE' ? (
                          <Button size='sm' variant='outline' className='h-7 text-xs'
                            onClick={() => { void pauseTransfer(t.id); }}>إيقاف</Button>
                        ) : (
                          <Button size='sm' variant='outline' className='h-7 text-xs'
                            onClick={() => { void resumeTransfer(t.id); }}>استئناف</Button>
                        )}
                        <Button size='sm' variant='ghost' className='h-7 text-destructive text-xs'
                          onClick={() => { void deleteTransfer(t.id); }}>
                          <Icons.trash className='size-3' />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
                {!transfers.loading && !transfers.data?.length && (
                  <tr><td colSpan={7} className='text-muted-foreground text-sm'>لا توجد تحويلات دورية.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}

// ─── Split Payments ──────────────────────────────────────────────────────────

type SplitRequest = {
  id: number;
  title?: string;
  totalAmount?: number;
  currency?: string;
  status?: string;
  createdAt?: string;
  expiresAt?: string;
};

export function SplitPage() {
  const splits = useBackend<SplitRequest[]>('/split/my');
  const [form, setForm] = useState({ title: '', totalAmount: '', currency: 'USD', participantIds: '' });

  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    await apiClient<SplitRequest>('/split', {
      method: 'POST',
      headers: { 'X-Idempotency-Key': idempotencyKey() },
      body: JSON.stringify({
        title: form.title,
        totalAmount: Number(form.totalAmount),
        currency: form.currency,
        participantUserIds: form.participantIds.split(',').map((s) => Number(s.trim())).filter(Boolean)
      })
    });
    toast.success('تم إنشاء طلب التقسيم');
    setForm({ title: '', totalAmount: '', currency: 'USD', participantIds: '' });
    splits.refetch();
  }

  const statusVariant = (s?: string) => {
    if (s === 'COMPLETED') return 'default' as const;
    if (s === 'CANCELLED' || s === 'EXPIRED') return 'destructive' as const;
    if (s === 'PARTIALLY_PAID') return 'secondary' as const;
    return 'outline' as const;
  };

  const statusLabel = (s?: string) => {
    const map: Record<string, string> = {
      PENDING: 'معلق', PARTIALLY_PAID: 'مدفوع جزئياً',
      COMPLETED: 'مكتمل', EXPIRED: 'منتهي', CANCELLED: 'ملغى'
    };
    return map[s ?? ''] ?? s ?? '-';
  };

  return (
    <PageContainer
      pageTitle='تقسيم المدفوعات'
      pageDescription='تقسيم التكاليف بين مجموعة من المشاركين.'
    >
      <div className='grid gap-4 xl:grid-cols-[360px_1fr]'>
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>طلب تقسيم جديد</h2>
          </div>
          <form className='grid gap-3 p-4' onSubmit={(e) => { void create(e); }}>
            <Field id='splitTitle' label='العنوان' value={form.title}
              onChange={(v) => setForm((c) => ({ ...c, title: v }))} placeholder='مثال: عشاء جماعي' />
            <div className='grid grid-cols-2 gap-2'>
              <Field id='splitAmt' label='المبلغ الكلي' value={form.totalAmount}
                onChange={(v) => setForm((c) => ({ ...c, totalAmount: v }))} type='number' />
              <Field id='splitCur' label='العملة' value={form.currency}
                onChange={(v) => setForm((c) => ({ ...c, currency: v }))} />
            </div>
            <div className='space-y-1.5'>
              <Label htmlFor='splitParts' className='text-xs'>معرفات المشاركين (مفصولة بفاصلة)</Label>
              <Input id='splitParts' value={form.participantIds}
                onChange={(e) => setForm((c) => ({ ...c, participantIds: e.target.value }))}
                placeholder='1,2,3' className='h-9 rounded-sm' />
            </div>
            <Button type='submit' className='h-9 rounded-sm'>إنشاء التقسيم</Button>
          </form>
        </div>

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>طلبات التقسيم ({splits.data?.length ?? 0})</h2>
            {splits.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead><tr><th>#</th><th>العنوان</th><th>المبلغ</th><th>الحالة</th><th>ينتهي في</th><th>التاريخ</th></tr></thead>
              <tbody>
                {(splits.data ?? []).map((s) => (
                  <tr key={s.id}>
                    <td>{s.id}</td>
                    <td>{s.title ?? '-'}</td>
                    <td className='tabular-nums'>{fmt(s.totalAmount, s.currency ?? '')}</td>
                    <td><Badge variant={statusVariant(s.status)}>{statusLabel(s.status)}</Badge></td>
                    <td className='text-xs text-muted-foreground'>{s.expiresAt ? new Date(s.expiresAt).toLocaleDateString('ar-SY') : '-'}</td>
                    <td className='text-xs text-muted-foreground'>{s.createdAt ? new Date(s.createdAt).toLocaleDateString('ar-SY') : '-'}</td>
                  </tr>
                ))}
                {!splits.loading && !splits.data?.length && (
                  <tr><td colSpan={6} className='text-muted-foreground text-sm'>لا توجد طلبات تقسيم.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}

// ─── Escrow ──────────────────────────────────────────────────────────────────

type EscrowContractResponse = {
  id: number;
  initiatorUserId?: number;
  beneficiaryUserId?: number;
  amount?: number;
  currency?: string;
  title?: string;
  description?: string;
  conditionType?: string;
  status?: string;
  releaseDate?: string;
  fundedAt?: string;
  releasedAt?: string;
  expiresAt?: string;
  createdAt?: string;
};

type CreateEscrowForm = {
  beneficiaryUserId: string;
  amount: string;
  currency: string;
  title: string;
  description: string;
  conditionType: string;
  releaseDate: string;
};

export function EscrowPage() {
  const contracts = useBackend<EscrowContractResponse[]>('/escrow/my-contracts');
  const [createForm, setCreateForm] = useState<CreateEscrowForm>({
    beneficiaryUserId: '', amount: '', currency: 'USD',
    title: '', description: '', conditionType: 'DATE', releaseDate: ''
  });
  const [disputeForm, setDisputeForm] = useState({ contractId: '', reason: '' });
  const [showCreate, setShowCreate] = useState(false);

  async function createContract(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    await apiClient('/escrow', {
      method: 'POST',
      headers: { 'X-Idempotency-Key': idempotencyKey() },
      body: JSON.stringify({
        beneficiaryUserId: Number(createForm.beneficiaryUserId),
        amount: Number(createForm.amount),
        currency: createForm.currency,
        title: createForm.title,
        description: createForm.description,
        conditionType: createForm.conditionType,
        releaseDate: createForm.releaseDate || undefined
      })
    });
    toast.success('تم إنشاء عقد الضمان');
    setCreateForm({ beneficiaryUserId: '', amount: '', currency: 'USD', title: '', description: '', conditionType: 'DATE', releaseDate: '' });
    setShowCreate(false);
    contracts.refetch();
  }

  async function fundContract(id: number) {
    await apiClient(`/escrow/${id}/fund`, {
      method: 'POST',
      headers: { 'X-Idempotency-Key': idempotencyKey() }
    });
    toast.success('تم تمويل عقد الضمان');
    contracts.refetch();
  }

  async function releaseContract(id: number) {
    await apiClient(`/escrow/${id}/release`, {
      method: 'POST',
      headers: { 'X-Idempotency-Key': idempotencyKey() }
    });
    toast.success('تم الإفراج عن مبلغ الضمان');
    contracts.refetch();
  }

  async function submitDispute(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    await apiClient(`/escrow/${disputeForm.contractId}/dispute`, {
      method: 'POST',
      headers: { 'X-Idempotency-Key': idempotencyKey() },
      body: JSON.stringify({ reasonCategory: disputeForm.reason })
    });
    toast.success('تم فتح نزاع على العقد');
    setDisputeForm({ contractId: '', reason: '' });
    contracts.refetch();
  }

  const statusVariant = (s?: string) => {
    if (s === 'FUNDED') return 'secondary' as const;
    if (s === 'RELEASED') return 'default' as const;
    if (s === 'DISPUTED') return 'destructive' as const;
    return 'outline' as const;
  };

  const statusLabel = (s?: string) => {
    const map: Record<string, string> = {
      PENDING: 'معلق', FUNDED: 'ممول', RELEASED: 'مُفرج عنه',
      DISPUTED: 'متنازع عليه', EXPIRED: 'منتهي', CANCELLED: 'ملغى'
    };
    return map[s ?? ''] ?? s ?? '-';
  };

  return (
    <PageContainer
      pageTitle='عقود الضمان'
      pageDescription='إدارة عقود الضمان المالي (Escrow) وتسوية النزاعات.'
    >
      <div className='grid gap-4 xl:grid-cols-[380px_1fr]'>
        <div className='space-y-4'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>إجراءات العقد</h2>
              <Button size='sm' className='h-7 text-xs' onClick={() => setShowCreate(!showCreate)}>
                {showCreate ? 'إلغاء' : 'عقد جديد'}
              </Button>
            </div>

            {showCreate && (
              <form className='grid gap-3 p-4 border-b border-border' onSubmit={(e) => { void createContract(e); }}>
                <div className='text-xs font-medium text-muted-foreground mb-1'>إنشاء عقد ضمان جديد</div>
                <Field id='escBenef' label='معرف المستفيد (البائع)' value={createForm.beneficiaryUserId}
                  onChange={(v) => setCreateForm((c) => ({ ...c, beneficiaryUserId: v }))} type='number' />
                <Field id='escTitle' label='عنوان العقد' value={createForm.title}
                  onChange={(v) => setCreateForm((c) => ({ ...c, title: v }))} placeholder='مثال: شراء عقار' />
                <div className='grid grid-cols-2 gap-2'>
                  <Field id='escAmt' label='المبلغ' value={createForm.amount}
                    onChange={(v) => setCreateForm((c) => ({ ...c, amount: v }))} type='number' />
                  <Field id='escCur' label='العملة' value={createForm.currency}
                    onChange={(v) => setCreateForm((c) => ({ ...c, currency: v }))} />
                </div>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>نوع الشرط</Label>
                  <select value={createForm.conditionType} onChange={(e) => setCreateForm((c) => ({ ...c, conditionType: e.target.value }))}
                    className='h-9 w-full rounded-sm border border-border bg-background px-2 text-sm'>
                    <option value='DATE'>تاريخ الإفراج</option>
                    <option value='DOCUMENT'>شرط وثيقة</option>
                    <option value='MANUAL'>يدوي</option>
                  </select>
                </div>
                {createForm.conditionType === 'DATE' && (
                  <Field id='escDate' label='تاريخ الإفراج' value={createForm.releaseDate}
                    onChange={(v) => setCreateForm((c) => ({ ...c, releaseDate: v }))} type='date' />
                )}
                <div className='space-y-1.5'>
                  <Label className='text-xs'>الوصف</Label>
                  <Textarea value={createForm.description}
                    onChange={(e) => setCreateForm((c) => ({ ...c, description: e.target.value }))}
                    className='min-h-16 rounded-sm text-sm' />
                </div>
                <Button type='submit' className='h-9 rounded-sm'>إنشاء العقد</Button>
              </form>
            )}

            <div className='grid gap-3 p-4'>
              <div className='text-xs font-medium text-muted-foreground mb-1'>فتح نزاع على عقد</div>
              <form className='grid gap-3' onSubmit={(e) => { void submitDispute(e); }}>
                <Field id='escrowDisputeId' label='معرف العقد' value={disputeForm.contractId}
                  onChange={(v) => setDisputeForm((c) => ({ ...c, contractId: v }))} type='number' />
                <div className='space-y-1.5'>
                  <Label className='text-xs'>سبب النزاع</Label>
                  <Textarea value={disputeForm.reason}
                    onChange={(e) => setDisputeForm((c) => ({ ...c, reason: e.target.value }))}
                    className='min-h-16 rounded-sm text-sm' />
                </div>
                <Button type='submit' variant='destructive' className='h-9 rounded-sm'>فتح نزاع</Button>
              </form>
            </div>
          </div>
        </div>

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>عقودي ({contracts.data?.length ?? 0})</h2>
            {contracts.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>#</th><th>العنوان</th><th>المُبادر</th><th>المستفيد</th>
                  <th>المبلغ</th><th>الحالة</th><th>الإفراج</th><th>إجراء</th>
                </tr>
              </thead>
              <tbody>
                {(contracts.data ?? []).map((c) => (
                  <tr key={c.id}>
                    <td>{c.id}</td>
                    <td className='max-w-32 truncate'>{c.title ?? '-'}</td>
                    <td>{c.initiatorUserId}</td>
                    <td>{c.beneficiaryUserId}</td>
                    <td className='tabular-nums'>{fmt(c.amount, c.currency ?? '')}</td>
                    <td><Badge variant={statusVariant(c.status)}>{statusLabel(c.status)}</Badge></td>
                    <td className='text-xs'>{c.releaseDate ?? '-'}</td>
                    <td>
                      <div className='flex gap-1'>
                        {c.status === 'PENDING' && (
                          <Button size='sm' variant='outline' className='h-7 text-xs'
                            onClick={() => { void fundContract(c.id); }}>
                            تمويل
                          </Button>
                        )}
                        {c.status === 'FUNDED' && (
                          <Button size='sm' variant='outline' className='h-7 text-xs'
                            onClick={() => { void releaseContract(c.id); }}>
                            إفراج
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
                {!contracts.loading && !contracts.data?.length && (
                  <tr><td colSpan={8} className='text-muted-foreground text-sm'>لا توجد عقود.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}

// ─── Payout Reservations ─────────────────────────────────────────────────────

export function PayoutReservationsPage() {
  const reservations = useBackend<PayoutReservation[]>('/payout-reservations/mine');
  const [form, setForm] = useState({ amount: '', currency: 'USD', branchId: '1' });

  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    await apiClient('/payout-reservations', {
      method: 'POST',
      headers: { 'X-Idempotency-Key': idempotencyKey() },
      body: JSON.stringify({
        amount: Number(form.amount),
        currency: form.currency,
        branchId: Number(form.branchId)
      })
    });
    toast.success('تم إنشاء حجز الدفع');
    setForm({ amount: '', currency: 'USD', branchId: '1' });
    reservations.refetch();
  }

  async function cancelReservation(id: number) {
    await apiClient(`/payout-reservations/${id}/cancel`, { method: 'POST' });
    toast.success('تم إلغاء الحجز');
    reservations.refetch();
  }

  const statusVariant = (s?: string) => {
    if (s === 'ACTIVE') return 'default' as const;
    if (s === 'CANCELLED') return 'destructive' as const;
    if (s === 'USED') return 'secondary' as const;
    return 'outline' as const;
  };

  const statusLabel = (s?: string) => {
    const map: Record<string, string> = { ACTIVE: 'نشط', CANCELLED: 'ملغى', USED: 'مستخدم', EXPIRED: 'منتهي' };
    return map[s ?? ''] ?? s ?? '-';
  };

  return (
    <PageContainer
      pageTitle='حجوزات الدفع النقدي'
      pageDescription='حجز مبالغ للسحب النقدي من الفروع.'
    >
      <div className='grid gap-4 xl:grid-cols-[320px_1fr]'>
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>حجز جديد</h2>
          </div>
          <form className='grid gap-3 p-4' onSubmit={(e) => { void create(e); }}>
            <Field id='prAmt' label='المبلغ' value={form.amount}
              onChange={(v) => setForm((c) => ({ ...c, amount: v }))} type='number' />
            <div className='grid grid-cols-2 gap-2'>
              <Field id='prCur' label='العملة' value={form.currency}
                onChange={(v) => setForm((c) => ({ ...c, currency: v }))} />
              <Field id='prBranch' label='الفرع' value={form.branchId}
                onChange={(v) => setForm((c) => ({ ...c, branchId: v }))} type='number' />
            </div>
            <Button type='submit' className='h-9 rounded-sm'>إنشاء الحجز</Button>
          </form>

          <div className='px-4 pb-4 text-xs text-muted-foreground space-y-1'>
            <p>• يمكن تقديم الحجز مسبقاً لضمان توفر السيولة في الفرع.</p>
            <p>• تنتهي صلاحية الحجز تلقائياً إذا لم يُستخدم خلال المدة المحددة.</p>
          </div>
        </div>

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>حجوزاتي ({reservations.data?.length ?? 0})</h2>
            {reservations.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead><tr><th>#</th><th>المبلغ</th><th>العملة</th><th>الفرع</th><th>الحالة</th><th>ينتهي في</th><th>إجراء</th></tr></thead>
              <tbody>
                {(reservations.data ?? []).map((r) => (
                  <tr key={r.id}>
                    <td>{r.id}</td>
                    <td className='tabular-nums font-medium'>{fmt(r.amount)}</td>
                    <td>{r.currency}</td>
                    <td>{r.branchId}</td>
                    <td><Badge variant={statusVariant(r.status)}>{statusLabel(r.status)}</Badge></td>
                    <td className='text-xs text-muted-foreground'>
                      {r.reservedUntil ? new Date(r.reservedUntil).toLocaleString('ar-SY') : '-'}
                    </td>
                    <td>
                      {r.status === 'ACTIVE' && (
                        <Button size='sm' variant='destructive' className='h-7 text-xs'
                          onClick={() => { void cancelReservation(r.id); }}>
                          إلغاء
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
                {!reservations.loading && !reservations.data?.length && (
                  <tr><td colSpan={7} className='text-muted-foreground text-sm'>لا توجد حجوزات.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
