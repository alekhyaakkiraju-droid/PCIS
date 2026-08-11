/** Keycloak realm roles mapped from legacy ROLE_MENU_T. */
export type PcisRole =
  | 'CLAIMS_ADJUSTER'
  | 'CLAIMS_SUPERVISOR'
  | 'CSR'
  | 'UNDERWRITER'
  | 'FINANCE'
  | 'COMPLIANCE'
  | 'BATCH_SVC'

export type SessionUser = {
  sub: string
  name: string
  email: string
  roles: PcisRole[]
  authority_limit?: number
}

export type AuthSession = {
  authenticated: boolean
  user: SessionUser | null
}

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'
