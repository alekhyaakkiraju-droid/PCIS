import { describe, expect, it } from 'vitest'
import { formatClaimNbr, normalizeClaimNbr } from './claims-fixture-fallback'

describe('claim number normalization', () => {
  it('pads wireframe shorthand CLM-0004821 to backend CLM000004821', () => {
    expect(normalizeClaimNbr('CLM-0004821')).toBe('CLM000004821')
  })

  it('preserves full display form CLM-000004821', () => {
    expect(normalizeClaimNbr('CLM-000004821')).toBe('CLM000004821')
  })

  it('preserves backend form CLM000004821', () => {
    expect(normalizeClaimNbr('CLM000004821')).toBe('CLM000004821')
  })

  it('preserves newly issued FNOL numbers', () => {
    expect(normalizeClaimNbr('CLM-971323629')).toBe('CLM971323629')
    expect(normalizeClaimNbr('CLM971323629')).toBe('CLM971323629')
  })

  it('formats backend keys for display', () => {
    expect(formatClaimNbr('CLM000004821')).toBe('CLM-000004821')
    expect(formatClaimNbr('CLM-0004821')).toBe('CLM-000004821')
  })
})
