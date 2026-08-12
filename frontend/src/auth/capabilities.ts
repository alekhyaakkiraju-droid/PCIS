import type { PcisRole } from './types'
import type { NavLink } from '@/layout/nav-config'

/** Permission strings aligned with backend SecurityConfig role→scope maps. */
export type PcisPermission =
  | 'claims:read'
  | 'claims:write'
  | 'claims:approve'
  | 'customer:read'
  | 'customer:write'
  | 'billing:read'
  | 'billing:write'
  | 'batch:read'
  | 'batch:write'
  | 'admin:read'

const ALL_PERMISSIONS: PcisPermission[] = [
  'claims:read',
  'claims:write',
  'claims:approve',
  'customer:read',
  'customer:write',
  'billing:read',
  'billing:write',
  'batch:read',
  'batch:write',
  'admin:read',
]

const ROLE_PERMISSIONS: Record<PcisRole, PcisPermission[]> = {
  ADMIN: ALL_PERMISSIONS,
  CLAIMS_ADJUSTER: ['claims:read', 'claims:write'],
  CLAIMS_SUPERVISOR: [
    'claims:read',
    'claims:write',
    'claims:approve',
    'customer:read',
    'customer:write',
    'billing:read',
    'billing:write',
    'batch:read',
    'batch:write',
  ],
  CSR: ['customer:read', 'customer:write', 'claims:read'],
  UNDERWRITER: ['claims:read'],
  FINANCE: ['billing:read', 'billing:write'],
  COMPLIANCE: ['billing:read', 'billing:write', 'batch:read', 'batch:write', 'admin:read', 'claims:read'],
  BATCH_SVC: ['batch:read', 'batch:write'],
}

export function permissionsForRoles(roles: PcisRole[]): Set<PcisPermission> {
  const out = new Set<PcisPermission>()
  for (const role of roles) {
    for (const perm of ROLE_PERMISSIONS[role] ?? []) {
      out.add(perm)
    }
  }
  return out
}

export function hasAnyRole(roles: PcisRole[], required: PcisRole[]): boolean {
  if (roles.includes('ADMIN')) return true
  if (required.length === 0) return true
  return required.some((role) => roles.includes(role))
}

export function hasPermission(roles: PcisRole[], permission: PcisPermission): boolean {
  return permissionsForRoles(roles).has(permission)
}

export function canAccessNavItem(roles: PcisRole[], item: NavLink): boolean {
  return hasAnyRole(roles, item.roles)
}
