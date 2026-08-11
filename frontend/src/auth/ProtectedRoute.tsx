import { useLocation } from 'react-router'
import { useAuth } from './AuthContext'

export type ProtectedRouteProps = {
  children: React.ReactNode
}

/** Ensures the user is authenticated. Route-level RBAC is handled by AuthorizedOutlet. */
export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { status, user, login } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <p role="status">Checking session…</p>
  }

  if (status === 'unauthenticated' || !user) {
    const returnUrl = `${location.pathname}${location.search}`
    void login(returnUrl)
    return <p role="status">Redirecting to sign in…</p>
  }

  return <>{children}</>
}
