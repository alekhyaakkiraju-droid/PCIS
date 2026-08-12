import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { billingApi } from '@/api/billing-api'
import { BlueprintCard, Button, Badge, DataTable, MoneyDisplay, Tabs, type TabItem } from '@/components/ui'

function InstallmentsView() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['billing-installments'],
    queryFn: () => billingApi.listInstallments(),
  })

  if (isLoading) return <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>Loading installments…</p>
  if (error) return <p role="alert">Unable to load installments.</p>

  return (
    <DataTable
      aria-label="Installment schedule"
      rows={data ?? []}
      columns={[
        { id: 'policyId', label: 'Policy', accessor: (r) => r.policyId, render: (r) => <span className="mono">{r.policyId}</span> },
        { id: 'dueDate', label: 'Due date', accessor: (r) => r.dueDate },
        { id: 'amount', label: 'Amount', accessor: (r) => r.amount, render: (r) => <MoneyDisplay value={r.amount} className="mono" /> },
        {
          id: 'status',
          label: 'Status',
          accessor: (r) => r.status,
          render: (r) => <Badge status={r.status.toUpperCase() === 'PAID' ? 'Approved' : 'Active'}>{r.status}</Badge>,
        },
      ]}
      getRowId={(r) => r.id}
      emptyMessage="No installments."
    />
  )
}

const NOT_WIRED_NOTE = (
  <p style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)', marginBottom: 'var(--pcis-space-2)' }}>
    billing-svc does not yet expose an invoice or skip-candidate list endpoint, so this tab is not wired to a live source.
  </p>
)

function InvoicesView() {
  const rows = [
    { id: 'INV-240118', policyId: 'POL000004006', amount: 1050.00, status: 'Open' },
    { id: 'INV-240119', policyId: 'POL000004008', amount: 145.83, status: 'Open' },
  ]
  return (
    <>
      {NOT_WIRED_NOTE}
      <DataTable
        aria-label="Invoices"
        rows={rows}
        columns={[
          { id: 'id', label: 'Invoice', accessor: (r) => r.id, render: (r) => <span className="mono">{r.id}</span> },
          { id: 'policyId', label: 'Policy', accessor: (r) => r.policyId },
          { id: 'amount', label: 'Amount', accessor: (r) => r.amount, render: (r) => <MoneyDisplay value={r.amount} className="mono" /> },
          { id: 'status', label: 'Status', accessor: (r) => r.status },
        ]}
        getRowId={(r) => r.id}
        emptyMessage="No invoices."
      />
    </>
  )
}

const SKIPPED_ROWS = [
  { policyId: 'POL000004009', nextDue: '2026-09-14', daysOut: 35, reason: 'Policy cancelled — billing plan inactive' },
]

function SkippedView() {
  return (
    <>
      {NOT_WIRED_NOTE}
      <DataTable
        aria-label="Skipped candidates"
        rows={SKIPPED_ROWS}
        columns={[
          { id: 'policyId', label: 'Policy', accessor: (r) => r.policyId },
          { id: 'nextDue', label: 'Next due', accessor: (r) => r.nextDue },
          { id: 'daysOut', label: 'Days out', accessor: (r) => r.daysOut, render: (r) => <span className="mono">{r.daysOut}</span> },
          { id: 'reason', label: 'Reason', accessor: (r) => r.reason },
        ]}
        getRowId={(r) => r.policyId}
        emptyMessage="No skipped candidates."
      />
    </>
  )
}

