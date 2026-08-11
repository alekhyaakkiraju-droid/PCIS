import { NavLink, Outlet } from 'react-router'

const links = [
  { to: '/', label: 'Dashboard' },
  { to: '/customers', label: 'Customers' },
  { to: '/policies', label: 'Policies' },
  { to: '/claims', label: 'Claims' },
  { to: '/billing', label: 'Billing' },
  { to: '/reports', label: 'Reports' },
] as const

export function AppLayout() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <strong>PCIS</strong>
        <nav aria-label="Primary">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.to === '/'}>
              {link.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  )
}
