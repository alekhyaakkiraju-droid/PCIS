import type { User } from 'oidc-client-ts'
import type { PcisRole, SessionUser } from './types'

const PCIS_ROLES: PcisRole[] = [
  'ADMIN',
  'CLAIMS_ADJUSTER',
  'CLAIMS_SUPERVISOR',
  'CSR',
  'UNDERWRITER',
  'FINANCE',
  'COMPLIANCE',
  'BATCH_SVC',
]

function parseRoles(profile: Record<string, unknown>): PcisRole[] {
  const realmAccess = profile.realm_access as { roles?: string[] } | undefined
  const roles = realmAccess?.roles ?? []
  return roles.filter((role): role is PcisRole => PCIS_ROLES.includes(role as PcisRole))
}

export function sessionUserFromOidcUser(user: User | null | undefined): SessionUser | null {
  if (!user?.profile) {
    return null
  }

  const profile = user.profile as Record<string, unknown>
  const authorityLimit = profile.authority_limit
  const parsedLimit =
    typeof authorityLimit === 'number'
      ? authorityLimit
      : typeof authorityLimit === 'string' && authorityLimit.trim() !== ''
        ? Number(authorityLimit)
        : undefined

  return {
    sub: String(profile.sub ?? ''),
    name: String(profile.name ?? profile.preferred_username ?? ''),
    email: String(profile.email ?? ''),
    roles: parseRoles(profile),
    authority_limit: Number.isFinite(parsedLimit) ? parsedLimit : undefined,
  }
}
