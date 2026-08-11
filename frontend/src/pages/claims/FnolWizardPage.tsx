import { useState } from 'react'
import { useNavigate } from 'react-router'
import { claimsApi, type CreateClaimRequest } from '@/api/claims-api'
import { Button, Card, Input, Select, TextArea } from '@/components/ui'

type FnolStep = 'policy' | 'incident' | 'review'

const CLAIM_TYPES = [
  { value: 'PRP', label: 'Property' },
  { value: 'AUT', label: 'Auto' },
  { value: 'GL', label: 'General Liability' },
]

const emptyForm: CreateClaimRequest = {
  polNbr: '',
  custId: 0,
  lossDate: '',
  claimType: 'PRP',
  description: '',
}

export function FnolWizardPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<FnolStep>('policy')
  const [form, setForm] = useState<CreateClaimRequest>(emptyForm)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

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
    if (!form.lossDate) next.lossDate = 'Loss date is required.'
    if (!form.claimType) next.claimType = 'Claim type is required.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const validateIncidentStep = () => {
    const next: Record<string, string> = {}
    if (!form.description?.trim()) next.description = 'Incident description is required.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const goNext = () => {
    if (step === 'policy' && validatePolicyStep()) setStep('incident')
    if (step === 'incident' && validateIncidentStep()) setStep('review')
  }

  const goBack = () => {
    if (step === 'incident') setStep('policy')
    if (step === 'review') setStep('incident')
  }

  const handleSubmit = async () => {
    if (!validatePolicyStep() || !validateIncidentStep()) {
      setStep('policy')
      return
    }
    setSubmitting(true)
    setSubmitError(null)
    try {
      const claim = await claimsApi.create(form)
      navigate(`/claims/payments?claimNbr=${encodeURIComponent(claim.claimNbr)}`)
    } catch {
      setSubmitError('Unable to submit FNOL. Please verify the policy and try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section aria-labelledby="fnol-heading">
      <h1 id="fnol-heading">First Notice of Loss</h1>
      <p>
        Step {step === 'policy' ? 1 : step === 'incident' ? 2 : 3} of 3
      </p>

      <Card>
        {step === 'policy' ? (
          <fieldset>
            <legend>Policy &amp; loss information</legend>
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
            <Input
              label="Loss date"
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
              errorMessage={errors.claimType}
              required
            />
          </fieldset>
        ) : null}

        {step === 'incident' ? (
          <fieldset>
            <legend>Incident details</legend>
            <TextArea
              label="Description"
              name="description"
              value={form.description ?? ''}
              onChange={(e) => updateField('description', e.target.value)}
              errorMessage={errors.description}
              rows={6}
              required
            />
          </fieldset>
        ) : null}

        {step === 'review' ? (
          <dl>
            <div>
              <dt>Policy</dt>
              <dd>{form.polNbr}</dd>
            </div>
            <div>
              <dt>Customer ID</dt>
              <dd>{form.custId}</dd>
            </div>
            <div>
              <dt>Loss date</dt>
              <dd>{form.lossDate}</dd>
            </div>
            <div>
              <dt>Claim type</dt>
              <dd>{form.claimType}</dd>
            </div>
            <div>
              <dt>Description</dt>
              <dd>{form.description}</dd>
            </div>
          </dl>
        ) : null}

        {submitError ? (
          <p role="alert" style={{ color: 'var(--c-error, #da1e28)' }}>
            {submitError}
          </p>
        ) : null}

        <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
          {step !== 'policy' ? (
            <Button variant="secondary" onClick={goBack} disabled={submitting}>
              Back
            </Button>
          ) : null}
          {step !== 'review' ? (
            <Button onClick={goNext}>Continue</Button>
          ) : (
            <Button onClick={handleSubmit} loading={submitting}>
              Submit FNOL
            </Button>
          )}
        </div>
      </Card>
    </section>
  )
}
