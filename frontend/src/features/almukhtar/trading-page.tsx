'use client';

import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { apiClient } from '@/lib/api-client';
import type { YFChartData, YFQuote } from '@/app/api/market/route';
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState
} from 'react';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import { useSession } from './session';
import type {
  PriceAlert,
  TradableAsset,
  TradingAccount,
  TradingOrder,
  TradingPosition
} from './types';

// ─── Helpers ──────────────────────────────────────────────────────────────────

function idempotencyKey() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
}

const NUM = new Intl.NumberFormat('en-US', { maximumFractionDigits: 4 });
const PRICE = (v?: number) =>
  v == null ? '—' : v < 1 ? v.toFixed(4) : v.toFixed(2);

function pct(v?: number) {
  if (v == null) return '—';
  const sign = v >= 0 ? '+' : '';
  return `${sign}${v.toFixed(2)}%`;
}

function compactNum(n?: number): string {
  if (n == null) return '—';
  if (n >= 1e12) return `${(n / 1e12).toFixed(2)}T`;
  if (n >= 1e9) return `${(n / 1e9).toFixed(2)}B`;
  if (n >= 1e6) return `${(n / 1e6).toFixed(2)}M`;
  if (n >= 1e3) return `${(n / 1e3).toFixed(1)}K`;
  return String(n);
}

function changeClass(v?: number) {
  if (v == null || v === 0) return 'text-muted-foreground';
  return v > 0 ? 'text-emerald-500' : 'text-red-500';
}

function changeBg(v?: number) {
  if (v == null || v === 0) return '';
  return v > 0 ? 'bg-emerald-500/10' : 'bg-red-500/10';
}

// ─── Market Data Hooks ────────────────────────────────────────────────────────

function useMarketData<T>(params: string, refreshInterval = 60_000) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [fetchedAt, setFetchedAt] = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetch_ = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(`/api/market?${params}`);
      const json = (await res.json()) as { data?: T; error?: string; fetchedAt?: string };
      if (json.error && !json.data) {
        setError(json.error);
      } else {
        setData((json.data as T) ?? null);
        setFetchedAt(json.fetchedAt ?? null);
        setError(null);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Fetch failed');
    } finally {
      setLoading(false);
    }
  }, [params]);

  useEffect(() => {
    void fetch_();
    timerRef.current = setInterval(() => { void fetch_(); }, refreshInterval);
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [fetch_, refreshInterval]);

  return { data, loading, error, fetchedAt, refetch: fetch_ };
}

// ─── Sparkline SVG ────────────────────────────────────────────────────────────

function Sparkline({ closes, positive }: { closes: number[]; positive: boolean }) {
  if (!closes.length) return null;
  const w = 80, h = 28;
  const min = Math.min(...closes);
  const max = Math.max(...closes);
  const range = max - min || 1;
  const pts = closes
    .map((v, i) => {
      const x = (i / (closes.length - 1)) * w;
      const y = h - ((v - min) / range) * h;
      return `${x},${y}`;
    })
    .join(' ');
  return (
    <svg width={w} height={h} viewBox={`0 0 ${w} ${h}`} className='overflow-visible'>
      <polyline
        points={pts}
        fill='none'
        stroke={positive ? '#10b981' : '#ef4444'}
        strokeWidth='1.5'
        strokeLinejoin='round'
        strokeLinecap='round'
      />
    </svg>
  );
}

// ─── Futures Ticker Bar ───────────────────────────────────────────────────────

const FUTURES_LABELS: Record<string, string> = {
  'ES=F': 'S&P 500', 'YM=F': 'Dow Jones', 'NQ=F': 'Nasdaq',
  'RTY=F': 'Russell', '^VIX': 'VIX', 'GC=F': 'Gold', 'CL=F': 'Oil'
};

function FuturesBar() {
  const { data, loading, refetch, fetchedAt } = useMarketData<YFQuote[]>('type=futures', 90_000);
  const quotes = data ?? [];

  return (
    <div className='flex items-center gap-0 overflow-x-auto border-b border-border bg-card/50 px-2 py-1.5 text-xs'>
      {loading && !quotes.length && (
        <div className='flex items-center gap-1.5 text-muted-foreground px-2'>
          <Icons.spinner className='size-3 animate-spin' />
          تحميل الأسواق...
        </div>
      )}
      {quotes.map((q) => {
        const up = (q.regularMarketChange ?? 0) >= 0;
        return (
          <div
            key={q.symbol}
            className={`flex items-center gap-2 border-r border-border px-3 py-0.5 shrink-0 ${changeBg(q.regularMarketChange)}`}
          >
            <span className='font-semibold text-foreground/80'>
              {FUTURES_LABELS[q.symbol] ?? q.symbol}
            </span>
            <span className='tabular-nums font-mono font-medium'>
              {PRICE(q.regularMarketPrice)}
            </span>
            <span className={`tabular-nums ${changeClass(q.regularMarketChange)}`}>
              {up ? '▲' : '▼'} {pct(q.regularMarketChangePercent)}
            </span>
          </div>
        );
      })}
      <div className='ms-auto flex items-center gap-2 px-3 text-muted-foreground'>
        {fetchedAt && (
          <span className='tabular-nums text-[10px]'>
            {new Date(fetchedAt).toLocaleTimeString('ar-SY')}
          </span>
        )}
        <button
          type='button'
          onClick={() => { void refetch(); }}
          className='rounded p-0.5 hover:text-foreground transition-colors'
          title='تحديث'
        >
          <Icons.refresh className='size-3' />
        </button>
      </div>
    </div>
  );
}

// ─── Stock Row ────────────────────────────────────────────────────────────────

