import type { HTMLAttributes } from 'react'

export interface MoneyDisplayProps extends HTMLAttributes<HTMLSpanElement> {
  value: number | string | null | undefined
  locale?: string
  currency?: string
}

function toNumber(value: number | string | null | undefined): number | null {
  if (value === null || value === undefined) return null
  const n = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(n) ? n : null
}

export function formatMoney(
  value: number | string | null | undefined,
  locale = 'en-US',
  currency = 'USD',
): string {
  const n = toNumber(value)
  if (n === null) return '—'
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(n)
}

export function MoneyDisplay({
  value,
  locale = 'en-US',
  currency = 'USD',
  className,
  ...rest
}: MoneyDisplayProps) {
  const formatted = formatMoney(value, locale, currency)
  return (
    <span className={className} {...rest}>
      {formatted}
    </span>
  )
}
