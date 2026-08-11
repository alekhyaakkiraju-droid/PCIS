import type { PcisRole } from './types'

export type NavItem = {
  to: string
  label: string
  /** Legacy PCISMENU option code for traceability. */
  menuOption: string
  roles: PcisRole[]
}

/** Role-filtered navigation mirroring legacy ROLE_MENU_T module groups. */
export const NAV_ITEMS: NavItem[] = [
  {
    to: '/',
    label: 'Dashboard',
    menuOption: 'HOME',
    roles: [
      'CLAIMS_ADJUSTER',
      'CLAIMS_SUPERVISOR',
      'CSR',
      'UNDERWRITER',
      'FINANCE',
      'COMPLIANCE',
    ],
  },
  {
    to: '/customers',
    label: 'Customers',
    menuOption: 'CUS',
    roles: ['CSR', 'UNDERWRITER', 'CLAIMS_ADJUSTER', 'CLAIMS_SUPERVISOR', 'COMPLIANCE'],
  },
  {
    to: '/policies',
    label: 'Policies',
    menuOption: 'POL',
    roles: ['UNDERWRITER', 'CSR', 'CLAIMS_ADJUSTER', 'CLAIMS_SUPERVISOR'],
  },
  {
    to: '/claims',
    label: 'Claims',
    menuOption: 'CLM',
    roles: ['CLAIMS_ADJUSTER', 'CLAIMS_SUPERVISOR', 'COMPLIANCE'],
  },
  {
    to: '/billing',
    label: 'Billing',
    menuOption: 'BIL',
    roles: ['FINANCE', 'CSR', 'COMPLIANCE'],
  },
  {
    to: '/reports',
    label: 'Reports',
    menuOption: 'RPT',
    roles: ['FINANCE', 'COMPLIANCE', 'CLAIMS_SUPERVISOR'],
  },
]

export function filterNavItemsForRoles(roles: PcisRole[]): NavItem[] {
  const roleSet = new Set(roles)
  return NAV_ITEMS.filter((item) => item.roles.some((role) => roleSet.has(role)))
}

export function isRouteAllowedForRoles(pathname: string, roles: PcisRole[]): boolean {
  const normalized = pathname === '' ? '/' : pathname
  const item = NAV_ITEMS.find((nav) => nav.to === normalized)
  if (!item) {
    return true
  }
  return item.roles.some((role) => roles.includes(role))
}

export function requiredRolesForPath(pathname: string): PcisRole[] | undefined {
  const normalized = pathname === '' ? '/' : pathname
  return NAV_ITEMS.find((nav) => nav.to === normalized)?.roles
}
