import { AppTopBar } from './AppTopBar'
import { AuthorizedOutlet } from './AuthorizedOutlet'
import { Sidebar } from './Sidebar'

export function AppLayout() {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="app-content">
        <AppTopBar />
        <main className="app-main">
          <AuthorizedOutlet />
        </main>
      </div>
    </div>
  )
}
