import { describe, expect, it } from 'vitest'
import { breakpoints, colors, elevation, radii, spacing, tokens, typography } from './tokens'

describe('design tokens', () => {
  it('exposes complete token groups', () => {
    expect(tokens.colors.primary[600]).toBe('#0b6e6a')
    expect(colors.neutral[800]).toBe('#1a2332')
    expect(typography.fontSize.md).toBe('1rem')
    expect(spacing[4]).toBe('1rem')
    expect(radii.md).toBeTruthy()
    expect(elevation.md).toContain('rgb')
    expect(breakpoints.md).toBe(768)
  })
})
