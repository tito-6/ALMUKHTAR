import { NextResponse } from 'next/server';

export interface SpRate {
  code: string;         // USD, EUR, TRY …
  nameAr: string;       // Arabic name
  buy: number;          // شراء
  sell: number;         // مبيع
  change: number;       // التغير %
  high: number;         // أعلى
  low: number;          // أدنى
  flag: string;         // emoji flag
}

const FLAGS: Record<string, string> = {
  USD: '🇺🇸', EUR: '🇪🇺', TRY: '🇹🇷', SAR: '🇸🇦',
  AED: '🇦🇪', EGP: '🇪🇬', LYD: '🇱🇾', JOD: '🇯🇴',
  KWD: '🇰🇼', GBP: '🇬🇧', QAR: '🇶🇦', BHD: '🇧🇭',
  OMR: '🇴🇲', IQD: '🇮🇶', LBP: '🇱🇧', XAU: '🥇',
  CHF: '🇨🇭', CAD: '🇨🇦', AUD: '🇦🇺', CNY: '🇨🇳',
};

const NAMES: Record<string, string> = {
  USD: 'دولار أمريكي', EUR: 'يورو', TRY: 'ليرة تركية',
  SAR: 'ريال سعودي', AED: 'درهم إماراتي', EGP: 'جنيه مصري',
  LYD: 'دينار ليبي', JOD: 'دينار أردني', KWD: 'دينار كويتي',
  GBP: 'جنيه إسترليني', QAR: 'ريال قطري', BHD: 'دينار بحريني',
  OMR: 'ريال عُماني', IQD: 'دينار عراقي', LBP: 'ليرة لبنانية',
  XAU: 'ذهب (أوقية)', CHF: 'فرنك سويسري', CAD: 'دولار كندي',
  AUD: 'دولار أسترالي', CNY: 'يوان صيني',
};

const KNOWN_CODES = new Set([
  'USD', 'EUR', 'GBP', 'TRY', 'SAR', 'AED', 'EGP', 'LYD',
  'JOD', 'KWD', 'QAR', 'BHD', 'OMR', 'IQD', 'LBP', 'XAU',
  'CHF', 'CAD', 'AUD', 'CNY'
]);

function normalizeRate(raw: Record<string, unknown>): SpRate | null {
  const code = String(raw.code ?? raw.currency ?? raw.symbol ?? raw.iso ?? '').toUpperCase().trim();
  if (!code || code.length < 2 || code.length > 4) return null;

  const buy = Number(raw.buy ?? raw.buyRate ?? raw.buying ?? raw.purchase ?? raw.bid ?? 0);
  const sell = Number(raw.sell ?? raw.sellRate ?? raw.selling ?? raw.ask ?? raw.sale ?? 0);

  if (buy === 0 && sell === 0) return null;

  return {
    code,
    nameAr: NAMES[code] ?? (raw.name as string) ?? (raw.nameAr as string) ?? code,
    buy,
    sell: sell || buy,
    change: Number(raw.change ?? raw.changePercent ?? raw.percent ?? raw.diff ?? 0),
    high: Number(raw.high ?? raw.highRate ?? raw.max ?? 0),
    low: Number(raw.low ?? raw.lowRate ?? raw.min ?? 0),
    flag: FLAGS[code] ?? '🌐',
  };
}

/** Try to extract rates from any JSON structure sp-today might return */
function extractFromJson(data: unknown): SpRate[] {
  if (!data) return [];

  // Direct array of rate objects
  if (Array.isArray(data) && data.length > 0 && typeof data[0] === 'object') {
    const rates = (data as Array<Record<string, unknown>>)
      .map(normalizeRate)
      .filter((r): r is SpRate => r !== null);
    if (rates.length > 0) return rates;
  }

  // Object with a 'data' or 'rates' or 'currencies' array key
  if (typeof data === 'object' && data !== null) {
    const obj = data as Record<string, unknown>;
    for (const key of ['data', 'rates', 'currencies', 'result', 'items', 'list']) {
      if (Array.isArray(obj[key])) {
        const rates = (obj[key] as Array<Record<string, unknown>>)
          .map(normalizeRate)
          .filter((r): r is SpRate => r !== null);
        if (rates.length > 0) return rates;
      }
    }

    // Object where each key is a currency code
    if (Object.keys(obj).some((k) => KNOWN_CODES.has(k.toUpperCase()))) {
      const rates: SpRate[] = [];
      for (const [k, v] of Object.entries(obj)) {
        const code = k.toUpperCase();
        if (!KNOWN_CODES.has(code)) continue;
        if (typeof v === 'number') {
          rates.push({ code, nameAr: NAMES[code] ?? code, buy: v, sell: v, change: 0, high: 0, low: 0, flag: FLAGS[code] ?? '🌐' });
        } else if (typeof v === 'object' && v !== null) {
          const r = normalizeRate({ code, ...(v as Record<string, unknown>) });
          if (r) rates.push(r);
        }
      }
      if (rates.length > 0) return rates;
    }
  }

  return [];
}

