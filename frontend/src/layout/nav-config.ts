import type { PcisRole } from '../auth/types'

export type NavLink = {
  to: string
  label: string
  menuOption: string
  /** Optional count badge shown in wireframe nav */
  badge?: string
  /** Empty array = all authenticated roles (wireframe dashboard / design system). */
  roles: PcisRole[]
}

export type NavSection = {
  title?: string
  items: NavLink[]
}

/** Wireframe screenPerms from PCIS Modernization UI.dc.html */
export const NAV_SECTIONS: NavSection[] = [
  {
    title: 'Platform',
    items: [{ to: '/', label: 'Dashboard', menuOption: 'HOME', roles: [] }],
  },
  {
    title: 'Operations',
    items: [
      {
        to: '/claims/fnol',
        label: 'FNOL Intake',
        menuOption: 'FNOL',
        roles: ['CLAIMS_ADJUSTER', 'CLAIMS_SUPERVISOR'],
      },
      {
        to: '/claims',
        label: 'Claims',
        menuOption: 'CLMINQ',
        badge: '34',
        roles: ['CLAIMS_ADJUSTER', 'CLAIMS_SUPERVISOR'],
      },
      {
        to: '/claims/payments',
        label: 'Payments',
        menuOption: 'CLMPAY',
        roles: ['CLAIMS_ADJUSTER', 'CLAIMS_SUPERVISOR'],
      },
      {
        to: '/customers',
        label: 'Customer 360',
        menuOption: 'CUS',
        roles: ['CSR', 'CLAIMS_SUPERVISOR'],
      },
      {
        to: '/policies',
        label: 'Policies',
        menuOption: 'POL',
        badge: '7',
        roles: ['UNDERWRITER', 'CLAIMS_SUPERVISOR'],
      },
      {
        to: '/billing',
        label: 'Billing',
        menuOption: 'BIL',
        badge: '12',
        roles: ['COMPLIANCE', 'CLAIMS_SUPERVISOR', 'FINANCE'],
      },
    ],
  },
  {
    title: 'Control',
    items: [
      {
        to: '/batch',
        label: 'Batch Operations',
        menuOption: 'BAT',
        badge: '1',
        roles: ['COMPLIANCE', 'CLAIMS_SUPERVISOR', 'BATCH_SVC'],
      },
      {
        to: '/admin',
        label: 'Admin & Compliance',
        menuOption: 'ADM',
        roles: ['COMPLIANCE'],
      },
    ],
  },
]

export const ALL_NAV_ITEMS: NavLink[] = NAV_SECTIONS.flatMap((section) => section.items)

export const ROUTE_TITLES: Record<string, string> = {
  '/': 'Operations Dashboard',
  '/design-system': 'Design System',
  '/claims/fnol': 'Claim FNOL Intake',
  '/claims': 'Claim Inquiry',
  '/claims/payments': 'Claim Payment & Authority Approval',
  '/customers': 'Customer 360',
  '/policies': 'Policy Issuance & Premium Breakdown',
  '/billing': 'Billing Generation & Parallel-Run Reconciliation',
  '/batch': 'Batch Operations Console',
  '/admin': 'Admin — Tunables, Classification & Audit Retention',
}

/** Sidebar always shows every wireframe nav item; RBAC is enforced on the route body. */
export function allNavSections(): NavSection[] {
  return NAV_SECTIONS
}

export function resolveRouteTitle(pathname: string): string {
  if (pathname.startsWith('/customers/')) return 'Customer 360'
  const normalized = pathname.replace(/\/$/, '') || '/'
  const exact = ROUTE_TITLES[normalized]
  if (exact) return exact
  const match = Object.entries(ROUTE_TITLES)
    .filter(([path]) => path !== '/')
    .sort((a, b) => b[0].length - a[0].length)
    .find(([path]) => normalized.startsWith(path))
  return match?.[1] ?? 'PCIS'
}

function resolveNavItem(pathname: string): NavLink | undefined {
  const normalized = pathname.replace(/\/$/, '') || '/'
  if (normalized.startsWith('/customers/')) {
    return ALL_NAV_ITEMS.find((i) => i.to === '/customers')
  }
  return (
    ALL_NAV_ITEMS.find((nav) => nav.to === normalized) ??
    ALL_NAV_ITEMS.find((nav) => nav.to !== '/' && normalized.startsWith(nav.to))
  )
}

export function isRouteAllowedForRoles(pathname: string, roles: PcisRole[]): boolean {
  const item = resolveNavItem(pathname)
  if (!item || item.roles.length === 0) return true
  return item.roles.some((role) => roles.includes(role))
}

export function requiredRolesForPath(pathname: string): PcisRole[] | undefined {
  const item = resolveNavItem(pathname)
  if (!item || item.roles.length === 0) return undefined
  return item.roles
}

/** @deprecated Prefer useCapabilities().canAccessNavItem — wireframe shows all links with disabled state. */
export function filterNavSectionsForRoles(roles: PcisRole[]): NavSection[] {
  const roleSet = new Set(roles)
  return NAV_SECTIONS.map((section) => ({
    ...section,
    items: section.items.filter((item) => item.roles.length === 0 || item.roles.some((role) => roleSet.has(role))),
  })).filter((section) => section.items.length > 0)
}