function StockRow({
  q,
  selected,
  chart,
  onClick
}: {
  q: YFQuote;
  selected: boolean;
  chart?: number[];
  onClick: () => void;
}) {
  const up = (q.regularMarketChangePercent ?? 0) >= 0;
  return (
    <tr
      className={`cursor-pointer transition-colors hover:bg-muted/40 ${
        selected ? 'bg-primary/5 border-l-2 border-primary' : ''
      }`}
      onClick={onClick}
    >
      <td className='font-bold text-sm py-2.5'>
        <div>{q.symbol}</div>
        <div className='text-[10px] text-muted-foreground font-normal truncate max-w-[120px]'>
          {q.shortName ?? q.longName ?? ''}
        </div>
      </td>
      <td className='tabular-nums font-mono text-sm font-semibold text-right'>
        {PRICE(q.regularMarketPrice)}
        <div className='text-[10px] text-muted-foreground'>{q.currency ?? 'USD'}</div>
      </td>
      <td className={`tabular-nums text-right font-medium ${changeClass(q.regularMarketChangePercent)}`}>
        {up ? '+' : ''}{PRICE(q.regularMarketChange)}
        <div className='text-[10px]'>{pct(q.regularMarketChangePercent)}</div>
      </td>
      <td className='text-right text-muted-foreground text-xs hidden md:table-cell'>
        {compactNum(q.regularMarketVolume)}
        <div className='text-[10px]'>avg {compactNum(q.averageDailyVolume3Month)}</div>
      </td>
      <td className='text-right text-xs hidden lg:table-cell'>
        {compactNum(q.marketCap)}
      </td>
      <td className='hidden xl:table-cell'>
        {chart && (
          <div className='flex justify-end'>
            <Sparkline closes={chart} positive={up} />
          </div>
        )}
      </td>
    </tr>
  );
}

// ─── Mini Chart (full detail panel) ──────────────────────────────────────────

