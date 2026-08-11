/**
 * PCIS design tokens (TypeScript mirrors of CSS custom properties).
 * Visual direction: deep teal + charcoal + warm off-white.
 */
export const colors = {
  primary: {
    50: '#e8f6f5',
    100: '#c5eae7',
    200: '#9fd9d4',
    300: '#6fc2bb',
    400: '#3ea39b',
    500: '#1f877f',
    600: '#0b6e6a',
    700: '#085853',
    800: '#064540',
    900: '#03312e',
  },
  neutral: {
    0: '#ffffff',
    50: '#f5f2eb',
    100: '#ebe6dc',
    200: '#d6cfc0',
    300: '#b8b0a0',
    400: '#8f8778',
    500: '#6b6458',
    600: '#4a453c',
    700: '#322e28',
    800: '#1a2332',
    900: '#0f141c',
  },
  success: { 50: '#e8f7ef', 500: '#1f8a4c', 700: '#146338' },
  warning: { 50: '#fff6e5', 500: '#c47a00', 700: '#8a5600' },
  error: { 50: '#fdecea', 500: '#c0392b', 700: '#8e2a1f' },
  info: { 50: '#e8f1f8', 500: '#1f5f8a', 700: '#144262' },
} as const

export const typography = {
  fontFamily: {
    sans: "'IBM Plex Sans', 'Segoe UI', sans-serif",
    mono: "'IBM Plex Mono', ui-monospace, monospace",
  },
  fontSize: {
    xs: '0.75rem',
    sm: '0.875rem',
    md: '1rem',
    lg: '1.125rem',
    xl: '1.25rem',
    '2xl': '1.5rem',
  },
  fontWeight: {
    regular: 400,
    medium: 500,
    semibold: 600,
    bold: 700,
  },
  lineHeight: {
    tight: 1.25,
    normal: 1.5,
    relaxed: 1.625,
  },
} as const

/** Spacing scale with 4px base unit */
export const spacing = {
  0: '0',
  1: '0.25rem',
  2: '0.5rem',
  3: '0.75rem',
  4: '1rem',
  5: '1.25rem',
  6: '1.5rem',
  8: '2rem',
  10: '2.5rem',
  12: '3rem',
  16: '4rem',
} as const

export const radii = {
  sm: '0.25rem',
  md: '0.375rem',
  lg: '0.5rem',
  xl: '0.75rem',
  full: '9999px',
} as const

export const elevation = {
  sm: '0 1px 2px rgb(15 20 28 / 0.08)',
  md: '0 4px 12px rgb(15 20 28 / 0.12)',
  lg: '0 12px 28px rgb(15 20 28 / 0.16)',
  xl: '0 20px 40px rgb(15 20 28 / 0.2)',
} as const

export const breakpoints = {
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
} as const

export const tokens = {
  colors,
  typography,
  spacing,
  radii,
  elevation,
  breakpoints,
} as const

export type Tokens = typeof tokens