export function BillingDashboard() {
  const [signedOff, setSignedOff] = useState(false)

  const installmentsQuery = useQuery({
    queryKey: ['billing-installments'],
    queryFn: () => billingApi.listInstallments(),
  })
  const agingQuery = useQuery({
    queryKey: ['billing-aging'],
    queryFn: () => billingApi.listAging(),
  })

  const installments = installmentsQuery.data ?? []
  const aging = agingQuery.data ?? []
  const openCount = installments.filter((i) => i.status.toUpperCase() === 'OPEN').length
  const paidCount = installments.filter((i) => i.status.toUpperCase() === 'PAID').length
  const overdueBucket = aging.find((b) => b.bucket === '90+ days')
  const overdueCount = overdueBucket?.invoiceCount ?? 0
  const totalAmountDue = aging.reduce((sum, b) => sum + b.amountDue, 0)
  const kpisLoaded = !installmentsQuery.isLoading && !agingQuery.isLoading

  const tabs: TabItem[] = [
    { id: 'installments', label: 'Installment comparison', content: <InstallmentsView /> },
    { id: 'invoices', label: 'Invoices', content: <InvoicesView /> },
    { id: 'skipped', label: 'Skipped candidates (318)', content: <SkippedView /> },
    { id: 'spec', label: 'Arithmetic specification', content: <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>Replaces legacy program BIL003B — installment schedule and invoice generation now run as billing-generation-job (Spring Batch).</p> },
  ]

  return (
    <section aria-labelledby="billing-heading">
      <h1 id="billing-heading">Billing Overview</h1>
      <p className="wf-page-lede">
        Installment schedule and aging status, read directly from billing-svc — the modernized replacement for the
        legacy BIL003B billing-generation batch program.
      </p>

      {kpisLoaded ? (
        <div className="wf-kpi-grid wf-kpi-grid--5">
          <div className="wf-kpi-card">
            <div className="wf-stat-label">Total installments</div>
            <div className="wf-kpi-value">{installments.length}</div>
          </div>
          <div className="wf-kpi-card">
            <div className="wf-stat-label">Paid</div>
            <div className="wf-kpi-value" style={{ color: 'var(--pcis-token-success)' }}>{paidCount}</div>
          </div>
          <div className="wf-kpi-card">
            <div className="wf-stat-label">Open</div>
            <div className="wf-kpi-value">{openCount}</div>
          </div>
          <div className="wf-kpi-card">
            <div className="wf-stat-label">Overdue (90+ days)</div>
            <div className="wf-kpi-value" style={{ color: overdueCount > 0 ? 'var(--pcis-token-error)' : undefined }}>{overdueCount}</div>
          </div>
          <div className="wf-kpi-card">
            <div className="wf-stat-label">Total amount due</div>
            <div className="wf-kpi-value"><MoneyDisplay value={totalAmountDue} /></div>
          </div>
        </div>
      ) : null}

      <Tabs items={tabs} defaultTabId="installments" aria-label="Billing views" />

      <BlueprintCard kicker="Phase gate sign-off" style={{ marginTop: 'var(--pcis-space-6)' }}>
        <div className="wf-header-row" style={{ marginBottom: 0 }}>
          <p style={{ fontSize: 'var(--pcis-font-size-sm)', margin: 0, maxWidth: '52ch' }}>
            Finance &amp; Actuarial sign-off requires 0 installments overdue past the 90-day bucket.
          </p>
          <Badge status={overdueCount > 0 ? 'Denied' : 'Approved'}>{overdueCount > 0 ? `Blocked — ${overdueCount} overdue` : 'Ready'}</Badge>
        </div>
        <div className="wf-sticky-footer" style={{ position: 'static', borderTop: 'none', paddingTop: 'var(--pcis-space-4)' }}>
          <span style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)' }}>
            Export evidence pack for audit retention
          </span>
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)' }}>
            <Button variant="ghost" disabled title="Evidence pack export is not wired up yet">Export evidence pack</Button>
            <Button variant="secondary" disabled title="Re-run is not wired up yet — there is no legacy COBOL system running in this environment to diff against">Re-run comparison</Button>
            <Button variant="primary" onClick={() => setSignedOff(true)} disabled={signedOff || overdueCount > 0}>
              {signedOff ? 'Signed off ✓' : 'Approve cutover gate'}
            </Button>
          </div>
        </div>
      </BlueprintCard>
    </section>
  )
}
