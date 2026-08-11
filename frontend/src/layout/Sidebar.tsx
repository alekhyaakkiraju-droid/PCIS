import { NavLink } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { filterNavItemsForRoles } from '../auth/role-menu-config'
import { Button } from '../components/ui/Button'

export function Sidebar() {
  const { user, status, login, logout } = useAuth()
  const links =
    user && status === 'authenticated' ? filterNavItemsForRoles(user.roles) : []

  return (
    <aside className="app-sidebar" aria-label="Module navigation">
      <div className="app-sidebar__brand">
        <strong>PCIS</strong>
      </div>
      <nav aria-label="Primary">
        {links.map((link) => (
          <NavLink key={link.to} to={link.to} end={link.to === '/'}>
            {link.label}
          </NavLink>
        ))}
      </nav>
      <div className="app-sidebar__actions">
        {status === 'authenticated' && user ? (
          <>
            <span className="app-sidebar__user">{user.name}</span>
            <Button type="button" variant="ghost" onClick={() => void logout()}>
              Sign out
            </Button>
          </>
        ) : (
          <Button type="button" variant="secondary" onClick={() => void login('/')}>
            Sign in
          </Button>
        )}
      </div>
    </aside>
  )
}
