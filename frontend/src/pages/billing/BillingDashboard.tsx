import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { billingApi } from '@/api/billing-api'
import { BlueprintCard, Button, Badge, Alert, DataTable, MoneyDisplay, Tabs, type TabItem } from '@/components/ui'

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
          render: (r) => <Badge status={r.status === 'PAID' ? 'Approved' : 'Active'}>{r.status}</Badge>,
        },
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
        { id: 'id', label: 'Invoice', accessor: (r) => r.id, render: (r) => <span className="mono">{r.id}</span> },
        { id: 'policyId', label: 'Policy', accessor: (r) => r.policyId },
        { id: 'amount', label: 'Amount', accessor: (r) => r.amount, render: (r) => <MoneyDisplay value={r.amount} className="mono" /> },
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
        { id: 'daysOut', label: 'Days out', accessor: (r) => r.daysOut, render: (r) => <span className="mono">{r.daysOut}</span> },
        { id: 'reason', label: 'Reason', accessor: (r) => r.reason },
      ]}
      getRowId={(r) => r.policyId}
      emptyMessage="No skipped candidates."
    />
  )
}

export function BillingDashboard() {
  const [signedOff, setSignedOff] = useState(false)
  const breaks = 2

  const tabs: TabItem[] = [
    { id: 'installments', label: 'Installment comparison', content: <InstallmentsView /> },
    { id: 'invoices', label: 'Invoices', content: <InvoicesView /> },
    { id: 'skipped', label: 'Skipped candidates (318)', content: <SkippedView /> },
    { id: 'spec', label: 'Arithmetic specification', content: <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>COBOL BIL003B vs Spring Batch billing-generation-job — same seeded data, reference date 2026-08-10.</p> },
  ]

  return (
    <section aria-labelledby="billing-heading">
      <h1 id="billing-heading">Billing generation — parallel run reconciliation</h1>
      <p className="wf-page-lede mono" style={{ fontSize: 'var(--pcis-font-size-xs)' }}>
        COBOL BIL003B baseline vs Spring Batch billing-generation-job · same seeded data · reference date 2026-08-10
      </p>

      <div className="wf-kpi-grid wf-kpi-grid--5">
        {[
          { label: 'Rows compared', value: '12,480' },
          { label: 'Matched exactly', value: '12,478', tone: 'success' as const },
          { label: 'Breaks', value: String(breaks), tone: 'error' as const },
          { label: 'Lead-window skips', value: '318', tone: 'warning' as const },
          { label: 'Coverage (monetary code)', value: '94%' },
        ].map((kpi) => (
          <div key={kpi.label} className="wf-kpi-card">
            <div className="wf-stat-label">{kpi.label}</div>
            <div
              className="wf-kpi-value"
              style={{
                color:
                  kpi.tone === 'success'
                    ? 'var(--pcis-token-success)'
                    : kpi.tone === 'error'
                      ? 'var(--pcis-token-error)'
                      : kpi.tone === 'warning'
                        ? 'var(--pcis-token-warning)'
                        : undefined,
              }}
            >
              {kpi.value}
            </div>
          </div>
        ))}
      </div>

      <Alert variant="warning">
        {breaks} breaks on frequency values outside M/Q/S. Both resolved to the annual fallback in COBOL; Java reader must apply the same +1 YEAR default. Gate stays closed until breaks = 0.
      </Alert>

      <Tabs items={tabs} defaultTabId="installments" aria-label="Billing views" />

      <BlueprintCard kicker="Phase gate sign-off" style={{ marginTop: 'var(--pcis-space-6)' }}>
        <div className="wf-header-row" style={{ marginBottom: 0 }}>
          <p style={{ fontSize: 'var(--pcis-font-size-sm)', margin: 0, maxWidth: '52ch' }}>
            Finance &amp; Actuarial sign-off requires 0 breaks across 3 consecutive cycles. Db2 for i remains system of record until then.
          </p>
          <Badge status={breaks > 0 ? 'Denied' : 'Approved'}>{breaks > 0 ? 'Blocked — 2 breaks' : 'Ready'}</Badge>
        </div>
        <div className="wf-sticky-footer" style={{ position: 'static', borderTop: 'none', paddingTop: 'var(--pcis-space-4)' }}>
          <span style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)' }}>
            Export evidence pack for audit retention
          </span>
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)' }}>
            <Button variant="ghost">Export evidence pack</Button>
            <Button variant="secondary">Re-run comparison</Button>
            <Button variant="primary" onClick={() => setSignedOff(true)} disabled={signedOff || breaks > 0}>
              {signedOff ? 'Signed off ✓' : 'Approve cutover gate'}
            </Button>
          </div>
        </div>
      </BlueprintCard>
    </section>
  )
}
