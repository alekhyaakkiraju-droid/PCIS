import { useLocation } from 'react-router'
import batchJobsFixture from '../../fixtures/batch/jobs.json'
import { useAuth } from '../auth/AuthContext'
import type { PcisRole } from '../auth/types'
import { resolveRouteTitle } from './nav-config'
import { DEMO_PERSONAS, useDemoRole } from '../demo/demo-role'
import { Badge } from '../components/ui/Badge'
import { ThemeToggle } from '../components/ui/ThemeToggle'

function initials(name: string): string {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}

export function AppTopBar() {
  const { pathname } = useLocation()
  const { user, status } = useAuth()
  const { demoEnabled, demoRole, setDemoRole, activePersona } = useDemoRole()
  const screenTitle = resolveRouteTitle(pathname)

  const persona = user ? activePersona(user.roles) : null
  const limitLabel =
    user?.authority_limit != null
      ? ` · limit ${user.authority_limit.toLocaleString()}`
      : persona?.role === 'CLAIMS_SUPERVISOR'
        ? ' · limit 250,000'
        : ''
  const roleLabel = persona
    ? `${persona.label} — ${persona.displayName}${limitLabel}`
    : user?.roles?.[0]?.replace(/_/g, ' ') ?? 'Guest'
  const avatarInitials = persona?.initials ?? (user ? initials(user.name) : '?')

  const hasFailedJob = batchJobsFixture.jobs.some((j) => j.status === 'Failed')
  const controlStatus = hasFailedJob ? '1 job failed' : 'All controls enforced'

  return (
    <header className="app-topbar" aria-label="Application header">
      <div className="app-topbar__breadcrumb">
        PCIS / <strong>{screenTitle}</strong>
      </div>
      <div className="app-topbar__actions">
        {status === 'authenticated' && user ? (
          <>
            {demoEnabled ? (
              <label className="app-topbar__view-as">
                <span className="app-topbar__view-as-label">View as</span>
                <select
                  className="app-topbar__role-select"
                  aria-label="Demo role switcher"
                  value={demoRole ?? user.roles[0] ?? ''}
                  onChange={(e) => setDemoRole(e.target.value as PcisRole)}
                >
                  {DEMO_PERSONAS.map((p) => (
                    <option key={p.role} value={p.role}>
                      {p.displayName} · {p.label}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <span className="app-topbar__role">{roleLabel}</span>
            )}
            <Badge status={hasFailedJob ? 'Pending' : 'Active'}>{controlStatus}</Badge>
            <ThemeToggle />
            <span className="app-topbar__divider" aria-hidden="true" />
            <span className="app-topbar__avatar" title={roleLabel}>
              {avatarInitials}
            </span>
          </>
        ) : null}
      </div>
    </header>
  )
}
