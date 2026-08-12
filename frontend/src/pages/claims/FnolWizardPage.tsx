import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { claimsApi, type CreateClaimRequest } from '@/api/claims-api'
import { customerApi, type Customer } from '@/api/customer-api'
import { policyApi, type Policy } from '@/api/policy-api'
import { formatClaimNbr, normalizePolNbr, shouldUseClaimsFixtureFallback } from '@/api/claims-fixture-fallback'
import { HttpError } from '@/api/types'
import { BlueprintCard, Button, Input, Select, TextArea, Alert } from '@/components/ui'

const CLAIM_TYPES = [
  { value: 'PRP', label: 'Property — Water Damage' },
  { value: 'FIR', label: 'Property — Fire' },
  { value: 'AUT', label: 'Auto Collision' },
  { value: 'GL', label: 'Liability — Third Party' },
]

const STEPS = ['Loss Details', 'Policy Lookup', 'Reserve & Adjuster', 'Notes & Documents', 'Review & Submit'] as const

const DEFAULT_CUST_ID = 19284
const DEFAULT_POL_NBR = 'POL000003001'

const emptyForm: CreateClaimRequest = {
  polNbr: '',
  custId: 0,
  lossDate: '2026-08-05',
  claimType: 'PRP',
  description: '',
}

function custLabel(customer: Customer): string {
  return `${customer.custName} · CUS-${String(customer.custId).padStart(7, '0')}`
}

function lossDateInTerm(lossDate: string, policy: Policy | null): boolean {
  if (!policy?.effectiveDate || !policy?.expirationDate) {
    return lossDate >= '2026-01-01' && lossDate <= '2027-01-01'
  }
  return lossDate >= policy.effectiveDate && lossDate <= policy.expirationDate
}

function summaryRow(label: string, value: string) {
  return (
    <div key={label} style={{ display: 'flex', justifyContent: 'space-between', gap: 'var(--pcis-space-4)', padding: 'var(--pcis-space-2) 0', borderBottom: '1px solid var(--pcis-color-border)' }}>
      <span style={{ color: 'var(--pcis-color-text-muted)' }}>{label}</span>
      <strong style={{ textAlign: 'right' }}>{value}</strong>
    </div>
  )
}

