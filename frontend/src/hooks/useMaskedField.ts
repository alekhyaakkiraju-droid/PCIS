import { useCallback, useMemo, useState } from 'react'
import { useCapabilities } from '@/auth/useCapabilities'

export type MaskFieldType = 'taxId' | 'phone' | 'email' | 'generic'

function maskTaxId(value: string): string {
  const digits = value.replace(/\D/g, '')
  if (digits.length < 4) return '***'
  return `***-**-${digits.slice(-4)}`
}

function maskPhone(value: string): string {
  const digits = value.replace(/\D/g, '')
  if (digits.length < 4) return '***'
  return `***-***-${digits.slice(-4)}`
}

function maskEmail(value: string): string {
  const at = value.indexOf('@')
  if (at <= 1) return '***@***'
  return `${value[0]}***${value.slice(at)}`
}

function maskGeneric(value: string): string {
  if (value.length <= 4) return '****'
  return `${value.slice(0, 2)}${'*'.repeat(Math.max(4, value.length - 4))}${value.slice(-2)}`
}

export function maskPiiValue(value: string | undefined | null, type: MaskFieldType): string {
  if (!value) return '—'
  switch (type) {
    case 'taxId':
      return maskTaxId(value)
    case 'phone':
      return maskPhone(value)
    case 'email':
      return maskEmail(value)
    default:
      return maskGeneric(value)
  }
}

export function useMaskedField(
  value: string | undefined | null,
  type: MaskFieldType = 'generic',
) {
  const { hasRole } = useCapabilities()
  const canViewUnmasked = hasRole('COMPLIANCE')
  const [revealed, setRevealed] = useState(false)

  const displayValue = useMemo(() => {
    if (!value) return '—'
    if (canViewUnmasked || revealed) return value
    return maskPiiValue(value, type)
  }, [value, type, canViewUnmasked, revealed])

  const toggleReveal = useCallback(() => {
    if (canViewUnmasked) return
    setRevealed((prev) => !prev)
  }, [canViewUnmasked])

  return {
    displayValue,
    isMasked: !canViewUnmasked && !revealed,
    canToggle: !canViewUnmasked && Boolean(value),
    revealed,
    toggleReveal,
  }
}
