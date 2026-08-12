import { NavLink } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { useCapabilities } from '../auth/useCapabilities'
import { allNavSections } from './nav-config'
import { formatRoleList } from '../demo/demo-role'
import { useNavBadges } from './useNavBadges'

function NavIcon({ name }: { name: string }) {
  const props = { width: 18, height: 18, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.5 }
  switch (name) {
    case 'dashboard':
      return (
        <svg {...props}>
          <rect x="3" y="3" width="7" height="7" />
          <rect x="14" y="3" width="7" height="7" />
          <rect x="14" y="14" width="7" height="7" />
          <rect x="3" y="14" width="7" height="7" />
        </svg>
      )
    case 'design':
      return (
        <svg {...props}>
          <circle cx="12" cy="12" r="9" />
          <path d="M12 3a9 9 0 0 1 0 18 4.5 4.5 0 0 1 0-9 4.5 4.5 0 0 0 0-9z" />
        </svg>
      )
    case 'fnol':
      return (
        <svg {...props}>
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <path d="M14 2v6h6" />
          <path d="M8 13h8M8 17h8M8 9h2" />
        </svg>
      )
    case 'inquiry':
      return (
        <svg {...props}>
          <circle cx="10" cy="10" r="6" />
          <path d="M20 20l-4-4" />
        </svg>
      )
    case 'payment':
      return (
        <svg {...props}>
          <path d="M12 2l8 4v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6z" />
          <path d="M9 12l2 2 4-4" />
        </svg>
      )
    case 'customer':
      return (
        <svg {...props}>
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21c0-4 4-6 8-6s8 2 8 6" />
        </svg>
      )
    case 'policy':
      return (
        <svg {...props}>
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <path d="M14 2v6h6" />
          <path d="M9 15l2 2 4-4" />
        </svg>
      )
    case 'billing':
      return (
        <svg {...props}>
          <path d="M6 2h12v20l-3-2-3 2-3-2-3 2z" />
          <path d="M9 7h6M9 11h6" />
        </svg>
      )
    case 'batch':
      return (
        <svg {...props}>
          <rect x="3" y="4" width="18" height="6" rx="1" />
          <rect x="3" y="14" width="18" height="6" rx="1" />
          <path d="M7 7h.01M7 17h.01" />
        </svg>
      )
    case 'admin':
      return (
        <svg {...props}>
          <path d="M4 6h16M4 12h16M4 18h16" />
          <circle cx="9" cy="6" r="2" />
          <circle cx="15" cy="12" r="2" />
          <circle cx="7" cy="18" r="2" />
        </svg>
      )
    default:
      return null
  }
}

const ICON_BY_PATH: Record<string, string> = {
  '/': 'dashboard',
  '/design-system': 'design',
  '/claims/fnol': 'fnol',
  '/claims': 'inquiry',
  '/claims/payments': 'payment',
  '/customers': 'customer',
  '/policies': 'policy',
  '/billing': 'billing',
  '/batch': 'batch',
  '/admin': 'admin',
}

function NavItemLabel({ label, badge }: { label: string; badge?: string }) {
  return (
    <>
      <span>{label}</span>
      {badge ? <span className="app-sidebar__link-badge">{badge}</span> : null}
    </>
  )
}

export function Sidebar() {
  const { user, status, login, logout } = useAuth()
  const { canAccessNavItem } = useCapabilities()
  const sections = user && status === 'authenticated' ? allNavSections() : []
  const liveBadges = useNavBadges()

  return (
    <aside className="app-sidebar" aria-label="Module navigation">
      <div className="app-sidebar__brand">
        <strong>PCIS</strong>
      </div>
      <nav aria-label="Primary">
        {sections.map((section) => (
          <div key={section.title ?? 'root'} className="app-sidebar__section">
            {section.title ? <div className="app-sidebar__section-title">{section.title}</div> : null}
            {section.items.map((link) => {
              const allowed = canAccessNavItem(link)
              const deniedTitle =
                link.roles.length > 0
                  ? `Requires ${formatRoleList(link.roles)} role`
                  : undefined
              const badge = liveBadges[link.to] ?? link.badge

              if (!allowed) {
                return (
                  <span
                    key={link.to}
                    className="app-sidebar__link app-sidebar__link--disabled"
                    aria-disabled="true"
                    aria-label={link.label}
                    title={deniedTitle}
                    role="link"
                  >
                    <NavIcon name={ICON_BY_PATH[link.to] ?? 'dashboard'} />
                    <NavItemLabel label={link.label} badge={badge} />
                  </span>
                )
              }

              return (
                <NavLink
                  key={link.to}
                  to={link.to}
                  end={link.to === '/'}
                  className="app-sidebar__link"
                  aria-label={link.label}
                  title={deniedTitle}
                >
                  <NavIcon name={ICON_BY_PATH[link.to] ?? 'dashboard'} />
                  <NavItemLabel label={link.label} badge={badge} />
                </NavLink>
              )
            })}
          </div>
        ))}
      </nav>
      <div className="app-sidebar__footer">Phase 2 · Claims live · Billing parallel run</div>
      <div className="app-sidebar__actions">
        {status === 'authenticated' && user ? (
          <button type="button" className="app-sidebar__signout" onClick={() => void logout()}>
            Sign out ({user.name})
          </button>
        ) : (
          <button type="button" className="app-sidebar__signout" onClick={() => void login('/')}>
            Sign in
          </button>
        )}
      </div>
    </aside>
  )
}
