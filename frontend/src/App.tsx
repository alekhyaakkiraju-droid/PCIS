import { lazy, Suspense } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import { DemoRoleProvider } from './demo/demo-role'
import { ThemeProvider } from './theme/ThemeProvider'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { AppLayout } from './layout/AppLayout'
import { QueryProvider } from './providers/QueryProvider'
import { ForbiddenPage } from './pages/ForbiddenPage'
import { LoginCallback } from './pages/LoginCallback'

const DashboardPage = lazy(() =>
  import('./pages/dashboard/DashboardPage').then((m) => ({ default: m.DashboardPage })),
)
const DesignSystemPage = lazy(() =>
  import('./pages/design-system/DesignSystemPage').then((m) => ({ default: m.DesignSystemPage })),
)
const CustomersPage = lazy(() =>
  import('./pages/customers/CustomersPage').then((m) => ({ default: m.CustomersPage })),
)
const PoliciesPage = lazy(() =>
  import('./pages/policies/PolicyAdminPage').then((m) => ({ default: m.PolicyAdminPage })),
)
const ClaimInquiryPage = lazy(() =>
  import('./pages/claims/ClaimInquiryPage').then((m) => ({ default: m.ClaimInquiryPage })),
)
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
const BatchOperationsPage = lazy(() =>
  import('./pages/batch/BatchOperationsPage').then((m) => ({ default: m.BatchOperationsPage })),
)
const AdminCompliancePage = lazy(() =>
  import('./pages/admin/AdminCompliancePage').then((m) => ({ default: m.AdminCompliancePage })),
)

export default function App() {
  return (
    <BrowserRouter>
      <QueryProvider>
        <ThemeProvider>
        <AuthProvider>
          <DemoRoleProvider>
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
                <Route path="design-system" element={<DesignSystemPage />} />
                <Route path="customers" element={<CustomersPage />} />
                <Route path="customers/:customerId" element={<CustomersPage />} />
                <Route path="policies" element={<PoliciesPage />} />
                <Route path="claims" element={<ClaimInquiryPage />} />
                <Route path="claims/fnol" element={<FnolWizardPage />} />
                <Route path="claims/payments" element={<ClaimsPaymentWorkspace />} />
                <Route path="billing" element={<BillingPage />} />
                <Route path="batch" element={<BatchOperationsPage />} />
                <Route path="admin" element={<AdminCompliancePage />} />
              </Route>
            </Routes>
          </Suspense>
          </DemoRoleProvider>
        </AuthProvider>
        </ThemeProvider>
      </QueryProvider>
    </BrowserRouter>
  )
}
