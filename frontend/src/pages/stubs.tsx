type StubPageProps = {
  title: string
}

function StubPage({ title }: StubPageProps) {
  return (
    <section>
      <h1>{title}</h1>
      <p>Route stub — domain UI will be delivered in subsequent stories.</p>
    </section>
  )
}

export function DashboardPage() {
  return <StubPage title="Dashboard" />
}

export function CustomersPage() {
  return <StubPage title="Customers" />
}

export function PoliciesPage() {
  return <StubPage title="Policies" />
}

export function ClaimsPage() {
  return <StubPage title="Claims" />
}

export function BillingPage() {
  return <StubPage title="Billing" />
}

export function ReportsPage() {
  return <StubPage title="Reports" />
}
