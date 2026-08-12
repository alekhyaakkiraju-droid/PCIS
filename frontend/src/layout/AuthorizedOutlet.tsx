import { Outlet, useLocation } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { useCapabilities } from '@/auth/useCapabilities'
import { ForbiddenPage } from '@/pages/ForbiddenPage'

/**
 * Renders route content or an inline 403 card (wireframe behavior).
 * Sidebar and top bar remain visible when access is denied.
 */
export function AuthorizedOutlet() {
  const { pathname } = useLocation()
  const { user, status } = useAuth()
  const { canAccessRoute } = useCapabilities()

  if (status !== 'authenticated' || !user) {
    return <Outlet />
  }

  const allowed = canAccessRoute(pathname)
  if (!allowed) {
    return <ForbiddenPage />
  }

  return <Outlet />
}
