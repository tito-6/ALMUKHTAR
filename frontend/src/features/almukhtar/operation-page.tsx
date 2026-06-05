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
import type { Fund, LiquidityAlert, PageResponse, Transaction, WalletResponse } from './types';

const moneyFormatter = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });

function formatMoney(value?: number | string, currency = 'USD') {
  return `${moneyFormatter.format(Number(value ?? 0))} ${currency}`;
}

function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

function JsonPreview({ value }: { value: unknown }) {
  return (
    <pre className='bg-muted/70 border-border max-h-72 overflow-auto rounded-sm border p-3 text-left text-xs' dir='ltr'>
      {JSON.stringify(value ?? {}, null, 2)}
    </pre>
  );
}

function Field({
  id,
  label,
  value,
  onChange,
  type = 'text'
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
}) {
  return (
    <div className='space-y-1.5'>
      <Label htmlFor={id} className='text-xs'>
        {label}
      </Label>
      <Input
        id={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className='h-9 rounded-sm'
        required
      />
    </div>
  );
}

export function TransfersPage() {
  const transactions = useBackend<Transaction[]>('/transactions');
  const [payload, setPayload] = useState({
    senderId: '1',
    receiverId: '2',
    fundId: '1',
    amount: '100'
  });

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const result = await apiClient<Transaction>('/transactions/transfer', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey() },
      body: JSON.stringify({
        senderId: Number(payload.senderId),
        receiverId: Number(payload.receiverId),
        fundId: Number(payload.fundId),
        amount: Number(payload.amount)
      })
    });
    toast.success(`تم إنشاء الحوالة رقم ${result.id}`);
  }

  return (
    <PageContainer
      pageTitle='مكتب الحوالات'
      pageDescription='إنشاء حوالة ومراجعة الحركة الأخيرة بنفس نمط شبابيك الفروع.'
    >
      <div className='grid gap-4 xl:grid-cols-[420px_1fr]'>
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>أمر حوالة جديد</h2>
            <p className='text-muted-foreground text-xs'>كل أمر يرسل بمفتاح Idempotency-Key مستقل.</p>
          </div>
          <form
            className='grid gap-3 p-4'
            onSubmit={(event) => {
              void submit(event);
            }}
          >
            <div className='grid grid-cols-2 gap-3'>
              <Field
                id='senderId'
                label='معرف المرسل'
                value={payload.senderId}
                onChange={(value) => setPayload((current) => ({ ...current, senderId: value }))}
                type='number'
              />
              <Field
                id='receiverId'
                label='معرف المستلم'
                value={payload.receiverId}
                onChange={(value) => setPayload((current) => ({ ...current, receiverId: value }))}
                type='number'
              />
            </div>
            <div className='grid grid-cols-2 gap-3'>
              <Field
                id='fundId'
                label='معرف الصندوق'
                value={payload.fundId}
                onChange={(value) => setPayload((current) => ({ ...current, fundId: value }))}
                type='number'
              />
              <Field
                id='amount'
                label='المبلغ'
                value={payload.amount}
                onChange={(value) => setPayload((current) => ({ ...current, amount: value }))}
                type='number'
              />
            </div>
            <Button type='submit' className='mt-2 h-9 rounded-sm'>
              <Icons.send className='ml-2 size-4' />
              تثبيت الحوالة
            </Button>
          </form>
        </div>

        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
            <div>
              <h2 className='text-sm font-semibold'>سجل الحوالات</h2>
              <p className='text-muted-foreground text-xs'>آخر العمليات المقروءة من الخادم</p>
            </div>
            {transactions.error && <Badge variant='destructive'>تعذر الاتصال</Badge>}
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>الرقم</th>
                  <th>المبلغ</th>
                  <th>الحالة</th>
                  <th>التاريخ</th>
                </tr>
              </thead>
              <tbody>
                {(transactions.data ?? []).slice(0, 12).map((transaction) => (
                  <tr key={transaction.id}>
                    <td className='font-medium'>#{transaction.id}</td>
                    <td>{formatMoney(transaction.amount, transaction.currencyCode ?? 'USD')}</td>
                    <td>
                      <Badge variant={transaction.status === 'COMPLETED' ? 'default' : 'secondary'}>
                        {transaction.status}
                      </Badge>
                    </td>
                    <td className='text-muted-foreground text-xs'>{transaction.createdAt ?? '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}

export function WalletPage() {
  const wallet = useBackend<WalletResponse>('/wallet/my-wallet');
  const history = useBackend<PageResponse<unknown>>('/wallet/my-wallet/transactions?size=10');
  const [cashOut, setCashOut] = useState({ branchId: '1', amount: '25', currency: 'USD' });

  async function requestCashOut(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const result = await apiClient('/wallet/cashout/request', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey() },
      body: JSON.stringify({
        branchId: Number(cashOut.branchId),
        amount: Number(cashOut.amount),
        currency: cashOut.currency
      })
    });
    toast.success('تم تسجيل طلب السحب النقدي');
    return result;
  }

  return (
    <PageContainer
      pageTitle='مكتب المحافظ'
      pageDescription='رصيد المحفظة، حدود KYC، حركة الحساب، وطلب السحب من الفرع.'
    >
      <div className='grid gap-4 xl:grid-cols-[360px_1fr]'>
        <div className='space-y-4'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>ملف المحفظة</h2>
            </div>
            <div className='space-y-3 p-4'>
              {wallet.error && <Badge variant='destructive'>تعذر جلب المحفظة</Badge>}
              <div>
                <div className='text-muted-foreground text-xs'>رقم المحفظة</div>
                <div className='font-semibold'>{wallet.data?.walletNumber ?? '-'}</div>
              </div>
              <div className='grid grid-cols-2 gap-3'>
                <div className='border-border rounded-sm border p-3'>
                  <div className='text-muted-foreground text-xs'>الحالة</div>
                  <div className='font-semibold'>{wallet.data?.status ?? '-'}</div>
                </div>
                <div className='border-border rounded-sm border p-3'>
                  <div className='text-muted-foreground text-xs'>مستوى KYC</div>
                  <div className='font-semibold'>{wallet.data?.kycTier ?? '-'}</div>
                </div>
              </div>
            </div>
          </div>

          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>طلب سحب نقدي</h2>
            </div>
            <form
              className='grid gap-3 p-4'
              onSubmit={(event) => {
                void requestCashOut(event);
              }}
            >
              <Field
                id='cashoutBranch'
                label='معرف الفرع'
                value={cashOut.branchId}
                onChange={(value) => setCashOut((current) => ({ ...current, branchId: value }))}
                type='number'
              />
              <div className='grid grid-cols-2 gap-3'>
                <Field
                  id='cashoutAmount'
                  label='المبلغ'
                  value={cashOut.amount}
                  onChange={(value) => setCashOut((current) => ({ ...current, amount: value }))}
                  type='number'
                />
                <Field
                  id='cashoutCurrency'
                  label='العملة'
                  value={cashOut.currency}
                  onChange={(value) => setCashOut((current) => ({ ...current, currency: value }))}
                />
              </div>
              <Button type='submit' className='h-9 rounded-sm'>
                تثبيت طلب السحب
              </Button>
            </form>
          </div>
        </div>

        <div className='space-y-4'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>الأرصدة حسب العملة</h2>
            </div>
            <div className='overflow-x-auto'>
              <table className='gov-table'>
                <thead>
                  <tr>
                    <th>العملة</th>
                    <th>المتاح</th>
                    <th>المجمد</th>
                  </tr>
                </thead>
                <tbody>
                  {(wallet.data?.balances ?? []).map((balance) => (
                    <tr key={balance.currencyCode}>
                      <td className='font-medium'>{balance.currencyCode}</td>
                      <td>{formatMoney(balance.availableBalance, balance.currencyCode)}</td>
                      <td>{formatMoney(balance.lockedBalance, balance.currencyCode)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>استجابة سجل الحركة</h2>
            </div>
            <div className='p-4'>
              <JsonPreview value={history.data} />
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}

export function LiquidityPage() {
  const alerts = useBackend<LiquidityAlert[]>('/liquidity/alerts');
  const funds = useBackend<Fund[]>('/funds');

  return (
    <PageContainer
      pageTitle='الفروع والسيولة'
      pageDescription='مراقبة النقد، الصناديق، والتنبيهات التشغيلية للفروع.'
    >
      <div className='grid gap-4 xl:grid-cols-[1fr_420px]'>
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>تنبيهات السيولة</h2>
          </div>
          <div className='overflow-x-auto'>
            <table className='gov-table'>
              <thead>
                <tr>
                  <th>الفرع</th>
                  <th>العملة</th>
                  <th>الشدة</th>
                  <th>الرسالة</th>
                </tr>
              </thead>
              <tbody>
                {(alerts.data ?? []).map((alert) => (
                  <tr key={alert.id}>
                    <td>{alert.branchId ?? '-'}</td>
                    <td>{alert.currency ?? '-'}</td>
                    <td>
                      <Badge variant={alert.severity === 'HIGH' ? 'destructive' : 'secondary'}>
                        {alert.severity ?? 'INFO'}
                      </Badge>
                    </td>
                    <td className='text-sm'>{alert.message ?? alert.status ?? 'تنبيه سيولة'}</td>
                  </tr>
                ))}
                {!alerts.loading && !alerts.data?.length && (
                  <tr>
                    <td colSpan={4} className='text-muted-foreground text-sm'>
                      لا توجد تنبيهات مفتوحة.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
        <div className='gov-panel rounded-md'>
          <div className='gov-panel-header rounded-t-md px-4 py-3'>
            <h2 className='text-sm font-semibold'>الصناديق المتاحة</h2>
          </div>
          <div className='divide-border divide-y'>
            {(funds.data ?? []).map((fund) => (
              <div key={fund.id} className='flex items-center justify-between gap-3 px-4 py-3'>
                <div>
                  <div className='font-medium'>{fund.name}</div>
                  <div className='text-muted-foreground text-xs'>{formatMoney(fund.balance)}</div>
                </div>
                <Badge>{fund.status}</Badge>
              </div>
            ))}
          </div>
        </div>
      </div>
    </PageContainer>
  );
}

export function AssistantPage() {
  const [message, setMessage] = useState('لخص حالة السيولة والحوالات اليوم.');
  const [answer, setAnswer] = useState<unknown>();

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const result = await apiClient('/ai/chat', {
      method: 'POST',
      body: JSON.stringify({ message })
    });
    setAnswer(result);
  }

  return (
    <PageContainer
      pageTitle='مساعد العمليات'
      pageDescription='واجهة مساعدة للموظف والمشرف، مرتبطة بنقطة /api/ai/chat.'
    >
      <div className='gov-panel rounded-md'>
        <div className='gov-panel-header rounded-t-md px-4 py-3'>
          <h2 className='text-sm font-semibold'>استعلام تشغيلي</h2>
        </div>
        <div className='grid gap-4 p-4 lg:grid-cols-[420px_1fr]'>
          <form
            className='space-y-3'
            onSubmit={(event) => {
              void submit(event);
            }}
          >
            <Label htmlFor='ai-message'>الرسالة</Label>
            <Textarea
              id='ai-message'
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              className='min-h-32 rounded-sm'
            />
            <Button type='submit' className='h-9 rounded-sm'>
              إرسال للمساعد
            </Button>
          </form>
          <JsonPreview value={answer} />
        </div>
      </div>
    </PageContainer>
  );
}

export function BackendFeaturePage({
  title,
  description,
  endpoint
}: {
  title: string;
  description: string;
  endpoint: string;
}) {
  const state = useBackend<unknown>(endpoint);
  return (
    <PageContainer pageTitle={title} pageDescription={description}>
      <div className='gov-panel rounded-md'>
        <div className='gov-panel-header rounded-t-md px-4 py-3'>
          <h2 className='text-sm font-semibold'>بيانات الخادم</h2>
        </div>
        <div className='space-y-3 p-4'>
          {state.error && <Badge variant='destructive'>تعذر جلب البيانات</Badge>}
          <JsonPreview value={state.data} />
        </div>
      </div>
    </PageContainer>
  );
}
