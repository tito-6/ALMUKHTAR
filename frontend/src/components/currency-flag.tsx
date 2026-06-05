'use client';

import { currencyFlagUrl, currencyFlagUrl2x, currencyFlag } from '@/lib/currencies';

interface CurrencyFlagProps {
  code: string;
  size?: number;
  className?: string;
}

/**
 * Renders a real country flag image (flagcdn.com) with emoji fallback.
 * Reliable on all platforms including Windows where flag emoji may not render.
 */
export function CurrencyFlag({ code, size = 20, className = '' }: CurrencyFlagProps) {
  const url = currencyFlagUrl(code);
  const url2x = currencyFlagUrl2x(code);
  const fallback = currencyFlag(code);

  if (!url) {
    return (
      <span
        className={`inline-flex items-center justify-center text-sm leading-none ${className}`}
        style={{ width: size, height: size * 0.75 }}
        aria-label={code}
      >
        {fallback}
      </span>
    );
  }

  return (
    <img
      src={url}
      srcSet={url2x ? `${url2x} 2x` : undefined}
      alt={code}
      width={size}
      height={Math.round(size * 0.75)}
      className={`inline-block rounded-[2px] object-cover align-middle ${className}`}
      loading='lazy'
      onError={(e) => {
        const target = e.currentTarget;
        target.style.display = 'none';
        const span = document.createElement('span');
        span.textContent = fallback;
        span.className = 'text-sm leading-none';
        target.parentNode?.insertBefore(span, target.nextSibling);
      }}
    />
  );
}
