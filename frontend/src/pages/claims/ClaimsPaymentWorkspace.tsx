import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Badge,
  BlueprintCard,
  Button,
  DataTable,
  Input,
  formatMoney,
  Select,
  Tabs,
  TextArea,
  type TabItem,
} from '@/components/ui'
import { claimsApi, type Approval, type ClaimDetail, type ReserveLedgerEntry } from '@/api/claims-api'
import {
  custNameForId,
  formatClaimNbr,
  normalizeClaimNbr,
  shouldUseClaimsFixtureFallback,
} from '@/api/claims-fixture-fallback'
import { HttpError } from '@/api/types'
import { useCapabilities } from '@/auth/useCapabilities'

const REINSURANCE_THRESHOLD = 100000

const FIXTURE_LEDGER: ReserveLedgerEntry[] = [
  {
    ledgerId: 1,
    claimNbr: 'CLM000004821',
    reserveId: 1,
    eventDate: '2026-06-02',
    reason: 'Initial FNOL reserve',
    amount: 10000,
    balanceAfter: 10000,
    actorId: 'ADJ90001',
    eventType: 'SET',
  },
  {
    ledgerId: 2,
    claimNbr: 'CLM000004821',
    reserveId: 1,
    eventDate: '2026-06-20',
    reason: 'Increase — engineer report received',
    amount: 38000,
    balanceAfter: 48000,
    actorId: 'ADJ90001',
    eventType: 'INCR',
  },
  {
    ledgerId: 3,
    claimNbr: 'CLM000004821',
    reserveId: 1,
    eventDate: '2026-07-14',
    reason: 'Drawdown on payment CLM-PMT-0231',
    amount: -20000,
    balanceAfter: 28000,
    actorId: 'ADJ90001',
    eventType: 'DRAW',
  },
]

const FIXTURE_DETAIL: ClaimDetail = {
  claimNbr: 'CLM000004821',
  polNbr: 'POL000003001',
  custId: 19284,
  lossDate: '2026-06-02',
  claimType: 'PRP',
  claimStatus: 'O',
  version: 1,
  authorityLimit: 25000,
  adjusterId: 'ADJ90001',
  adjusterName: 'K. Alvarez',
  reserveRemaining: 28000,
  reserves: [
    {
      reserveId: 1,
      claimNbr: 'CLM000004821',
      reserveType: 'PRO',
      approvedAmt: 48000,
      paidToDate: 20000,
      reserveStatus: 'O',
    },
  ],
  payments: [
    {
      paymentId: 1,
      claimNbr: 'CLM000004821',
      paymentAmt: 20000,
      paymentStatus: 'P',
      payeeId: 19284,
      approvalId: 1,
      adjusterId: 'ADJ90001',
    },
  ],
  notes: [
    {
      noteId: 1,
      claimNbr: 'CLM000004821',
      noteText:
        'Insured reports upstairs pipe burst overnight; water damage to kitchen ceiling and hallway flooring. Emergency mitigation vendor dispatched.',
      createdAt: '2026-06-02T09:15:00',
    },
    {
      noteId: 2,
      claimNbr: 'CLM000004821',
      noteText:
        'Structural engineer report received; reserve increased to reflect subfloor replacement scope.',
      createdAt: '2026-06-20T14:30:00',
    },
  ],
  reserveLedger: FIXTURE_LEDGER,
}

const FIXTURE_APPROVALS: Approval[] = [
  {
    approvalId: 1,
    claimNbr: 'CLM000004821',
    reserveId: 1,
    approverId: 'SUP90001',
    approvalStatus: 'A',
    approvalDate: '2026-07-14T10:00:00',
  },
]

function paymentStatusLabel(status: string): string {
  if (status === 'C') return 'Completed'
  if (status === 'F') return 'Failed'
  return 'Issued'
}

function sumPaidToDate(detail: ClaimDetail): number {
  return (detail.payments ?? []).reduce((sum, p) => sum + (p.paymentAmt ?? 0), 0)
}

