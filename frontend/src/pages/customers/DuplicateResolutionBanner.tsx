import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useAuth } from '@/auth/AuthContext'
import { useCapabilities } from '@/auth/useCapabilities'
import {
  customerApi,
  type CreateCustomerRequest,
  type DuplicateOverrideRequest,
} from '@/api/customer-api'
import { HttpError } from '@/api/types'
import { Button, Input } from '@/components/ui'

const OVERRIDE_PERMISSION = 'customer:duplicate-override'

type DuplicateResolutionBannerProps = {
  pendingCustomer: CreateCustomerRequest
  matchedCustomerId?: number
  matchedCustomerName?: string
  onUseExisting?: () => void
}

export function DuplicateResolutionBanner({
  pendingCustomer,
  matchedCustomerId = 19284,
  matchedCustomerName = 'Marta Field',
  onUseExisting,
}: DuplicateResolutionBannerProps) {
  const navigate = useNavigate()
  const { user } = useAuth()
  const { roles, hasAnyRole } = useCapabilities()
  const roleLabel = roles[0]?.replace(/_/g, ' ') ?? 'Guest'

  const canOverride = hasAnyRole(['CSR', 'CLAIMS_SUPERVISOR'])

  const [overrideOpen, setOverrideOpen] = useState(false)
  const [overrideReason, setOverrideReason] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const matchedLabel = `CUS-${String(matchedCustomerId).padStart(7, '0')}`

  if (confirmed) {
    return (
      <div
        style={{
          fontSize: 'var(--pcis-font-size-sm)',
          padding: '8px 10px',
          border: '1px solid var(--pcis-color-primary-600)',
          marginBottom: 'var(--pcis-space-4)',
        }}
      >
        New customer created with duplicate override. Audit event recorded: overriding principal,
        matched customer {matchedLabel}, and justification.
      </div>
    )
  }

  return (
    <div
      className="card"
      style={{
        padding: 'var(--pcis-space-3) var(--pcis-space-4)',
        marginBottom: 'var(--pcis-space-4)',
        border: '1px solid var(--pcis-color-primary-600)',
        background: 'var(--pcis-color-surface)',
        borderRadius: 'var(--pcis-radius-md)',
      }}
    >
      <div style={{ fontSize: 'var(--pcis-font-size-sm)', fontWeight: 600, marginBottom: 4 }}>
        Possible duplicate tax ID — blocking, not dismissible
      </div>
      <div style={{ fontSize: 'var(--pcis-font-size-sm)', marginBottom: 8 }}>
        1 existing record shares this tax ID: customer {matchedLabel} ({matchedCustomerName}).
        Resolution is required before a new record can be created.
      </div>

      {!overrideOpen ? (
        <>
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)' }}>
            <Button
              variant="secondary"
              onClick={() => {
                if (onUseExisting) {
                  onUseExisting()
                  return
                }
                navigate(`/customers/${matchedCustomerId}`)
              }}
            >
              Use existing customer
            </Button>
            {canOverride ? (
              <Button variant="ghost" onClick={() => setOverrideOpen(true)}>
                Override with reason →
              </Button>
            ) : null}
          </div>
          {!canOverride ? (
            <div
              style={{
                fontSize: 'var(--pcis-font-size-xs)',
                marginTop: 8,
                color: 'var(--pcis-color-text-muted)',
              }}
            >
              Override button not rendered — {roleLabel} lacks {OVERRIDE_PERMISSION} permission.
            </div>
          ) : null}
        </>
      ) : (
        <>
          <Input
            label="Justification (required, audited)"
            name="dupOverrideReason"
            placeholder="e.g. Separate legal entity — trust account"
            value={overrideReason}
            onChange={(event) => setOverrideReason(event.target.value)}
          />
          {error ? (
            <p role="alert" style={{ color: 'var(--c-error, #da1e28)', marginTop: 8 }}>
              {error}
            </p>
          ) : null}
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)', marginTop: 6 }}>
            <Button variant="secondary" onClick={() => setOverrideOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              disabled={overrideReason.trim().length < 10 || submitting}
              onClick={async () => {
                setSubmitting(true)
                setError(null)
                try {
                  const payload: DuplicateOverrideRequest = {
                    overrideReason: overrideReason.trim(),
                    customer: pendingCustomer,
                  }
                  const created = await customerApi.createWithDuplicateOverride(payload)
                  setConfirmed(true)
                  navigate(`/customers/${created.custId}`)
                } catch (err) {
                  if (err instanceof HttpError) {
                    setError(err.message)
                  } else {
                    setError('Unable to create customer with override.')
                  }
                } finally {
                  setSubmitting(false)
                }
              }}
            >
              Create with override
            </Button>
          </div>
        </>
      )}
    </div>
  )
}
