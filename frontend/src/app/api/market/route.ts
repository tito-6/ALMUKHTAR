import { NextRequest, NextResponse } from 'next/server';

/**
 * Yahoo Finance proxy — avoids CORS and lets us cache/rate-limit.
 *
 * Query params:
 *   type=screener  scrId=most_actives|day_gainers|day_losers  count=25
 *   type=quote     symbols=AAPL,MSFT,NVDA
 *   type=chart     symbol=AAPL  range=1d|5d|1mo  interval=1m|5m|1d
 *   type=futures                                             (indices/futures)
 */

const BROWSER_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36',
  Accept: 'application/json, text/plain, */*',
  'Accept-Language': 'en-US,en;q=0.9',
  Referer: 'https://finance.yahoo.com/',
  Origin: 'https://finance.yahoo.com'
};

const YF1 = 'https://query1.finance.yahoo.com';
const YF2 = 'https://query2.finance.yahoo.com';

// Compact "1d" cache for volatile market data
const CACHE_S = 60;

export type YFQuote = {
  symbol: string;
  shortName?: string;
  longName?: string;
  regularMarketPrice?: number;
  regularMarketChange?: number;
  regularMarketChangePercent?: number;
  regularMarketVolume?: number;
  averageDailyVolume3Month?: number;
  marketCap?: number;
  trailingPE?: number;
  fiftyTwoWeekLow?: number;
  fiftyTwoWeekHigh?: number;
  fiftyTwoWeekChangePercent?: number;
  regularMarketOpen?: number;
  regularMarketDayHigh?: number;
  regularMarketDayLow?: number;
  regularMarketPreviousClose?: number;
  currency?: string;
  exchange?: string;
  quoteType?: string;
};

export type YFChartMeta = {
  symbol: string;
  currency: string;
  regularMarketPrice: number;
  chartPreviousClose: number;
};

export type YFChartData = {
  meta: YFChartMeta;
  timestamps: number[];
  closes: number[];
};

async function tryFetch(urls: string[]): Promise<Response | null> {
  for (const url of urls) {
    try {
      const res = await fetch(url, {
        headers: BROWSER_HEADERS,
        next: { revalidate: CACHE_S }
      });
      if (res.ok) return res;
    } catch {
      // try next
    }
  }
  return null;
}

// ─── Screener ─────────────────────────────────────────────────────────────────

async function fetchScreener(scrId: string, count: number): Promise<YFQuote[]> {
  const qs = `scrIds=${scrId}&count=${count}&formatted=false&lang=en-US&region=US&corsDomain=finance.yahoo.com`;
  const res = await tryFetch([
    `${YF1}/v1/finance/screener/predefined/saved?${qs}`,
    `${YF2}/v1/finance/screener/predefined/saved?${qs}`
  ]);
  if (!res) return [];

  const json = (await res.json()) as {
    finance?: { result?: Array<{ quotes?: YFQuote[] }> };
  };
  return json.finance?.result?.[0]?.quotes ?? [];
}

// ─── Quote ────────────────────────────────────────────────────────────────────

async function fetchQuotes(symbols: string): Promise<YFQuote[]> {
  const qs = `symbols=${symbols}&formatted=false&lang=en-US&region=US`;
  const res = await tryFetch([
    `${YF1}/v7/finance/quote?${qs}`,
    `${YF2}/v7/finance/quote?${qs}`
  ]);
  if (!res) return [];

  const json = (await res.json()) as {
    quoteResponse?: { result?: YFQuote[] };
  };
  return json.quoteResponse?.result ?? [];
}

// ─── Chart ────────────────────────────────────────────────────────────────────

async function fetchChart(
  symbol: string,
  range: string,
  interval: string
): Promise<YFChartData | null> {
  const qs = `interval=${interval}&range=${range}&includePrePost=false`;
  const res = await tryFetch([
    `${YF1}/v8/finance/chart/${symbol}?${qs}`,
    `${YF2}/v8/finance/chart/${symbol}?${qs}`
  ]);
  if (!res) return null;

  const json = (await res.json()) as {
    chart?: {
      result?: Array<{
        meta?: YFChartMeta;
        timestamp?: number[];
        indicators?: { quote?: Array<{ close?: (number | null)[] }> };
      }>;
    };
  };

  const result = json.chart?.result?.[0];
  if (!result?.meta) return null;

  const closes = (result.indicators?.quote?.[0]?.close ?? [])
    .map((v) => v ?? 0);

  return {
    meta: result.meta,
    timestamps: result.timestamp ?? [],
    closes
  };
}

// ─── Futures/Indices ──────────────────────────────────────────────────────────

const FUTURES_SYMBOLS = [
  'ES=F',   // S&P 500 Futures
  'YM=F',   // Dow Futures
  'NQ=F',   // Nasdaq Futures
  'RTY=F',  // Russell 2000
  '^VIX',   // VIX
  'GC=F',   // Gold
  'CL=F'    // Oil
].join(',');

async function fetchFutures(): Promise<YFQuote[]> {
  return fetchQuotes(FUTURES_SYMBOLS);
}

// ─── Route Handler ────────────────────────────────────────────────────────────

export async function GET(req: NextRequest) {
  const sp = new URL(req.url).searchParams;
  const type = sp.get('type') ?? 'screener';

  try {
    let data: unknown;

    switch (type) {
      case 'screener': {
        const scrId = sp.get('scrId') ?? 'most_actives';
        const count = Math.min(Number(sp.get('count') ?? '30'), 50);
        data = await fetchScreener(scrId, count);
        break;
      }
      case 'quote': {
        const symbols = sp.get('symbols') ?? '';
        if (!symbols) return NextResponse.json({ error: 'symbols required' }, { status: 400 });
        data = await fetchQuotes(symbols);
        break;
      }
      case 'chart': {
        const symbol = sp.get('symbol') ?? '';
        if (!symbol) return NextResponse.json({ error: 'symbol required' }, { status: 400 });
        const range = sp.get('range') ?? '5d';
        const interval = sp.get('interval') ?? '1d';
        data = await fetchChart(symbol, range, interval);
        break;
      }
      case 'futures': {
        data = await fetchFutures();
        break;
      }
      default:
        return NextResponse.json({ error: 'Unknown type' }, { status: 400 });
    }

    return NextResponse.json(
      { data, fetchedAt: new Date().toISOString() },
      { headers: { 'Cache-Control': `public, max-age=${CACHE_S}, stale-while-revalidate=120` } }
    );
  } catch (err) {
    return NextResponse.json(
      { error: err instanceof Error ? err.message : 'Market data fetch failed', data: [] },
      { status: 502 }
    );
  }
}
