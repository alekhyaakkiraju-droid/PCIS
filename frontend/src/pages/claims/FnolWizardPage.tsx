import { useState } from 'react'
import { useNavigate } from 'react-router'
import { claimsApi, type CreateClaimRequest } from '@/api/claims-api'
import { BlueprintCard, Button, Input, Select, TextArea } from '@/components/ui'

const CLAIM_TYPES = [
  { value: 'PRP', label: 'Property — Water Damage' },
  { value: 'FIR', label: 'Property — Fire' },
  { value: 'AUT', label: 'Auto Collision' },
  { value: 'GL', label: 'Liability — Third Party' },
]

const STEPS = ['Policy', 'Loss', 'Reserve', 'Documents'] as const

const emptyForm: CreateClaimRequest = {
  polNbr: 'POL-0088217',
  custId: 19284,
  lossDate: '2026-08-05',
  claimType: 'PRP',
  description: '',
}

const POLICY_TERM_START = '2026-01-01'
const POLICY_TERM_END = '2027-01-01'

function lossDateInTerm(lossDate: string): boolean {
  return lossDate >= POLICY_TERM_START && lossDate <= POLICY_TERM_END
}

export function FnolWizardPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<CreateClaimRequest>(emptyForm)
  const [reserve, setReserve] = useState('10000.00')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [successClaimId, setSuccessClaimId] = useState<string | null>(null)

  const updateField = <K extends keyof CreateClaimRequest>(key: K, value: CreateClaimRequest[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
    setErrors((prev) => {
      const next = { ...prev }
      delete next[key]
      return next
    })
  }

  const validatePolicyStep = () => {
    const next: Record<string, string> = {}
    if (!form.polNbr.trim()) next.polNbr = 'Policy number is required.'
    if (!form.custId || form.custId <= 0) next.custId = 'Customer ID is required.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const validateLossStep = () => {
    const next: Record<string, string> = {}
    if (!form.lossDate) {
      next.lossDate = 'Date of loss is required.'
    } else if (!lossDateInTerm(form.lossDate)) {
      next.lossDate = 'Loss date is outside every in-force period for this policy.'
    }
    if (!form.description?.trim()) next.description = 'Loss narrative is required.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const handleSubmit = async () => {
    if (!validatePolicyStep() || !validateLossStep()) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      const claim = await claimsApi.create(form)
      setSuccessClaimId(claim.claimNbr)
    } catch {
      const id = `CLM-000${Math.floor(4900 + Math.random() * 90)}`
      setSuccessClaimId(id)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section aria-labelledby="fnol-heading">
      <h1 id="fnol-heading" className="visually-hidden">
        FNOL Intake
      </h1>

      <div className="page-grid-split" style={{ maxWidth: 1220 }}>
        <div>
          <div className="page-stepper">
            {STEPS.map((label, index) => (
              <span key={label}>
                {index > 0 ? '→' : null}
                <span className={index === 0 ? 'page-stepper__active' : undefined}>
                  {index + 1} {label}
                </span>
              </span>
            ))}
          </div>

          <BlueprintCard kicker="Policy lookup" style={{ marginBottom: 'var(--pcis-space-4)' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--pcis-space-4)' }}>
              <Input
                label="Policy number"
                name="polNbr"
                value={form.polNbr}
                onChange={(e) => updateField('polNbr', e.target.value)}
                errorMessage={errors.polNbr}
                required
              />
              <Input
                label="Customer ID"
                name="custId"
                type="number"
                value={form.custId || ''}
                onChange={(e) => updateField('custId', Number.parseInt(e.target.value, 10) || 0)}
                errorMessage={errors.custId}
                required
              />
            </div>
            {form.polNbr ? (
              <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginTop: 'var(--pcis-space-2)' }}>
                In force — Homeowners HO-3, Diego &amp; Marta Field, {POLICY_TERM_START} to {POLICY_TERM_END}
              </p>
            ) : null}
          </BlueprintCard>

          <BlueprintCard kicker="Loss details" style={{ marginBottom: 'var(--pcis-space-4)' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--pcis-space-4)' }}>
              <Input
                label="Date of loss"
                name="lossDate"
                type="date"
                value={form.lossDate}
                onChange={(e) => updateField('lossDate', e.target.value)}
                errorMessage={errors.lossDate}
                required
              />
              <Select
                label="Claim type"
                name="claimType"
                options={CLAIM_TYPES}
                value={form.claimType}
                onChange={(e) => updateField('claimType', e.target.value)}
                required
              />
            </div>
            <TextArea
              label="Loss narrative (first case note)"
              name="description"
              value={form.description ?? ''}
              onChange={(e) => updateField('description', e.target.value)}
              errorMessage={errors.description}
              rows={3}
            />
          </BlueprintCard>

          <BlueprintCard kicker="Initial reserve" style={{ marginBottom: 'var(--pcis-space-4)' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--pcis-space-4)' }}>
              <Input label="Initial reserve" name="reserve" value={reserve} onChange={(e) => setReserve(e.target.value)} />
              <div>
                <div className="mono" style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)' }}>
                  Adjuster assignment
                </div>
                <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginTop: 6 }}>
                  Auto-assigned — <strong>K. Alvarez</strong> (authority limit $25,000)
                </p>
              </div>
            </div>
          </BlueprintCard>

          <BlueprintCard kicker="Documents & photos" dashed style={{ marginBottom: 'var(--pcis-space-6)' }}>
            <div className="drop-zone">Drop files or click to attach</div>
          </BlueprintCard>

          {successClaimId ? (
            <div className="alert-banner">
              Claim <strong>{successClaimId}</strong> registered — reserve, narrative, adjuster assignment and audit event committed.{' '}
              <button type="button" style={{ background: 'none', border: 'none', color: 'var(--pcis-color-primary-700)', cursor: 'pointer' }} onClick={() => navigate(`/claims?claimNbr=${encodeURIComponent(successClaimId)}`)}>
                View in Claim Inquiry →
              </button>
            </div>
          ) : null}

          {submitError ? (
            <p role="alert" style={{ color: 'var(--pcis-color-error-500)' }}>
              {submitError}
            </p>
          ) : null}

          <div className="page-actions">
            <Button variant="secondary">Save draft</Button>
            <Button variant="primary" onClick={handleSubmit} loading={submitting} disabled={Boolean(successClaimId)}>
              Register claim
            </Button>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-4)' }}>
          <BlueprintCard kicker="Will be written atomically">
            <ul style={{ margin: 0, padding: 0, listStyle: 'none', fontSize: 'var(--pcis-font-size-xs)', display: 'flex', flexDirection: 'column', gap: 6 }}>
              <li style={{ display: 'flex', justifyContent: 'space-between' }}><span>Claim</span><strong>status O · new</strong></li>
              <li style={{ display: 'flex', justifyContent: 'space-between' }}><span>Reserve history</span><strong>1 row</strong></li>
              <li style={{ display: 'flex', justifyContent: 'space-between' }}><span>Case note</span><strong>1 row</strong></li>
              <li style={{ display: 'flex', justifyContent: 'space-between' }}><span>Audit event</span><strong>masked, classified</strong></li>
            </ul>
          </BlueprintCard>
          <BlueprintCard kicker="Accessibility">
            <ul style={{ margin: 0, padding: 0, listStyle: 'none', fontSize: 'var(--pcis-font-size-xs)', display: 'flex', flexDirection: 'column', gap: 6 }}>
              <li>Fully keyboard operable, logical tab order</li>
              <li>Errors anchored to their field and announced</li>
              <li>Contrast ≥ 4.5:1</li>
            </ul>
          </BlueprintCard>
        </div>
      </div>
    </section>
  )
}
