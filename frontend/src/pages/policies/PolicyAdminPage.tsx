import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { policyApi, type Policy, type PolicyCancelRequest, type PolicyEndorseRequest } from '@/api/policy-api'
import { Button, Card, DataTable, Input, Modal, MoneyDisplay, Skeleton } from '@/components/ui'

export function PolicyAdminPage() {
  const queryClient = useQueryClient()
  const [endorseTarget, setEndorseTarget] = useState<Policy | null>(null)
  const [cancelTarget, setCancelTarget] = useState<Policy | null>(null)

  const policiesQuery = useQuery({
    queryKey: ['policies'],
    queryFn: () => policyApi.list({ size: 50 }),
  })

  return (
    <section aria-labelledby="policies-heading">
      <h1 id="policies-heading">Policy Administration</h1>

      <Card>
        {policiesQuery.isLoading ? (
          <Skeleton variant="text" lines={5} />
        ) : policiesQuery.error ? (
          <p role="alert">Unable to load policies.</p>
        ) : (
          <DataTable
            aria-label="Policies"
            rows={policiesQuery.data?.content ?? []}
            columns={[
              {
                id: 'policyNumber',
                label: 'Policy',
                accessor: (r) => r.policyNumber,
                sortable: true,
              },
              { id: 'policyType', label: 'Type', accessor: (r) => r.policyType, sortable: true },
              { id: 'status', label: 'Status', accessor: (r) => r.status, sortable: true },
              {
                id: 'annualPremium',
                label: 'Premium',
                accessor: (r) => r.annualPremium,
                render: (r) => <MoneyDisplay value={r.annualPremium} />,
              },
              {
                id: 'actions',
                label: 'Actions',
                accessor: () => '',
                render: (r) => (
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <Button size="sm" variant="secondary" onClick={() => setEndorseTarget(r)}>
                      Endorse
                    </Button>
                    <Button size="sm" variant="danger" onClick={() => setCancelTarget(r)}>
                      Cancel
                    </Button>
                  </div>
                ),
              },
            ]}
            getRowId={(r) => r.policyNumber}
            emptyMessage="No policies found."
          />
        )}
      </Card>

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
    endorsementType: 'ADDR_CHG',
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
    <Modal open title={`Endorse ${policy.policyNumber}`} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <Input
          label="Endorsement type"
          value={form.endorsementType}
          onChange={(e) => setForm((f) => ({ ...f, endorsementType: e.target.value }))}
          required
        />
        <Input
          label="Effective date"
          type="date"
          value={form.effectiveDate}
          onChange={(e) => setForm((f) => ({ ...f, effectiveDate: e.target.value }))}
          required
        />
        <Input
          label="Reason"
          value={form.reason}
          onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))}
          required
        />
        {error ? <p role="alert">{error}</p> : null}
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
          <Button type="submit" loading={submitting}>
            Submit endorsement
          </Button>
          <Button type="button" variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
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
    reason: 'INSREQ',
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
    <Modal open title={`Cancel ${policy.policyNumber}`} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <Input
          label="Cancellation date"
          type="date"
          value={form.cancellationDate}
          onChange={(e) => setForm((f) => ({ ...f, cancellationDate: e.target.value }))}
          required
        />
        <Input
          label="Reason code"
          value={form.reason}
          onChange={(e) => setForm((f) => ({ ...f, reason: e.target.value }))}
          required
        />
        {error ? <p role="alert">{error}</p> : null}
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
          <Button type="submit" variant="danger" loading={submitting}>
            Confirm cancellation
          </Button>
          <Button type="button" variant="secondary" onClick={onClose}>
            Close
          </Button>
        </div>
      </form>
    </Modal>
  )
}
