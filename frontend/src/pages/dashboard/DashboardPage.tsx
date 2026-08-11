import { Link } from 'react-router'
import dashboardFixture from '../../../fixtures/dashboard.json'
import { useAuth } from '@/auth/AuthContext'
import { Badge, BlueprintCard, Button, DataTable, Tabs, type TabItem } from '@/components/ui'

type WorkRow = {
  id: string
  name: string
  status: string
  tagClass: string
  amount: string
}

export function DashboardPage() {
  const { user } = useAuth()
  const greetingName = user?.name?.split(' ')[0] ?? 'there'

  const dashTabs: TabItem[] = [
    {
      id: 'claims',
      label: 'My claims',
      content: <WorkQueueTable rows={dashboardFixture.workQueue.claims as WorkRow[]} />,
    },
    {
      id: 'approvals',
      label: 'Approvals awaiting me',
      content: <WorkQueueTable rows={dashboardFixture.workQueue.approvals as WorkRow[]} />,
    },
    {
      id: 'renewal',
      label: 'Renewal exceptions',
      content: <WorkQueueTable rows={dashboardFixture.workQueue.renewal as WorkRow[]} />,
    },
  ]

  return (
    <section aria-labelledby="dashboard-heading">
      <h1 id="dashboard-heading">Good morning, {greetingName}</h1>
      <p style={{ fontSize: 'var(--pcis-font-size-sm)', color: 'var(--pcis-color-text-muted)', marginBottom: 'var(--pcis-space-6)' }}>
        Monday 10 August 2026 · Claims domain live on the modernized platform (Phase 2) · Billing in parallel run
      </p>

      <div className="page-grid-kpi" style={{ marginBottom: 'var(--pcis-space-6)' }}>
        {dashboardFixture.kpis.map((kpi) => (
          <BlueprintCard key={kpi.label} kicker={kpi.label}>
            <div style={{ fontSize: '1.875rem', fontWeight: 600, marginTop: 2 }}>{kpi.value}</div>
            <div className="mono" style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)' }}>
              {kpi.sub}
            </div>
          </BlueprintCard>
        ))}
      </div>

      <div className="page-grid-2">
        <BlueprintCard>
          <Tabs items={dashTabs} defaultTabId="claims" aria-label="Dashboard work queue" />
        </BlueprintCard>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-4)' }}>
          <BlueprintCard kicker="Batch window">
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-2)', marginTop: 'var(--pcis-space-2)' }}>
              {dashboardFixture.jobs.map((job) => (
                <div key={job.name} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--pcis-font-size-sm)', padding: '4px 0', borderBottom: '1px solid var(--pcis-color-border)' }}>
                  <span>{job.name}</span>
                  <Badge status="Active">{job.window}</Badge>
                </div>
              ))}
            </div>
            <Link to="/batch" style={{ display: 'block', marginTop: 'var(--pcis-space-3)' }}>
              <Button variant="secondary" style={{ width: '100%' }}>
                Open Batch Operations Console
              </Button>
            </Link>
          </BlueprintCard>

          <BlueprintCard kicker="Control alerts">
            <ul style={{ margin: 'var(--pcis-space-2) 0 0', padding: 0, listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-3)' }}>
              {dashboardFixture.alerts.map((alert) => (
                <li key={alert.text} style={{ fontSize: 'var(--pcis-font-size-sm)' }}>
                  {alert.text}
                </li>
              ))}
            </ul>
          </BlueprintCard>
        </div>
      </div>
    </section>
  )
}

function WorkQueueTable({ rows }: { rows: WorkRow[] }) {
  return (
    <DataTable
      aria-label="Work queue"
      rows={rows}
      columns={[
        { id: 'id', label: 'Claim', accessor: (r) => r.id, render: (r) => <span className="mono">{r.id}</span> },
        { id: 'name', label: 'Detail', accessor: (r) => r.name },
        { id: 'status', label: 'Status', accessor: (r) => r.status, render: (r) => <Badge status="Active">{r.status}</Badge> },
        { id: 'amount', label: 'Requested', accessor: (r) => r.amount, render: (r) => <span className="mono">{r.amount}</span> },
        {
          id: 'action',
          label: '',
          accessor: () => '',
          render: () => (
            <Link to="/claims/payments" style={{ fontSize: 'var(--pcis-font-size-sm)' }}>
              Open →
            </Link>
          ),
        },
      ]}
      getRowId={(r) => r.id}
      emptyMessage="No items in this queue."
    />
  )
}
