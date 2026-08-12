import { ConnectionError, HttpError } from '@/api/types'

/** Use wireframe fixtures when the claims API is unreachable in local dev/e2e. */
export function shouldUseClaimsFixtureFallback(error: unknown): boolean {
  if (!import.meta.env.DEV) {
    return false
  }
  if (error instanceof ConnectionError) {
    return true
  }
  return error instanceof HttpError && error.status >= 500
}

const CUST_NAMES: Record<number, string> = {
  19284: 'Diego & Marta Field',
  100001: 'Priya Nair',
}

export function custNameForId(custId: number): string {
  return CUST_NAMES[custId] ?? `Customer ${custId}`
}

/** CLM-0004821 or CLM-000004821 → CLM000004821 (12-char backend key). */
export function normalizeClaimNbr(value: string): string {
  const stripped = value.replace(/-/g, '').toUpperCase().trim()
  if (!stripped.startsWith('CLM')) {
    return stripped
  }
  const digits = stripped.slice(3).replace(/\D/g, '')
  if (!digits) {
    return stripped
  }
  return `CLM${digits.padStart(9, '0')}`
}

/** CLM000004821 → CLM-000004821 */
export function formatClaimNbr(value: string): string {
  const normalized = normalizeClaimNbr(value)
  if (normalized.length === 12 && normalized.startsWith('CLM')) {
    return `CLM-${normalized.slice(3)}`
  }
  return value
}

/** POL-0088217 → POL000088217 style (strip dashes, pad if needed). */
export function normalizePolNbr(value: string): string {
  return value.replace(/-/g, '').toUpperCase()
}
