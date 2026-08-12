import { useMemo } from 'react'
import { useAuth } from './AuthContext'
import { canAccessNavItem, hasAnyRole, hasPermission, permissionsForRoles, type PcisPermission } from './capabilities'
import { isRouteAllowedForRoles, requiredRolesForPath, type NavLink } from '@/layout/nav-config'
import { useDemoRole } from '@/demo/demo-role'
import type { PcisRole } from './types'

export type Capabilities = {
  /** Effective roles after dev demo-role override. */
  roles: PcisRole[]
  hasRole: (role: PcisRole) => boolean
  hasAnyRole: (required: PcisRole[]) => boolean
  hasPermission: (permission: PcisPermission) => boolean
  permissions: Set<PcisPermission>
  canAccessRoute: (pathname: string) => boolean
  canAccessNavItem: (item: NavLink) => boolean
  requiredRolesForPath: (pathname: string) => PcisRole[] | undefined
  authorityLimit: number | undefined
}

/**
 * PRD capability gating — derives effective roles from the validated session
 * (with optional dev demo-role override) and exposes route/nav permission checks.
 */
export function useCapabilities(): Capabilities {
  const { user, status } = useAuth()
  const { effectiveRoles } = useDemoRole()

  const roles = useMemo(
    () => (status === 'authenticated' && user ? effectiveRoles(user.roles) : []),
    [status, user, effectiveRoles],
  )

  const permissions = useMemo(() => permissionsForRoles(roles), [roles])

  return useMemo(
    () => ({
      roles,
      hasRole: (role: PcisRole) => roles.includes(role),
      hasAnyRole: (required: PcisRole[]) => hasAnyRole(roles, required),
      hasPermission: (permission: PcisPermission) => hasPermission(roles, permission),
      permissions,
      canAccessRoute: (pathname: string) => isRouteAllowedForRoles(pathname, roles),
      canAccessNavItem: (item: NavLink) => canAccessNavItem(roles, item),
      requiredRolesForPath: (pathname: string) => requiredRolesForPath(pathname),
      authorityLimit: user?.authority_limit,
    }),
    [roles, permissions, user?.authority_limit],
  )
}
