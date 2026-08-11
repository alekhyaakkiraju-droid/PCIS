import { useState } from 'react'
import { useAuth } from '@/auth/AuthContext'
import { useDemoRole } from '@/demo/demo-role'
import { Button, Input } from '@/components/ui'

const OVERRIDE_PERMISSION = 'customer:override-duplicate-tax-id'

type DuplicateResolutionBannerProps = {
  matchedCustomerId?: string
  matchedCustomerName?: string
  onUseExisting?: () => void
  onOverrideComplete?: (justification: string) => void
}

export function DuplicateResolutionBanner({
  matchedCustomerId = 'CUS-0019284',
  matchedCustomerName = 'Marta Field',
  onUseExisting,
  onOverrideComplete,
}: DuplicateResolutionBannerProps) {
  const { user } = useAuth()
  const { effectiveRoles } = useDemoRole()
  const roles = effectiveRoles(user?.roles ?? [])
  const roleLabel = roles[0]?.replace(/_/g, ' ') ?? 'Guest'

  const canOverride = roles.includes('CSR') || roles.includes('CLAIMS_SUPERVISOR')

  const [overrideOpen, setOverrideOpen] = useState(false)
  const [overrideReason, setOverrideReason] = useState('')
  const [confirmed, setConfirmed] = useState(false)

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
        matched customer {matchedCustomerId}, and justification.
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
        1 existing record shares this tax ID: customer {matchedCustomerId} ({matchedCustomerName}).
        Resolution is required before a new record can be created.
      </div>

      {!overrideOpen ? (
        <>
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)' }}>
            <Button variant="secondary" onClick={onUseExisting}>
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
            onChange={(e) => setOverrideReason(e.target.value)}
          />
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)', marginTop: 6 }}>
            <Button variant="secondary" onClick={() => setOverrideOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              disabled={overrideReason.trim().length < 10}
              onClick={() => {
                onOverrideComplete?.(overrideReason.trim())
                setConfirmed(true)
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
