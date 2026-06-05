'use client';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { apiClient, storeSession } from '@/lib/api-client';
import { useRouter } from 'next/navigation';
import { FormEvent, useState } from 'react';
import { toast } from 'sonner';
import type { LoginResponse } from '@/features/almukhtar/types';

export default function SignInViewPage() {
  const router = useRouter();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    try {
      const session = await apiClient<LoginResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password })
      });
      storeSession(session);
      toast.success('تم تسجيل الدخول بنجاح');
      router.push('/dashboard/overview');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'فشل تسجيل الدخول');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className='from-background via-accent/30 to-background flex min-h-screen items-center justify-center bg-gradient-to-br p-4'>
      <div className='grid w-full max-w-5xl gap-6 lg:grid-cols-[1.1fr_0.9fr]'>
        <section className='flex min-h-[520px] flex-col justify-between rounded-lg bg-[radial-gradient(circle_at_top_right,oklch(0.78_0.12_86_/_0.32),transparent_30%),linear-gradient(135deg,oklch(0.18_0.04_165),oklch(0.31_0.1_164))] p-8 text-white shadow-xl'>
          <div>
            <div className='mb-8 flex items-center gap-3'>
              <div className='flex h-12 w-12 items-center justify-center rounded-md bg-white/12 text-xl font-bold text-secondary'>
                م
              </div>
              <div>
                <p className='text-2xl font-semibold'>المختار</p>
                <p className='text-sm text-white/70'>لوحة تشغيل الحوالات السورية</p>
              </div>
            </div>
            <h1 className='max-w-xl text-4xl leading-tight font-semibold md:text-5xl'>
              إدارة الحوالات، المحافظ، والسيولة من واجهة عربية واحدة.
            </h1>
          </div>
          <div className='grid gap-3 text-sm text-white/80 md:grid-cols-3'>
            <div className='rounded-md border border-white/15 bg-white/10 p-3'>JWT محلي من الخادم</div>
            <div className='rounded-md border border-white/15 bg-white/10 p-3'>RTL كامل</div>
            <div className='rounded-md border border-white/15 bg-white/10 p-3'>متصل بـ Spring Boot</div>
          </div>
        </section>

        <Card className='self-center'>
          <CardHeader className='text-right'>
            <CardTitle className='text-2xl'>تسجيل الدخول</CardTitle>
            <p className='text-muted-foreground text-sm'>
              استخدم حسابات التطوير المزروعة في الخادم المحلي.
            </p>
          </CardHeader>
          <CardContent>
            <form onSubmit={onSubmit} className='space-y-4 text-right'>
              <div className='space-y-2'>
                <Label htmlFor='username'>اسم المستخدم</Label>
                <Input
                  id='username'
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  autoComplete='username'
                  required
                />
              </div>
              <div className='space-y-2'>
                <Label htmlFor='password'>كلمة المرور</Label>
                <Input
                  id='password'
                  type='password'
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  autoComplete='current-password'
                  required
                />
              </div>
              <Button type='submit' className='w-full' disabled={loading}>
                {loading ? 'جار التحقق...' : 'دخول'}
              </Button>
              <div className='text-muted-foreground rounded-md border p-3 text-xs leading-6'>
                حسابات متاحة: admin/admin123، manager/manager123، cashier/cashier123،
                auditor/auditor123.
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