function primaryOpenReserve(detail: ClaimDetail) {
  return (detail.reserves ?? []).find((r) => r.reserveStatus === 'O')
}

export function ClaimsPaymentWorkspace() {
  const queryClient = useQueryClient()
  const [searchParams] = useSearchParams()
  const displayClaimNbr = searchParams.get('claimNbr') ?? 'CLM-000004821'
  const apiClaimNbr = normalizeClaimNbr(displayClaimNbr)
  const { hasPermission } = useCapabilities()
  const canApprovePayments = hasPermission('claims:approve')

  const [payAmount, setPayAmount] = useState('10000')
  const [payMethod, setPayMethod] = useState('ACH')
  const [payDecision, setPayDecision] = useState<string | null>(null)
  const [approvalConfirmed, setApprovalConfirmed] = useState(false)
  const [increaseAmt, setIncreaseAmt] = useState('48000')
  const [increaseReason, setIncreaseReason] = useState('Increase — engineer report received')
  const [newNote, setNewNote] = useState('')

  const detailQuery = useQuery({
    queryKey: ['claim-detail', apiClaimNbr],
    queryFn: async () => {
      try {
        return await claimsApi.getByClaimNbr(apiClaimNbr)
      } catch (error) {
        if (shouldUseClaimsFixtureFallback(error)) {
          return FIXTURE_DETAIL
        }
        throw error
      }
    },
  })

  const approvalsQuery = useQuery({
    queryKey: ['claim-approvals', apiClaimNbr],
    queryFn: async () => {
      try {
        return await claimsApi.listApprovals(apiClaimNbr)
      } catch (error) {
        if (shouldUseClaimsFixtureFallback(error)) {
          return FIXTURE_APPROVALS
        }
        throw error
      }
    },
  })

  const detail = detailQuery.data
  const approvals = approvalsQuery.data ?? []
  const hasApprovedOnFile = approvals.some((a) => a.approvalStatus === 'A')

  const authorityLimit = detail?.authorityLimit ?? 25000
  const paidToDate = detail ? sumPaidToDate(detail) : 0
  const amountNum = Number.parseFloat(payAmount.replace(/,/g, '')) || 0
  const cumulative = paidToDate + amountNum
  const approvalLinked = hasApprovedOnFile || approvalConfirmed
  const authorityOk = cumulative <= authorityLimit || approvalLinked

  const openReserve = detail ? primaryOpenReserve(detail) : undefined
  const reserveBalance = detail?.reserveRemaining ?? 0

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['claim-detail', apiClaimNbr] })
    queryClient.invalidateQueries({ queryKey: ['claims-inquiry'] })
  }

  const paymentMutation = useMutation({
    mutationFn: async () => {
      if (!openReserve?.reserveId) throw new Error('No open reserve on this claim.')
      return claimsApi.initiatePayment(apiClaimNbr, {
        reserveId: openReserve.reserveId,
        amount: amountNum,
        payeeId: detail?.custId,
      })
    },
    onSuccess: () => {
      setPayDecision('Approved — payment, reserve drawdown and audit event committed in one transaction.')
      invalidate()
    },
    onError: (error: unknown) => {
      setPayDecision(error instanceof HttpError ? `Denied — ${error.message}` : `Denied — ${(error as Error).message}`)
    },
  })

  const approvalMutation = useMutation({
    mutationFn: async () => {
      if (!openReserve?.reserveId) throw new Error('No open reserve on this claim.')
      return claimsApi.createApproval(apiClaimNbr, { reserveId: openReserve.reserveId })
    },
    onSuccess: () => {
      setApprovalConfirmed(true)
      invalidate()
      queryClient.invalidateQueries({ queryKey: ['claim-approvals', apiClaimNbr] })
    },
    onError: (error: unknown) => {
      if (error instanceof HttpError && error.status === 409) {
        setApprovalConfirmed(true)
        return
      }
      setPayDecision(error instanceof HttpError ? error.message : (error as Error).message)
    },
  })

  const reserveMutation = useMutation({
    mutationFn: async () => {
      const amt = Number.parseFloat(increaseAmt.replace(/,/g, ''))
      if (!openReserve) throw new Error('No open reserve.')
      return claimsApi.createReserve(apiClaimNbr, {
        reserveType: openReserve.reserveType,
        approvedAmt: amt,
        reason: increaseReason,
      })
    },
    onSuccess: () => invalidate(),
  })

  const noteMutation = useMutation({
    mutationFn: async () => claimsApi.createNote(apiClaimNbr, newNote),
    onSuccess: () => {
      setNewNote('')
      invalidate()
    },
  })

  const closeMutation = useMutation({
    mutationFn: async () =>
      claimsApi.update(apiClaimNbr, { claimStatus: 'C' }, detail?.version ?? undefined),
    onSuccess: () => invalidate(),
  })

  const authorityCheck = useMemo(
    () => ({
      approvalLabel: approvalLinked ? 'Yes — linked' : 'None on file',
      cumulativeLabel: `$${cumulative.toLocaleString()}${authorityOk ? ' — within limit' : ' — exceeds limit'}`,
      aboveThreshold: amountNum > REINSURANCE_THRESHOLD,
      exceeded: !authorityOk,
      badge: authorityOk ? 'Cleared' : 'Blocked',
      icon: authorityOk ? '✓' : '✕',
    }),
    [amountNum, approvalLinked, authorityOk, cumulative],
  )

  const requestPayment = () => {
    if (!approvalLinked && !authorityOk) {
      setPayDecision(
        'Denied — no qualifying approval on file and cumulative payout exceeds the authority limit. Reason codes: NO_QUALIFYING_APPROVAL, AUTHORITY_LIMIT_EXCEEDED.',
      )
      return
    }
    paymentMutation.mutate()
  }

  const ledgerRows = useMemo(() => {
    const ledger = detail?.reserveLedger ?? []
    return ledger.map((row) => ({
      id: String(row.ledgerId),
      date: row.eventDate,
      reason: row.reason,
      amount: row.amount,
      balance: row.balanceAfter,
    }))
  }, [detail])

  const paymentRows = useMemo(() => {
    if (!detail?.payments?.length) return []
    return detail.payments.map((p) => ({
      id: `CLM-PMT-${String(p.paymentId).padStart(4, '0')}`,
      date: '—',
      payee: custNameForId(p.payeeId ?? detail.custId),
      amount: p.paymentAmt ?? 0,
      status: paymentStatusLabel(p.paymentStatus ?? 'P'),
      approval: p.approvalId ? `APR-${String(p.approvalId).padStart(6, '0')}` : '—',
    }))
  }, [detail])

  const tabs: TabItem[] = [
    {
      id: 'reserves',
      label: 'Reserves',
      content: (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-4)' }}>
          <DataTable
            aria-label="Reserve history"
            rows={ledgerRows}
            columns={[
              { id: 'date', label: 'Date', accessor: (r) => r.date },
              { id: 'reason', label: 'Reason', accessor: (r) => r.reason },
              {
                id: 'amount',
                label: 'Amount',
                accessor: (r) => r.amount,
                render: (r) => {
                  const prefix = r.amount < 0 ? '−' : r.amount > 0 && r.reason.includes('Increase') ? '+' : ''
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
          {detail?.claimStatus === 'O' && openReserve ? (
            <BlueprintCard kicker="Increase reserve" style={{ maxWidth: 560 }}>
              <Input
                label="New approved amount"
                name="increaseAmt"
                value={increaseAmt}
                onChange={(e) => setIncreaseAmt(e.target.value)}
              />
              <TextArea
                label="Reason"
                name="increaseReason"
                value={increaseReason}
                onChange={(e) => setIncreaseReason(e.target.value)}
                rows={2}
              />
              <Button
                variant="secondary"
                onClick={() => reserveMutation.mutate()}
                loading={reserveMutation.isPending}
                style={{ marginTop: 'var(--pcis-space-3)' }}
              >
                Post reserve increase
              </Button>
            </BlueprintCard>
          ) : null}
        </div>
      ),
    },
    {
      id: 'payments',
      label: 'Payments',
      content: (
        <DataTable
          aria-label="Claim payments"
          rows={paymentRows}
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
      ),
    },
    {
      id: 'approvals',
      label: 'Approvals',
      content: (
        <BlueprintCard kicker={`Pending approval — ${formatMoney(amountNum)} request`} style={{ maxWidth: 560 }}>
          <p style={{ fontSize: 'var(--pcis-font-size-sm)', margin: '8px 0' }}>
            Requested by {detail?.adjusterName ?? 'K. Alvarez'} (limit {formatMoney(authorityLimit)}, {formatMoney(paidToDate)} already paid).
            {cumulative > authorityLimit
              ? ` Approval required because cumulative payout would reach ${formatMoney(cumulative)}.`
              : ' Within adjuster authority.'}
          </p>
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)' }}>
            <Button
              variant="secondary"
              disabled={!canApprovePayments}
              title={canApprovePayments ? undefined : 'Requires Claims Supervisor role'}
              onClick={() => {
                setPayDecision('Denied by supervisor — escalate to higher authority or reduce request.')
                setApprovalConfirmed(false)
              }}
            >
              Deny
            </Button>
            <Button
              variant="primary"
              disabled={!canApprovePayments}
              title={canApprovePayments ? undefined : 'Requires Claims Supervisor role'}
              onClick={() => approvalMutation.mutate()}
              loading={approvalMutation.isPending}
            >
              Approve with rationale
            </Button>
          </div>
          {!canApprovePayments ? (
            <p style={{ marginTop: 'var(--pcis-space-2)', fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)' }}>
              Approval actions require Claims Supervisor role.
            </p>
          ) : null}
          {approvalLinked ? (
            <p style={{ marginTop: 'var(--pcis-space-3)', fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-primary-800)' }}>
              Linked approval record on file — disbursement can proceed.
            </p>
          ) : null}
        </BlueprintCard>
      ),
    },
    {
      id: 'notes',
      label: 'Notes',
      content: (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-4)' }}>
          <div style={{ fontSize: 'var(--pcis-font-size-sm)', lineHeight: 1.7 }}>
            {(detail?.notes ?? []).map((note) => (
              <p key={note.noteId}>
                <strong>
                  {note.createdAt ? note.createdAt.slice(0, 10) : '—'}, {detail?.adjusterName ?? 'Adjuster'}
                </strong>{' '}
                — {note.noteText}
              </p>
            ))}
          </div>
          {detail?.claimStatus === 'O' ? (
            <BlueprintCard kicker="Add case note" style={{ maxWidth: 560 }}>
              <TextArea label="Note" name="newNote" value={newNote} onChange={(e) => setNewNote(e.target.value)} rows={3} />
              <Button
                variant="secondary"
                onClick={() => noteMutation.mutate()}
                loading={noteMutation.isPending}
                disabled={!newNote.trim()}
                style={{ marginTop: 'var(--pcis-space-3)' }}
              >
                Save note
              </Button>
            </BlueprintCard>
          ) : null}
        </div>
      ),
    },
  ]

  if (detailQuery.isLoading) {
    return <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>Loading claim {displayClaimNbr}…</p>
  }

  if (detailQuery.error && !detail) {
    return (
      <p role="alert" style={{ color: 'var(--c-error, #da1e28)' }}>
        {(detailQuery.error as Error).message}
      </p>
    )
  }

  const claimTitle =
    detail?.claimType === 'PRP'
      ? `Homeowners water damage — ${custNameForId(detail.custId)}`
      : `Claim — ${custNameForId(detail?.custId ?? 0)}`

  const authorityPanel = (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-4)' }}>
      <BlueprintCard kicker="Authority check">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--pcis-space-2)' }}>
          <span className="mono" style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-primary-600)' }}>
            BR-01 cumulative limit
          </span>
          <Badge status={authorityOk ? 'Active' : 'Denied'}>{authorityCheck.badge}</Badge>
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
          <Input
            label="Payee"
            name="payee"
            value={detail ? `${custNameForId(detail.custId)} (insured)` : '—'}
            readOnly
          />
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
            <span>Qualifying approval on file</span>
            <strong>{authorityCheck.approvalLabel}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>Cumulative vs {formatMoney(authorityLimit)} limit</span>
            <strong>{authorityCheck.cumulativeLabel}</strong>
          </div>
        </div>
        <Button
          variant="primary"
          style={{ width: '100%', marginTop: 'var(--pcis-space-3)' }}
          onClick={requestPayment}
          loading={paymentMutation.isPending}
          disabled={detail?.claimStatus === 'C'}
        >
          Submit payment request
        </Button>
        {payDecision ? (
          <Alert variant={payDecision.startsWith('Denied') ? 'error' : 'success'} title={payDecision.startsWith('Denied') ? 'Denied' : 'Result'}>
            {payDecision}
          </Alert>
        ) : null}
      </BlueprintCard>
      {authorityCheck.aboveThreshold ? (
        <BlueprintCard kicker="Reinsurance referral">
          <p style={{ fontSize: 'var(--pcis-font-size-sm)', margin: 0 }}>
            Above {formatMoney(REINSURANCE_THRESHOLD)} threshold — a tracked recovery referral will be created on disbursement.
          </p>
        </BlueprintCard>
      ) : null}
    </div>
  )

  return (
    <section aria-labelledby="claims-payments-heading">
      <div className="wf-header-row">
        <div>
          <div className="wf-entity-kicker">
            Claim {formatClaimNbr(detail?.claimNbr ?? displayClaimNbr)}
            {detail?.claimStatus === 'C' ? ' · Closed' : ' · Open'}
          </div>
          <h1 id="claims-payments-heading" className="wf-entity-title">
            {claimTitle}
          </h1>
          <p className="wf-page-lede" style={{ marginBottom: 0 }}>
            Adjuster {detail?.adjusterName ?? 'K. Alvarez'} · authority {formatMoney(authorityLimit)}
          </p>
        </div>
        {detail?.claimStatus === 'O' && reserveBalance <= 0 ? (
          <Button variant="secondary" onClick={() => closeMutation.mutate()} loading={closeMutation.isPending}>
            Close claim
          </Button>
        ) : null}
      </div>

      <div className="wf-metric-grid" style={{ marginBottom: 'var(--pcis-space-4)' }}>
        <div className="wf-metric-card">
          <div className="wf-stat-label">Reserve remaining</div>
          <strong>{formatMoney(reserveBalance)}</strong>
        </div>
        <div className="wf-metric-card">
          <div className="wf-stat-label">Paid to date</div>
          <strong>{formatMoney(paidToDate)}</strong>
        </div>
        <div className="wf-metric-card">
          <div className="wf-stat-label">Request amount</div>
          <strong>{formatMoney(amountNum)}</strong>
        </div>
        <div className="wf-metric-card">
          <div className="wf-stat-label">Authority status</div>
          <strong style={{ color: authorityOk ? 'var(--pcis-token-success)' : 'var(--pcis-token-error)' }}>
            {authorityCheck.badge}
          </strong>
        </div>
      </div>

      <div className="page-grid-split">
        <div>
          <Tabs items={tabs} defaultTabId="payments" aria-label="Payment workspace sections" />
        </div>
        {authorityPanel}
      </div>

      <p style={{ marginTop: 'var(--pcis-space-4)' }}>
        <Link to="/claims/fnol">Start FNOL →</Link>
        {' · '}
        <Link to="/claims">Claim Inquiry →</Link>
      </p>
    </section>
  )
}
