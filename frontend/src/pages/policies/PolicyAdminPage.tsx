import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { policyApi, type Policy, type PolicyCancelRequest, type PolicyEndorseRequest } from '@/api/policy-api'
import { BlueprintCard, Button, DataTable, Input, Modal, MoneyDisplay, Skeleton, Alert } from '@/components/ui'

type PolicyMode = 'create' | 'endorse' | 'inquiry'

const COVERAGES = [
  { name: 'Dwelling (Cov A)', limit: '$350,000', deductible: '$1,000', premium: '$1,420.00', mandatory: true },
  { name: 'Personal property (Cov C)', limit: '$175,000', deductible: '$1,000', premium: '$280.00', mandatory: false },
  { name: 'Liability (Cov E)', limit: '$300,000', deductible: '—', premium: '$140.00', mandatory: true },
]

export function PolicyAdminPage() {
  const queryClient = useQueryClient()
  const [mode, setMode] = useState<PolicyMode>('create')
  const [rated, setRated] = useState(false)
  const [endorseTarget, setEndorseTarget] = useState<Policy | null>(null)
  const [cancelTarget, setCancelTarget] = useState<Policy | null>(null)

  const policiesQuery = useQuery({
    queryKey: ['policies'],
    queryFn: () => policyApi.list({ size: 50 }),
  })

  return (
    <section aria-labelledby="policies-heading">
      <h1 id="policies-heading">Issue policy — new business quote</h1>
      <p className="wf-page-lede">
        Rate coverages, review underwriting alerts, and issue atomically — snapshot persisted for audit.
      </p>

      <div className="seg-control" role="radiogroup" aria-label="Policy mode">
        {(['create', 'endorse', 'inquiry'] as PolicyMode[]).map((m) => (
          <label key={m}>
            <input type="radio" name="policyMode" checked={mode === m} onChange={() => setMode(m)} />
            {m.charAt(0).toUpperCase() + m.slice(1)}
          </label>
        ))}
      </div>

      <BlueprintCard style={{ marginBottom: 'var(--pcis-space-4)' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 'var(--pcis-space-4)' }}>
          <Input label="Customer" name="customer" value="Marta Field · CUS-0019284" readOnly />
          <Input label="Agent" name="agent" value="R. Okafor · AGT-00412" readOnly />
          <Input label="Term" name="term" value="2027-01-01 → 2028-01-01" readOnly />
          <Input label="Policy type" name="policyType" value="Homeowners HO-3" readOnly />
        </div>
      </BlueprintCard>

      {mode === 'inquiry' ? (
        <BlueprintCard kicker="Policy inquiry">
          {policiesQuery.isLoading ? (
            <Skeleton variant="text" lines={5} />
          ) : policiesQuery.error ? (
            <p role="alert">Unable to load policies.</p>
          ) : (
            <DataTable
              aria-label="Policies"
              rows={policiesQuery.data?.content ?? []}
              columns={[
                { id: 'policyNumber', label: 'Policy', accessor: (r) => r.policyNumber, sortable: true },
                { id: 'policyType', label: 'Type', accessor: (r) => r.policyType, sortable: true },
                { id: 'status', label: 'Status', accessor: (r) => r.status, sortable: true },
                { id: 'annualPremium', label: 'Premium', accessor: (r) => r.annualPremium, render: (r) => <MoneyDisplay value={r.annualPremium} /> },
                {
                  id: 'actions',
                  label: 'Actions',
                  accessor: () => '',
                  render: (r) => (
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <Button size="sm" variant="secondary" onClick={() => setEndorseTarget(r)}>Endorse</Button>
                      <Button size="sm" variant="danger" onClick={() => setCancelTarget(r)}>Cancel</Button>
                    </div>
                  ),
                },
              ]}
              getRowId={(r) => r.policyNumber}
              emptyMessage="No policies found."
            />
          )}
        </BlueprintCard>
      ) : (
        <div className="page-grid-split">
          <div>
            <h2 style={{ fontSize: 'var(--pcis-font-size-sm)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>Coverages</h2>
            <DataTable
              aria-label="Coverages"
              rows={COVERAGES}
              columns={[
                { id: 'name', label: 'Coverage', accessor: (r) => r.name },
                { id: 'limit', label: 'Limit', accessor: (r) => r.limit },
                { id: 'deductible', label: 'Deductible', accessor: (r) => r.deductible },
                { id: 'premium', label: 'Premium', accessor: (r) => r.premium },
              ]}
              getRowId={(r) => r.name}
              emptyMessage="No coverages."
            />
            <Button variant="secondary" style={{ marginTop: 'var(--pcis-space-4)' }} onClick={() => setRated(true)}>
              Rate
            </Button>
          </div>

          <BlueprintCard kicker="Rating breakdown">
            {rated ? (
              <>
                <div className="wf-gauge">B</div>
                <p style={{ fontSize: 'var(--pcis-font-size-sm)', textAlign: 'center' }}>
                  Risk score / tier: <strong>612 · Tier B</strong>
                </p>
                <Alert variant="warning" title="Underwriting alert">
                  Roof age 18 years — factor ×1.08 applied. Wind/hail surcharge in coastal territory.
                </Alert>
                <table style={{ width: '100%', fontSize: 'var(--pcis-font-size-sm)', marginTop: 'var(--pcis-space-2)' }}>
                  <tbody>
                    <tr><td>Base rate</td><td style={{ textAlign: 'right' }}>$1,840.00</td></tr>
                    <tr><td>Roof age factor ×1.08</td><td style={{ textAlign: 'right' }}>+$147.20</td></tr>
                    <tr><td>Claims history factor ×0.95</td><td style={{ textAlign: 'right' }}>−$99.36</td></tr>
                    <tr><td>Multi-policy discount</td><td style={{ textAlign: 'right' }}>−$120.00</td></tr>
                    <tr><td>Wind/hail surcharge</td><td style={{ textAlign: 'right' }}>+$210.00</td></tr>
                    <tr><td>Premium tax</td><td style={{ textAlign: 'right' }}>$162.16</td></tr>
                    <tr><td style={{ fontWeight: 600 }}>Final premium</td><td style={{ textAlign: 'right', fontWeight: 600 }}>$2,140.00</td></tr>
                  </tbody>
                </table>
                <p style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-primary-800)', marginTop: 'var(--pcis-space-3)' }}>
                  Approve — rated and eligible for issue
                </p>
                <p style={{ fontSize: 11, color: 'var(--pcis-color-text-muted)' }}>Snapshot RTG-8817342 persisted for audit</p>
              </>
            ) : (
              <p style={{ color: 'var(--pcis-color-text-muted)' }}>Click Rate to generate premium breakdown.</p>
            )}
          </BlueprintCard>
        </div>
      )}

      {mode !== 'inquiry' ? (
        <div className="wf-sticky-footer">
          <span style={{ fontSize: 'var(--pcis-font-size-sm)', color: 'var(--pcis-color-text-muted)' }}>
            {rated ? 'Rated and eligible for issue — snapshot RTG-8817342 persisted' : 'Rate coverages before issuing'}
          </span>
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)' }}>
            <Button variant="secondary">Save quote</Button>
            <Button variant="primary" disabled={!rated}>Issue policy</Button>
          </div>
        </div>
      ) : null}

      {endorseTarget ? (
        <EndorseModal
          policy={endorseTarget}
          onClose={() => setEndorseTarget(null)}
          onSuccess={async () => {
            setEndorseTarget(null)
            await queryClient.invalidateQueries({ queryKey: ['policies'] })
          }}
        />
      ) : null}

      {cancelTarget ? (
        <CancelModal
          policy={cancelTarget}
          onClose={() => setCancelTarget(null)}
          onSuccess={async () => {
            setCancelTarget(null)
            await queryClient.invalidateQueries({ queryKey: ['policies'] })
          }}
        />
      ) : null}
    </section>
  )
}