function DetailChart({ symbol }: { symbol: string }) {
  const [range, setRange] = useState('5d');
  const params = useMemo(
    () => `type=chart&symbol=${symbol}&range=${range}&interval=${range === '1d' ? '5m' : '1d'}`,
    [symbol, range]
  );
  const { data, loading } = useMarketData<YFChartData>(params, 120_000);
  const closes = data?.closes ?? [];
  const min = closes.length ? Math.min(...closes) : 0;
  const max = closes.length ? Math.max(...closes) : 1;
  const range_ = max - min || 1;
  const up = closes.length > 1 && closes[closes.length - 1] >= closes[0];
  const W = 420, H = 100;

  const polyPts = closes
    .map((v, i) => `${(i / (closes.length - 1)) * W},${H - ((v - min) / range_) * H}`)
    .join(' ');

  const ranges = ['1d', '5d', '1mo', '3mo', '1y'];

  return (
    <div>
      <div className='flex items-center gap-1 mb-2'>
        {ranges.map((r) => (
          <button
            key={r}
            type='button'
            onClick={() => setRange(r)}
            className={`rounded px-2 py-0.5 text-[10px] font-medium transition-colors ${
              range === r ? 'bg-foreground text-background' : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            {r}
          </button>
        ))}
      </div>
      <div className='relative h-[100px] w-full overflow-hidden rounded-sm bg-muted/20 border border-border'>
        {loading && (
          <div className='absolute inset-0 flex items-center justify-center'>
            <Icons.spinner className='size-4 animate-spin text-muted-foreground' />
          </div>
        )}
        {closes.length > 1 && !loading && (
          <svg viewBox={`0 0 ${W} ${H}`} className='h-full w-full' preserveAspectRatio='none'>
            <defs>
              <linearGradient id={`grad-${symbol}`} x1='0' x2='0' y1='0' y2='1'>
                <stop offset='0%' stopColor={up ? '#10b981' : '#ef4444'} stopOpacity='0.3' />
                <stop offset='100%' stopColor={up ? '#10b981' : '#ef4444'} stopOpacity='0' />
              </linearGradient>
            </defs>
            <polygon
              points={`0,${H} ${polyPts} ${W},${H}`}
              fill={`url(#grad-${symbol})`}
            />
            <polyline
              points={polyPts}
              fill='none'
              stroke={up ? '#10b981' : '#ef4444'}
              strokeWidth='1.8'
              strokeLinejoin='round'
              strokeLinecap='round'
            />
          </svg>
        )}
        {/* Min/Max labels */}
        {closes.length > 1 && !loading && (
          <>
            <span className='absolute bottom-0.5 right-1.5 text-[9px] text-muted-foreground tabular-nums'>
              {PRICE(min)}
            </span>
            <span className='absolute top-0.5 right-1.5 text-[9px] text-muted-foreground tabular-nums'>
              {PRICE(max)}
            </span>
          </>
        )}
      </div>
    </div>
  );
}

// ─── Stock Detail Panel ───────────────────────────────────────────────────────

function StockDetailPanel({
  quote,
  onBuy,
  onClose
}: {
  quote: YFQuote;
  onBuy: (side: 'BUY' | 'SELL', qty: number, price: number | undefined, type: string) => void;
  onClose: () => void;
}) {
  const [qty, setQty] = useState('1');
  const [orderType, setOrderType] = useState('MARKET');
  const [limitPrice, setLimitPrice] = useState(PRICE(quote.regularMarketPrice));
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
  const up = (quote.regularMarketChangePercent ?? 0) >= 0;

  const price = quote.regularMarketPrice ?? 0;
  const totalEst = Number(qty) * (orderType === 'MARKET' ? price : Number(limitPrice));

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    onBuy(
      side,
      Number(qty),
      orderType !== 'MARKET' ? Number(limitPrice) : undefined,
      orderType
    );
  }

  return (
    <div className='gov-panel rounded-md flex flex-col h-full'>
      {/* Header */}
      <div className='gov-panel-header rounded-t-md px-4 py-3 flex items-start justify-between'>
        <div>
          <div className='flex items-center gap-2'>
            <span className='text-lg font-bold'>{quote.symbol}</span>
            <Badge variant={up ? 'default' : 'destructive'} className='text-xs'>
              {pct(quote.regularMarketChangePercent)}
            </Badge>
          </div>
          <div className='text-xs text-muted-foreground mt-0.5'>
            {quote.shortName ?? quote.longName ?? ''}
          </div>
        </div>
        <button type='button' onClick={onClose} className='p-1 hover:opacity-70'>
          <Icons.close className='size-4' />
        </button>
      </div>

      <div className='flex-1 overflow-y-auto p-4 space-y-4'>
        {/* Price */}
        <div>
          <div className='text-3xl font-bold tabular-nums'>
            {PRICE(quote.regularMarketPrice)}
            <span className='text-sm text-muted-foreground ms-1'>{quote.currency ?? 'USD'}</span>
          </div>
          <div className={`text-sm font-medium tabular-nums ${changeClass(quote.regularMarketChangePercent)}`}>
            {up ? '▲' : '▼'} {PRICE(quote.regularMarketChange)} ({pct(quote.regularMarketChangePercent)})
          </div>
        </div>

        {/* Chart */}
        <DetailChart symbol={quote.symbol} />

        {/* Stats Grid */}
        <div className='grid grid-cols-2 gap-2 text-xs'>
          {[
            ['افتتاح', PRICE(quote.regularMarketOpen)],
            ['أعلى', PRICE(quote.regularMarketDayHigh)],
            ['أدنى', PRICE(quote.regularMarketDayLow)],
            ['الإغلاق السابق', PRICE(quote.regularMarketPreviousClose)],
            ['حجم التداول', compactNum(quote.regularMarketVolume)],
            ['متوسط الحجم', compactNum(quote.averageDailyVolume3Month)],
            ['القيمة السوقية', compactNum(quote.marketCap)],
            ['نسبة P/E', quote.trailingPE != null ? NUM.format(quote.trailingPE) : '—'],
            ['أعلى 52 أسبوع', PRICE(quote.fiftyTwoWeekHigh)],
            ['أدنى 52 أسبوع', PRICE(quote.fiftyTwoWeekLow)]
          ].map(([label, value]) => (
            <div key={label} className='flex justify-between border-b border-border/50 pb-1.5'>
              <span className='text-muted-foreground'>{label}</span>
              <span className='tabular-nums font-medium'>{value}</span>
            </div>
          ))}
        </div>

        {/* 52-week range bar */}
        {quote.fiftyTwoWeekLow != null && quote.fiftyTwoWeekHigh != null && (
          <div>
            <div className='flex justify-between text-[10px] text-muted-foreground mb-1'>
              <span>{PRICE(quote.fiftyTwoWeekLow)}</span>
              <span className='text-[10px]'>نطاق 52 أسبوع</span>
              <span>{PRICE(quote.fiftyTwoWeekHigh)}</span>
            </div>
            <div className='h-1.5 bg-muted rounded-full overflow-hidden'>
              <div
                className='h-full bg-primary rounded-full'
                style={{
                  width: `${Math.max(
                    2,
                    ((( quote.regularMarketPrice ?? 0) - quote.fiftyTwoWeekLow) /
                      (quote.fiftyTwoWeekHigh - quote.fiftyTwoWeekLow)) * 100
                  )}%`
                }}
              />
            </div>
          </div>
        )}

        {/* ── Trade Form ── */}
        <div className='border-t border-border pt-3'>
          <h3 className='text-xs font-semibold mb-3 text-foreground/70'>تقديم أمر تداول</h3>
          <form onSubmit={(e) => { handleSubmit(e); }} className='space-y-3'>
            {/* BUY / SELL */}
            <div className='grid grid-cols-2 gap-1'>
              {(['BUY', 'SELL'] as const).map((s) => (
                <button
                  key={s}
                  type='button'
                  onClick={() => setSide(s)}
                  className={`h-9 rounded-sm text-sm font-semibold transition-colors ${
                    side === s
                      ? s === 'BUY'
                        ? 'bg-emerald-600 text-white'
                        : 'bg-red-600 text-white'
                      : 'border border-border hover:border-foreground/40 text-muted-foreground'
                  }`}
                >
                  {s === 'BUY' ? '▲ شراء' : '▼ بيع'}
                </button>
              ))}
            </div>

            <div className='grid grid-cols-2 gap-2'>
              <div className='space-y-1'>
                <Label className='text-xs'>نوع الأمر</Label>
                <Select value={orderType} onValueChange={setOrderType}>
                  <SelectTrigger className='h-8 rounded-sm text-xs'><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value='MARKET'>سوق</SelectItem>
                    <SelectItem value='LIMIT'>محدد</SelectItem>
                    <SelectItem value='STOP'>إيقاف</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className='space-y-1'>
                <Label className='text-xs'>الكمية</Label>
                <Input
                  type='number'
                  value={qty}
                  onChange={(e) => setQty(e.target.value)}
                  className='h-8 rounded-sm text-xs'
                  min='0.0001'
                  step='any'
                  required
                />
              </div>
            </div>

            {orderType !== 'MARKET' && (
              <div className='space-y-1'>
                <Label className='text-xs'>السعر</Label>
                <Input
                  type='number'
                  value={limitPrice}
                  onChange={(e) => setLimitPrice(e.target.value)}
                  className='h-8 rounded-sm text-xs'
                  step='any'
                  min='0.0001'
                  required
                />
              </div>
            )}

            {/* Estimated total */}
            <div className='rounded-sm bg-muted/40 px-3 py-2 text-xs flex justify-between'>
              <span className='text-muted-foreground'>التكلفة التقديرية</span>
              <span className='tabular-nums font-semibold'>
                {NUM.format(totalEst)} {quote.currency ?? 'USD'}
              </span>
            </div>

            <Button
              type='submit'
              className={`w-full h-9 rounded-sm font-semibold ${
                side === 'BUY'
                  ? 'bg-emerald-600 hover:bg-emerald-700'
                  : 'bg-red-600 hover:bg-red-700'
              }`}
            >
              {side === 'BUY' ? 'شراء' : 'بيع'} {quote.symbol}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}

// ─── Screener Table ────────────────────────────────────────────────────────────

const SCREENER_IDS: { id: string; label: string }[] = [
  { id: 'most_actives', label: 'الأكثر تداولاً' },
  { id: 'day_gainers', label: 'أكبر الرابحين' },
  { id: 'day_losers', label: 'أكبر الخاسرين' },
  { id: 'undervalued_growth_stocks', label: 'قيمة منخفضة' }
];

function ScreenerTab({
  account,
  onOrderPlaced
}: {
  account: TradingAccount | undefined;
  onOrderPlaced: () => void;
}) {
  const [screener, setScreener] = useState('most_actives');
  const [search, setSearch] = useState('');
  const [selectedSymbol, setSelectedSymbol] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const screenerParams = useMemo(
    () => `type=screener&scrId=${screener}&count=30`,
    [screener]
  );
  const { data: quotes, loading, error, refetch, fetchedAt } = useMarketData<YFQuote[]>(screenerParams);

  // Sparkline charts (fetch for top 6 visible)
  const [charts, setCharts] = useState<Record<string, number[]>>({});
  const chartsSymbols = useMemo(() => (quotes ?? []).slice(0, 10).map((q) => q.symbol), [quotes]);

  useEffect(() => {
    chartsSymbols.forEach(async (sym) => {
      if (charts[sym]) return;
      try {
        const res = await fetch(`/api/market?type=chart&symbol=${sym}&range=5d&interval=1d`);
        const json = (await res.json()) as { data?: { closes?: number[] } };
        const closes = json.data?.closes ?? [];
        if (closes.length) setCharts((p) => ({ ...p, [sym]: closes }));
      } catch { /* ignore */ }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chartsSymbols]);

  const filtered = useMemo(() => {
    const q = search.trim().toUpperCase();
    return (quotes ?? []).filter(
      (s) =>
        !q ||
        s.symbol.includes(q) ||
        (s.shortName ?? '').toUpperCase().includes(q) ||
        (s.longName ?? '').toUpperCase().includes(q)
    );
  }, [quotes, search]);

  const selectedQuote = useMemo(
    () => (quotes ?? []).find((q) => q.symbol === selectedSymbol) ?? null,
    [quotes, selectedSymbol]
  );

  async function handleOrder(
    side: 'BUY' | 'SELL',
    qty: number,
    price: number | undefined,
    orderType: string
  ) {
    if (!selectedQuote) return;
    if (!account) {
      toast.error('يجب فتح حساب تداول أولاً');
      return;
    }
    setSubmitting(true);
    try {
      await apiClient('/trading/orders', {
        method: 'POST',
        headers: { 'X-Idempotency-Key': idempotencyKey() },
        body: JSON.stringify({
          symbol: selectedQuote.symbol,
          type: orderType,
          side,
          quantity: qty,
          price: price
        })
      });
      toast.success(
        `✅ تم تقديم أمر ${side === 'BUY' ? 'شراء' : 'بيع'} ${qty} سهم من ${selectedQuote.symbol}`
      );
      onOrderPlaced();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تقديم الأمر');
    } finally {
      setSubmitting(false);
    }
    void submitting;
  }

  return (
    <div className='grid gap-4 xl:grid-cols-[1fr_340px]'>
      {/* ── Left: Table ── */}
      <div className='gov-panel rounded-md flex flex-col'>
        {/* Controls */}
        <div className='flex flex-wrap items-center gap-2 border-b border-border px-4 py-2.5'>
          <div className='flex gap-1 flex-wrap'>
            {SCREENER_IDS.map(({ id, label }) => (
              <button
                key={id}
                type='button'
                onClick={() => setScreener(id)}
                className={`rounded-full px-2.5 py-0.5 text-xs font-medium transition-colors ${
                  screener === id
                    ? 'bg-foreground text-background'
                    : 'border border-border text-muted-foreground hover:border-foreground/50'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
          <div className='ms-auto flex items-center gap-2'>
            <div className='relative'>
              <Icons.search className='absolute right-2 top-1/2 -translate-y-1/2 size-3.5 text-muted-foreground' />
              <Input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder='بحث...'
                className='h-7 rounded-sm text-xs pe-2 ps-7 w-32'
              />
            </div>
            <Button variant='ghost' size='sm' className='h-7 w-7 p-0' onClick={() => { void refetch(); }}>
              <Icons.refresh className='size-3.5' />
            </Button>
          </div>
        </div>

        {/* Timestamp */}
        {fetchedAt && (
          <div className='px-4 py-1 text-[10px] text-muted-foreground border-b border-border/50'>
            بيانات Yahoo Finance • آخر تحديث: {new Date(fetchedAt).toLocaleTimeString('ar-SY')}
          </div>
        )}

        {error && (
          <div className='mx-4 my-2 rounded-sm bg-amber-500/10 border border-amber-500/30 px-3 py-1.5 text-xs text-amber-700 dark:text-amber-400'>
            ⚠️ {error} — يُعرض آخر بيانات متوفرة.
          </div>
        )}

        <div className='overflow-x-auto flex-1'>
          <table className='w-full text-right'>
            <thead>
              <tr className='border-b border-border bg-muted/30 text-xs text-muted-foreground'>
                <th className='px-4 py-2 font-medium text-right'>الرمز / الاسم</th>
                <th className='px-3 py-2 font-medium text-right'>السعر</th>
                <th className='px-3 py-2 font-medium text-right'>التغير</th>
                <th className='px-3 py-2 font-medium text-right hidden md:table-cell'>الحجم</th>
                <th className='px-3 py-2 font-medium text-right hidden lg:table-cell'>القيمة السوقية</th>
                <th className='px-3 py-2 font-medium hidden xl:table-cell'>5D</th>
              </tr>
            </thead>
            <tbody className='divide-y divide-border/50'>
              {loading && !filtered.length && (
                <tr>
                  <td colSpan={6} className='py-8 text-center text-muted-foreground text-sm'>
                    <Icons.spinner className='size-5 animate-spin mx-auto mb-2' />
                    جاري تحميل بيانات السوق...
                  </td>
                </tr>
              )}
              {filtered.map((q) => (
                <StockRow
                  key={q.symbol}
                  q={q}
                  selected={selectedSymbol === q.symbol}
                  chart={charts[q.symbol]}
                  onClick={() => setSelectedSymbol((s) => (s === q.symbol ? null : q.symbol))}
                />
              ))}
              {!loading && !filtered.length && quotes?.length === 0 && (
                <tr>
                  <td colSpan={6} className='py-8 text-center text-muted-foreground text-sm'>
                    لا توجد نتائج
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Right: Detail Panel ── */}
      <div className={`transition-all ${selectedQuote ? 'block' : 'hidden xl:block'}`}>
        {selectedQuote ? (
          <StockDetailPanel
            quote={selectedQuote}
            onBuy={(side, qty, price, type) => { void handleOrder(side, qty, price, type); }}
            onClose={() => setSelectedSymbol(null)}
          />
        ) : (
          <div className='gov-panel rounded-md flex flex-col items-center justify-center h-full min-h-64 text-center p-6'>
            <span className='text-4xl mb-3'>📊</span>
            <p className='text-sm text-muted-foreground'>اختر سهماً من القائمة لعرض التفاصيل وتقديم الأوامر</p>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Portfolio Tab ────────────────────────────────────────────────────────────

function PortfolioTab() {
  const account = useBackend<TradingAccount>('/trading/account');
  const positions = useBackend<TradingPosition[]>('/trading/positions');
  const [opening, setOpening] = useState(false);

  const totalPnl = (positions.data ?? []).reduce((s, p) => s + Number(p.pnl ?? 0), 0);
  const totalValue = (positions.data ?? []).reduce(
    (s, p) => s + Number(p.quantity ?? 0) * Number(p.currentPrice ?? 0),
    0
  );

  async function openAccount() {
    setOpening(true);
    try {
      await apiClient('/trading/account/open', { method: 'POST', body: JSON.stringify({}) });
      toast.success('✅ تم فتح حساب التداول');
      account.refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل فتح الحساب');
    } finally {
      setOpening(false);
    }
  }

  return (
    <div className='space-y-4'>
      {/* Account Cards */}
      {account.data ? (
        <div className='grid gap-3 sm:grid-cols-2 lg:grid-cols-4'>
          {[
            { label: 'رصيد الحساب', value: `${NUM.format(Number(account.data.balance ?? 0))} ${account.data.currency ?? 'USD'}`, icon: '💰' },
            { label: 'حقوق الملكية', value: `${NUM.format(Number(account.data.equity ?? 0))} ${account.data.currency ?? 'USD'}`, icon: '📈' },
            { label: 'قوة الشراء', value: `${NUM.format(Number(account.data.buyingPower ?? account.data.buyingPowerUsd ?? 0))} USD`, icon: '⚡' },
            { label: 'قيمة المحفظة', value: `${NUM.format(totalValue)} USD`, icon: '🏦' }
          ].map(({ label, value, icon }) => (
            <div key={label} className='gov-panel rounded-md p-4'>
              <div className='flex items-center gap-2 mb-2'>
                <span className='text-xl'>{icon}</span>
                <span className='text-xs text-muted-foreground'>{label}</span>
              </div>
              <div className='text-lg font-bold tabular-nums'>{value}</div>
            </div>
          ))}
        </div>
      ) : !account.loading ? (
        <div className='gov-panel rounded-md flex flex-col items-center gap-3 p-8 text-center'>
          <span className='text-4xl'>📊</span>
          <p className='text-sm text-muted-foreground'>لا يوجد حساب تداول مفتوح</p>
          <Button className='h-9' onClick={() => { void openAccount(); }} disabled={opening}>
            {opening && <Icons.spinner className='me-2 size-4 animate-spin' />}
            فتح حساب تداول جديد
          </Button>
        </div>
      ) : null}

      {/* PnL Banner */}
      {positions.data && positions.data.length > 0 && (
        <div className={`rounded-md border px-4 py-2.5 flex items-center justify-between text-sm ${totalPnl >= 0 ? 'border-emerald-500/30 bg-emerald-500/10' : 'border-red-500/30 bg-red-500/10'}`}>
          <span className='text-muted-foreground'>إجمالي الأرباح / الخسائر غير المحققة</span>
          <span className={`font-bold tabular-nums text-base ${totalPnl >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
            {totalPnl >= 0 ? '+' : ''}{NUM.format(totalPnl)} USD
          </span>
        </div>
      )}

      {/* Positions Table */}
      <div className='gov-panel rounded-md'>
        <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
          <div>
            <h2 className='text-sm font-semibold'>المراكز المفتوحة</h2>
            <p className='text-muted-foreground text-xs'>{positions.data?.length ?? 0} مركز</p>
          </div>
          <Button variant='ghost' size='sm' className='h-7 w-7 p-0' onClick={() => positions.refetch()}>
            <Icons.refresh className='size-3.5' />
          </Button>
        </div>
        <div className='overflow-x-auto'>
          <table className='gov-table'>
            <thead>
              <tr>
                <th>الرمز</th><th>الاتجاه</th><th>الكمية</th>
                <th>سعر الدخول</th><th>السعر الحالي</th>
                <th>القيمة</th><th>الربح / الخسارة</th>
              </tr>
            </thead>
            <tbody>
              {(positions.data ?? []).map((p) => {
                const posVal = Number(p.quantity ?? 0) * Number(p.currentPrice ?? 0);
                const pnl = Number(p.pnl ?? 0);
                return (
                  <tr key={p.id} className='hover:bg-muted/20'>
                    <td className='font-bold'>{p.symbol}</td>
                    <td>
                      <Badge variant={p.side === 'LONG' ? 'default' : 'secondary'} className='text-xs'>
                        {p.side === 'LONG' ? '▲ طويل' : '▼ قصير'}
                      </Badge>
                    </td>
                    <td className='tabular-nums'>{p.quantity}</td>
                    <td className='tabular-nums'>{PRICE(Number(p.entryPrice))}</td>
                    <td className='tabular-nums font-medium'>{PRICE(Number(p.currentPrice))}</td>
                    <td className='tabular-nums'>{NUM.format(posVal)}</td>
                    <td className={`tabular-nums font-semibold ${pnl >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                      {pnl >= 0 ? '+' : ''}{PRICE(pnl)} {p.currency ?? ''}
                    </td>
                  </tr>
                );
              })}
              {!positions.loading && !positions.data?.length && (
                <tr>
                  <td colSpan={7} className='text-center text-muted-foreground text-sm py-6'>
                    {positions.error ?? 'لا توجد مراكز مفتوحة.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// ─── Orders Tab ───────────────────────────────────────────────────────────────

function OrdersTab({ refreshKey }: { refreshKey: number }) {
  const orders = useBackend<TradingOrder[]>(`/trading/orders?rk=${refreshKey}`);

  return (
    <div className='gov-panel rounded-md'>
      <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
        <div>
          <h2 className='text-sm font-semibold'>الأوامر المعلقة</h2>
          <p className='text-muted-foreground text-xs'>{orders.data?.length ?? 0} أمر</p>
        </div>
        <Button variant='ghost' size='sm' className='h-7 w-7 p-0' onClick={() => orders.refetch()}>
          <Icons.refresh className='size-3.5' />
        </Button>
      </div>
      <div className='overflow-x-auto'>
        <table className='gov-table'>
          <thead>
            <tr>
              <th>#</th><th>الرمز</th><th>النوع</th>
              <th>الاتجاه</th><th>الكمية</th><th>السعر</th>
              <th>الحالة</th><th>التاريخ</th>
            </tr>
          </thead>
          <tbody>
            {(orders.data ?? []).map((o) => (
              <tr key={o.id} className='hover:bg-muted/20'>
                <td className='font-mono text-xs'>#{o.id}</td>
                <td className='font-bold'>{o.symbol}</td>
                <td className='text-xs'>{o.type}</td>
                <td>
                  <Badge
                    variant={o.side === 'BUY' ? 'default' : 'destructive'}
                    className='text-xs'
                  >
                    {o.side === 'BUY' ? '▲ شراء' : '▼ بيع'}
                  </Badge>
                </td>
                <td className='tabular-nums'>{o.quantity}</td>
                <td className='tabular-nums'>{o.price ? PRICE(Number(o.price)) : 'سوق'}</td>
                <td>
                  <Badge
                    variant={
                      o.status === 'FILLED' ? 'default'
                        : o.status === 'CANCELLED' || o.status === 'REJECTED' ? 'destructive'
                          : 'secondary'
                    }
                    className='text-xs'
                  >
                    {o.status === 'PENDING' ? 'معلق' : o.status === 'FILLED' ? 'منفذ' : o.status ?? '—'}
                  </Badge>
                </td>
                <td className='text-xs text-muted-foreground'>{o.createdAt?.split('T')[0] ?? '—'}</td>
              </tr>
            ))}
            {!orders.loading && !orders.data?.length && (
              <tr>
                <td colSpan={8} className='text-center text-muted-foreground text-sm py-6'>
                  {orders.error ?? 'لا توجد أوامر.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─── Alerts Tab ───────────────────────────────────────────────────────────────

function AlertsTab() {
  const priceAlerts = useBackend<PriceAlert[]>('/trading/alerts');
  const [form, setForm] = useState({ symbol: '', targetPrice: '', direction: 'ABOVE' });
  const [submitting, setSubmitting] = useState(false);
  const [deleting, setDeleting] = useState<number | null>(null);
  const set = (k: string, v: string) => setForm((p) => ({ ...p, [k]: v }));

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!form.symbol.trim() || !form.targetPrice) return;
    setSubmitting(true);
    try {
      await apiClient('/trading/alerts', {
        method: 'POST',
        body: JSON.stringify({
          symbol: form.symbol.trim().toUpperCase(),
          targetPrice: Number(form.targetPrice),
          direction: form.direction
        })
      });
      toast.success('✅ تم إنشاء التنبيه');
      setForm({ symbol: '', targetPrice: '', direction: 'ABOVE' });
      priceAlerts.refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إنشاء التنبيه');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(id);
    try {
      await apiClient(`/trading/alerts/${id}`, { method: 'DELETE' });
      toast.success('تم حذف التنبيه');
      priceAlerts.refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل حذف التنبيه');
    } finally {
      setDeleting(null);
    }
  }

  return (
    <div className='grid gap-4 xl:grid-cols-[320px_1fr]'>
      <div className='gov-panel rounded-md'>
        <div className='gov-panel-header rounded-t-md px-4 py-3'>
          <h2 className='text-sm font-semibold'>إنشاء تنبيه سعر</h2>
        </div>
        <form className='grid gap-3 p-4' onSubmit={(e) => { void handleCreate(e); }}>
          <div className='space-y-1.5'>
            <Label className='text-xs'>الرمز</Label>
            <Input
              value={form.symbol}
              onChange={(e) => set('symbol', e.target.value.toUpperCase())}
              className='h-9 rounded-sm font-mono'
              placeholder='AAPL, NVDA, BTC...'
              required
            />
          </div>
          <div className='space-y-1.5'>
            <Label className='text-xs'>السعر المستهدف</Label>
            <Input
              type='number'
              value={form.targetPrice}
              onChange={(e) => set('targetPrice', e.target.value)}
              className='h-9 rounded-sm'
              step='any'
              min='0'
              required
            />
          </div>
          <div className='space-y-1.5'>
            <Label className='text-xs'>الشرط</Label>
            <Select value={form.direction} onValueChange={(v) => set('direction', v)}>
              <SelectTrigger className='h-9 rounded-sm'><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value='ABOVE'>▲ يتجاوز السعر المستهدف</SelectItem>
                <SelectItem value='BELOW'>▼ ينخفض دون السعر المستهدف</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <Button type='submit' className='h-9 rounded-sm' disabled={submitting}>
            {submitting && <Icons.spinner className='me-1.5 size-4 animate-spin' />}
            إنشاء التنبيه
          </Button>
        </form>
      </div>

      <div className='gov-panel rounded-md'>
        <div className='gov-panel-header flex items-center justify-between rounded-t-md px-4 py-3'>
          <h2 className='text-sm font-semibold'>تنبيهاتي ({priceAlerts.data?.length ?? 0})</h2>
          <Button variant='ghost' size='sm' className='h-7 w-7 p-0' onClick={() => priceAlerts.refetch()}>
            <Icons.refresh className='size-3.5' />
          </Button>
        </div>
        <div className='overflow-x-auto'>
          <table className='gov-table'>
            <thead>
              <tr><th>الرمز</th><th>السعر</th><th>الشرط</th><th>نشط</th><th>التاريخ</th><th>حذف</th></tr>
            </thead>
            <tbody>
              {(priceAlerts.data ?? []).map((a) => (
                <tr key={a.id} className='hover:bg-muted/20'>
                  <td className='font-bold'>{a.symbol}</td>
                  <td className='tabular-nums'>{PRICE(a.targetPrice)}</td>
                  <td>{a.direction === 'ABOVE' ? '▲ فوق' : '▼ تحت'}</td>
                  <td>
                    <Badge variant={a.isActive ? 'default' : 'secondary'} className='text-xs'>
                      {a.isActive ? 'نشط' : 'منتهي'}
                    </Badge>
                  </td>
                  <td className='text-xs text-muted-foreground'>{a.createdAt?.split('T')[0] ?? '—'}</td>
                  <td>
                    <Button
                      size='sm'
                      variant='ghost'
                      className='h-7 w-7 p-0 text-destructive'
                      onClick={() => { void handleDelete(a.id); }}
                      disabled={deleting === a.id}
                    >
                      {deleting === a.id
                        ? <Icons.spinner className='size-3 animate-spin' />
                        : <Icons.trash className='size-3' />}
                    </Button>
                  </td>
                </tr>
              ))}
              {!priceAlerts.loading && !priceAlerts.data?.length && (
                <tr>
                  <td colSpan={6} className='text-center text-muted-foreground text-sm py-6'>لا توجد تنبيهات.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// ─── Admin: Assets ────────────────────────────────────────────────────────────

function AdminAssetsTab() {
  const assets = useBackend<TradableAsset[]>('/trading/admin/tradable-assets');
  const [showAdd, setShowAdd] = useState(false);
  const [form, setForm] = useState({ symbol: '', name: '', assetType: 'STOCK', currentPrice: '', currency: 'USD' });
  const [adding, setAdding] = useState(false);
  const set = (k: string, v: string) => setForm((p) => ({ ...p, [k]: v }));

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    setAdding(true);
    try {
      await apiClient('/trading/admin/tradable-assets', {
        method: 'POST',
        body: JSON.stringify({
          symbol: form.symbol.trim().toUpperCase(),
          name: form.name.trim(),
          assetType: form.assetType,
          currentPrice: Number(form.currentPrice),
          currency: form.currency.toUpperCase()
        })
      });
      toast.success('✅ تم إضافة الأصل');
      setShowAdd(false);
      assets.refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل إضافة الأصل');
    } finally {
      setAdding(false);
    }
  }

  return (
    <div className='space-y-4'>
      <div className='flex justify-end'>
        <Button size='sm' className='h-8 text-xs' onClick={() => setShowAdd(true)}>
          + إضافة أصل جديد
        </Button>
      </div>

      <div className='grid gap-3 md:grid-cols-2 xl:grid-cols-3'>
        {(assets.data ?? []).map((a) => (
          <div key={a.id} className='gov-panel rounded-md p-4'>
            <div className='flex items-center justify-between mb-2'>
              <span className='font-bold text-base font-mono'>{a.symbol}</span>
              <Badge variant={a.isActive ? 'default' : 'secondary'} className='text-xs'>
                {a.isActive ? 'نشط' : 'موقوف'}
              </Badge>
            </div>
            <div className='text-xs text-muted-foreground'>{a.name}</div>
            <div className='text-xs text-muted-foreground mt-0.5'>{a.assetType}</div>
            <div className='mt-2 text-xl font-bold tabular-nums'>
              {PRICE(Number(a.currentPrice))}
              <span className='text-xs text-muted-foreground ms-1'>{a.currency}</span>
            </div>
          </div>
        ))}
      </div>

      {showAdd && (
        <Dialog open onOpenChange={(v) => { if (!v) setShowAdd(false); }}>
          <DialogContent className='max-w-md'>
            <DialogHeader><DialogTitle>إضافة أصل قابل للتداول</DialogTitle></DialogHeader>
            <form onSubmit={(e) => { void handleAdd(e); }} className='space-y-3'>
              <div className='grid grid-cols-2 gap-3'>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>الرمز *</Label>
                  <Input value={form.symbol} onChange={(e) => set('symbol', e.target.value.toUpperCase())}
                    className='h-9 rounded-sm font-mono' placeholder='AAPL' required />
                </div>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>النوع</Label>
                  <Select value={form.assetType} onValueChange={(v) => set('assetType', v)}>
                    <SelectTrigger className='h-9 rounded-sm'><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value='STOCK'>سهم</SelectItem>
                      <SelectItem value='CRYPTO'>عملة رقمية</SelectItem>
                      <SelectItem value='COMMODITY'>سلعة</SelectItem>
                      <SelectItem value='FOREX'>فوركس</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className='space-y-1.5'>
                <Label className='text-xs'>الاسم الكامل *</Label>
                <Input value={form.name} onChange={(e) => set('name', e.target.value)}
                  className='h-9 rounded-sm' required />
              </div>
              <div className='grid grid-cols-2 gap-3'>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>السعر الحالي</Label>
                  <Input type='number' value={form.currentPrice} onChange={(e) => set('currentPrice', e.target.value)}
                    className='h-9 rounded-sm' step='any' min='0' />
                </div>
                <div className='space-y-1.5'>
                  <Label className='text-xs'>العملة</Label>
                  <Input value={form.currency} onChange={(e) => set('currency', e.target.value.toUpperCase())}
                    className='h-9 rounded-sm' placeholder='USD' />
                </div>
              </div>
              <DialogFooter>
                <Button type='button' variant='outline' onClick={() => setShowAdd(false)} className='h-8 text-xs'>إلغاء</Button>
                <Button type='submit' className='h-8 text-xs' disabled={adding}>
                  {adding && <Icons.spinner className='me-1.5 size-3 animate-spin' />}إضافة
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}

// ─── AI Assistant Tab ──────────────────────────────────────────────────────────

const QUICK_PROMPTS = [
  'ما هي أفضل الأسهم الآن للشراء؟',
  'حلل أداء NVIDIA هذا الأسبوع',
  'ما هي مخاطر استثماري الحالية؟',
  'أعطني ملخصاً عن أكثر الأسهم تداولاً اليوم'
];

function AssistantTab() {
  const [messages, setMessages] = useState<{ role: 'user' | 'ai'; text: string }[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  async function send(text: string) {
    const t = text.trim();
    if (!t) return;
    setMessages((m) => [...m, { role: 'user', text: t }]);
    setInput('');
    setLoading(true);
    try {
      const res = await apiClient<{ response?: string; reply?: string; answer?: string }>(
        '/trading/assistant/chat',
        { method: 'POST', body: JSON.stringify({ message: t }) }
      );
      const reply = res.response ?? res.reply ?? res.answer ?? 'لا يوجد رد.';
      setMessages((m) => [...m, { role: 'ai', text: reply }]);
    } catch (err) {
      setMessages((m) => [
        ...m,
        { role: 'ai', text: `⚠️ ${err instanceof Error ? err.message : 'خطأ في الاتصال'}` }
      ]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className='gov-panel rounded-md flex flex-col h-[580px]'>
      <div className='gov-panel-header rounded-t-md px-4 py-3 flex items-center justify-between'>
        <div>
          <h2 className='text-sm font-semibold'>مساعد التداول الذكي</h2>
          <p className='text-muted-foreground text-xs'>اسأل عن الأسواق، التحليل، أو استراتيجيات التداول.</p>
        </div>
        {messages.length > 0 && (
          <Button variant='ghost' size='sm' className='h-7 text-xs' onClick={() => setMessages([])}>
            مسح
          </Button>
        )}
      </div>

      {/* Quick prompts */}
      {messages.length === 0 && (
        <div className='px-4 py-3 border-b border-border flex flex-wrap gap-2'>
          {QUICK_PROMPTS.map((p) => (
            <button
              key={p}
              type='button'
              onClick={() => { void send(p); }}
              className='rounded-full border border-border px-2.5 py-1 text-xs text-muted-foreground hover:border-primary hover:text-foreground transition-colors'
            >
              {p}
            </button>
          ))}
        </div>
      )}

      {/* Messages */}
      <div className='flex-1 overflow-y-auto p-4 space-y-3'>
        {messages.map((m, i) => (
          <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`max-w-[85%] rounded-lg px-3 py-2 text-sm whitespace-pre-wrap leading-relaxed ${
                m.role === 'user'
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-muted/60 border border-border'
              }`}
            >
              {m.text}
            </div>
          </div>
        ))}
        {loading && (
          <div className='flex justify-start'>
            <div className='bg-muted/60 border border-border rounded-lg px-4 py-2.5 flex items-center gap-1.5'>
              {[0, 150, 300].map((d) => (
                <span
                  key={d}
                  className='size-1.5 rounded-full bg-muted-foreground animate-bounce'
                  style={{ animationDelay: `${d}ms` }}
                />
              ))}
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <form
        className='flex gap-2 border-t border-border p-3'
        onSubmit={(e) => { e.preventDefault(); void send(input); }}
      >
        <Textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          className='min-h-[36px] max-h-20 rounded-sm resize-none text-sm'
          placeholder='اسأل عن سوق أو أصل أو استراتيجية...'
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              void send(input);
            }
          }}
        />
        <Button
          type='submit'
          className='h-9 rounded-sm self-end shrink-0'
          disabled={loading || !input.trim()}
        >
          <Icons.send className='size-4' />
        </Button>
      </form>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

type TabKey = 'market' | 'portfolio' | 'orders' | 'alerts' | 'admin' | 'assistant';

export function TradingPage() {
  const { session } = useSession();
  const isAdmin = useMemo(() => {
    const r = session?.role ?? '';
    return ['SUPER_ADMIN', 'ADMIN', 'OWNER', 'PLATFORM_OWNER', 'MOTHER_BRANCH_ADMIN'].includes(r);
  }, [session?.role]);

  const account = useBackend<TradingAccount>('/trading/account');
  const [tab, setTab] = useState<TabKey>('market');
  const [orderRefreshKey, setOrderRefreshKey] = useState(0);

  const tabs: { key: TabKey; label: string; adminOnly?: boolean }[] = [
    { key: 'market', label: '📊 السوق' },
    { key: 'portfolio', label: '💼 محفظتي' },
    { key: 'orders', label: '📋 الأوامر' },
    { key: 'alerts', label: '🔔 التنبيهات' },
    { key: 'assistant', label: '🤖 المساعد' },
    ...(isAdmin ? [{ key: 'admin' as TabKey, label: '⚙️ إدارة الأصول', adminOnly: true }] : [])
  ];

  return (
    <PageContainer
      pageTitle='منصة التداول'
      pageDescription='بيانات مباشرة من Yahoo Finance • تداول عبر المنصة • تتبع محفظتك'
    >
      {/* ── Futures Ticker ── */}
      <div className='-mx-4 -mt-4 mb-4'>
        <FuturesBar />
      </div>

      {/* ── Tab Navigation ── */}
      <div className='flex flex-wrap gap-1.5 mb-4'>
        {tabs.map(({ key, label }) => (
          <Button
            key={key}
            size='sm'
            variant={tab === key ? 'default' : 'outline'}
            className='h-8 text-xs'
            onClick={() => setTab(key)}
          >
            {label}
          </Button>
        ))}
      </div>

      {/* ── Tab Content ── */}
      {tab === 'market' && (
        <ScreenerTab
          account={account.data}
          onOrderPlaced={() => {
            setOrderRefreshKey((k) => k + 1);
            setTab('orders');
          }}
        />
      )}
      {tab === 'portfolio' && <PortfolioTab />}
      {tab === 'orders' && <OrdersTab refreshKey={orderRefreshKey} />}
      {tab === 'alerts' && <AlertsTab />}
      {tab === 'assistant' && <AssistantTab />}
      {tab === 'admin' && isAdmin && <AdminAssetsTab />}
    </PageContainer>
  );
}