export function FnolWizardPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<CreateClaimRequest>(emptyForm)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [activeStep, setActiveStep] = useState(0)
  const [attachedFiles, setAttachedFiles] = useState(['Engineer report.pdf', 'Kitchen damage.jpg'])
  const [successClaimId, setSuccessClaimId] = useState<string | null>(null)
  const [reserve, setReserve] = useState('10000.00')

  const [customerQuery, setCustomerQuery] = useState('')
  const [customerResults, setCustomerResults] = useState<Customer[]>([])
  const [customerSearchLoading, setCustomerSearchLoading] = useState(false)
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null)

  const [policies, setPolicies] = useState<Policy[]>([])
  const [policiesLoading, setPoliciesLoading] = useState(false)
  const [policy, setPolicy] = useState<Policy | null>(null)

  const reserveNum = Number.parseFloat(reserve.replace(/,/g, ''))
  const reserveInvalid = Number.isFinite(reserveNum) && reserveNum <= 0

  // Hydrate the default demo customer on first load so the wizard isn't empty.
  useEffect(() => {
    let active = true
    customerApi
      .getById(DEFAULT_CUST_ID)
      .then((customer) => {
        if (active) setSelectedCustomer(customer)
      })
      .catch(() => undefined)
    return () => {
      active = false
    }
  }, [])

  // Debounced customer search-as-you-type.
  useEffect(() => {
    if (!customerQuery.trim()) {
      setCustomerResults([])
      return
    }
    const handle = setTimeout(() => {
      setCustomerSearchLoading(true)
      customerApi
        .search(customerQuery.trim())
        .then(setCustomerResults)
        .catch(() => setCustomerResults([]))
        .finally(() => setCustomerSearchLoading(false))
    }, 300)
    return () => clearTimeout(handle)
  }, [customerQuery])

  // Load the selected customer's policies and default to one.
  useEffect(() => {
    if (!selectedCustomer) {
      setPolicies([])
      setPolicy(null)
      setForm((prev) => ({ ...prev, custId: 0, polNbr: '' }))
      return
    }
    let active = true
    setPoliciesLoading(true)
    policyApi
      .list({ customerId: selectedCustomer.custId, status: 'ACTIVE' })
      .then((page) => {
        if (!active) return
        setPolicies(page.content)
        const initial = page.content.find((p) => p.policyNumber === DEFAULT_POL_NBR) ?? page.content[0] ?? null
        setPolicy(initial)
        setForm((prev) => ({ ...prev, custId: selectedCustomer.custId, polNbr: initial?.policyNumber ?? '' }))
      })
      .catch(() => {
        if (active) setPolicies([])
      })
      .finally(() => {
        if (active) setPoliciesLoading(false)
      })
    return () => {
      active = false
    }
  }, [selectedCustomer])

  const updateField = <K extends keyof CreateClaimRequest>(key: K, value: CreateClaimRequest[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
    setErrors((prev) => {
      const next = { ...prev }
      delete next[key]
      return next
    })
  }

  const selectCustomer = (custId: number) => {
    const customer = [selectedCustomer, ...customerResults].find((c) => c?.custId === custId) ?? null
    setSelectedCustomer(customer)
    setErrors((prev) => {
      const next = { ...prev }
      delete next.custId
      delete next.polNbr
      return next
    })
  }

  const selectPolicy = (policyNumber: string) => {
    const selected = policies.find((p) => p.policyNumber === policyNumber) ?? null
    setPolicy(selected)
    updateField('polNbr', selected?.policyNumber ?? '')
  }

  const validateLossStep = () => {
    const next: Record<string, string> = {}
    if (!form.lossDate) {
      next.lossDate = 'Date of loss is required.'
    } else if (!lossDateInTerm(form.lossDate, policy)) {
      next.lossDate = 'Loss date is outside every in-force period for this policy.'
    }
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const validatePolicyStep = () => {
    const next: Record<string, string> = {}
    if (!selectedCustomer) next.custId = 'Select a customer.'
    if (!form.polNbr) next.polNbr = 'Select a policy.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const validateNotesStep = () => {
    const next: Record<string, string> = {}
    if (!form.description?.trim()) next.description = 'Loss narrative is required.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const stepValidators = [validateLossStep, validatePolicyStep, () => !reserveInvalid, validateNotesStep, () => true]

  const goToStep = (index: number) => {
    if (index > activeStep && !stepValidators.slice(activeStep, index).every((validate) => validate())) {
      return
    }
    setActiveStep(index)
  }

  const handleContinue = () => goToStep(activeStep + 1)
  const handleBack = () => goToStep(activeStep - 1)

  const handleSubmit = async () => {
    if (!validateLossStep() || !validatePolicyStep() || !validateNotesStep() || reserveInvalid) return
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
    ? `In force — ${policy.policyType}, ${selectedCustomer?.custName ?? ''}, ${policy.effectiveDate} to ${policy.expirationDate}`
    : policiesLoading
      ? 'Looking up policies…'
      : selectedCustomer
        ? 'No active policies found for this customer.'
        : 'Search and select a customer to see their policies.'

  const customerOptions = [selectedCustomer, ...customerResults].filter(
    (c, index, arr): c is Customer => c != null && arr.findIndex((other) => other?.custId === c.custId) === index,
  )

  const claimTypeLabel = CLAIM_TYPES.find((t) => t.value === form.claimType)?.label ?? form.claimType

  return (
    <section aria-labelledby="fnol-heading">
      <h1 id="fnol-heading">First Notice of Loss</h1>
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
                  onClick={() => goToStep(index)}
                >
                  {index + 1} {label}
                </button>
              </span>
            ))}
          </div>

          {activeStep === 0 ? (
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
          </BlueprintCard>
          ) : null}

          {activeStep === 1 ? (
          <BlueprintCard kicker="Policy lookup" style={{ marginBottom: 'var(--pcis-space-4)' }}>
            <Input
              label="Search customer"
              name="customerQuery"
              placeholder="Search by name, customer ID, or tax ID"
              value={customerQuery}
              onChange={(e) => setCustomerQuery(e.target.value)}
              hint="Type at least one character to search."
            />
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--pcis-space-4)', marginTop: 'var(--pcis-space-4)' }}>
              <Select
                label="Customer"
                name="custId"
                value={selectedCustomer ? String(selectedCustomer.custId) : ''}
                onChange={(e) => selectCustomer(Number.parseInt(e.target.value, 10))}
                options={customerOptions.map((c) => ({ value: String(c.custId), label: custLabel(c) }))}
                placeholder={customerSearchLoading ? 'Searching…' : 'Select customer'}
                errorMessage={errors.custId}
                required
              />
              <Select
                label="Policy"
                name="polNbr"
                value={form.polNbr}
                onChange={(e) => selectPolicy(e.target.value)}
                options={policies.map((p) => ({ value: p.policyNumber, label: `${p.policyNumber} — ${p.policyType}` }))}
                placeholder={policiesLoading ? 'Loading policies…' : !selectedCustomer ? 'Select a customer first' : 'Select policy'}
                disabled={!selectedCustomer || policies.length === 0}
                errorMessage={errors.polNbr}
                required
              />
            </div>
            {policy ? (
              <div className="wf-inline-ok">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
                  <path d="M12 2l8 4v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6z" />
                  <path d="M9 12l2 2 4-4" />
                </svg>
                <span>{policyLabel}</span>
              </div>
            ) : (
              <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginTop: 'var(--pcis-space-3)', color: 'var(--pcis-color-text-muted)' }}>
                {policyLabel}
              </p>
            )}
          </BlueprintCard>
          ) : null}

          {activeStep === 2 ? (
          <BlueprintCard kicker="Reserve & adjuster" style={{ marginBottom: 'var(--pcis-space-4)' }}>
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
          ) : null}

          {activeStep === 3 ? (
            <>
              <BlueprintCard kicker="Notes" style={{ marginBottom: 'var(--pcis-space-4)' }}>
                <TextArea
                  label="Loss narrative (first case note)"
                  name="description"
                  value={form.description ?? ''}
                  onChange={(e) => updateField('description', e.target.value)}
                  errorMessage={errors.description}
                  rows={3}
                />
              </BlueprintCard>
              <BlueprintCard kicker="Documents & photos" dashed style={{ marginBottom: 'var(--pcis-space-6)' }}>
                <div className="drop-zone">Drop files or click to attach (PDF, JPG, PNG)</div>
                <ul className="wf-file-list">
                  {attachedFiles.map((file) => (
                    <li key={file}>
                      <span>{file}</span>
                      <button
                        type="button"
                        style={{ background: 'none', border: 'none', color: 'var(--pcis-token-primary)', cursor: 'pointer' }}
                        onClick={() => setAttachedFiles((prev) => prev.filter((f) => f !== file))}
                      >
                        Remove
                      </button>
                    </li>
                  ))}
                </ul>
              </BlueprintCard>
            </>
          ) : null}

          {activeStep === 4 ? (
          <BlueprintCard kicker="Review & submit" style={{ marginBottom: 'var(--pcis-space-6)' }}>
            {summaryRow('Policy number', form.polNbr || '—')}
            {summaryRow('Customer', selectedCustomer ? custLabel(selectedCustomer) : '—')}
            {summaryRow('Date of loss', form.lossDate)}
            {summaryRow('Claim type', claimTypeLabel)}
            {summaryRow('Initial reserve', `$${reserve}`)}
            {summaryRow('Adjuster', 'K. Alvarez (authority limit $25,000)')}
            {summaryRow('Loss narrative', form.description?.trim() || '—')}
            {summaryRow('Attached documents', attachedFiles.length > 0 ? attachedFiles.join(', ') : 'None')}
          </BlueprintCard>
          ) : null}

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
            {activeStep > 0 ? (
              <Button variant="secondary" onClick={handleBack}>
                Back
              </Button>
            ) : null}
            {activeStep < STEPS.length - 1 ? (
              <Button variant="primary" onClick={handleContinue}>
                Continue →
              </Button>
            ) : (
              <>
                <Button variant="secondary">Save draft</Button>
                <Button variant="primary" onClick={handleSubmit} loading={submitting} disabled={Boolean(successClaimId) || reserveInvalid}>
                  Register claim
                </Button>
              </>
            )}
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