function EndorseModal({
  policy,
  onClose,
  onSuccess,
}: {
  policy: Policy
  onClose: () => void
  onSuccess: () => void
}) {
  const [form, setForm] = useState<PolicyEndorseRequest>({
    endorsementType: 'COVERAGE',
    effectiveDate: new Date().toISOString().slice(0, 10),
    coverageChanges: [],
    reason: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await policyApi.endorse(policy.policyNumber, form)
      onSuccess()
    } catch {
      setError('Endorsement failed.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal open title={`Endorse ${policy.policyNumber}`} onClose={onClose} footer={<Button type="submit" form="endorse-form" loading={submitting}>Submit endorsement</Button>}>
      <form id="endorse-form" onSubmit={handleSubmit}>
        <Input label="Effective date" name="effectiveDate" type="date" value={form.effectiveDate} onChange={(e) => setForm((f) => ({ ...f, effectiveDate: e.target.value }))} required />
        <Input label="Reason" name="reason" value={form.reason} onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))} required />
        {error ? <p role="alert">{error}</p> : null}
      </form>
    </Modal>
  )
}

function CancelModal({
  policy,
  onClose,
  onSuccess,
}: {
  policy: Policy
  onClose: () => void
  onSuccess: () => void
}) {
  const [form, setForm] = useState<PolicyCancelRequest>({
    cancellationDate: new Date().toISOString().slice(0, 10),
    reason: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await policyApi.cancel(policy.policyNumber, form)
      onSuccess()
    } catch {
      setError('Cancellation failed.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal open title={`Cancel ${policy.policyNumber}`} onClose={onClose} footer={<Button type="submit" form="cancel-form" variant="danger" loading={submitting}>Cancel policy</Button>}>
      <form id="cancel-form" onSubmit={handleSubmit}>
        <Input label="Cancellation date" name="cancellationDate" type="date" value={form.cancellationDate} onChange={(e) => setForm((f) => ({ ...f, cancellationDate: e.target.value }))} required />
        <Input label="Reason" name="reason" value={form.reason} onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))} required />
        {error ? <p role="alert">{error}</p> : null}
      </form>
    </Modal>
  )
}
