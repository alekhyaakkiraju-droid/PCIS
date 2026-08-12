import { describe, expect, it } from 'vitest'
import { breakpoints, designTokens, elevation, radii, spacing, tokens, typography } from './tokens'

describe('design tokens', () => {
  it('exposes wireframe token groups', () => {
    expect(tokens.colors.primary.light).toBe('#0f62fe')
    expect(designTokens.colors.nav_bg.light).toBe('#161616')
    expect(typography.fontSize.sm).toBe('12px')
    expect(spacing['4']).toBe('16px')
    expect(radii.md).toBe('4px')
    expect(elevation.md).toContain('rgba')
    expect(breakpoints.md).toBe(768)
  })
})
