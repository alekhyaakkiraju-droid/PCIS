import { useState } from 'react'
import { BlueprintCard, Button, Badge, DataTable, MoneyDisplay, Tabs, type TabItem } from '@/components/ui'

function InstallmentsView() {
  const rows = [
    { id: '1', policyId: 'POL-0088217', number: 7, dueDate: '2026-08-15', cobol: 178.33, java: 178.33 },
    { id: '2', policyId: 'POL-0091355', number: 4, dueDate: '2026-08-15', cobol: 275, java: 275 },
    { id: '3', policyId: 'POL-0079542', number: 2, dueDate: '2026-08-18', cobol: 1040.83, java: 1040.83 },
  ]
  return (
    <DataTable
      aria-label="Billing reconciliation installments"
      rows={rows}
      columns={[
        { id: 'policyId', label: 'Policy', accessor: (r) => r.policyId },
        { id: 'number', label: '#', accessor: (r) => r.number },
        { id: 'dueDate', label: 'Due date', accessor: (r) => r.dueDate },
        { id: 'cobol', label: 'COBOL amount', accessor: (r) => r.cobol, render: (r) => <MoneyDisplay value={r.cobol} /> },
        { id: 'java', label: 'Java amount', accessor: (r) => r.java, render: (r) => <MoneyDisplay value={r.java} /> },
        { id: 'match', label: 'Match', accessor: () => 'Match', render: () => <Badge status="Active">Match</Badge> },
      ]}
      getRowId={(r) => r.id}
      emptyMessage="No installments."
    />
  )
}

function InvoicesView() {
  const rows = [
    { id: 'INV-220134', policyId: 'POL-0088217', amount: 178.33, status: 'Open' },
    { id: 'INV-220091', policyId: 'POL-0091355', amount: 275, status: 'Open' },
  ]
  return (
    <DataTable
      aria-label="Invoices"
      rows={rows}
      columns={[
        { id: 'id', label: 'Invoice', accessor: (r) => r.id },
        { id: 'policyId', label: 'Policy', accessor: (r) => r.policyId },
        { id: 'amount', label: 'Amount', accessor: (r) => r.amount, render: (r) => <MoneyDisplay value={r.amount} /> },
        { id: 'status', label: 'Status', accessor: (r) => r.status },
      ]}
      getRowId={(r) => r.id}
      emptyMessage="No invoices."
    />
  )
}

function SkippedView() {
  const rows = [
    { policyId: 'POL-0083310', nextDue: '2026-09-14', daysOut: 35, reason: 'Outside 15-day lead window — re-evaluated next run' },
  ]
  return (
    <DataTable
      aria-label="Skipped candidates"
      rows={rows}
      columns={[
        { id: 'policyId', label: 'Policy', accessor: (r) => r.policyId },
        { id: 'nextDue', label: 'Next due', accessor: (r) => r.nextDue },
        { id: 'daysOut', label: 'Days out', accessor: (r) => r.daysOut },
        { id: 'reason', label: 'Reason', accessor: (r) => r.reason },
      ]}
      getRowId={(r) => r.policyId}
      emptyMessage="No skipped candidates."
    />
  )
}

export function BillingDashboard() {
  const [signedOff, setSignedOff] = useState(false)

  const tabs: TabItem[] = [
    { id: 'installments', label: 'Installments', content: <InstallmentsView /> },
    { id: 'invoices', label: 'Invoices', content: <InvoicesView /> },
    { id: 'skipped', label: 'Skipped candidates', content: <SkippedView /> },
  ]

  return (
    <section aria-labelledby="billing-heading">
      <h1 id="billing-heading" className="visually-hidden">
        Billing Reconciliation
      </h1>

      <div className="page-grid-kpi" style={{ marginBottom: 'var(--pcis-space-6)' }}>
        {[
          { label: 'Rows compared', value: '4,812' },
          { label: 'Matched', value: '4,812' },
          { label: 'Breaks', value: '0' },
          { label: 'Lead-window skips', value: '37' },
        ].map((kpi) => (
          <BlueprintCard key={kpi.label} kicker={kpi.label}>
            <div style={{ fontSize: '1.625rem', fontWeight: 600 }}>{kpi.value}</div>
          </BlueprintCard>
        ))}
      </div>

      <Tabs items={tabs} defaultTabId="installments" aria-label="Billing views" />

      <div style={{ marginTop: 'var(--pcis-space-6)', display: 'flex', justifyContent: 'flex-end' }}>
        <BlueprintCard kicker="Finance sign-off" style={{ maxWidth: 420 }}>
          <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>
            30-day parallel run to the cent, 0 unexplained breaks — ready for the Phase 1 cutover gate.
          </p>
          <Button variant="primary" onClick={() => setSignedOff(true)} disabled={signedOff}>
            {signedOff ? 'Signed off ✓' : 'Approve cutover gate'}
          </Button>
        </BlueprintCard>
      </div>
    </section>
  )
}
