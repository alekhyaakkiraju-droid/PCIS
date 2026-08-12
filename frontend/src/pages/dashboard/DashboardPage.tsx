import { Link } from 'react-router'
import dashboardFixture from '../../../fixtures/dashboard.json'
import { useAuth } from '@/auth/AuthContext'
import {
  Alert,
  Badge,
  BlueprintCard,
  Button,
  DataTable,
  Tabs,
  type TabItem,
} from '@/components/ui'
import type { BadgeStatus } from '@/components/ui/Badge'

type WorkRow = {
  id: string
  adjuster: string
  type: string
  requested: string
  cumulative: string
  authority: string
  aged: string
  tagClass: string
}

function authorityBadge(tagClass: string, label: string): BadgeStatus {
  if (tagClass === 'denied') return 'Denied'
  if (tagClass === 'renewal') return 'Renewal'
  if (tagClass === 'approved') return 'Approved'
  return 'Neutral'
}

export function DashboardPage() {
  const { user } = useAuth()
  const greetingName = user?.name?.split(' ')[0] ?? 'there'

  const dashTabs: TabItem[] = [
    {
      id: 'approvals',
      label: `Approvals awaiting me (${dashboardFixture.workQueue.approvals.length})`,
      content: <WorkQueueTable rows={dashboardFixture.workQueue.approvals as WorkRow[]} />,
    },
    {
      id: 'renewal',
      label: `Renewal exceptions (${dashboardFixture.workQueue.renewal.length})`,
      content: <WorkQueueTable rows={dashboardFixture.workQueue.renewal as WorkRow[]} />,
    },
    {
      id: 'claims',
      label: 'My claims',
      content: <WorkQueueTable rows={dashboardFixture.workQueue.claims as WorkRow[]} />,
    },
  ]

  return (
    <section aria-labelledby="dashboard-heading">
      <h1 id="dashboard-heading">Good morning, {greetingName}</h1>
      <p className="wf-page-lede">
        Monday 10 August 2026 · Claims domain live on the modernized platform (Phase 2) · Billing in parallel run
      </p>

      <div className="wf-kpi-grid">
        {dashboardFixture.kpis.map((kpi) => (
          <div key={kpi.label} className="wf-kpi-card">
            <div className="wf-stat-label">{kpi.label}</div>
            <div className="wf-kpi-value">{kpi.value}</div>
            <div className={`wf-kpi-sub${kpi.warn ? ' wf-kpi-sub--warn' : ''}`}>{kpi.sub}</div>
          </div>
        ))}
      </div>

      <div className="page-grid-2">
        <BlueprintCard kicker="My work queue">
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)', marginBottom: 'var(--pcis-space-4)' }}>
            <Link to="/claims/fnol">
              <Button variant="primary">New FNOL</Button>
            </Link>
            <Button variant="ghost">Export</Button>
          </div>
          <Tabs items={dashTabs} defaultTabId="approvals" aria-label="Dashboard work queue" />
        </BlueprintCard>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-4)' }}>
          <BlueprintCard kicker="Last night's batch window">
            <p style={{ fontSize: 'var(--pcis-font-size-sm)', margin: '0 0 var(--pcis-space-2)' }}>
              {dashboardFixture.batchWindow.used} used · {dashboardFixture.batchWindow.headroomPct}% headroom
            </p>
            <div className="progress-bar">
              <div
                className="progress-bar__fill"
                style={{ width: `${100 - dashboardFixture.batchWindow.headroomPct}%` }}
              />
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-2)', marginTop: 'var(--pcis-space-3)' }}>
              {dashboardFixture.jobs.map((job) => (
                <div
                  key={job.name}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    fontSize: 'var(--pcis-font-size-sm)',
                    padding: '4px 0',
                    borderBottom: '1px solid var(--pcis-color-border)',
                  }}
                >
                  <span className="mono">{job.name}</span>
                  <Badge status={authorityBadge(job.tagClass, job.status)}>{job.status}</Badge>
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
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-3)' }}>
              {dashboardFixture.alerts.map((alert) => (
                <Alert
                  key={alert.text}
                  variant={alert.variant as 'info' | 'success' | 'warning' | 'error'}
                  title={'title' in alert ? alert.title : undefined}
                  role={alert.variant === 'error' ? 'alert' : 'status'}
                >
                  {alert.text}
                </Alert>
              ))}
            </div>
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
        { id: 'adjuster', label: 'Adjuster', accessor: (r) => r.adjuster },
        { id: 'type', label: 'Type', accessor: (r) => r.type },
        {
          id: 'requested',
          label: 'Requested',
          accessor: (r) => r.requested,
          render: (r) => <span className="mono">{r.requested}</span>,
        },
        {
          id: 'cumulative',
          label: 'Cumulative',
          accessor: (r) => r.cumulative,
          render: (r) => <span className="mono">{r.cumulative}</span>,
        },
        {
          id: 'authority',
          label: 'Authority',
          accessor: (r) => r.authority,
          render: (r) => <Badge status={authorityBadge(r.tagClass, r.authority)}>{r.authority}</Badge>,
        },
        { id: 'aged', label: 'Aged', accessor: (r) => r.aged },
        {
          id: 'action',
          label: '',
          accessor: () => '',
          render: (r) => (
            <Link to={`/claims/payments?claimNbr=${encodeURIComponent(r.id)}`} style={{ fontSize: 'var(--pcis-font-size-sm)' }}>
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
