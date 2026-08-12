import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { claimsApi, type CreateClaimRequest, type PolicySnapshot } from '@/api/claims-api'
import {
  custNameForId,
  formatClaimNbr,
  normalizePolNbr,
  shouldUseClaimsFixtureFallback,
} from '@/api/claims-fixture-fallback'
import { HttpError } from '@/api/types'
import { BlueprintCard, Button, Input, Select, TextArea, Alert } from '@/components/ui'

const CLAIM_TYPES = [
  { value: 'PRP', label: 'Property — Water Damage' },
  { value: 'FIR', label: 'Property — Fire' },
  { value: 'AUT', label: 'Auto Collision' },
  { value: 'GL', label: 'Liability — Third Party' },
]

const STEPS = ['Policy', 'Loss', 'Reserve', 'Documents'] as const

const emptyForm: CreateClaimRequest = {
  polNbr: 'POL000003001',
  custId: 19284,
  lossDate: '2026-08-05',
  claimType: 'PRP',
  description: '',
}

function lossDateInTerm(lossDate: string, policy: PolicySnapshot | null): boolean {
  if (!policy?.effectiveDate || !policy?.expirationDate) {
    return lossDate >= '2026-01-01' && lossDate <= '2027-01-01'
  }
  return lossDate >= policy.effectiveDate && lossDate <= policy.expirationDate
}

