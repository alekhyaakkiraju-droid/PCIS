import { WebStorageStateStore, type UserManagerSettings } from 'oidc-client-ts'

const defaultRedirectUri = () =>
  typeof window !== 'undefined' ? `${window.location.origin}/auth/callback` : ''

const defaultPostLogoutUri = () =>
  typeof window !== 'undefined' ? window.location.origin : ''

export function readOidcEnv() {
  return {
    authority: import.meta.env.VITE_OIDC_AUTHORITY ?? '',
    clientId: import.meta.env.VITE_OIDC_CLIENT_ID ?? 'pcis-web',
    redirectUri: import.meta.env.VITE_OIDC_REDIRECT_URI ?? defaultRedirectUri(),
    postLogoutRedirectUri:
      import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI ?? defaultPostLogoutUri(),
    scope: import.meta.env.VITE_OIDC_SCOPE ?? 'openid profile email roles',
  }
}

/**
 * OIDC UserManager settings for Authorization Code + PKCE.
 *
 * BFF contract: tokens are exchanged server-side and stored in httpOnly cookies.
 * The SPA never persists access or refresh tokens — only transient PKCE state
 * lives in sessionStorage during the redirect dance.
 */
export function createOidcSettings(overrides?: Partial<UserManagerSettings>): UserManagerSettings {
  const env = readOidcEnv()

  return {
    authority: env.authority,
    client_id: env.clientId,
    redirect_uri: env.redirectUri,
    post_logout_redirect_uri: env.postLogoutRedirectUri,
    response_type: 'code',
    scope: env.scope,
    automaticSilentRenew: false,
    loadUserInfo: false,
    monitorSession: false,
    stateStore:
      typeof window !== 'undefined'
        ? new WebStorageStateStore({ store: window.sessionStorage })
        : undefined,
    ...overrides,
  }
}
