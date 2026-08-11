import { Outlet, useLocation } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { useDemoRole } from '@/demo/demo-role'
import { isRouteAllowedForRoles } from './nav-config'
import { ForbiddenPage } from '@/pages/ForbiddenPage'

/**
 * Renders route content or an inline 403 card (wireframe behavior).
 * Sidebar and top bar remain visible when access is denied.
 */
export function AuthorizedOutlet() {
  const { pathname } = useLocation()
  const { user, status } = useAuth()
  const { effectiveRoles } = useDemoRole()

  if (status !== 'authenticated' || !user) {
    return <Outlet />
  }

  const allowed = isRouteAllowedForRoles(pathname, effectiveRoles(user.roles))
  if (!allowed) {
    return <ForbiddenPage />
  }

  return <Outlet />
}
