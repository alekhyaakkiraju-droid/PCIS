import { useState } from 'react'
import adminFixture from '../../../fixtures/admin/compliance.json'
import { BlueprintCard, Button, DataTable, Input, Tabs, Alert, type TabItem } from '@/components/ui'

export function AdminCompliancePage() {
  const [editKey, setEditKey] = useState<string | null>(null)
  const [newValue, setNewValue] = useState('')
  const [reason, setReason] = useState('')

  const tunable = adminFixture.tunables.find((t) => t.key === editKey)

  const tabs: TabItem[] = [
    {
      id: 'tunables',
      label: 'Business tunables (6)',
      content: (
        <>
          <DataTable
            aria-label="Business tunables"
            rows={adminFixture.tunables}
            columns={[
              { id: 'key', label: 'Key', accessor: (r) => r.key },
              { id: 'legacy', label: 'Legacy literal', accessor: (r) => r.legacy, render: (r) => <span className="mono">{r.legacy}</span> },
              { id: 'value', label: 'Current value', accessor: (r) => `${r.value} ${r.unit}`, render: (r) => <span className="mono">{r.value} {r.unit}</span> },
              { id: 'updated', label: 'Effective from', accessor: (r) => r.updated },
              { id: 'scope', label: 'Scope', accessor: (r) => r.scope },
              { id: 'version', label: 'Version', accessor: (r) => r.version },
              {
                id: 'edit',
                label: '',
                accessor: () => '',
                render: (r) => (
                  <button type="button" style={{ background: 'none', border: 'none', color: 'var(--pcis-color-primary-700)', cursor: 'pointer' }} onClick={() => { setEditKey(r.key); setNewValue(r.value) }}>
                    Edit →
                  </button>
                ),
              },
            ]}
            getRowId={(r) => r.key}
            emptyMessage="No tunables."
          />
          {tunable ? (
            <BlueprintCard kicker={`Edit — ${tunable.label}`} elevation="md" style={{ marginTop: 'var(--pcis-space-6)', maxWidth: 480 }}>
              <Input label="New value" name="newValue" value={newValue} onChange={(e) => setNewValue(e.target.value)} />
              <Input label="Reason for change" name="reason" value={reason} onChange={(e) => setReason(e.target.value)} placeholder="e.g. shortened per Compliance directive C-2026-14" />
              <div className="page-actions">
                <Button variant="secondary" onClick={() => setEditKey(null)}>Discard</Button>
                <Button variant="primary" onClick={() => setEditKey(null)}>Save &amp; version</Button>
              </div>
            </BlueprintCard>
          ) : null}
        </>
      ),
    },
    {
      id: 'classification',
      label: 'Data classification (55)',
      content: (
        <>
          <div className="page-grid-kpi" style={{ marginBottom: 'var(--pcis-space-6)' }}>
            {adminFixture.classificationSummary.map((item) => (
              <BlueprintCard key={item.tier} kicker={item.tier}>
                <div style={{ fontSize: '1.5rem', fontWeight: 600 }}>{item.count}</div>
              </BlueprintCard>
            ))}
          </div>
          <DataTable
            aria-label="Classification registry"
            rows={[
              { table: 'CUSTOMER_T', tier: 'Restricted', retention: '7 years', masking: 'Tax ID → last 4 only' },
              { table: 'CLAIM_PAYMENT_T', tier: 'Confidential', retention: '10 years', masking: 'Payee name → initials' },
            ]}
            columns={[
              { id: 'table', label: 'Table', accessor: (r) => r.table },
              { id: 'tier', label: 'Tier', accessor: (r) => r.tier },
              { id: 'retention', label: 'Retention', accessor: (r) => r.retention },
              { id: 'masking', label: 'Masking rule', accessor: (r) => r.masking },
            ]}
            getRowId={(r) => r.table}
            emptyMessage="No tables."
          />
        </>
      ),
    },
    {
      id: 'masking',
      label: 'Masking rules',
      content: (
        <DataTable
          aria-label="Masking rules"
          rows={[
            { field: 'CUSTOMER_T.TAX_ID', strategy: 'Last 4 only', sample: '•••••4417' },
            { field: 'CUSTOMER_T.EMAIL', strategy: 'Local-part initial + domain hash', sample: 'a•••••@whitfield•••.com' },
          ]}
          columns={[
            { id: 'field', label: 'Field', accessor: (r) => r.field },
            { id: 'strategy', label: 'Strategy', accessor: (r) => r.strategy },
            { id: 'sample', label: 'Sample output', accessor: (r) => r.sample, render: (r) => <span className="mono">{r.sample}</span> },
          ]}
          getRowId={(r) => r.field}
          emptyMessage="No masking rules."
        />
      ),
    },
    {
      id: 'retention',
      label: 'Retention & purge',
      content: (
        <DataTable
          aria-label="Retention runs"
          rows={adminFixture.retentionRuns}
          columns={[
            { id: 'runDate', label: 'Run date', accessor: (r) => r.runDate },
            { id: 'archived', label: 'Archived', accessor: (r) => r.archived },
            { id: 'purged', label: 'Purged', accessor: (r) => r.purged },
            { id: 'verification', label: 'Verification', accessor: (r) => r.verification },
          ]}
          getRowId={(r) => r.runDate}
          emptyMessage="No retention runs."
        />
      ),
    },
    {
      id: 'history',
      label: 'Change history',
      content: (
        <DataTable
          aria-label="Change history"
          rows={adminFixture.changeHistory}
          columns={[
            { id: 'key', label: 'Key', accessor: (r) => r.key },
            { id: 'change', label: 'Old → New', accessor: (r) => r.change },
            { id: 'actor', label: 'Actor', accessor: (r) => r.actor },
            { id: 'when', label: 'When', accessor: (r) => r.when },
            { id: 'version', label: 'Version', accessor: (r) => r.version },
          ]}
          getRowId={(r) => `${r.key}-${r.when}`}
          emptyMessage="No changes."
        />
      ),
    },
  ]

  return (
    <section aria-labelledby="admin-heading">
      <h1 id="admin-heading">Configuration &amp; compliance</h1>
      <p className="wf-page-lede">
        Regulatory tunables, data classification, masking rules and retention — all changeable without a code deployment, every change versioned.
      </p>

      <Tabs items={tabs} defaultTabId="tunables" aria-label="Admin sections" />

      <Alert variant="info" title="Effective timing">
        Changes take effect within one scheduled run. Batch identity is a workload principal (svc-*-job) — no BATCHCLM-style literals remain.
      </Alert>

      <div className="wf-sticky-footer">
        <span style={{ fontSize: 'var(--pcis-font-size-sm)', color: 'var(--pcis-color-text-muted)' }}>
          Changes create a new version with full who/what/when history — no recompile, no library promotion.
        </span>
        <div style={{ display: 'flex', gap: 'var(--pcis-space-2)' }}>
          <Button variant="ghost">Discard</Button>
          <Button variant="primary">Save &amp; version</Button>
        </div>
      </div>
    </section>
  )
}
