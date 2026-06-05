'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api-client';
import { currencyFlag, currencyName, currencySymbol } from '@/lib/currencies';
import { CurrencyFlag } from '@/components/currency-flag';
import Link from 'next/link';
import { useSession } from './session';
import { useBackend } from './use-backend';
import type {
  AmlAlert,
  DashboardSnapshot,
  Fund,
  LiquidityAlert,
  PageResponse,
  Transaction,
  WalletResponse,
  WalletTransaction
} from './types';

const numberFormatter = new Intl.NumberFormat('ar-SY');
const moneyFormatter = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });

function formatMoney(value?: number | string, currency = 'USD') {
  return `${moneyFormatter.format(Number(value ?? 0))} ${currency}`;
}

function MetricCell({
  label,
  value,
  detail,
  tone = 'default',
  icon
}: {
  label: string;
  value: string;
  detail: string;
  tone?: 'default' | 'good' | 'warn' | 'danger';
  icon?: React.ReactNode;
}) {
  const toneClass =
    tone === 'good'
      ? 'border-emerald-700/20 bg-emerald-500/5'
      : tone === 'warn'
        ? 'border-amber-700/25 bg-amber-500/10'
        : tone === 'danger'
          ? 'border-destructive/25 bg-destructive/5'
          : '';
  return (
    <div className={`gov-panel rounded-md p-3 ${toneClass}`}>
      <div className='flex items-center justify-between mb-2'>
        <div className='text-muted-foreground text-xs font-medium'>{label}</div>
        {icon && <div className='text-muted-foreground'>{icon}</div>}
      </div>
      <div className='text-2xl font-semibold tabular-nums'>{value}</div>
      <div className='text-muted-foreground mt-1 text-xs'>{detail}</div>
    </div>
  );
}

function ErrorLabel({ error }: { error?: string }) {
  if (!error) return null;
  return (
    <div className='border-destructive/30 bg-destructive/10 text-destructive rounded-sm border px-2 py-1 text-xs'>
      تعذر جلب البيانات
    </div>
  );
}

function severityVariant(s?: string): 'default' | 'secondary' | 'destructive' | 'outline' {
  if (s === 'HIGH' || s === 'CRITICAL') return 'destructive';
  if (s === 'MEDIUM') return 'secondary';
  return 'outline';
}

