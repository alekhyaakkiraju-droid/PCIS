import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import {
  Badge,
  BlueprintCard,
  Button,
  DataTable,
  Input,
  formatMoney,
  Select,
  Tabs,
  type TabItem,
} from '@/components/ui'

const AUTHORITY_LIMIT = 25000
const PAID_TO_DATE = 20000
const REINSURANCE_THRESHOLD = 100000

const RESERVE_ROWS = [
  { id: '1', date: '2026-06-02', reason: 'Initial FNOL reserve', amount: 10000, balance: 10000 },
  { id: '2', date: '2026-06-20', reason: 'Increase — engineer report received', amount: 38000, balance: 48000 },
  { id: '3', date: '2026-07-14', reason: 'Drawdown on payment CLM-PMT-0231', amount: -20000, balance: 28000 },
]

const PAYMENT_ROWS = [
  {
    id: 'CLM-PMT-0231',
    date: '2026-07-14',
    payee: 'Diego Field',
    amount: 20000,
    status: 'Issued',
    approval: 'APR-004512',
  },
]

export function ClaimsPaymentWorkspace() {
  const [searchParams] = useSearchParams()
  const claimNbr = searchParams.get('claimNbr') ?? 'CLM-0004821'
  const [payAmount, setPayAmount] = useState('10000')
  const [payMethod, setPayMethod] = useState('ACH')
  const [payApprovalLinked, setPayApprovalLinked] = useState(false)
  const [payDecision, setPayDecision] = useState<string | null>(null)
  const [approvalConfirmed, setApprovalConfirmed] = useState(false)

  const amountNum = Number.parseFloat(payAmount.replace(/,/g, '')) || 0
  const cumulative = PAID_TO_DATE + amountNum
  const authorityOk = cumulative <= AUTHORITY_LIMIT

  const authorityCheck = useMemo(
    () => ({
      approvalLabel: payApprovalLinked ? 'Yes — linked' : 'None on file',
      cumulativeLabel: `$${cumulative.toLocaleString()}${authorityOk ? ' — within limit' : ' — exceeds limit'}`,
      aboveThreshold: amountNum > REINSURANCE_THRESHOLD,
      exceeded: !authorityOk,
      badge: authorityOk ? 'Cleared' : 'Blocked',
      icon: authorityOk ? '✓' : '✕',
    }),
    [amountNum, authorityOk, cumulative, payApprovalLinked],
  )

  const requestPayment = () => {
    if (!payApprovalLinked && !authorityOk) {
      setPayDecision(
        'Denied — no qualifying approval on file and cumulative payout exceeds the $25,000 authority limit. Reason codes: NO_QUALIFYING_APPROVAL, AUTHORITY_LIMIT_EXCEEDED.',
      )
    } else if (!authorityOk) {
      setPayDecision(
        `Denied — cumulative payout of $${cumulative.toLocaleString()} exceeds the $25,000 authority limit. Reason code: AUTHORITY_LIMIT_EXCEEDED.`,
      )
    } else {
      setPayDecision('Approved — payment, reserve drawdown and audit event committed in one transaction.')
    }
  }

  const tabs: TabItem[] = [
    {
      id: 'reserves',
      label: 'Reserves',
      content: (
        <DataTable
          aria-label="Reserve history"
          rows={RESERVE_ROWS}
          columns={[
            { id: 'date', label: 'Date', accessor: (r) => r.date },
            { id: 'reason', label: 'Reason', accessor: (r) => r.reason },
            {
              id: 'amount',
              label: 'Amount',
              accessor: (r) => r.amount,
              render: (r) => {
                const prefix = r.amount < 0 ? '−' : r.id === '2' ? '+' : ''
                return (
                  <span className="mono">
                    {prefix}
                    {formatMoney(Math.abs(r.amount))}
                  </span>
                )
              },
            },
            {
              id: 'balance',
              label: 'Balance',
              accessor: (r) => r.balance,
              render: (r) => <span className="mono">{formatMoney(r.balance)}</span>,
            },
          ]}
          getRowId={(r) => r.id}
          emptyMessage="No reserves."
        />
      ),
    },
    {
      id: 'payments',
      label: 'Payments',
      content: (
        <div className="page-grid-split">
          <DataTable
            aria-label="Claim payments"
            rows={PAYMENT_ROWS}
            columns={[
              { id: 'id', label: 'ID', accessor: (r) => r.id, render: (r) => <span className="mono">{r.id}</span> },
              { id: 'date', label: 'Date', accessor: (r) => r.date },
              { id: 'payee', label: 'Payee', accessor: (r) => r.payee },
              {
                id: 'amount',
                label: 'Amount',
                accessor: (r) => r.amount,
                render: (r) => <span className="mono">{formatMoney(r.amount)}</span>,
              },
              {
                id: 'status',
                label: 'Status',
                accessor: (r) => r.status,
                render: (r) => <Badge status="Active">{r.status}</Badge>,
              },
              { id: 'approval', label: 'Approval', accessor: (r) => r.approval },
            ]}
            getRowId={(r) => r.id}
            emptyMessage="No payments."
          />
          <BlueprintCard kicker="Authority check">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--pcis-space-2)' }}>
              <span className="mono" style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-primary-600)' }}>
                Authority check
              </span>
              <Badge status={authorityOk ? 'Active' : 'Pending'}>{authorityCheck.badge}</Badge>
            </div>
            <Input
              label="Amount"
              name="payAmount"
              value={payAmount}
              onChange={(e) => {
                setPayAmount(e.target.value)
                setPayDecision(null)
              }}
            />
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--pcis-space-3)' }}>
              <Input label="Payee" name="payee" value="Diego & Marta Field (insured)" readOnly />
              <Select
                label="Method"
                name="payMethod"
                value={payMethod}
                onChange={(e) => setPayMethod(e.target.value)}
                options={[
                  { value: 'ACH', label: 'ACH' },
                  { value: 'Check', label: 'Check' },
                ]}
              />
            </div>
            <div style={{ marginTop: 'var(--pcis-space-3)', display: 'flex', flexDirection: 'column', gap: 6, fontSize: 'var(--pcis-font-size-sm)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>✓ Qualifying approval on file</span>
                <strong>{authorityCheck.approvalLabel}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>
                  {authorityCheck.icon} Cumulative payout vs $25,000 limit
                </span>
                <strong>{authorityCheck.cumulativeLabel}</strong>
              </div>
            </div>
            {authorityCheck.aboveThreshold ? (
              <div
                style={{
                  marginTop: 'var(--pcis-space-3)',
                  fontSize: 'var(--pcis-font-size-xs)',
                  border: '1px solid var(--pcis-color-primary-600)',
                  padding: 8,
                  color: 'var(--pcis-color-primary-800)',
                }}
              >
                Above $100,000 reinsurance referral threshold — a tracked recovery referral will be created.
              </div>
            ) : null}
            <Button variant="primary" style={{ width: '100%', marginTop: 'var(--pcis-space-3)' }} onClick={requestPayment}>
              Submit payment request
            </Button>
            {authorityCheck.exceeded ? (
              <Button
                variant="secondary"
                style={{ width: '100%', marginTop: 'var(--pcis-space-2)' }}
                onClick={() =>
                  setPayDecision('Escalated to Claims Supervisor (limit $100,000.00) — routed to the approval queue.')
                }
              >
                Escalate to my authority ($25,000)
              </Button>
            ) : null}
            <Button
              variant="ghost"
              style={{ width: '100%', marginTop: 'var(--pcis-space-2)', fontSize: 'var(--pcis-font-size-xs)' }}
              onClick={() =>
                setPayDecision(
                  'ROLLED BACK — audit outbox insert failed (constraint violation). Per BR-03, the entire transaction reverted: no payment, no reserve drawdown, no audit record. Item recorded as exception for retry.',
                )
              }
            >
              Simulate audit-outbox failure (BR-03)
            </Button>
            {payDecision ? (
              <div
                style={{
                  marginTop: 'var(--pcis-space-3)',
                  fontSize: 'var(--pcis-font-size-sm)',
                  padding: '8px 10px',
                  border: '1px solid var(--pcis-color-border)',
                }}
              >
                {payDecision}
              </div>
            ) : null}
          </BlueprintCard>
        </div>
      ),
    },
    {
      id: 'approvals',
      label: 'Approvals',
      content: (
        <BlueprintCard kicker="Pending approval — $10,000.00 request" style={{ maxWidth: 560 }}>
          <p style={{ fontSize: 'var(--pcis-font-size-sm)', margin: '8px 0' }}>
            Requested by K. Alvarez (limit $25,000, $20,000 already paid). Approval required because cumulative payout would reach $30,000.
          </p>
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)' }}>
            <Button
              variant="secondary"
              onClick={() => {
                setPayApprovalLinked(false)
                setPayDecision('Denied by supervisor — escalate to higher authority or reduce request.')
                setApprovalConfirmed(false)
              }}
            >
              Deny
            </Button>
            <Button
              variant="primary"
              onClick={() => {
                setPayApprovalLinked(true)
                setApprovalConfirmed(true)
              }}
            >
              Approve with rationale
            </Button>
          </div>
          {approvalConfirmed ? (
            <p style={{ marginTop: 'var(--pcis-space-3)', fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-primary-800)' }}>
              Linked approval record created — disbursement can now proceed.
            </p>
          ) : null}
        </BlueprintCard>
      ),
    },
    {
      id: 'notes',
      label: 'Notes',
      content: (
        <div style={{ fontSize: 'var(--pcis-font-size-sm)', lineHeight: 1.7 }}>
          <p>
            <strong>2026-06-02, K. Alvarez</strong> — Insured reports upstairs pipe burst overnight; water damage to kitchen ceiling and hallway flooring. Emergency mitigation vendor dispatched.
          </p>
          <p>
            <strong>2026-06-20, K. Alvarez</strong> — Structural engineer report received; reserve increased to reflect subfloor replacement scope.
          </p>
        </div>
      ),
    },
  ]

  return (
    <section aria-labelledby="claims-payments-heading">
      <h1 id="claims-payments-heading" className="visually-hidden">
        Payment &amp; Authority
      </h1>

      <BlueprintCard
        style={{
          marginBottom: 'var(--pcis-space-4)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <div>
          <div className="mono" style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-primary-600)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            Claim {claimNbr}
          </div>
          <div style={{ fontSize: 'var(--pcis-font-size-xl)', fontWeight: 600 }}>
            Homeowners water damage — Diego &amp; Marta Field
          </div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="mono" style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)' }}>
            Reserve remaining
          </div>
          <div style={{ fontSize: '1.375rem', fontWeight: 600 }}>$28,000.00</div>
        </div>
      </BlueprintCard>

      <Tabs items={tabs} defaultTabId="payments" aria-label="Payment workspace sections" />

      <p style={{ marginTop: 'var(--pcis-space-4)' }}>
        <Link to="/claims/fnol">Start FNOL →</Link>
        {' · '}
        <Link to="/claims">Claim Inquiry →</Link>
      </p>
    </section>
  )
}
