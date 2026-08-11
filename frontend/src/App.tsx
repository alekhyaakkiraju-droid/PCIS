import { lazy, Suspense } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { AppLayout } from './layout/AppLayout'
import { ForbiddenPage } from './pages/ForbiddenPage'
import { LoginCallback } from './pages/LoginCallback'

const DashboardPage = lazy(() =>
  import('./pages/stubs').then((m) => ({ default: m.DashboardPage })),
)
const CustomersPage = lazy(() =>
  import('./pages/stubs').then((m) => ({ default: m.CustomersPage })),
)
const PoliciesPage = lazy(() =>
  import('./pages/stubs').then((m) => ({ default: m.PoliciesPage })),
)
const ClaimsPage = lazy(() => import('./pages/stubs').then((m) => ({ default: m.ClaimsPage })))
const BillingPage = lazy(() =>
  import('./pages/stubs').then((m) => ({ default: m.BillingPage })),
)
const ReportsPage = lazy(() =>
  import('./pages/stubs').then((m) => ({ default: m.ReportsPage })),
)

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Suspense fallback={<p>Loading…</p>}>
          <Routes>
            <Route path="/auth/callback" element={<LoginCallback />} />
            <Route path="/forbidden" element={<ForbiddenPage />} />
            <Route
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<DashboardPage />} />
              <Route path="customers" element={<CustomersPage />} />
              <Route path="policies" element={<PoliciesPage />} />
              <Route path="claims" element={<ClaimsPage />} />
              <Route path="billing" element={<BillingPage />} />
              <Route path="reports" element={<ReportsPage />} />
            </Route>
          </Routes>
        </Suspense>
      </AuthProvider>
    </BrowserRouter>
  )
}
