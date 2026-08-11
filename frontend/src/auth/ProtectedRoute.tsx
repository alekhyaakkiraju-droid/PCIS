import { Navigate, useLocation } from 'react-router'
import { useAuth } from './AuthContext'
import { isRouteAllowedForRoles } from './role-menu-config'
import type { PcisRole } from './types'
import { ForbiddenPage } from '../pages/ForbiddenPage'

export type ProtectedRouteProps = {
  children: React.ReactNode
  roles?: PcisRole[]
}

export function ProtectedRoute({ children, roles }: ProtectedRouteProps) {
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

  const allowed = isRouteAllowedForRoles(location.pathname, user.roles)

  if (!allowed) {
    return <ForbiddenPage />
  }

  if (roles && !roles.some((role) => user.roles.includes(role))) {
    return <ForbiddenPage />
  }

  return <>{children}</>
}

/** Convenience redirect for unknown authenticated routes. */
export function AuthenticatedIndexRedirect() {
  return <Navigate to="/" replace />
}
