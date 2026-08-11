import type { AuthSession } from './types'

const SESSION_PATH = '/api/auth/session'
const CALLBACK_PATH = '/api/auth/callback'
const LOGOUT_PATH = '/api/auth/logout'

const fetchOptions: RequestInit = {
  credentials: 'include',
  headers: { Accept: 'application/json' },
}

export async function fetchSession(): Promise<AuthSession> {
  const response = await fetch(SESSION_PATH, fetchOptions)
  if (response.status === 401) {
    return { authenticated: false, user: null }
  }
  if (!response.ok) {
    throw new Error(`Session request failed (${response.status})`)
  }
  return (await response.json()) as AuthSession
}

/** Hand OIDC callback result to the BFF so it can set httpOnly session cookies. */
export async function establishSession(code: string, state: string): Promise<void> {
  const response = await fetch(CALLBACK_PATH, {
    ...fetchOptions,
    method: 'POST',
    headers: { ...fetchOptions.headers, 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, state }),
  })
  if (!response.ok) {
    throw new Error(`Session establishment failed (${response.status})`)
  }
}

export async function logoutSession(): Promise<void> {
  await fetch(LOGOUT_PATH, { ...fetchOptions, method: 'POST' })
}
