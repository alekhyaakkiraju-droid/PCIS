import type { PcisRole } from './types'
import {
  ALL_NAV_ITEMS,
  filterNavSectionsForRoles,
  isRouteAllowedForRoles,
  requiredRolesForPath,
  resolveRouteTitle,
  type NavLink,
} from '../layout/nav-config'

export {
  ALL_NAV_ITEMS as NAV_ITEMS,
  filterNavSectionsForRoles,
  isRouteAllowedForRoles,
  requiredRolesForPath,
  resolveRouteTitle,
  type NavLink,
}

export { useCapabilities } from './useCapabilities'
export type { Capabilities } from './useCapabilities'
export { hasPermission, permissionsForRoles, type PcisPermission } from './capabilities'

export function filterNavItemsForRoles(roles: PcisRole[]): NavLink[] {
  return filterNavSectionsForRoles(roles).flatMap((section) => section.items)
}
