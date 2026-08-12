import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { PcisRole } from '@/auth/types'

export type DemoPersona = {
  role: PcisRole
  label: string
  displayName: string
  initials: string
}

/** Wireframe personas from PCIS Modernization UI.dc.html */
export const DEMO_PERSONAS: DemoPersona[] = [
  { role: 'ADMIN', label: 'Admin (All Access)', displayName: 'A. Root', initials: 'AR' },
  { role: 'CLAIMS_ADJUSTER', label: 'Claims Adjuster', displayName: 'K. Alvarez', initials: 'KA' },
  { role: 'CLAIMS_SUPERVISOR', label: 'Claims Supervisor', displayName: 'M. Kowalski', initials: 'MK' },
  { role: 'CSR', label: 'CSR', displayName: 'J. Reyes', initials: 'JR' },
  { role: 'UNDERWRITER', label: 'Underwriter', displayName: 'D. Alvarado', initials: 'DA' },
  { role: 'COMPLIANCE', label: 'Compliance', displayName: 'S. Mehta', initials: 'SM' },
  { role: 'FINANCE', label: 'Finance', displayName: 'F. Nguyen', initials: 'FN' },
  { role: 'BATCH_SVC', label: 'Batch Operator', displayName: 'B. Frank', initials: 'BF' },
]

const STORAGE_KEY = 'pcis.demoRole'

type DemoRoleContextValue = {
  demoEnabled: boolean
  demoRole: PcisRole | null
  setDemoRole: (role: PcisRole | null) => void
  effectiveRoles: (sessionRoles: PcisRole[]) => PcisRole[]
  activePersona: (sessionRoles: PcisRole[]) => DemoPersona | null
}

const DemoRoleContext = createContext<DemoRoleContextValue | null>(null)

function readStoredRole(): PcisRole | null {
  if (typeof window === 'undefined') return null
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  return DEMO_PERSONAS.some((p) => p.role === raw) ? (raw as PcisRole) : null
}

export function DemoRoleProvider({ children }: { children: ReactNode }) {
  const demoEnabled = import.meta.env.DEV
  const [demoRole, setDemoRoleState] = useState<PcisRole | null>(() =>
    demoEnabled ? readStoredRole() : null,
  )

  const setDemoRole = useCallback(
    (role: PcisRole | null) => {
      if (!demoEnabled) return
      setDemoRoleState(role)
      if (role) sessionStorage.setItem(STORAGE_KEY, role)
      else sessionStorage.removeItem(STORAGE_KEY)
    },
    [demoEnabled],
  )

  const effectiveRoles = useCallback(
    (sessionRoles: PcisRole[]) => {
      if (demoEnabled && demoRole) return [demoRole]
      return sessionRoles
    },
    [demoEnabled, demoRole],
  )

  const activePersona = useCallback(
    (sessionRoles: PcisRole[]) => {
      const role = demoEnabled && demoRole ? demoRole : sessionRoles[0]
      if (!role) return null
      return DEMO_PERSONAS.find((p) => p.role === role) ?? null
    },
    [demoEnabled, demoRole],
  )

  const value = useMemo(
    () => ({ demoEnabled, demoRole, setDemoRole, effectiveRoles, activePersona }),
    [demoEnabled, demoRole, setDemoRole, effectiveRoles, activePersona],
  )

  return <DemoRoleContext.Provider value={value}>{children}</DemoRoleContext.Provider>
}

export function useDemoRole() {
  const ctx = useContext(DemoRoleContext)
  if (!ctx) {
    throw new Error('useDemoRole must be used within DemoRoleProvider')
  }
  return ctx
}

export function formatRoleList(roles: PcisRole[]): string {
  return roles.map((r) => r.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())).join(' or ')
}
