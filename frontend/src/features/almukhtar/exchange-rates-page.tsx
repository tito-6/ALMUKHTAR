'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { currencyFlag, currencyFlagUrl, currencyName, currencySymbol } from '@/lib/currencies';
import { CurrencyFlag } from '@/components/currency-flag';
import { apiClient } from '@/lib/api-client';
import { FormEvent, useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import type { ExchangeRate } from './types';
import type { SpRate } from '@/app/api/sp-rates/route';

type ConvertResult = {
  fromAmount?: number;
  toAmount?: number;
  fromCurrency?: string;
  toCurrency?: string;
  rate?: number;
  appliedAt?: string;
};

type RateMap = Record<string, number>;

const MAJOR_CURRENCIES = ['USD', 'EUR', 'GBP', 'SYP', 'TRY', 'SAR', 'AED', 'JOD'];

// ─── sp-today Live Rates Panel ────────────────────────────────────────────────

function SpTodayPanel() {
  const [rates, setRates] = useState<SpRate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastFetch, setLastFetch] = useState<string | null>(null);

  const fetchRates = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch('/api/sp-rates');
      const data = await res.json() as { rates?: SpRate[]; error?: string; cachedAt?: string };
      if (data.error && (!data.rates || data.rates.length === 0)) {
        setError(data.error);
      } else {
        setRates(data.rates ?? []);
        setLastFetch(data.cachedAt ?? new Date().toISOString());
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'فشل الاتصال');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void fetchRates(); }, [fetchRates]);

  const fmt = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });

  return (
    <div className='gov-panel rounded-md'>
      <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
        <div>
          <h2 className='text-sm font-semibold'>أسعار الليرة السورية — sp-today.com</h2>
          {lastFetch && (
            <p className='text-muted-foreground text-xs mt-0.5'>
              آخر تحديث: {new Date(lastFetch).toLocaleTimeString('ar-SY')}
            </p>
          )}
        </div>
        <div className='flex items-center gap-2'>
          {loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
          <Button
            variant='outline'
            size='sm'
            className='h-7 rounded-sm text-xs'
            onClick={() => { void fetchRates(); }}
            disabled={loading}
          >
            تحديث
          </Button>
        </div>
      </div>

      {error && (
        <div className='border-b border-amber-500/20 bg-amber-500/5 px-4 py-2 text-xs text-amber-700 dark:text-amber-400'>
          ⚠️ {error} — قد يكون الموقع غير متاح حالياً.
        </div>
      )}

      {rates.length > 0 ? (
        <div className='overflow-x-auto'>
          <table className='gov-table'>
            <thead>
              <tr>
                <th>العملة</th>
                <th>شراء (ل.س)</th>
                <th>مبيع (ل.س)</th>
                <th>التغير</th>
                <th>أعلى</th>
                <th>أدنى</th>
              </tr>
            </thead>
            <tbody>
              {rates.map((r) => (
                <tr key={r.code}>
                  <td>
                    <div className='flex items-center gap-2'>
                      {currencyFlagUrl(r.code)
                        ? <img src={currencyFlagUrl(r.code)!} alt={r.code} width={22} height={16} className='rounded-[2px] object-cover' />
                        : <span className='text-base'>{r.flag}</span>}
                      <div>
                        <div className='font-semibold text-sm'>{r.code}</div>
                        <div className='text-muted-foreground text-xs'>{r.nameAr}</div>
                      </div>
                    </div>
                  </td>
                  <td className='tabular-nums font-medium'>
                    {fmt.format(r.buy)} ل.س
                  </td>
                  <td className='tabular-nums font-medium'>
                    {fmt.format(r.sell)} ل.س
                  </td>
                  <td>
                    <span className={r.change >= 0 ? 'text-emerald-600' : 'text-destructive'}>
                      {r.change >= 0 ? '▲' : '▼'} {Math.abs(r.change).toFixed(2)}%
                    </span>
                  </td>
                  <td className='tabular-nums text-muted-foreground text-sm'>
                    {r.high > 0 ? fmt.format(r.high) : '-'}
                  </td>
                  <td className='tabular-nums text-muted-foreground text-sm'>
                    {r.low > 0 ? fmt.format(r.low) : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : !loading ? (
        <div className='px-4 py-8 text-center text-muted-foreground text-sm'>
          لا توجد بيانات متاحة من sp-today.com حالياً.
          <br />
          <span className='text-xs'>يرجى التحقق من الاتصال بالإنترنت أو المحاولة لاحقاً.</span>
        </div>
      ) : (
        <div className='px-4 py-8 text-center text-muted-foreground text-sm'>
          جاري تحميل الأسعار…
        </div>
      )}
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export function ExchangeRatesPage() {
  const [baseCurrency, setBaseCurrency] = useState('USD');
  const allRates = useBackend<RateMap>(`/exchange-rates/all/${baseCurrency}`);
  const [convert, setConvert] = useState({ from: 'USD', to: 'SYP', amount: '100' });
  const [result, setResult] = useState<ConvertResult | null>(null);
  const [pairFrom, setPairFrom] = useState('USD');
  const [pairTo, setPairTo] = useState('SYP');
  const pairRate = useBackend<ExchangeRate>(pairFrom && pairTo ? `/exchange-rates/${pairFrom}/${pairTo}` : null);

  async function doConvert(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    try {
      const res = await apiClient<ConvertResult>('/exchange-rates/convert', {
        method: 'POST',
        body: JSON.stringify({
          fromCurrency: convert.from,
          toCurrency: convert.to,
          amount: Number(convert.amount)
        })
      });
      setResult(res);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل التحويل');
    }
  }

  const rateEntries = Object.entries(allRates.data ?? {});
  const highlighted = rateEntries.filter(([cur]) => MAJOR_CURRENCIES.includes(cur));
  const rest = rateEntries.filter(([cur]) => !MAJOR_CURRENCIES.includes(cur));

  return (
    <PageContainer
      pageTitle='أسعار الصرف'
      pageDescription='أسعار الصرف الحية من sp-today.com، تحويل المبالغ، واستعلام أزواج العملات.'
    >
      <div className='space-y-6'>

        {/* ── sp-today Live Rates ── */}
        <SpTodayPanel />

        {/* ── System Exchange Rates (from backend) ── */}
        <div className='space-y-4'>
          <div className='flex items-center gap-3'>
            <Label className='text-xs whitespace-nowrap'>العملة الأساسية (النظام)</Label>
            <select
              value={baseCurrency}
              onChange={(e) => setBaseCurrency(e.target.value)}
              className='h-8 rounded-sm border border-border bg-background px-2 text-sm'
            >
              {MAJOR_CURRENCIES.map((c) => (
                <option key={c} value={c}>
                  {currencyFlag(c)} {c} — {currencyName(c)}
                </option>
              ))}
            </select>
          </div>

          <div className='grid gap-4 xl:grid-cols-[1fr_340px]'>
            {/* Rate Grid */}
            <div className='space-y-4'>
              <div className='gov-panel rounded-md'>
                <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
                  <h2 className='text-sm font-semibold flex items-center gap-1.5'>
                    أسعار النظام مقابل <CurrencyFlag code={baseCurrency} size={18} /> {baseCurrency}
                  </h2>
                  {allRates.loading && <Icons.spinner className='size-4 animate-spin text-muted-foreground' />}
                </div>
                {highlighted.length > 0 ? (
                  <>
                    <div className='grid gap-2 grid-cols-2 md:grid-cols-4 p-4'>
                      {highlighted.map(([cur, rate]) => (
                        <div key={cur} className='border-border rounded-sm border p-3'>
                          <div className='flex items-center gap-1.5 mb-1'>
                            <CurrencyFlag code={cur} size={20} />
                            <span className='text-xs font-bold'>{cur}</span>
                          </div>
                          <div className='text-lg font-semibold tabular-nums'>
                            {currencySymbol(cur)} {Number(rate).toFixed(4)}
                          </div>
                          <div className='text-muted-foreground text-xs mt-0.5'>{currencyName(cur)}</div>
                        </div>
                      ))}
                    </div>
                    {rest.length > 0 && (
                      <div className='overflow-x-auto px-4 pb-4'>
                        <table className='gov-table'>
                          <thead>
                            <tr><th>العملة</th><th>الاسم</th><th>السعر</th></tr>
                          </thead>
                          <tbody>
                            {rest.map(([cur, rate]) => (
                              <tr key={cur}>
                                <td>
                                  <span className='inline-flex items-center gap-1.5'>
                                    <CurrencyFlag code={cur} size={16} />
                                    <span className='font-medium'>{cur}</span>
                                  </span>
                                </td>
                                <td className='text-muted-foreground text-xs'>{currencyName(cur)}</td>
                                <td className='tabular-nums'>{Number(rate).toFixed(6)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </>
                ) : !allRates.loading ? (
                  <div className='px-4 py-6 text-center text-muted-foreground text-sm'>
                    لا توجد أسعار من النظام — سيتم عرضها عند توفرها من الخادم.
                  </div>
                ) : null}
              </div>
            </div>

            {/* Converter + Pair Query */}
            <div className='space-y-4'>
              <div className='gov-panel rounded-md'>
                <div className='gov-panel-header rounded-t-md px-4 py-3'>
                  <h2 className='text-sm font-semibold'>محوّل العملات</h2>
                </div>
                <form className='grid gap-3 p-4' onSubmit={(e) => { void doConvert(e); }}>
                  <div className='grid grid-cols-2 gap-2'>
                    <div className='space-y-1.5'>
                      <Label className='text-xs'>من</Label>
                      <div className='flex items-center gap-1.5'>
                        <CurrencyFlag code={convert.from} size={22} />
                        <Input
                          value={convert.from}
                          onChange={(e) => setConvert((c) => ({ ...c, from: e.target.value.toUpperCase() }))}
                          className='h-9 rounded-sm uppercase flex-1'
                          maxLength={4}
                        />
                      </div>
                    </div>
                    <div className='space-y-1.5'>
                      <Label className='text-xs'>إلى</Label>
                      <div className='flex items-center gap-1.5'>
                        <CurrencyFlag code={convert.to} size={22} />
                        <Input
                          value={convert.to}
                          onChange={(e) => setConvert((c) => ({ ...c, to: e.target.value.toUpperCase() }))}
                          className='h-9 rounded-sm uppercase flex-1'
                          maxLength={4}
                        />
                      </div>
                    </div>
                  </div>
                  <div className='space-y-1.5'>
                    <Label className='text-xs'>المبلغ</Label>
                    <Input
                      type='number'
                      value={convert.amount}
                      onChange={(e) => setConvert((c) => ({ ...c, amount: e.target.value }))}
                      className='h-9 rounded-sm'
                    />
                  </div>
                  <Button type='submit' className='h-9 rounded-sm'>
                    {currencyFlag(convert.from)} ← → {currencyFlag(convert.to)} تحويل
                  </Button>
                  {result && (
                    <div className='bg-muted/50 rounded-sm p-3 text-sm space-y-1'>
                      <div className='flex justify-between'>
                        <span className='text-muted-foreground'>النتيجة</span>
                        <span className='font-semibold tabular-nums'>
                          {currencyFlag(result.toCurrency ?? '')} {result.toAmount?.toFixed(2)} {result.toCurrency}
                        </span>
                      </div>
                      <div className='flex justify-between text-xs text-muted-foreground'>
                        <span>السعر المطبق</span>
                        <span>{result.rate?.toFixed(6)}</span>
                      </div>
                    </div>
                  )}
                </form>
              </div>

              <div className='gov-panel rounded-md'>
                <div className='gov-panel-header rounded-t-md px-4 py-3'>
                  <h2 className='text-sm font-semibold'>استعلام سعر محدد</h2>
                </div>
                <div className='p-4 space-y-3'>
                  <div className='grid grid-cols-2 gap-2'>
                    <div className='space-y-1.5'>
                      <Label className='text-xs'>من</Label>
                      <div className='flex items-center gap-1.5'>
                        <span className='text-base'>{currencyFlag(pairFrom)}</span>
                        <Input
                          value={pairFrom}
                          onChange={(e) => setPairFrom(e.target.value.toUpperCase())}
                          className='h-9 rounded-sm flex-1'
                          maxLength={4}
                        />
                      </div>
                    </div>
                    <div className='space-y-1.5'>
                      <Label className='text-xs'>إلى</Label>
                      <div className='flex items-center gap-1.5'>
                        <span className='text-base'>{currencyFlag(pairTo)}</span>
                        <Input
                          value={pairTo}
                          onChange={(e) => setPairTo(e.target.value.toUpperCase())}
                          className='h-9 rounded-sm flex-1'
                          maxLength={4}
                        />
                      </div>
                    </div>
                  </div>
                  {pairRate.loading && (
                    <div className='flex justify-center py-2'>
                      <Icons.spinner className='size-4 animate-spin text-muted-foreground' />
                    </div>
                  )}
                  {pairRate.data && (
                    <div className='bg-muted/50 rounded-sm p-3 text-sm space-y-1.5'>
                      <div className='flex items-center justify-between'>
                        <span className='text-muted-foreground'>
                          1 {currencyFlag(pairFrom)} {pairFrom} =
                        </span>
                        <span className='font-semibold tabular-nums'>
                          {pairRate.data.rate?.toFixed(6)} {currencyFlag(pairTo)} {pairTo}
                        </span>
                      </div>
                      <div className='flex justify-between text-xs text-muted-foreground'>
                        <span>آخر تحديث</span>
                        <span>{pairRate.data.updatedAt ?? '-'}</span>
                      </div>
                    </div>
                  )}
                  {pairRate.error && (
                    <Badge variant='destructive' className='text-xs'>
                      الزوج غير متاح في النظام
                    </Badge>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
