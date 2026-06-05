'use client';

import { useSession } from '@/features/almukhtar/session';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Icons } from '@/components/icons';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'مدير النظام',
  MANAGER: 'مدير',
  CASHIER: 'صراف',
  OPERATOR: 'مشغّل',
  VIEWER: 'مشاهد',
  SUPER_ADMIN: 'مدير عام'
};

const ROLE_VARIANTS: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  ADMIN: 'destructive',
  SUPER_ADMIN: 'destructive',
  MANAGER: 'default',
  CASHIER: 'secondary',
  OPERATOR: 'secondary',
  VIEWER: 'outline'
};

function SessionTimer({ expiresIn }: { expiresIn: number }) {
  const expiryDate = new Date(expiresIn * 1000);
  const isExpired = expiryDate < new Date();
  const formatted = expiryDate.toLocaleString('ar-SY', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });

  return (
    <div className='flex items-center gap-2 text-sm'>
      <span className={isExpired ? 'text-destructive' : 'text-muted-foreground'}>
        {isExpired ? 'الجلسة منتهية' : `تنتهي في: ${formatted}`}
      </span>
      <span
        className={`inline-block h-2 w-2 rounded-full ${isExpired ? 'bg-destructive' : 'bg-emerald-500'}`}
      />
    </div>
  );
}

export default function ProfileViewPage() {
  const { session, ready } = useSession();

  if (!ready) {
    return (
      <div className='flex w-full flex-col gap-6 p-6'>
        <div className='h-8 w-48 animate-pulse rounded-md bg-muted' />
        <div className='h-40 animate-pulse rounded-xl bg-muted' />
      </div>
    );
  }

  if (!session) {
    return (
      <div className='flex w-full flex-col items-center justify-center gap-4 p-12'>
        <Icons.user className='text-muted-foreground h-16 w-16' />
        <p className='text-muted-foreground text-lg'>لم يتم تسجيل الدخول</p>
      </div>
    );
  }

  const roleLabel = ROLE_LABELS[session.role] ?? session.role;
  const roleVariant = ROLE_VARIANTS[session.role] ?? 'secondary';

  return (
    <div className='flex w-full flex-col gap-6 p-6 md:p-8'>
      <div>
        <h1 className='text-2xl font-semibold tracking-tight'>الملف الشخصي</h1>
        <p className='text-muted-foreground text-sm mt-1'>بيانات حساب المستخدم والجلسة الحالية</p>
      </div>

      <div className='grid gap-6 md:grid-cols-2'>
        {/* User identity card */}
        <Card>
          <CardHeader className='pb-3'>
            <div className='flex items-center gap-3'>
              <div className='bg-primary/10 flex h-12 w-12 items-center justify-center rounded-full'>
                <Icons.user className='text-primary h-6 w-6' />
              </div>
              <div>
                <CardTitle className='text-lg'>{session.username}</CardTitle>
                <CardDescription>معرّف المستخدم في النظام</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Separator className='mb-4' />
            <dl className='grid gap-3 text-sm'>
              <div className='flex items-center justify-between'>
                <dt className='text-muted-foreground'>اسم المستخدم</dt>
                <dd className='font-medium'>{session.username}</dd>
              </div>
              <div className='flex items-center justify-between'>
                <dt className='text-muted-foreground'>الصلاحية</dt>
                <dd>
                  <Badge variant={roleVariant}>{roleLabel}</Badge>
                </dd>
              </div>
            </dl>
          </CardContent>
        </Card>

        {/* Session card */}
        <Card>
          <CardHeader className='pb-3'>
            <div className='flex items-center gap-3'>
              <div className='bg-primary/10 flex h-12 w-12 items-center justify-center rounded-full'>
                <Icons.notification className='text-primary h-6 w-6' />
              </div>
              <div>
                <CardTitle className='text-lg'>الجلسة الحالية</CardTitle>
                <CardDescription>معلومات رمز المصادقة النشط</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Separator className='mb-4' />
            <dl className='grid gap-3 text-sm'>
              <div className='flex items-center justify-between'>
                <dt className='text-muted-foreground'>حالة الجلسة</dt>
                <dd className='flex items-center gap-2'>
                  <span className='inline-block h-2 w-2 rounded-full bg-emerald-500' />
                  <span className='font-medium'>نشطة</span>
                </dd>
              </div>
              <div className='flex items-start justify-between gap-4'>
                <dt className='text-muted-foreground shrink-0'>انتهاء الجلسة</dt>
                <dd className='text-left'>
                  <SessionTimer expiresIn={session.expiresIn} />
                </dd>
              </div>
              <div className='flex items-start justify-between gap-4'>
                <dt className='text-muted-foreground shrink-0'>الرمز</dt>
                <dd className='font-mono text-xs text-muted-foreground break-all text-left max-w-[200px]'>
                  {session.token.slice(0, 32)}…
                </dd>
              </div>
            </dl>
          </CardContent>
        </Card>
      </div>

      {/* Permissions card */}
      <Card>
        <CardHeader>
          <CardTitle className='text-base'>الصلاحيات والوصول</CardTitle>
          <CardDescription>نطاق العمليات المسموح بها لهذا الحساب</CardDescription>
        </CardHeader>
        <CardContent>
          <Separator className='mb-4' />
          <div className='grid gap-2 text-sm sm:grid-cols-2'>
            {getPermissionsForRole(session.role).map(({ label, allowed }) => (
              <div key={label} className='flex items-center gap-2'>
                {allowed ? (
                  <Icons.check className='text-emerald-500 h-4 w-4 shrink-0' />
                ) : (
                  <Icons.close className='text-muted-foreground/40 h-4 w-4 shrink-0' />
                )}
                <span className={allowed ? '' : 'text-muted-foreground/50'}>{label}</span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function getPermissionsForRole(role: string): { label: string; allowed: boolean }[] {
  const all: { label: string; roles: string[] }[] = [
    { label: 'عرض لوحة التحكم', roles: ['ADMIN', 'SUPER_ADMIN', 'MANAGER', 'CASHIER', 'OPERATOR', 'VIEWER'] },
    { label: 'إدارة الحوالات', roles: ['ADMIN', 'SUPER_ADMIN', 'MANAGER', 'CASHIER', 'OPERATOR'] },
    { label: 'إدارة المحافظ', roles: ['ADMIN', 'SUPER_ADMIN', 'MANAGER', 'CASHIER'] },
    { label: 'إدارة السيولة', roles: ['ADMIN', 'SUPER_ADMIN', 'MANAGER'] },
    { label: 'إدارة المستخدمين', roles: ['ADMIN', 'SUPER_ADMIN'] },
    { label: 'الإعدادات العامة', roles: ['ADMIN', 'SUPER_ADMIN'] },
    { label: 'الامتثال والتقارير', roles: ['ADMIN', 'SUPER_ADMIN', 'MANAGER'] },
    { label: 'عمليات الدُفعات', roles: ['ADMIN', 'SUPER_ADMIN', 'MANAGER', 'OPERATOR'] }
  ];
  return all.map(({ label, roles }) => ({ label, allowed: roles.includes(role) }));
}