export function FnolWizardPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<CreateClaimRequest>(emptyForm)
  const [policy, setPolicy] = useState<PolicySnapshot | null>(null)
  const [policyLoading, setPolicyLoading] = useState(false)
  const [reserve, setReserve] = useState('10000.00')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [activeStep, setActiveStep] = useState(0)
  const [attachedFiles, setAttachedFiles] = useState(['Engineer report.pdf', 'Kitchen damage.jpg'])
  const [successClaimId, setSuccessClaimId] = useState<string | null>(null)

  const reserveNum = Number.parseFloat(reserve.replace(/,/g, ''))
  const reserveInvalid = Number.isFinite(reserveNum) && reserveNum <= 0

  const lookupPolicy = useCallback(async (polNbr: string) => {
    const normalized = normalizePolNbr(polNbr)
    if (!normalized) return
    setPolicyLoading(true)
    try {
      const snapshot = await claimsApi.lookupPolicy(normalized)
      setPolicy(snapshot)
      if (snapshot.customerId) {
        setForm((prev) => ({ ...prev, custId: snapshot.customerId }))
      }
    } catch {
      setPolicy(null)
    } finally {
      setPolicyLoading(false)
    }
  }, [])

  useEffect(() => {
    lookupPolicy(form.polNbr)
  }, [form.polNbr, lookupPolicy])

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
    } else if (!lossDateInTerm(form.lossDate, policy)) {
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
    const reserveAmt = Number.parseFloat(reserve.replace(/,/g, ''))
    const payload: CreateClaimRequest = {
      ...form,
      polNbr: normalizePolNbr(form.polNbr),
      initialReserveType: 'PRO',
      initialReserveAmt: Number.isFinite(reserveAmt) && reserveAmt > 0 ? reserveAmt : undefined,
    }
    try {
      const claim = await claimsApi.create(payload)
      setSuccessClaimId(formatClaimNbr(claim.claimNbr))
    } catch (error) {
      if (shouldUseClaimsFixtureFallback(error)) {
        setSubmitError(
          'Claims API is unavailable — start the local stack with ./scripts/run-local.sh, then retry.',
        )
      } else if (error instanceof HttpError) {
        setSubmitError(error.message)
      } else {
        setSubmitError('Unable to register claim.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  const policyLabel = policy
    ? `In force — ${policy.policyType ?? 'Homeowners HO-3'}, ${custNameForId(form.custId)}, ${policy.effectiveDate} to ${policy.expirationDate}`
    : policyLoading
      ? 'Looking up policy…'
      : form.polNbr
        ? 'Enter policy number and tab out to verify in-force status.'
        : null

  return (
    <section aria-labelledby="fnol-heading">
      <h1 id="fnol-heading">Register claim — first notice of loss (FNOL)</h1>
      <p className="wf-page-lede">Policy validation, reserve setup, and indexed documents commit atomically on register.</p>

      <div className="page-grid-split" style={{ maxWidth: 1220 }}>
        <div>
          <div className="wf-stepper" role="group" aria-label="FNOL steps">
            {STEPS.map((label, index) => (
              <span key={label} style={{ display: 'contents' }}>
                {index > 0 ? <span className="wf-stepper__sep" aria-hidden="true">→</span> : null}
                <button
                  type="button"
                  className={`wf-stepper__pill${index === activeStep ? ' wf-stepper__pill--active' : ''}`}
                  onClick={() => setActiveStep(index)}
                >
                  {index + 1} {label}
                </button>
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
                onBlur={() => lookupPolicy(form.polNbr)}
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
            {policy?.status === 'ACTIVE' || policyLabel?.startsWith('In force') ? (
              <div className="wf-inline-ok">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
                  <path d="M12 2l8 4v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6z" />
                  <path d="M9 12l2 2 4-4" />
                </svg>
                <span>
                  In force — {policy?.policyType ?? 'Homeowners HO-3'}, {custNameForId(form.custId)},{' '}
                  {policy?.effectiveDate ?? '2026-01-01'} to {policy?.expirationDate ?? '2027-01-01'}
                </span>
              </div>
            ) : policyLabel ? (
              <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginTop: 'var(--pcis-space-3)', color: 'var(--pcis-color-text-muted)' }}>
                {policyLabel}
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
              <Input
                label="Initial reserve estimate"
                name="reserve"
                value={reserve}
                onChange={(e) => setReserve(e.target.value)}
                validationState={reserveInvalid ? 'error' : 'default'}
                errorMessage={reserveInvalid ? 'Reserve must be greater than 0.00 — nothing has been persisted.' : undefined}
              />
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
            <div className="drop-zone">Drop files or click to attach (PDF, JPG, PNG)</div>
            <ul className="wf-file-list">
              {attachedFiles.map((file) => (
                <li key={file}>
                  <span>{file}</span>
                  <button type="button" style={{ background: 'none', border: 'none', color: 'var(--pcis-token-primary)', cursor: 'pointer' }}>Remove</button>
                </li>
              ))}
            </ul>
          </BlueprintCard>

          {successClaimId ? (
            <Alert variant="success" title="Claim registered">
              Claim <strong>{successClaimId}</strong> registered — reserve, narrative, adjuster assignment and audit event committed.{' '}
              <button
                type="button"
                style={{ background: 'none', border: 'none', color: 'var(--pcis-token-primary)', cursor: 'pointer', padding: 0, textDecoration: 'underline' }}
                onClick={() => navigate(`/claims?claimNbr=${encodeURIComponent(successClaimId)}`)}
              >
                View in Claim Inquiry →
              </button>
            </Alert>
          ) : null}

          {submitError ? (
            <p role="alert" style={{ color: 'var(--pcis-color-error-500)' }}>
              {submitError}
            </p>
          ) : null}

          <div className="page-actions">
            <Button variant="secondary">Save draft</Button>
            <Button variant="primary" onClick={handleSubmit} loading={submitting} disabled={Boolean(successClaimId) || reserveInvalid}>
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
              <li style={{ display: 'flex', justifyContent: 'space-between' }}><span>Indexed documents</span><strong>{attachedFiles.length} rows</strong></li>
              <li style={{ display: 'flex', justifyContent: 'space-between' }}><span>Audit event</span><strong>masked, classified</strong></li>
            </ul>
          </BlueprintCard>
          <BlueprintCard kicker="Accessibility">
            <ul style={{ margin: 0, paddingLeft: '1rem', fontSize: 'var(--pcis-font-size-xs)' }}>
              <li>Keyboard path through all fields</li>
              <li>Error summaries announced via alert region</li>
              <li>Mono font for policy and reserve amounts</li>
            </ul>
          </BlueprintCard>
        </div>
      </div>
    </section>
  )
}
