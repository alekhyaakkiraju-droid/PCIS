import { lazy, Suspense } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { AppLayout } from './layout/AppLayout'
import { QueryProvider } from './providers/QueryProvider'
import { ForbiddenPage } from './pages/ForbiddenPage'
import { LoginCallback } from './pages/LoginCallback'

const DashboardPage = lazy(() =>
  import('./pages/stubs').then((m) => ({ default: m.DashboardPage })),
)
const CustomersPage = lazy(() =>
  import('./pages/customers/CustomersPage').then((m) => ({ default: m.CustomersPage })),
)
const PoliciesPage = lazy(() =>
  import('./pages/policies/PolicyAdminPage').then((m) => ({ default: m.PolicyAdminPage })),
)
const ClaimsPage = lazy(() => import('./pages/stubs').then((m) => ({ default: m.ClaimsPage })))
const FnolWizardPage = lazy(() =>
  import('./pages/claims/FnolWizardPage').then((m) => ({ default: m.FnolWizardPage })),
)
const ClaimsPaymentWorkspace = lazy(() =>
  import('./pages/claims/ClaimsPaymentWorkspace').then((m) => ({
    default: m.ClaimsPaymentWorkspace,
  })),
)
const BillingPage = lazy(() =>
  import('./pages/billing/BillingDashboard').then((m) => ({ default: m.BillingDashboard })),
)
const ReportsPage = lazy(() =>
  import('./pages/stubs').then((m) => ({ default: m.ReportsPage })),
)

export default function App() {
  return (
    <BrowserRouter>
      <QueryProvider>
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
                <Route path="customers/:customerId" element={<CustomersPage />} />
                <Route path="policies" element={<PoliciesPage />} />
                <Route path="claims" element={<ClaimsPage />} />
                <Route path="claims/fnol" element={<FnolWizardPage />} />
                <Route path="claims/payments" element={<ClaimsPaymentWorkspace />} />
                <Route path="billing" element={<BillingPage />} />
                <Route path="reports" element={<ReportsPage />} />
              </Route>
            </Routes>
          </Suspense>
        </AuthProvider>
      </QueryProvider>
    </BrowserRouter>
  )
}
