// Currency metadata: flags, symbols, Arabic names, colors

export const CURRENCY_FLAG: Record<string, string> = {
  USD: '🇺🇸',
  EUR: '🇪🇺',
  GBP: '🇬🇧',
  TRY: '🇹🇷',
  TL:  '🇹🇷',
  SAR: '🇸🇦',
  AED: '🇦🇪',
  EGP: '🇪🇬',
  LYD: '🇱🇾',
  JOD: '🇯🇴',
  KWD: '🇰🇼',
  QAR: '🇶🇦',
  BHD: '🇧🇭',
  OMR: '🇴🇲',
  IQD: '🇮🇶',
  LBP: '🇱🇧',
  SYP: '🇸🇾',
  XAU: '🥇',
  XAG: '🥈',
  BTC: '₿',
  CNY: '🇨🇳',
  JPY: '🇯🇵',
  CHF: '🇨🇭',
  CAD: '🇨🇦',
  AUD: '🇦🇺',
  RUB: '🇷🇺',
};

/**
 * ISO 3166-1 alpha-2 country codes for flag image rendering.
 * Use with flagcdn.com: https://flagcdn.com/w20/{code}.png
 */
export const CURRENCY_COUNTRY_CODE: Record<string, string> = {
  USD: 'us',
  EUR: 'eu',
  GBP: 'gb',
  TRY: 'tr',
  TL:  'tr',
  SAR: 'sa',
  AED: 'ae',
  EGP: 'eg',
  LYD: 'ly',
  JOD: 'jo',
  KWD: 'kw',
  QAR: 'qa',
  BHD: 'bh',
  OMR: 'om',
  IQD: 'iq',
  LBP: 'lb',
  SYP: 'sy',
  CNY: 'cn',
  JPY: 'jp',
  CHF: 'ch',
  CAD: 'ca',
  AUD: 'au',
  RUB: 'ru',
};

/** Return CDN flag image URL for a currency code */
export function currencyFlagUrl(code: string): string | null {
  const cc = CURRENCY_COUNTRY_CODE[code?.toUpperCase()];
  if (!cc) return null;
  return `https://flagcdn.com/w20/${cc}.png`;
}

/** Return 2x CDN flag image URL for retina */
export function currencyFlagUrl2x(code: string): string | null {
  const cc = CURRENCY_COUNTRY_CODE[code?.toUpperCase()];
  if (!cc) return null;
  return `https://flagcdn.com/w40/${cc}.png`;
}

export const CURRENCY_SYMBOL: Record<string, string> = {
  USD: '$',
  EUR: '€',
  GBP: '£',
  TRY: '₺',
  TL:  '₺',
  SAR: 'ر.س',
  AED: 'د.إ',
  EGP: 'ج.م',
  LYD: 'ل.د',
  JOD: 'JD',
  KWD: 'K.D',
  QAR: 'ر.ق',
  BHD: 'BD',
  OMR: 'ر.ع',
  IQD: 'ع.د',
  LBP: 'ل.ل',
  SYP: 'ل.س',
  XAU: 'أوقية',
  XAG: 'أوقية',
  BTC: '₿',
  CNY: '¥',
  JPY: '¥',
  CHF: 'CHF',
  CAD: 'C$',
  AUD: 'A$',
  RUB: '₽',
};

export const CURRENCY_NAME_AR: Record<string, string> = {
  USD: 'دولار أمريكي',
  EUR: 'يورو',
  GBP: 'جنيه إسترليني',
  TRY: 'ليرة تركية',
  TL:  'ليرة تركية',
  SAR: 'ريال سعودي',
  AED: 'درهم إماراتي',
  EGP: 'جنيه مصري',
  LYD: 'دينار ليبي',
  JOD: 'دينار أردني',
  KWD: 'دينار كويتي',
  QAR: 'ريال قطري',
  BHD: 'دينار بحريني',
  OMR: 'ريال عُماني',
  IQD: 'دينار عراقي',
  LBP: 'ليرة لبنانية',
  SYP: 'ليرة سورية',
  XAU: 'ذهب (أوقية)',
  XAG: 'فضة (أوقية)',
  BTC: 'بيتكوين',
  CNY: 'يوان صيني',
  JPY: 'ين ياباني',
  CHF: 'فرنك سويسري',
  CAD: 'دولار كندي',
  AUD: 'دولار أسترالي',
  RUB: 'روبل روسي',
};

/** Return flag emoji for currency code, falls back to globe */
export function currencyFlag(code: string): string {
  return CURRENCY_FLAG[code.toUpperCase()] ?? '🌐';
}

/** Return currency symbol, falls back to the code itself */
export function currencySymbol(code: string): string {
  return CURRENCY_SYMBOL[code.toUpperCase()] ?? code;
}

/** Return Arabic name, falls back to the code itself */
export function currencyName(code: string): string {
  return CURRENCY_NAME_AR[code.toUpperCase()] ?? code;
}

/** Format an amount with flag + symbol + number */
export function formatCurrency(amount: number | string | undefined, code: string): string {
  const num = Number(amount ?? 0);
  const fmt = new Intl.NumberFormat('ar-SY', { maximumFractionDigits: 2 });
  const sym = currencySymbol(code);
  return `${fmt.format(num)} ${sym}`;
}
