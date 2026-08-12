export type AuthMode = 'bff' | 'bearer'

/** BFF cookie sessions for local dev; bearer JWT for deployed environments. */
export function getAuthMode(): AuthMode {
  const mode = import.meta.env.VITE_AUTH_MODE
  return mode === 'bearer' ? 'bearer' : 'bff'
}

export function isBearerAuthMode(): boolean {
  return getAuthMode() === 'bearer'
}