export default function AlmukhtarOverviewPage() {
  const { session, ready } = useSession();
  const wallet = useBackend<WalletResponse>(session ? '/wallet/my-wallet' : null);
  const walletTransactions = useBackend<PageResponse<WalletTransaction>>(
    session ? '/wallet/my-wallet/transactions?size=6' : null
  );
  const funds = useBackend<Fund[]>(session ? '/funds' : null);
  const transactions = useBackend<PageResponse<Transaction> | Transaction[]>(session ? '/transactions?size=20' : null);
  const liquidityAlerts = useBackend<LiquidityAlert[]>(session ? '/liquidity/alerts' : null);
  const amlAlerts = useBackend<AmlAlert[]>(session ? '/aml/alerts' : null);
  const owner = useBackend<DashboardSnapshot>(session ? '/owner/dashboard' : null);

  const balances = wallet.data?.balances ?? [];
  const primaryBalance = balances[0];
  const totalFunds = funds.data?.reduce((sum, f) => sum + Number(f.balance || 0), 0) ?? 0;

  const txList: Transaction[] = Array.isArray(transactions.data)
    ? transactions.data
    : (transactions.data as PageResponse<Transaction>)?.content ?? [];

  const completedCount = txList.filter((t) => t.status === 'COMPLETED' || t.status === 'RELEASED').length;
  const pendingCount = txList.filter((t) => t.status === 'PENDING' || t.status === 'READY_FOR_PICKUP').length;
  const openAlerts = liquidityAlerts.data?.length ?? 0;
  const openAmlAlerts = amlAlerts.data?.length ?? 0;

  async function pingBackend() {
    await apiClient('/exchange-rates/health');
  }

  if (ready && !session) {
    return (
      <PageContainer>
        <div className='flex min-h-[55vh] items-center justify-center'>
          <div className='gov-panel w-full max-w-md rounded-md p-5 text-right'>
            <div className='text-lg font-semibold'>يلزم تسجيل الدخول</div>
            <p className='text-muted-foreground mt-2 text-sm'>
              افتح جلسة تشغيل للوصول إلى مكتب العمليات وربط الواجهة بالخادم المحلي.
            </p>
            <Button asChild className='mt-5 w-full'>
              <Link href='/auth/sign-in'>فتح صفحة الدخول</Link>
            </Button>
          </div>
        </div>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      pageTitle='مركز قيادة العمليات'
      pageDescription='قراءة تشغيلية مختصرة للحوالات، المحافظ، الصناديق، السيولة، والتنبيهات.'
      pageHeaderAction={
        <Button
          variant='outline'
          size='sm'
          onClick={() => { void pingBackend(); }}
        >
          <Icons.check className='me-1.5 size-4' />
          فحص الاتصال
        </Button>
      }
    >
      <div className='space-y-4'>
        {/* ── Metrics Grid ── */}
        <section className='grid gap-3 md:grid-cols-2 xl:grid-cols-4'>
          <MetricCell
            label='رصيد المحفظة الأساسية'
            value={formatMoney(primaryBalance?.availableBalance, primaryBalance?.currencyCode)}
            detail={`الحالة: ${wallet.data?.status ?? '—'} | KYC: ${wallet.data?.kycTier ?? '—'}`}
            tone='good'
            icon={<Icons.creditCard className='size-4' />}
          />
          <MetricCell
            label='إجمالي الصناديق التشغيلية'
            value={formatMoney(totalFunds)}
            detail={`${numberFormatter.format(funds.data?.length ?? 0)} صندوق نشط`}
            icon={<Icons.billing className='size-4' />}
          />
          <MetricCell
            label='حوالات معلقة / مكتملة'
            value={`${numberFormatter.format(pendingCount)} / ${numberFormatter.format(completedCount)}`}
            detail={`من أصل ${numberFormatter.format(txList.length)} حوالة مقروءة`}
            tone={pendingCount > 10 ? 'warn' : 'default'}
            icon={<Icons.send className='size-4' />}
          />
          <MetricCell
            label='تنبيهات السيولة / AML'
            value={`${numberFormatter.format(openAlerts)} / ${numberFormatter.format(openAmlAlerts)}`}
            detail='حدود النقد | مشبوهات غسل أموال'
            tone={openAlerts > 0 || openAmlAlerts > 0 ? 'warn' : 'good'}
            icon={<Icons.warning className='size-4' />}
          />
        </section>

        {/* ── Wallet Balances + Recent Movements ── */}
        <section className='grid gap-4 xl:grid-cols-[1.15fr_0.85fr]'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <div>
                <h2 className='text-sm font-semibold'>مراقبة أرصدة المحفظة</h2>
                <p className='text-muted-foreground text-xs'>الأرصدة المتاحة والمجمدة حسب العملة</p>
              </div>
              <ErrorLabel error={wallet.error} />
            </div>
            <div className='overflow-x-auto'>
              <table className='gov-table'>
                <thead>
                  <tr>
                    <th>العملة</th>
                    <th>المتاح</th>
                    <th>المجمد</th>
                    <th>الحالة</th>
                  </tr>
                </thead>
                <tbody>
                  {balances.map((b) => (
                    <tr key={b.currencyCode}>
                      <td className='font-medium'>
                        <span className='inline-flex items-center gap-1.5'>
                          <CurrencyFlag code={b.currencyCode} size={18} />
                          <span>{b.currencyCode}</span>
                          <span className='text-muted-foreground text-xs hidden sm:inline'>{currencyName(b.currencyCode)}</span>
                        </span>
                      </td>
                      <td className='tabular-nums'>{currencySymbol(b.currencyCode)} {moneyFormatter.format(Number(b.availableBalance ?? 0))}</td>
                      <td className='tabular-nums'>{currencySymbol(b.currencyCode)} {moneyFormatter.format(Number(b.lockedBalance ?? 0))}</td>
                      <td>
                        <Badge variant={Number(b.lockedBalance) > 0 ? 'secondary' : 'outline'}>
                          {Number(b.lockedBalance) > 0 ? 'قيد مراجعة' : 'سليم'}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                  {!balances.length && (
                    <tr>
                      <td colSpan={4} className='text-muted-foreground text-sm py-4'>
                        لا توجد أرصدة لهذا الحساب.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <div>
                <h2 className='text-sm font-semibold'>آخر حركات المحفظة</h2>
                <p className='text-muted-foreground text-xs'>سجل مختصر للحركة المالية</p>
              </div>
              <ErrorLabel error={walletTransactions.error} />
            </div>
            <div className='divide-border divide-y'>
              {(walletTransactions.data?.content ?? []).map((item) => (
                <div key={item.id} className='flex items-center justify-between gap-3 px-4 py-3'>
                  <div>
                    <div className='font-medium text-sm'>{item.type}</div>
                    <div className='text-muted-foreground text-xs'>{item.createdAt ?? 'بدون تاريخ'}</div>
                  </div>
                  <Badge variant='outline'>{formatMoney(item.amount, item.currencyCode)}</Badge>
                </div>
              ))}
              {!walletTransactions.loading && !walletTransactions.data?.content?.length && (
                <div className='text-muted-foreground px-4 py-5 text-sm'>لا توجد حركات حديثة.</div>
              )}
            </div>
          </div>
        </section>

        {/* ── Funds + Owner ── */}
        <section className='grid gap-4 lg:grid-cols-[1fr_360px]'>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <div>
                <h2 className='text-sm font-semibold'>الصناديق التشغيلية</h2>
                <p className='text-muted-foreground text-xs'>صورة مالية سريعة للصناديق النشطة</p>
              </div>
              <ErrorLabel error={funds.error} />
            </div>
            <div className='overflow-x-auto'>
              <table className='gov-table'>
                <thead>
                  <tr>
                    <th>الصندوق</th>
                    <th>الرصيد</th>
                    <th>الحالة</th>
                  </tr>
                </thead>
                <tbody>
                  {(funds.data ?? []).map((fund) => (
                    <tr key={fund.id}>
                      <td className='font-medium'>{fund.name}</td>
                      <td className='tabular-nums'>{formatMoney(fund.balance)}</td>
                      <td>
                        <Badge variant={fund.status === 'ACTIVE' ? 'default' : 'secondary'}>
                          {fund.status}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                  {!funds.data?.length && (
                    <tr>
                      <td colSpan={3} className='text-muted-foreground text-sm py-4'>لا توجد صناديق.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header rounded-t-md px-4 py-3'>
              <h2 className='text-sm font-semibold'>مؤشرات المالك</h2>
              <p className='text-muted-foreground text-xs'>تظهر حسب صلاحية الحساب</p>
            </div>
            <div className='space-y-2 p-4'>
              <ErrorLabel error={owner.error} />
              {Object.entries(owner.data ?? {})
                .slice(0, 8)
                .map(([key, value]) => (
                  <div key={key} className='flex items-center justify-between gap-3 text-sm'>
                    <span className='text-muted-foreground text-xs'>{key}</span>
                    <span className='font-medium text-xs tabular-nums'>{String(value)}</span>
                  </div>
                ))}
              {!owner.loading && !owner.data && (
                <p className='text-muted-foreground text-sm'>مخصصة لحسابات الإدارة العليا.</p>
              )}
            </div>
          </div>
        </section>

        {/* ── AML Alerts + Liquidity Alerts ── */}
        <section className='grid gap-4 xl:grid-cols-2'>
          {/* AML Alerts */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <div>
                <h2 className='text-sm font-semibold'>تنبيهات AML المفتوحة</h2>
                <p className='text-muted-foreground text-xs'>نظام مكافحة غسل الأموال</p>
              </div>
              {openAmlAlerts > 0 && (
                <Badge variant='destructive' className='text-xs'>
                  {numberFormatter.format(openAmlAlerts)} تنبيه
                </Badge>
              )}
            </div>
            <div className='overflow-x-auto max-h-64 overflow-y-auto'>
              <table className='gov-table'>
                <thead>
                  <tr>
                    <th>الرقم</th>
                    <th>نوع التنبيه</th>
                    <th>الشدة</th>
                    <th>الحالة</th>
                    <th>التاريخ</th>
                  </tr>
                </thead>
                <tbody>
                  {(amlAlerts.data ?? []).map((alert) => (
                    <tr key={alert.id} className='hover:bg-muted/20 transition-colors'>
                      <td className='font-mono text-xs'>#{alert.id}</td>
                      <td className='text-xs'>{alert.alertType ?? alert.ruleTriggered ?? '—'}</td>
                      <td>
                        <Badge variant={severityVariant(alert.severity)} className='text-xs'>
                          {alert.severity ?? 'INFO'}
                        </Badge>
                      </td>
                      <td>
                        <Badge variant='outline' className='text-xs'>{alert.status ?? '—'}</Badge>
                      </td>
                      <td className='text-muted-foreground text-xs'>{alert.createdAt ?? '—'}</td>
                    </tr>
                  ))}
                  {!amlAlerts.loading && !amlAlerts.data?.length && (
                    <tr>
                      <td colSpan={5} className='text-center text-muted-foreground text-sm py-4'>
                        {amlAlerts.error ? 'تعذر جلب تنبيهات AML' : 'لا توجد تنبيهات مفتوحة.'}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Liquidity Alerts */}
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <div>
                <h2 className='text-sm font-semibold'>إنذارات السيولة</h2>
                <p className='text-muted-foreground text-xs'>حدود النقد والفروع والعملات</p>
              </div>
              {openAlerts > 0 && (
                <Badge variant='secondary' className='text-xs text-amber-600'>
                  {numberFormatter.format(openAlerts)} تنبيه
                </Badge>
              )}
            </div>
            <div className='overflow-x-auto max-h-64 overflow-y-auto'>
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
                  {(liquidityAlerts.data ?? []).map((alert) => (
                    <tr key={alert.id} className='hover:bg-muted/20 transition-colors'>
                      <td>{alert.branchId ?? '—'}</td>
                      <td>{alert.currency ?? '—'}</td>
                      <td>
                        <Badge variant={severityVariant(alert.severity)} className='text-xs'>
                          {alert.severity ?? 'INFO'}
                        </Badge>
                      </td>
                      <td className='text-xs max-w-[200px] truncate'>
                        {alert.message ?? alert.status ?? '—'}
                      </td>
                    </tr>
                  ))}
                  {!liquidityAlerts.loading && !liquidityAlerts.data?.length && (
                    <tr>
                      <td colSpan={4} className='text-center text-muted-foreground text-sm py-4'>
                        {liquidityAlerts.error ? 'تعذر جلب التنبيهات' : 'لا توجد تنبيهات مفتوحة.'}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>

        {/* ── Recent Transactions ── */}
        <section>
          <div className='gov-panel rounded-md'>
            <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
              <div>
                <h2 className='text-sm font-semibold'>آخر الحوالات</h2>
                <p className='text-muted-foreground text-xs'>
                  {numberFormatter.format(txList.length)} حوالة | {numberFormatter.format(completedCount)} مكتملة
                </p>
              </div>
              <ErrorLabel error={transactions.error} />
            </div>
            <div className='overflow-x-auto'>
              <table className='gov-table'>
                <thead>
                  <tr>
                    <th>الرقم</th>
                    <th>المبلغ</th>
                    <th>العملة</th>
                    <th>الحالة</th>
                    <th>التاريخ</th>
                  </tr>
                </thead>
                <tbody>
                  {txList.slice(0, 10).map((tx) => (
                    <tr key={tx.id} className='hover:bg-muted/20 transition-colors'>
                      <td className='font-mono text-xs'>#{tx.id}</td>
                      <td className='tabular-nums'>{formatMoney(tx.amount, tx.currencyCode ?? 'USD')}</td>
                      <td>
                        <span className='inline-flex items-center gap-1'>
                          <CurrencyFlag code={tx.currencyCode ?? 'USD'} size={16} />
                          <span className='text-xs'>{tx.currencyCode ?? '—'}</span>
                        </span>
                      </td>
                      <td>
                        <Badge
                          variant={
                            tx.status === 'COMPLETED' || tx.status === 'RELEASED'
                              ? 'default'
                              : tx.status === 'FAILED'
                                ? 'destructive'
                                : 'secondary'
                          }
                          className='text-xs'
                        >
                          {tx.status}
                        </Badge>
                      </td>
                      <td className='text-muted-foreground text-xs'>{tx.createdAt ?? '—'}</td>
                    </tr>
                  ))}
                  {!txList.length && (
                    <tr>
                      <td colSpan={5} className='text-muted-foreground text-sm text-center py-4'>
                        لا توجد حوالات مسجّلة.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>
    </PageContainer>
  );
}
