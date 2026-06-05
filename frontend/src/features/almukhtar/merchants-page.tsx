'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { apiClient } from '@/lib/api-client';
import { FormEvent, useState, useCallback } from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import type { Merchant } from './types';

function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

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
      <Input
        id={id} type={type} value={value} placeholder={placeholder} required={required}
        onChange={(e) => onChange(e.target.value)} className='h-9 rounded-sm'
      />
    </div>
  );
}

type MyMerchant = {
  id: number;
  businessName?: string;
  name?: string;
  category?: string;
  businessType?: string;
  registrationNumber?: string;
  address?: string;
  city?: string;
  country?: string;
  status?: string;
  qrCodeData?: string;
};

type Settlement = {
  id: number;
  merchantId?: number;
  amount?: number;
  currency?: string;
  status?: string;
  createdAt?: string;
  processedAt?: string;
};

type MerchantStats = {
  total?: number;
  pending?: number;
  approved?: number;
  suspended?: number;
  rejected?: number;
};

const CATEGORIES = [
  { value: 'RETAIL', label: 'تجزئة' },
  { value: 'FOOD', label: 'مطاعم وأغذية' },
  { value: 'SERVICES', label: 'خدمات' },
  { value: 'ONLINE', label: 'تجارة إلكترونية' },
  { value: 'HEALTHCARE', label: 'رعاية صحية' },
  { value: 'EDUCATION', label: 'تعليم' },
  { value: 'REAL_ESTATE', label: 'عقارات' },
  { value: 'OTHER', label: 'أخرى' },
];

const STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  APPROVED: 'default',
  PENDING: 'secondary',
  REJECTED: 'destructive',
  SUSPENDED: 'outline',
};

const STATUS_LABEL: Record<string, string> = {
  APPROVED: 'موافق عليه',
  PENDING: 'قيد المراجعة',
  REJECTED: 'مرفوض',
  SUSPENDED: 'موقوف',
  ACTIVE: 'نشط',
};

const EMPTY_REG = {
  businessName: '', category: 'RETAIL', registrationNumber: '',
  address: '', city: '', country: 'SY', branchId: '',
};

const EMPTY_PAY = { merchantId: '', amount: '', currency: 'USD', description: '' };