/** Parse rates from sp-today HTML page using multiple strategies */
function parseHtmlRates(html: string): SpRate[] {
  // Strategy 1: Embedded JSON in script tag
  for (const pattern of [
    /"currencies"\s*:\s*(\[[\s\S]*?\])/,
    /"rates"\s*:\s*(\[[\s\S]*?\])/,
    /window\.__data\s*=\s*({[\s\S]*?});/,
    /window\.currencies\s*=\s*(\[[\s\S]*?\]);/,
  ]) {
    const m = html.match(pattern);
    if (m) {
      try {
        const parsed = JSON.parse(m[1]) as unknown;
        const rates = extractFromJson(parsed);
        if (rates.length > 0) return rates;
      } catch { /* fall through */ }
    }
  }

  // Strategy 2: Table rows with currency codes
  const rates: SpRate[] = [];
  const codeRegex = /\b(USD|EUR|GBP|TRY|SAR|AED|EGP|LYD|JOD|KWD|QAR|BHD|OMR|IQD|LBP|XAU|CHF|CAD|AUD|CNY)\b/i;
  const numRegex = /[\d,]+\.?\d*/;
  const rowRegex = /<tr[^>]*>[\s\S]*?<\/tr>/gi;
  const cellRegex = /<td[^>]*>([\s\S]*?)<\/td>/gi;
  const seen = new Set<string>();

  const rows = html.match(rowRegex) ?? [];
  for (const row of rows) {
    const codeM = row.match(codeRegex);
    if (!codeM) continue;
    const code = codeM[1].toUpperCase();
    if (seen.has(code)) continue;

    const cells: string[] = [];
    let m;
    cellRegex.lastIndex = 0;
    while ((m = cellRegex.exec(row)) !== null) {
      cells.push(m[1].replace(/<[^>]+>/g, '').replace(/&[a-z]+;/g, ' ').trim());
    }

    const nums = cells
      .map((c) => { const n = c.match(numRegex); return n ? parseFloat(n[0].replace(/,/g, '')) : 0; })
      .filter((n) => n > 0);

    if (nums.length >= 2) {
      seen.add(code);
      rates.push({
        code,
        nameAr: NAMES[code] ?? code,
        buy: nums[0],
        sell: nums[1] ?? nums[0],
        change: nums[2] ?? 0,
        high: nums[3] ?? 0,
        low: nums[4] ?? 0,
        flag: FLAGS[code] ?? '🌐',
      });
    }
  }

  return rates;
}

export async function GET() {
  const CACHE_HEADERS = { 'Cache-Control': 'public, s-maxage=300, stale-while-revalidate=60' };

  // Phase 1: Try known JSON API endpoints
  const jsonEndpoints = [
    'https://sp-today.com/api/currencies',
    'https://sp-today.com/api/v1/currencies',
    'https://sp-today.com/api/rates',
    'https://sp-today.com/en/api/rates',
    'https://sp-today.com/api/v2/currencies',
  ];

  for (const ep of jsonEndpoints) {
    try {
      const res = await fetch(ep, {
        headers: {
          Accept: 'application/json',
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124',
          Referer: 'https://sp-today.com/',
        },
        signal: AbortSignal.timeout(5000),
        next: { revalidate: 300 },
      });
      if (res.ok) {
        const data = await res.json() as unknown;
        const rates = extractFromJson(data);
        if (rates.length > 0) {
          return NextResponse.json(
            { rates, source: ep, cachedAt: new Date().toISOString() },
            { headers: CACHE_HEADERS }
          );
        }
      }
    } catch { /* try next */ }
  }

  // Phase 2: Scrape HTML from main currencies page
  try {
    const htmlRes = await fetch('https://sp-today.com/currencies', {
      headers: {
        Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'ar,ar-SY;q=0.9,en;q=0.5',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        Referer: 'https://sp-today.com/',
        'Cache-Control': 'no-cache',
      },
      signal: AbortSignal.timeout(10000),
    });

    if (htmlRes.ok) {
      const html = await htmlRes.text();
      const rates = parseHtmlRates(html);
      if (rates.length > 0) {
        return NextResponse.json(
          { rates, source: 'https://sp-today.com/currencies', cachedAt: new Date().toISOString() },
          { headers: CACHE_HEADERS }
        );
      }
    }
  } catch (e) {
    const errMsg = e instanceof Error ? e.message : 'Unknown error';
    return NextResponse.json({ error: errMsg, rates: [] }, { status: 502 });
  }

  return NextResponse.json(
    { error: 'Could not parse rates from sp-today.com — the site structure may have changed.', rates: [] },
    { status: 502 }
  );
}
