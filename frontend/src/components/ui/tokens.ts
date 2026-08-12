/**
 * PCIS design tokens — mirrors wireframe JSON (light theme defaults).
 */
export const designTokens = {
  colors: {
    info: { dark: '#78a9ff', light: '#0043ce' },
    error: { dark: '#ff8389', light: '#da1e28' },
    accent: { dark: '#be95ff', light: '#8a3ffc' },
    border: { dark: '#30363d', light: '#dcdcdc' },
    nav_bg: { dark: '#0a0d12', light: '#161616' },
    info_bg: { dark: '#0f2440', light: '#edf5ff' },
    primary: { dark: '#78a9ff', light: '#0f62fe' },
    success: { dark: '#42be65', light: '#0e6027' },
    surface: { dark: '#161b22', light: '#ffffff' },
    warning: { dark: '#f1c21b', light: '#8e6a00' },
    error_bg: { dark: '#341316', light: '#fff1f1' },
    secondary: { dark: '#c6c6c6', light: '#393939' },
    background: { dark: '#0d1117', light: '#f4f4f4' },
    nav_active: { dark: '#21262d', light: '#333333' },
    success_bg: { dark: '#0c2b16', light: '#defbe6' },
    text_muted: { dark: '#8d8d8d', light: '#6f6f6f' },
    warning_bg: { dark: '#2e2510', light: '#fcf4d6' },
    surface_alt: { dark: '#1c2128', light: '#f7f7f8' },
    text_primary: { dark: '#f4f4f4', light: '#161616' },
    primary_hover: { dark: '#a6c8ff', light: '#0353e9' },
    text_secondary: { dark: '#c6c6c6', light: '#4c4c4c' },
  },
  shadows: {
    sm: { dark: '0 1px 2px rgba(0,0,0,.5)', light: '0 1px 2px rgba(0,0,0,.08)' },
    md: { dark: '0 2px 6px rgba(0,0,0,.55)', light: '0 2px 6px rgba(0,0,0,.10)' },
    lg: { dark: '0 8px 20px rgba(0,0,0,.6)', light: '0 8px 20px rgba(0,0,0,.14)' },
    xl: { dark: '0 16px 40px rgba(0,0,0,.7)', light: '0 16px 40px rgba(0,0,0,.18)' },
  },
  spacing: {
    '1': '4px',
    '2': '8px',
    '3': '12px',
    '4': '16px',
    '6': '24px',
    '8': '32px',
    '12': '48px',
    '16': '64px',
    '24': '96px',
  },
  typography: {
    sizes: { lg: '16px', md: '14px', sm: '12px', xl: '20px', xs: '11px', '2xl': '24px', '3xl': '32px' },
    usage:
      'sm (12px) is the default density for data grids mirroring 5250 subfile efficiency; mono is mandatory for all monetary and identifier values',
    weights: { bold: 700, medium: 500, regular: 400, semibold: 600 },
    font_mono: "'IBM Plex Mono', ui-monospace, SFMono-Regular, monospace",
    font_sans: "'IBM Plex Sans', -apple-system, 'Segoe UI', Roboto, sans-serif",
    line_heights: { snug: 1.3, tight: 1.2, normal: 1.45, relaxed: 1.6 },
  },
  border_radius: { lg: '8px', md: '4px', sm: '2px', full: '999px' },
} as const

export const colors = {
  primary: designTokens.colors.primary,
  semantic: {
    success: designTokens.colors.success,
    warning: designTokens.colors.warning,
    error: designTokens.colors.error,
    info: designTokens.colors.info,
  },
  surface: designTokens.colors.surface,
  background: designTokens.colors.background,
} as const

export const typography = {
  fontFamily: {
    sans: designTokens.typography.font_sans,
    mono: designTokens.typography.font_mono,
  },
  fontSize: designTokens.typography.sizes,
  fontWeight: designTokens.typography.weights,
  lineHeight: designTokens.typography.line_heights,
} as const

export const spacing = designTokens.spacing
export const radii = designTokens.border_radius
export const elevation = {
  sm: designTokens.shadows.sm.light,
  md: designTokens.shadows.md.light,
  lg: designTokens.shadows.lg.light,
  xl: designTokens.shadows.xl.light,
} as const

export const breakpoints = { sm: 640, md: 768, lg: 1024, xl: 1280 } as const

export const tokens = { colors, typography, spacing, radii, elevation, breakpoints, designTokens } as const
export type Tokens = typeof tokens
