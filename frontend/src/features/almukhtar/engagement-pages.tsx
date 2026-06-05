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
import type { FamilyGroup, Campaign } from './types';

function Field({ id, label, value, onChange, type = 'text', placeholder }: {
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

// ─── Gamification ─────────────────────────────────────────────────────────────

type TrustScoreResponse = {
  userId?: number;
  score?: number;
  tier?: string;
  feeDiscountPct?: number;
};

type BadgeResponse = {
  id: number;
  badgeType?: string;
  awardedAt?: string;
};

type LeaderboardEntry = {
  userId?: number;
  score?: number;
  tier?: string;
};

const tierLabel = (tier?: string) => {
  const map: Record<string, string> = { BRONZE: 'برونزي', SILVER: 'فضي', GOLD: 'ذهبي', PLATINUM: 'بلاتيني' };
  return map[tier ?? ''] ?? tier ?? '-';
};

const badgeLabel = (type?: string) => {
  const map: Record<string, string> = {
    FIRST_TRANSFER: 'أول تحويل',
    LOYAL_USER: 'مستخدم وفي',
    HIGH_VALUE: 'مرسل متميز',
    REFERRER: 'مُحيل نشط',
    VERIFIED: 'موثق',
  };
  return map[type ?? ''] ?? type ?? '-';
};

export function GamificationPage() {
  const score = useBackend<TrustScoreResponse>('/gamification/my-score');
  const badges = useBackend<BadgeResponse[]>('/gamification/my-badges');
  const leaderboard = useBackend<{ content: LeaderboardEntry[] } | LeaderboardEntry[]>('/gamification/leaderboard?size=10');

  const leaderboardList = Array.isArray(leaderboard.data)
    ? leaderboard.data
    : (leaderboard.data as { content?: LeaderboardEntry[] } | undefined)?.content ?? [];

  return (
    <PageContainer
      pageTitle='نقاط الولاء والشارات'
      pageDescription='متابعة نقاطك، شاراتك المكتسبة، وترتيبك في لوحة المتصدرين.'
    >
      <div className='space-y-4'>
        {score.data && (
          <div className='grid gap-3 md:grid-cols-3'>
            <div className='gov-panel rounded-md p-4 text-center'>
              <div className='text-3xl font-bold tabular-nums'>{score.data.score ?? 0}</div>
              <div className='text-xs text-muted-foreground mt-1'>مجموع النقاط</div>
            </div>
            <div className='gov-panel rounded-md p-4 text-center'>
              <div className='text-2xl font-bold'>{tierLabel(score.data.tier)}</div>
              <div className='text-xs text-muted-foreground mt-1'>المستوى الحالي</div>
            </div>
            <div className='gov-panel rounded-md p-4 text-center'>
              <div className='text-2xl font-bold tabular-nums'>
                {score.data.feeDiscountPct ? `${score.data.feeDiscountPct}%` : '0%'}
              </div>
              <div className='text-xs text-muted-foreground mt-1'>خصم الرسوم</div>
            </div>
          </div>
        )}
        {score.loading && (
          <div className='flex justify-center py-8'>
            <Icons.spinner className='size-6 animate-spin text-muted-foreground' />
          </div>
        )}

        <div className='grid gap-4 xl:grid-cols-2'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>شاراتي</h2>
              <p className='text-muted-foreground text-xs'>{badges.data?.length ?? 0} شارة</p>
            </div>
            <div className='grid grid-cols-2 gap-3 p-4'>
              {(badges.data ?? []).map((b) => (
                <div key={b.id} className='border-border rounded-sm border p-3 text-center space-y-1'>
                  <div className='text-2xl'>🏅</div>
                  <div className='text-xs font-medium'>{badgeLabel(b.badgeType)}</div>
                  <div className='text-xs text-muted-foreground'>
                    {b.awardedAt ? new Date(b.awardedAt).toLocaleDateString('ar-SY') : ''}
                  </div>
                </div>
              ))}
              {!badges.loading && !badges.data?.length && (
                <p className='text-muted-foreground text-sm col-span-full'>لا توجد شارات بعد.</p>
              )}
            </div>
          </div>

          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>لوحة المتصدرين</h2>
            </div>
            <div className='overflow-x-auto'>
              <table className='gov-table'>
                <thead><tr><th>الترتيب</th><th>المستخدم</th><th>النقاط</th><th>الفئة</th></tr></thead>
                <tbody>
                  {leaderboardList.map((entry, i) => (
                    <tr key={i} className={i === 0 ? 'bg-amber-500/10' : ''}>
                      <td className='font-bold text-center'>
                        {i === 0 ? '🥇' : i === 1 ? '🥈' : i === 2 ? '🥉' : i + 1}
                      </td>
                      <td>{`مستخدم ${entry.userId ?? '-'}`}</td>
                      <td className='font-semibold tabular-nums'>{entry.score ?? 0}</td>
                      <td><Badge variant='outline' className='text-xs'>{tierLabel(entry.tier)}</Badge></td>
                    </tr>
                  ))}
                  {!leaderboard.loading && leaderboardList.length === 0 && (
                    <tr><td colSpan={4} className='text-muted-foreground text-sm'>لا توجد بيانات.</td></tr>
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

// ─── Referral ─────────────────────────────────────────────────────────────────

type ReferralInfo = {
  referralCode?: string;
  totalReferrals?: number;
  rewardsEarned?: number;
  currency?: string;
};

export function ReferralPage() {
  const referral = useBackend<ReferralInfo>('/referral/my-code');

  const copyCode = () => {
    if (referral.data?.referralCode) {
      void navigator.clipboard.writeText(referral.data.referralCode);
      toast.success('تم نسخ رمز الإحالة');
    }
  };

  return (
    <PageContainer
      pageTitle='برنامج الإحالة'
      pageDescription='شارك رمز الإحالة واكسب مكافآت عند تسجيل أشخاص جدد.'
    >
      <div className='grid gap-4 xl:grid-cols-3'>
        {referral.loading && (
          <div className='col-span-3 flex justify-center py-8'>
            <Icons.spinner className='size-6 animate-spin text-muted-foreground' />
          </div>
        )}
        {referral.data && (
          <>
            <div className='gov-panel rounded-md p-6 text-center'>
              <div className='text-xs text-muted-foreground mb-2'>رمز الإحالة الخاص بك</div>
              <div className='text-3xl font-bold tracking-widest mb-3 font-mono'>
                {referral.data.referralCode ?? '-'}
              </div>
              <Button className='h-9 rounded-sm w-full' onClick={copyCode}>
                <Icons.copy className='ml-2 size-4' />
                نسخ الرمز
              </Button>
            </div>
            <div className='gov-panel rounded-md p-4 text-center'>
              <div className='text-3xl font-bold tabular-nums'>{referral.data.totalReferrals ?? 0}</div>
              <div className='text-xs text-muted-foreground mt-2'>إجمالي الإحالات</div>
            </div>
            <div className='gov-panel rounded-md p-4 text-center'>
              <div className='text-3xl font-bold tabular-nums'>
                {referral.data.rewardsEarned ?? 0} {referral.data.currency ?? 'USD'}
              </div>
              <div className='text-xs text-muted-foreground mt-2'>المكافآت المكتسبة</div>
            </div>
          </>
        )}
        {!referral.loading && !referral.data && (
          <div className='col-span-3 gov-panel rounded-md p-6 text-center text-sm text-muted-foreground'>
            <Icons.info className='mx-auto mb-2 size-6' />
            لا يوجد برنامج إحالة مفعّل لهذا الحساب.
          </div>
        )}
        {referral.error && (
          <div className='col-span-3 gov-panel rounded-md p-6 text-center text-sm text-destructive'>
            {referral.error}
          </div>
        )}
      </div>

      <div className='mt-4 gov-panel rounded-md p-4'>
        <h3 className='text-sm font-semibold mb-2'>كيف يعمل البرنامج؟</h3>
        <ul className='space-y-2 text-sm text-muted-foreground'>
          <li className='flex items-start gap-2'>
            <span className='text-primary font-bold'>١.</span>
            شارك رمز الإحالة الخاص بك مع الأصدقاء والعائلة.
          </li>
          <li className='flex items-start gap-2'>
            <span className='text-primary font-bold'>٢.</span>
            عند تسجيلهم باستخدام رمزك وإتمام أول تحويل، تحصل على مكافأة.
          </li>
          <li className='flex items-start gap-2'>
            <span className='text-primary font-bold'>٣.</span>
            تراكم المكافآت في حسابك تلقائياً.
          </li>
        </ul>
      </div>
    </PageContainer>
  );
}

// ─── Family Wallet ─────────────────────────────────────────────────────────────

type FamilyMemberEntry = {
  id: number;
  userId?: number;
  role?: string;
  monthlySpendingLimit?: number;
  active?: boolean;
};

type StatementData = {
  groupId?: number;
  groupName?: string;
  month?: string;
  members?: Array<{ userId?: number; role?: string; spent?: number; limit?: number }>;
};

export function FamilyPage() {
  const groups = useBackend<FamilyGroup[]>('/family/groups/my');
  const [form, setForm] = useState({ name: '', currency: 'USD', monthlySpendingLimit: '' });
  const [selectedGroup, setSelectedGroup] = useState<number | null>(null);
  const statement = useBackend<StatementData>(selectedGroup ? `/family/groups/${selectedGroup}/statement` : null);
  const [addMemberUserId, setAddMemberUserId] = useState('');
  const [addMemberLimit, setAddMemberLimit] = useState('');

  async function createGroup(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    await apiClient<FamilyGroup>('/family/groups', {
      method: 'POST',
      body: JSON.stringify({
        name: form.name,
        currency: form.currency,
        monthlySpendingLimit: form.monthlySpendingLimit ? Number(form.monthlySpendingLimit) : null
      })
    });
    toast.success('تم إنشاء المجموعة العائلية');
    setForm({ name: '', currency: 'USD', monthlySpendingLimit: '' });
    groups.refetch();
  }

  async function addMemberToGroup() {
    if (!selectedGroup || !addMemberUserId) return;
    await apiClient(`/family/groups/${selectedGroup}/members`, {
      method: 'POST',
      body: JSON.stringify({
        memberUserId: Number(addMemberUserId),
        memberLimit: addMemberLimit ? Number(addMemberLimit) : null
      })
    });
    toast.success('تمت إضافة العضو');
    setAddMemberUserId('');
    setAddMemberLimit('');
    statement.refetch();
  }

  const members = statement.data?.members ?? [];

  return (
    <PageContainer
      pageTitle='المحفظة العائلية'
      pageDescription='إنشاء وإدارة مجموعات العائلة للتحكم في المصاريف.'
    >
      <div className='grid gap-4 xl:grid-cols-[320px_1fr]'>
        <div className='space-y-4'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>مجموعة جديدة</h2>
            </div>
            <form className='grid gap-3 p-4' onSubmit={(e) => { void createGroup(e); }}>
              <Field id='familyName' label='اسم المجموعة' value={form.name}
                onChange={(v) => setForm((c) => ({ ...c, name: v }))} placeholder='مثال: عائلة المختار' />
              <div className='grid grid-cols-2 gap-2'>
                <Field id='familyCur' label='العملة' value={form.currency}
                  onChange={(v) => setForm((c) => ({ ...c, currency: v }))} />
                <Field id='familyLimit' label='حد الإنفاق الشهري' value={form.monthlySpendingLimit}
                  onChange={(v) => setForm((c) => ({ ...c, monthlySpendingLimit: v }))} type='number' />
              </div>
              <Button type='submit' className='h-9 rounded-sm'>إنشاء المجموعة</Button>
            </form>
          </div>

          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>مجموعاتي ({groups.data?.length ?? 0})</h2>
              {groups.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
            </div>
            <div className='divide-y divide-border'>
              {(groups.data ?? []).map((g) => (
                <button key={g.id}
                  className={`w-full flex items-center justify-between px-4 py-3 text-right hover:bg-muted/40 ${selectedGroup === g.id ? 'bg-muted/60' : ''}`}
                  onClick={() => setSelectedGroup(g.id)}>
                  <div>
                    <div className='text-sm font-medium'>{g.name}</div>
                    <div className='text-xs text-muted-foreground'>{g.currency ?? 'USD'}</div>
                  </div>
                  <Icons.chevronLeft className='size-4 text-muted-foreground' />
                </button>
              ))}
              {!groups.loading && !groups.data?.length && (
                <div className='px-4 py-4 text-sm text-muted-foreground'>لا توجد مجموعات.</div>
              )}
            </div>
          </div>
        </div>

        {selectedGroup ? (
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>
                {statement.data?.groupName ?? `المجموعة #${selectedGroup}`}
              </h2>
              <span className='text-xs text-muted-foreground'>{statement.data?.month ?? ''}</span>
            </div>
            <div className='p-4 space-y-4'>
              <div className='flex gap-2'>
                <Input value={addMemberUserId} onChange={(e) => setAddMemberUserId(e.target.value)}
                  placeholder='معرف المستخدم...' className='h-9 rounded-sm' type='number' />
                <Input value={addMemberLimit} onChange={(e) => setAddMemberLimit(e.target.value)}
                  placeholder='حد الإنفاق (اختياري)...' className='h-9 rounded-sm flex-1' type='number' />
                <Button className='h-9 rounded-sm shrink-0' onClick={() => { void addMemberToGroup(); }}>
                  إضافة
                </Button>
              </div>
              {statement.loading ? (
                <div className='flex justify-center py-4'>
                  <Icons.spinner className='size-5 animate-spin text-muted-foreground' />
                </div>
              ) : (
                <div className='overflow-x-auto'>
                  <table className='gov-table'>
                    <thead>
                      <tr><th>معرف المستخدم</th><th>الدور</th><th>الإنفاق هذا الشهر</th><th>الحد المسموح</th></tr>
                    </thead>
                    <tbody>
                      {members.map((m, i) => (
                        <tr key={i}>
                          <td>{m.userId ?? '-'}</td>
                          <td><Badge variant='outline'>{m.role === 'OWNER' ? 'مالك' : 'عضو'}</Badge></td>
                          <td className='tabular-nums'>{m.spent ?? 0}</td>
                          <td className='text-muted-foreground'>{m.limit != null ? m.limit : 'غير محدود'}</td>
                        </tr>
                      ))}
                      {!members.length && (
                        <tr><td colSpan={4} className='text-muted-foreground text-sm'>لا يوجد أعضاء.</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className='gov-panel rounded-md flex items-center justify-center text-muted-foreground text-sm min-h-48'>
            اختر مجموعة لعرض التفاصيل
          </div>
        )}
      </div>
    </PageContainer>
  );
}

// ─── Campaigns ─────────────────────────────────────────────────────────────────

const AUDIENCE_OPTIONS = [
  { value: 'ALL_USERS', label: 'جميع المستخدمين' },
  { value: 'ACTIVE_USERS', label: 'المستخدمون النشطون' },
  { value: 'INACTIVE_USERS', label: 'المستخدمون غير النشطين' },
  { value: 'HIGH_VALUE', label: 'العملاء ذوو القيمة العالية' },
  { value: 'NEW_USERS', label: 'المستخدمون الجدد' },
  { value: 'MERCHANTS', label: 'التجار' },
  { value: 'BRANCH_MANAGERS', label: 'مديرو الفروع' },
  { value: 'CORPORATE', label: 'الحسابات المؤسسية' },
];

const CAMPAIGN_STATUS_LABEL: Record<string, string> = {
  DRAFT: 'مسودة',
  SCHEDULED: 'مجدولة',
  EXECUTING: 'قيد التنفيذ',
  EXECUTED: 'منفذة',
  FAILED: 'فاشلة',
  CANCELLED: 'ملغاة',
};

const CAMPAIGN_STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  DRAFT: 'outline',
  SCHEDULED: 'secondary',
  EXECUTING: 'secondary',
  EXECUTED: 'default',
  FAILED: 'destructive',
  CANCELLED: 'outline',
};

const EMPTY_CAMPAIGN = { name: '', targetAudience: 'ALL_USERS', messageTemplate: '' };

const TEMPLATE_HINTS = [
  { label: 'ترحيب', text: 'مرحباً {{name}}، شكراً لاختيارك المختار للخدمات المالية. استمتع بأفضل تجربة تحويل.' },
  { label: 'خصم خاص', text: 'عرض حصري لك {{name}}! احصل على خصم 10% على رسوم التحويل اليوم فقط. التاريخ: {{date}}' },
  { label: 'تذكير بالرصيد', text: 'عزيزي {{name}}، يمكنك الآن تعبئة محفظتك بكل سهولة من أقرب فرع لك.' },
];

export function CampaignsPage() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(EMPTY_CAMPAIGN);
  const [creating, setCreating] = useState(false);
  const [scheduleId, setScheduleId] = useState<number | null>(null);
  const [scheduleAt, setScheduleAt] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [previewCampaign, setPreviewCampaign] = useState<Campaign | null>(null);

  const loadCampaigns = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiClient<Campaign[] | { content: Campaign[] }>('/campaigns');
      setCampaigns(Array.isArray(data) ? data : (data as { content: Campaign[] }).content ?? []);
    } catch {
      // Backend may not have a list endpoint — show empty gracefully
    } finally {
      setLoading(false);
    }
  }, []);

  useState(() => { void loadCampaigns(); });

  const filteredCampaigns = statusFilter
    ? campaigns.filter((c) => (c.status ?? 'DRAFT') === statusFilter)
    : campaigns;

  // stats
  const statsMap: Record<string, number> = {};
  campaigns.forEach((c) => {
    const k = c.status ?? 'DRAFT';
    statsMap[k] = (statsMap[k] ?? 0) + 1;
  });

  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!form.name.trim()) { toast.error('اسم الحملة مطلوب'); return; }
    if (!form.messageTemplate.trim()) { toast.error('قالب الرسالة مطلوب'); return; }
    setCreating(true);
    try {
      const campaign = await apiClient<Campaign>('/campaigns', {
        method: 'POST',
        body: JSON.stringify({
          name: form.name,
          targetAudience: form.targetAudience,
          messageTemplate: form.messageTemplate,
        })
      });
      toast.success('تم إنشاء الحملة بنجاح');
      setCampaigns((prev) => [campaign, ...prev]);
      setForm(EMPTY_CAMPAIGN);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إنشاء الحملة');
    } finally {
      setCreating(false);
    }
  }

  async function executeCampaign(id: number) {
    try {
      await apiClient(`/campaigns/${id}/execute`, { method: 'POST' });
      toast.success('بدأ تنفيذ الحملة');
      void loadCampaigns();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التنفيذ');
    }
  }

  async function cancelCampaign(id: number) {
    try {
      await apiClient(`/campaigns/${id}/cancel`, { method: 'POST' });
      toast.success('تم إلغاء الحملة');
      void loadCampaigns();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الإلغاء');
    }
  }

  async function scheduleCampaign() {
    if (!scheduleId || !scheduleAt) { toast.error('يرجى تحديد الحملة والوقت'); return; }
    try {
      await apiClient(`/campaigns/${scheduleId}/schedule?scheduledAt=${encodeURIComponent(scheduleAt)}`, { method: 'POST' });
      toast.success('تم جدولة الحملة بنجاح');
      setScheduleId(null);
      setScheduleAt('');
      void loadCampaigns();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل الجدولة');
    }
  }

  return (
    <PageContainer
      pageTitle='الحملات التسويقية'
      pageDescription='إنشاء وجدولة وتنفيذ حملات التواصل عبر واتساب والإشعارات الداخلية.'
    >
      <div className='space-y-4'>
        {/* Stats Row */}
        {campaigns.length > 0 && (
          <div className='grid grid-cols-3 md:grid-cols-6 gap-3'>
            {[
              { label: 'الإجمالي', value: campaigns.length, color: '' },
              { label: 'مسودة', value: statsMap['DRAFT'] ?? 0, color: 'text-muted-foreground' },
              { label: 'مجدولة', value: statsMap['SCHEDULED'] ?? 0, color: 'text-amber-600' },
              { label: 'قيد التنفيذ', value: statsMap['EXECUTING'] ?? 0, color: 'text-blue-600' },
              { label: 'منفذة', value: statsMap['EXECUTED'] ?? 0, color: 'text-emerald-600' },
              { label: 'ملغاة/فاشلة', value: (statsMap['CANCELLED'] ?? 0) + (statsMap['FAILED'] ?? 0), color: 'text-destructive' },
            ].map((s) => (
              <div key={s.label} className='gov-panel rounded-md p-3 text-center'>
                <div className={`text-xl font-bold tabular-nums ${s.color}`}>{s.value}</div>
                <div className='text-xs text-muted-foreground mt-0.5'>{s.label}</div>
              </div>
            ))}
          </div>
        )}

        <div className='grid gap-4 xl:grid-cols-[360px_1fr]'>
          <div className='space-y-4'>
            {/* Create Form */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>حملة جديدة</h2>
              </div>
              <form className='grid gap-3 p-4' onSubmit={(e) => { void create(e); }}>
                <Field id='campName' label='اسم الحملة' value={form.name}
                  onChange={(v) => setForm((c) => ({ ...c, name: v }))}
                  placeholder='مثال: حملة العيد الوطني' />
                <div className='space-y-1.5'>
                  <Label className='text-xs'>الجمهور المستهدف <span className='text-destructive'>*</span></Label>
                  <select value={form.targetAudience}
                    onChange={(e) => setForm((c) => ({ ...c, targetAudience: e.target.value }))}
                    className='h-9 w-full rounded-sm border border-border bg-background px-2 text-sm'>
                    {AUDIENCE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                </div>
                <div className='space-y-1.5'>
                  <div className='flex items-center justify-between'>
                    <Label className='text-xs'>قالب الرسالة <span className='text-destructive'>*</span></Label>
                    <div className='flex gap-1 flex-wrap justify-end'>
                      {TEMPLATE_HINTS.map((h) => (
                        <button
                          key={h.label}
                          type='button'
                          className='text-xs text-primary underline underline-offset-2'
                          onClick={() => setForm((c) => ({ ...c, messageTemplate: h.text }))}
                        >
                          {h.label}
                        </button>
                      ))}
                    </div>
                  </div>
                  <textarea
                    value={form.messageTemplate}
                    onChange={(e) => setForm((c) => ({ ...c, messageTemplate: e.target.value }))}
                    rows={4}
                    className='w-full rounded-sm border border-border bg-background px-3 py-2 text-sm resize-none'
                    placeholder='مرحباً {{name}}، نوفر لك اليوم خصم 10% على كل تحويل...' />
                  <p className='text-xs text-muted-foreground'>
                    متغيرات: <span dir='ltr' className='font-mono'>{'{{name}}'} {'{{amount}}'} {'{{date}}'}</span>
                  </p>
                </div>
                {/* Preview */}
                {form.messageTemplate && (
                  <div className='rounded-sm bg-muted/60 p-3 text-xs space-y-1'>
                    <div className='text-muted-foreground text-xs font-medium mb-1'>معاينة:</div>
                    <p dir='rtl' className='leading-relaxed'>
                      {form.messageTemplate
                        .replace('{{name}}', 'أحمد المختار')
                        .replace('{{amount}}', '250 USD')
                        .replace('{{date}}', new Date().toLocaleDateString('ar-SY'))}
                    </p>
                  </div>
                )}
                <Button type='submit' className='h-9 rounded-sm' disabled={creating}>
                  {creating && <Icons.spinner className='ml-2 size-4 animate-spin' />}
                  إنشاء الحملة
                </Button>
              </form>
            </div>

            {/* Schedule Form */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>جدولة حملة</h2>
                <p className='text-muted-foreground text-xs'>تحديد وقت تنفيذ الحملة مستقبلاً</p>
              </div>
              <div className='grid gap-3 p-4'>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>الحملة</Label>
                  <select value={scheduleId ?? ''}
                    onChange={(e) => setScheduleId(e.target.value ? Number(e.target.value) : null)}
                    className='h-9 w-full rounded-sm border border-border bg-background px-2 text-sm'>
                    <option value=''>اختر حملة...</option>
                    {campaigns.filter((c) => c.status === 'DRAFT' || !c.status).map((c) => (
                      <option key={c.id} value={c.id}>{c.name ?? `حملة #${c.id}`}</option>
                    ))}
                  </select>
                </div>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>وقت الجدولة</Label>
                  <Input type='datetime-local' value={scheduleAt}
                    onChange={(e) => setScheduleAt(e.target.value)}
                    className='h-9 rounded-sm text-sm' />
                </div>
                <Button variant='outline' className='h-9 rounded-sm'
                  onClick={() => { void scheduleCampaign(); }}>
                  جدولة الحملة
                </Button>
              </div>
            </div>
          </div>

          <div className='space-y-4'>
            {/* Preview panel */}
            {previewCampaign && (
              <div className='gov-panel rounded-md'>
                <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                  <h2 className='text-sm font-semibold'>معاينة الحملة: {previewCampaign.name}</h2>
                  <Button size='sm' variant='ghost' className='h-7 text-xs'
                    onClick={() => setPreviewCampaign(null)}>
                    <Icons.close className='size-3.5' />
                  </Button>
                </div>
                <div className='p-4 space-y-3'>
                  <div className='grid grid-cols-2 gap-3 text-sm'>
                    <div>
                      <span className='text-muted-foreground'>الجمهور: </span>
                      <span>{AUDIENCE_OPTIONS.find((o) => o.value === previewCampaign.targetAudience)?.label ?? previewCampaign.targetAudience}</span>
                    </div>
                    <div>
                      <span className='text-muted-foreground'>الحالة: </span>
                      <Badge variant={CAMPAIGN_STATUS_VARIANT[previewCampaign.status ?? ''] ?? 'outline'} className='ml-1'>
                        {CAMPAIGN_STATUS_LABEL[previewCampaign.status ?? ''] ?? 'مسودة'}
                      </Badge>
                    </div>
                  </div>
                  <div className='rounded-sm border border-border p-3'>
                    <div className='text-xs text-muted-foreground mb-1'>نص الرسالة:</div>
                    <p className='text-sm leading-relaxed whitespace-pre-wrap'>
                      {previewCampaign.messageTemplate}
                    </p>
                  </div>
                  <div className='rounded-sm bg-muted/60 p-3'>
                    <div className='text-xs text-muted-foreground mb-1'>معاينة للمستلم &ldquo;أحمد المختار&rdquo;:</div>
                    <p className='text-sm leading-relaxed'>
                      {(previewCampaign.messageTemplate ?? '')
                        .replace('{{name}}', 'أحمد المختار')
                        .replace('{{amount}}', '250 USD')
                        .replace('{{date}}', new Date().toLocaleDateString('ar-SY'))}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Campaigns Table */}
            <div className='gov-panel rounded-md'>
              <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                <h2 className='text-sm font-semibold'>
                  الحملات
                  {filteredCampaigns.length > 0 && (
                    <span className='text-muted-foreground font-normal mr-1'>({filteredCampaigns.length})</span>
                  )}
                </h2>
                <div className='flex items-center gap-2'>
                  <select value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className='h-7 rounded-sm border border-border bg-background px-2 text-xs'>
                    <option value=''>كل الحالات</option>
                    <option value='DRAFT'>مسودة</option>
                    <option value='SCHEDULED'>مجدولة</option>
                    <option value='EXECUTING'>قيد التنفيذ</option>
                    <option value='EXECUTED'>منفذة</option>
                    <option value='CANCELLED'>ملغاة</option>
                    <option value='FAILED'>فاشلة</option>
                  </select>
                  <Button size='sm' variant='ghost' className='h-7 text-xs' onClick={() => { void loadCampaigns(); }}>
                    {loading ? <Icons.spinner className='size-3.5 animate-spin' /> : <Icons.spinner className='size-3.5' />}
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
                      <th>الجمهور</th>
                      <th>الحالة</th>
                      <th>وقت الجدولة</th>
                      <th>وقت التنفيذ</th>
                      <th>إجراءات</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredCampaigns.map((c) => (
                      <tr key={c.id}>
                        <td className='text-muted-foreground'>{c.id}</td>
                        <td className='font-medium'>{c.name ?? '-'}</td>
                        <td className='text-xs'>
                          {AUDIENCE_OPTIONS.find((o) => o.value === c.targetAudience)?.label ?? c.targetAudience ?? '-'}
                        </td>
                        <td>
                          <Badge variant={CAMPAIGN_STATUS_VARIANT[c.status ?? ''] ?? 'outline'}>
                            {CAMPAIGN_STATUS_LABEL[c.status ?? ''] ?? c.status ?? 'مسودة'}
                          </Badge>
                        </td>
                        <td className='text-xs text-muted-foreground'>{c.scheduledAt ?? '-'}</td>
                        <td className='text-xs text-muted-foreground'>{c.executedAt ?? '-'}</td>
                        <td>
                          <div className='flex items-center gap-1'>
                            <Button size='sm' variant='ghost' className='h-7 text-xs'
                              onClick={() => setPreviewCampaign(c)}>
                              معاينة
                            </Button>
                            {(c.status === 'DRAFT' || c.status === 'SCHEDULED' || !c.status) && (
                              <Button size='sm' variant='outline' className='h-7 text-xs'
                                onClick={() => { void executeCampaign(c.id); }}>
                                تنفيذ
                              </Button>
                            )}
                            {c.status !== 'EXECUTED' && c.status !== 'CANCELLED' && c.status !== 'FAILED' && (
                              <Button size='sm' variant='ghost'
                                className='h-7 text-xs text-destructive hover:text-destructive'
                                onClick={() => { void cancelCampaign(c.id); }}>
                                إلغاء
                              </Button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                    {!loading && !filteredCampaigns.length && (
                      <tr>
                        <td colSpan={7} className='py-8 text-center'>
                          <div className='text-muted-foreground text-sm'>لا توجد حملات.</div>
                          {statusFilter && (
                            <div className='text-xs text-muted-foreground mt-1'>
                              جرب تغيير فلتر الحالة أو أنشئ حملة جديدة.
                            </div>
                          )}
                        </td>
                      </tr>
                    )}
                    {loading && (
                      <tr>
                        <td colSpan={7} className='py-6 text-center'>
                          <Icons.spinner className='size-5 animate-spin text-muted-foreground inline' />
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