export function MerchantsPage() {
  const [tab, setTab] = useState<'my' | 'admin' | 'settlements'>('my');
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);

  const myMerchant = useBackend<MyMerchant>(`/merchants/my-merchant?_t=${tick}`);
  const myQrData = useBackend<string>(`/merchants/my-merchant/qr?_t=${tick}`);

  const [adminStatus, setAdminStatus] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const adminMerchants = useBackend<Merchant[]>(
    `/merchants/admin${adminStatus ? `?status=${adminStatus}` : ''}${adminStatus ? '&' : '?'}_t=${tick}`
  );

  const pendingSettlements = useBackend<Settlement[]>(`/merchants/admin/settlements/pending?_t=${tick}`);
  const allSettlements = useBackend<Settlement[]>(`/merchants/admin/settlements?_t=${tick}`);

  const [registerForm, setRegisterForm] = useState(EMPTY_REG);
  const [registering, setRegistering] = useState(false);
  const [payForm, setPayForm] = useState(EMPTY_PAY);
  const [paying, setPaying] = useState(false);
  const [rejectNote, setRejectNote] = useState<{ id: number; note: string } | null>(null);
  const [settleTab, setSettleTab] = useState<'pending' | 'all'>('pending');

  const setReg = useCallback(
    <K extends keyof typeof EMPTY_REG>(k: K, v: string) =>
      setRegisterForm((c) => ({ ...c, [k]: v })),
    []
  );

  const filteredMerchants = (adminMerchants.data ?? []).filter((m) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      (m.businessName ?? m.name ?? '').toLowerCase().includes(q) ||
      String(m.id).includes(q) ||
      (m.city ?? '').toLowerCase().includes(q)
    );
  });

  // ── compute stats from admin list
  const stats: MerchantStats = {
    total: adminMerchants.data?.length ?? 0,
    pending: adminMerchants.data?.filter((m) => m.status === 'PENDING').length ?? 0,
    approved: adminMerchants.data?.filter((m) => m.status === 'APPROVED').length ?? 0,
    suspended: adminMerchants.data?.filter((m) => m.status === 'SUSPENDED').length ?? 0,
    rejected: adminMerchants.data?.filter((m) => m.status === 'REJECTED').length ?? 0,
  };

  async function registerMerchant(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!registerForm.businessName.trim()) { toast.error('اسم المتجر مطلوب'); return; }
    setRegistering(true);
    try {
      await apiClient('/merchants/register', {
        method: 'POST',
        body: JSON.stringify({
          businessName: registerForm.businessName,
          category: registerForm.category || undefined,
          registrationNumber: registerForm.registrationNumber || undefined,
          address: registerForm.address || undefined,
          city: registerForm.city || undefined,
          country: registerForm.country || undefined,
          branchId: registerForm.branchId ? Number(registerForm.branchId) : undefined,
        })
      });
      toast.success('تم تقديم طلب تسجيل التاجر بنجاح');
      setRegisterForm(EMPTY_REG);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التسجيل');
    } finally {
      setRegistering(false);
    }
  }

  async function payMerchant(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!payForm.merchantId || !payForm.amount) { toast.error('يرجى إدخال معرف التاجر والمبلغ'); return; }
    setPaying(true);
    try {
      await apiClient('/merchants/pay', {
        method: 'POST',
        headers: { 'X-Idempotency-Key': idempotencyKey() },
        body: JSON.stringify({
          merchantId: Number(payForm.merchantId),
          amount: Number(payForm.amount),
          currency: payForm.currency,
          description: payForm.description || undefined,
        })
      });
      toast.success('تم تنفيذ الدفع للتاجر بنجاح');
      setPayForm(EMPTY_PAY);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الدفع');
    } finally {
      setPaying(false);
    }
  }

  async function approveMerchant(id: number) {
    try {
      await apiClient(`/merchants/admin/${id}/approve`, { method: 'PUT' });
      toast.success('تمت الموافقة على التاجر');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإجراء');
    }
  }

  async function rejectMerchant(id: number, note: string) {
    try {
      await apiClient(`/merchants/admin/${id}/reject`, {
        method: 'PUT',
        body: JSON.stringify({ note: note || undefined }),
      });
      toast.success('تم رفض طلب التاجر');
      setRejectNote(null);
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الرفض');
    }
  }

  async function suspendMerchant(id: number) {
    try {
      await apiClient(`/merchants/admin/${id}/suspend`, { method: 'PUT' });
      toast.success('تم إيقاف التاجر');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإيقاف');
    }
  }

  async function reactivateMerchant(id: number) {
    try {
      await apiClient(`/merchants/admin/${id}/reactivate`, { method: 'PUT' });
      toast.success('تم إعادة تفعيل التاجر');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إعادة التفعيل');
    }
  }

  async function processSettlement(id: number) {
    try {
      await apiClient(`/merchants/admin/settlements/${id}/process`, {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey() },
      });
      toast.success('تمت معالجة التسوية بنجاح');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل معالجة التسوية');
    }
  }

  const displayName = (m: MyMerchant | Merchant) =>
    (m as MyMerchant).businessName ?? m.name ?? '-';

  const displayCategory = (m: MyMerchant | Merchant) =>
    (m as MyMerchant).category ?? (m as Merchant).businessType ?? '-';

  const settlementsData = settleTab === 'pending'
    ? (pendingSettlements.data ?? [])
    : (allSettlements.data ?? []);

  return (
    <PageContainer
      pageTitle='التجار'
      pageDescription='تسجيل التجار، الدفع بالـ QR، إدارة الموافقات والتسويات.'
    >
      <div className='space-y-4'>
        {/* Tab Bar */}
        <div className='flex gap-2 flex-wrap'>
          {(['my', 'admin', 'settlements'] as const).map((t) => (
            <Button key={t} size='sm' variant={tab === t ? 'default' : 'outline'}
              className='h-8 text-xs' onClick={() => setTab(t)}>
              {t === 'my' ? 'تاجري' : t === 'admin' ? 'إدارة التجار' : 'التسويات'}
            </Button>
          ))}
          <Button size='sm' variant='ghost' className='h-8 text-xs mr-auto' onClick={refresh}>
            <Icons.spinner className='ml-1.5 size-3.5' />
            تحديث
          </Button>
        </div>

        {/* ── My Merchant Tab ── */}
        {tab === 'my' && (
          <div className='grid gap-4 xl:grid-cols-2'>
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>بياناتي كتاجر</h2>
              </div>
              <div className='p-4'>
                {myMerchant.loading && (
                  <div className='flex justify-center py-6'>
                    <Icons.spinner className='size-5 animate-spin text-muted-foreground' />
                  </div>
                )}
                {!myMerchant.loading && myMerchant.data ? (
                  <div className='space-y-3 text-sm'>
                    <div className='flex justify-between'>
                      <span className='text-muted-foreground'>اسم المتجر</span>
                      <span className='font-medium'>{displayName(myMerchant.data)}</span>
                    </div>
                    <div className='flex justify-between'>
                      <span className='text-muted-foreground'>الفئة</span>
                      <span>{displayCategory(myMerchant.data)}</span>
                    </div>
                    {myMerchant.data.city && (
                      <div className='flex justify-between'>
                        <span className='text-muted-foreground'>المدينة</span>
                        <span>{myMerchant.data.city}</span>
                      </div>
                    )}
                    {myMerchant.data.registrationNumber && (
                      <div className='flex justify-between'>
                        <span className='text-muted-foreground'>رقم السجل التجاري</span>
                        <span className='font-mono text-xs'>{myMerchant.data.registrationNumber}</span>
                      </div>
                    )}
                    <div className='flex justify-between items-center'>
                      <span className='text-muted-foreground'>الحالة</span>
                      <Badge variant={STATUS_VARIANT[myMerchant.data.status ?? ''] ?? 'outline'}>
                        {STATUS_LABEL[myMerchant.data.status ?? ''] ?? myMerchant.data.status ?? '-'}
                      </Badge>
                    </div>
                    {(myMerchant.data.qrCodeData ?? myQrData.data) && (
                      <div>
                        <div className='text-muted-foreground text-xs mb-1'>بيانات QR التاجر</div>
                        <pre className='bg-muted/60 rounded-sm p-2 text-xs break-all max-h-20 overflow-auto'>
                          {myMerchant.data.qrCodeData ?? myQrData.data}
                        </pre>
                        <Button
                          size='sm'
                          variant='outline'
                          className='mt-2 h-7 text-xs w-full rounded-sm'
                          onClick={() => {
                            const val = myMerchant.data?.qrCodeData ?? myQrData.data;
                            if (val) { void navigator.clipboard.writeText(val); toast.success('تم نسخ بيانات QR'); }
                          }}
                        >
                          <Icons.copy className='ml-1.5 size-3.5' />
                          نسخ بيانات QR
                        </Button>
                      </div>
                    )}
                  </div>
                ) : !myMerchant.loading && (
                  <div>
                    <p className='text-sm text-muted-foreground mb-4'>
                      لم يتم تسجيل حساب تاجر بعد. أكمل النموذج أدناه.
                    </p>
                    <form className='grid gap-3' onSubmit={(e) => { void registerMerchant(e); }}>
                      <Field id='mchBizName' label='اسم المتجر' required value={registerForm.businessName}
                        onChange={(v) => setReg('businessName', v)} placeholder='محل الأمل للمواد الغذائية' />
                      <div className='space-y-1.5'>
                        <Label className='text-xs'>الفئة</Label>
                        <select value={registerForm.category} onChange={(e) => setReg('category', e.target.value)}
                          className='h-9 w-full rounded-sm border border-border bg-background px-2 text-sm'>
                          {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
                        </select>
                      </div>
                      <Field id='mchRegNo' label='رقم السجل التجاري' value={registerForm.registrationNumber}
                        onChange={(v) => setReg('registrationNumber', v)} placeholder='اختياري' />
                      <div className='grid grid-cols-2 gap-2'>
                        <Field id='mchCity' label='المدينة' value={registerForm.city}
                          onChange={(v) => setReg('city', v)} placeholder='دمشق' />
                        <Field id='mchCountry' label='الدولة' value={registerForm.country}
                          onChange={(v) => setReg('country', v)} placeholder='SY' />
                      </div>
                      <Field id='mchAddr' label='العنوان' value={registerForm.address}
                        onChange={(v) => setReg('address', v)} placeholder='الشارع، الحي...' />
                      <Field id='mchBranch' label='معرف الفرع' value={registerForm.branchId}
                        onChange={(v) => setReg('branchId', v)} type='number' placeholder='اختياري' />
                      <Button type='submit' className='h-9 rounded-sm' disabled={registering}>
                        {registering && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                        تقديم طلب التسجيل
                      </Button>
                    </form>
                  </div>
                )}
              </div>
            </div>

            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>الدفع لتاجر</h2>
                <p className='text-muted-foreground text-xs'>ادفع مباشرةً لتاجر مسجّل في المنصة</p>
              </div>
              <form className='grid gap-3 p-4' onSubmit={(e) => { void payMerchant(e); }}>
                <Field id='payMchId' label='معرف التاجر' required value={payForm.merchantId}
                  onChange={(v) => setPayForm((c) => ({ ...c, merchantId: v }))} type='number'
                  placeholder='مثال: 5' />
                <div className='grid grid-cols-2 gap-2'>
                  <Field id='payMchAmt' label='المبلغ' required value={payForm.amount}
                    onChange={(v) => setPayForm((c) => ({ ...c, amount: v }))} type='number'
                    placeholder='0.00' />
                  <div className='space-y-1.5'>
                    <Label className='text-xs'>العملة</Label>
                    <select value={payForm.currency}
                      onChange={(e) => setPayForm((c) => ({ ...c, currency: e.target.value }))}
                      className='h-9 w-full rounded-sm border border-border bg-background px-2 text-sm'>
                      <option value='USD'>USD</option>
                      <option value='SYP'>SYP</option>
                      <option value='EUR'>EUR</option>
                    </select>
                  </div>
                </div>
                <Field id='payMchDesc' label='ملاحظة (اختياري)' value={payForm.description}
                  onChange={(v) => setPayForm((c) => ({ ...c, description: v }))}
                  placeholder='وصف الدفعة...' />
                <Button type='submit' className='h-9 rounded-sm' disabled={paying}>
                  {paying
                    ? <Icons.spinner className='ml-2 size-4 animate-spin' />
                    : <Icons.send className='ml-2 size-4' />}
                  دفع للتاجر
                </Button>
              </form>
            </div>
          </div>
        )}

        {/* ── Admin Tab ── */}
        {tab === 'admin' && (
          <div className='space-y-4'>
            {/* Stats */}
            {adminMerchants.data && (
              <div className='grid grid-cols-2 md:grid-cols-5 gap-3'>
                {[
                  { label: 'الإجمالي', value: stats.total, color: '' },
                  { label: 'قيد المراجعة', value: stats.pending, color: 'text-amber-600' },
                  { label: 'موافق عليه', value: stats.approved, color: 'text-emerald-600' },
                  { label: 'موقوف', value: stats.suspended, color: 'text-muted-foreground' },
                  { label: 'مرفوض', value: stats.rejected, color: 'text-destructive' },
                ].map((s) => (
                  <div key={s.label} className='gov-panel rounded-md p-3 text-center'>
                    <div className={`text-xl font-bold tabular-nums ${s.color}`}>{s.value}</div>
                    <div className='text-xs text-muted-foreground mt-0.5'>{s.label}</div>
                  </div>
                ))}
              </div>
            )}

            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                <div>
                  <h2 className='text-sm font-semibold'>التجار — إدارة</h2>
                  {filteredMerchants.length > 0 && (
                    <p className='text-muted-foreground text-xs'>{filteredMerchants.length} تاجر</p>
                  )}
                </div>
                <div className='flex items-center gap-2 flex-wrap'>
                  <Input
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder='بحث باسم أو مدينة...'
                    className='h-8 w-44 rounded-sm text-xs'
                  />
                  <select value={adminStatus}
                    onChange={(e) => { setAdminStatus(e.target.value); refresh(); }}
                    className='h-8 rounded-sm border border-border bg-background px-2 text-xs'>
                    <option value=''>الكل</option>
                    <option value='PENDING'>قيد المراجعة</option>
                    <option value='APPROVED'>موافق عليه</option>
                    <option value='REJECTED'>مرفوض</option>
                    <option value='SUSPENDED'>موقوف</option>
                  </select>
                  {adminMerchants.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
                </div>
              </div>
              <div className='overflow-x-auto'>
                <table className='gov-table'>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>اسم المتجر</th>
                      <th>الفئة</th>
                      <th>المستخدم</th>
                      <th>المدينة</th>
                      <th>الحالة</th>
                      <th>التاريخ</th>
                      <th>إجراءات</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredMerchants.map((m) => (
                      <tr key={m.id}>
                        <td className='text-muted-foreground'>{m.id}</td>
                        <td className='font-medium'>{m.businessName ?? m.name ?? '-'}</td>
                        <td className='text-xs'>{m.category ?? m.businessType ?? '-'}</td>
                        <td className='text-xs text-muted-foreground'>{m.userId ?? '-'}</td>
                        <td className='text-xs'>{m.city ?? '-'}</td>
                        <td>
                          <Badge variant={STATUS_VARIANT[m.status ?? ''] ?? 'outline'}>
                            {STATUS_LABEL[m.status ?? ''] ?? m.status ?? '-'}
                          </Badge>
                        </td>
                        <td className='text-xs text-muted-foreground'>{m.createdAt ?? '-'}</td>
                        <td>
                          <div className='flex items-center gap-1 flex-wrap'>
                            {m.status === 'PENDING' && (
                              <>
                                <Button size='sm' variant='outline'
                                  className='h-7 text-xs text-emerald-600 border-emerald-200'
                                  onClick={() => { void approveMerchant(m.id); }}>
                                  موافقة
                                </Button>
                                <Button size='sm' variant='outline'
                                  className='h-7 text-xs text-destructive border-destructive/30'
                                  onClick={() => setRejectNote({ id: m.id, note: '' })}>
                                  رفض
                                </Button>
                              </>
                            )}
                            {m.status === 'APPROVED' && (
                              <Button size='sm' variant='outline' className='h-7 text-xs text-amber-600 border-amber-200'
                                onClick={() => { void suspendMerchant(m.id); }}>
                                إيقاف
                              </Button>
                            )}
                            {m.status === 'SUSPENDED' && (
                              <Button size='sm' variant='outline' className='h-7 text-xs text-emerald-600 border-emerald-200'
                                onClick={() => { void reactivateMerchant(m.id); }}>
                                إعادة تفعيل
                              </Button>
                            )}
                            {m.status === 'REJECTED' && (
                              <Button size='sm' variant='outline' className='h-7 text-xs'
                                onClick={() => { void approveMerchant(m.id); }}>
                                إعادة فتح
                              </Button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                    {!adminMerchants.loading && !filteredMerchants.length && (
                      <tr>
                        <td colSpan={8} className='py-8 text-center text-muted-foreground text-sm'>
                          لا يوجد تجار{adminStatus ? ` بحالة ${STATUS_LABEL[adminStatus] ?? adminStatus}` : ''}.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Reject note dialog (inline) */}
            {rejectNote && (
              <div className='gov-panel rounded-md border-destructive/20 bg-destructive/5'>
                <div className='gov-panel-header rounded-t-md px-4 py-3 border-destructive/20'>
                  <h2 className='text-sm font-semibold text-destructive'>رفض التاجر #{rejectNote.id}</h2>
                </div>
                <div className='p-4 space-y-3'>
                  <div className='space-y-1.5'>
                    <Label className='text-xs'>سبب الرفض (اختياري)</Label>
                    <Input
                      value={rejectNote.note}
                      onChange={(e) => setRejectNote((r) => r ? { ...r, note: e.target.value } : r)}
                      placeholder='أدخل سبب الرفض...'
                      className='h-9 rounded-sm'
                    />
                  </div>
                  <div className='flex gap-2'>
                    <Button size='sm' variant='destructive' className='h-8 text-xs rounded-sm'
                      onClick={() => { void rejectMerchant(rejectNote.id, rejectNote.note); }}>
                      تأكيد الرفض
                    </Button>
                    <Button size='sm' variant='outline' className='h-8 text-xs rounded-sm'
                      onClick={() => setRejectNote(null)}>
                      إلغاء
                    </Button>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ── Settlements Tab ── */}
        {tab === 'settlements' && (
          <div className='space-y-4'>
            <div className='flex gap-2'>
              {(['pending', 'all'] as const).map((t) => (
                <Button key={t} size='sm' variant={settleTab === t ? 'default' : 'outline'}
                  className='h-8 text-xs' onClick={() => setSettleTab(t)}>
                  {t === 'pending' ? 'المعلقة' : 'جميع التسويات'}
                </Button>
              ))}
            </div>

            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>
                  التسويات {settleTab === 'pending' ? 'المعلقة' : 'جميعها'}
                </h2>
                {(pendingSettlements.loading || allSettlements.loading) && (
                  <Icons.spinner className='size-4 animate-spin' />
                )}
              </div>
              <div className='overflow-x-auto'>
                <table className='gov-table'>
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>معرف التاجر</th>
                      <th>المبلغ</th>
                      <th>العملة</th>
                      <th>الحالة</th>
                      <th>تاريخ الطلب</th>
                      <th>تاريخ المعالجة</th>
                      <th>إجراء</th>
                    </tr>
                  </thead>
                  <tbody>
                    {settlementsData.map((s) => (
                      <tr key={s.id}>
                        <td className='text-muted-foreground'>{s.id}</td>
                        <td>{s.merchantId ?? '-'}</td>
                        <td className='tabular-nums font-medium'>{fmt(s.amount, s.currency)}</td>
                        <td>{s.currency ?? '-'}</td>
                        <td>
                          <Badge variant={s.status === 'PROCESSED' ? 'default' : 'secondary'}>
                            {s.status === 'PROCESSED' ? 'تمت المعالجة'
                              : s.status === 'PENDING' ? 'معلقة' : s.status ?? '-'}
                          </Badge>
                        </td>
                        <td className='text-xs text-muted-foreground'>{s.createdAt ?? '-'}</td>
                        <td className='text-xs text-muted-foreground'>{s.processedAt ?? '-'}</td>
                        <td>
                          {s.status !== 'PROCESSED' && (
                            <Button size='sm' className='h-7 text-xs'
                              onClick={() => { void processSettlement(s.id); }}>
                              معالجة
                            </Button>
                          )}
                        </td>
                      </tr>
                    ))}
                    {settlementsData.length === 0 && (
                      <tr>
                        <td colSpan={8} className='py-8 text-center text-muted-foreground text-sm'>
                          لا توجد تسويات.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>
    </PageContainer>
  );
}
