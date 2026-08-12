import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { UserManager } from 'oidc-client-ts'
import { isBearerAuthMode } from './auth-mode'
import { sessionUserFromOidcUser } from './jwt-user'
import { createOidcSettings } from './oidc-config'
import { setAccessTokenProvider } from './token-provider'
import { fetchSession, logoutSession } from './session-api'
import type { AuthStatus, SessionUser } from './types'

const RETURN_URL_KEY = 'pcis.auth.returnUrl'

export type AuthContextValue = {
  status: AuthStatus
  user: SessionUser | null
  login: (returnUrl?: string) => Promise<void>
  logout: () => Promise<void>
  refreshSession: () => Promise<void>
  hasRole: (role: string) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export type AuthProviderProps = {
  children: ReactNode
  userManager?: UserManager
}

export function AuthProvider({ children, userManager: userManagerProp }: AuthProviderProps) {
  const userManagerRef = useRef<UserManager | null>(userManagerProp ?? null)
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<SessionUser | null>(null)

  if (!userManagerRef.current && typeof window !== 'undefined' && !userManagerProp) {
    userManagerRef.current = new UserManager(createOidcSettings())
  } else if (userManagerProp && userManagerRef.current !== userManagerProp) {
    userManagerRef.current = userManagerProp
  }

  const refreshSession = useCallback(async () => {
    if (isBearerAuthMode()) {
      const manager = userManagerRef.current
      if (!manager) {
        setUser(null)
        setStatus('unauthenticated')
        return
      }
      const oidcUser = await manager.getUser()
      const sessionUser = sessionUserFromOidcUser(oidcUser)
      if (sessionUser) {
        setUser(sessionUser)
        setStatus('authenticated')
      } else {
        setUser(null)
        setStatus('unauthenticated')
      }
      return
    }

    const session = await fetchSession()
    if (session.authenticated && session.user) {
      setUser(session.user)
      setStatus('authenticated')
    } else {
      setUser(null)
      setStatus('unauthenticated')
    }
  }, [])

  useEffect(() => {
    if (isBearerAuthMode()) {
      setAccessTokenProvider(async () => {
        const manager = userManagerRef.current
        if (!manager) {
          return null
        }
        let oidcUser = await manager.getUser()
        if (oidcUser?.expired) {
          try {
            oidcUser = await manager.signinSilent()
          } catch {
            return null
          }
        }
        return oidcUser?.access_token ?? null
      })
    } else {
      setAccessTokenProvider(null)
    }

    void refreshSession()
    return () => setAccessTokenProvider(null)
  }, [refreshSession])

  const login = useCallback(async (returnUrl?: string) => {
    const manager = userManagerRef.current
    if (!manager) {
      throw new Error('UserManager is not available')
    }
    const target = returnUrl ?? `${window.location.pathname}${window.location.search}`
    sessionStorage.setItem(RETURN_URL_KEY, target)
    await manager.signinRedirect({ state: target })
  }, [])

  const logout = useCallback(async () => {
    if (!isBearerAuthMode()) {
      await logoutSession()
    }
    setUser(null)
    setStatus('unauthenticated')
    const manager = userManagerRef.current
    if (manager) {
      await manager.removeUser()
      await manager.signoutRedirect()
    }
  }, [])

  const hasRole = useCallback(
    (role: string) => user?.roles.includes(role as SessionUser['roles'][number]) ?? false,
    [user],
  )

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      login,
      logout,
      refreshSession,
      hasRole,
    }),
    [status, user, login, logout, refreshSession, hasRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

export function consumeReturnUrl(): string {
  const stored = sessionStorage.getItem(RETURN_URL_KEY) ?? '/'
  sessionStorage.removeItem(RETURN_URL_KEY)
  return stored
}

export { RETURN_URL_KEY }
