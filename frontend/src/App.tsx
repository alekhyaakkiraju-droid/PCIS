import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AppLayout } from './layout/AppLayout'

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
      <Suspense fallback={<p>Loading…</p>}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="customers" element={<CustomersPage />} />
            <Route path="policies" element={<PoliciesPage />} />
            <Route path="claims" element={<ClaimsPage />} />
            <Route path="billing" element={<BillingPage />} />
            <Route path="reports" element={<ReportsPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
