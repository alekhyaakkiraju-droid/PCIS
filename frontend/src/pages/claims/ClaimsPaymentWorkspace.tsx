import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { claimsApi, type ClaimDetail, type ClaimReserve } from '@/api/claims-api'
import { useAuth } from '@/auth/AuthContext'
import { Button, Card, DataTable, Input, MoneyDisplay, Skeleton } from '@/components/ui'

export function ClaimsPaymentWorkspace() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [searchParams] = useSearchParams()
  const initialClaim = searchParams.get('claimNbr') ?? ''
  const [selectedClaimNbr, setSelectedClaimNbr] = useState(initialClaim)
  const [reserveId, setReserveId] = useState('')
  const [paymentAmt, setPaymentAmt] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const claimsQuery = useQuery({
    queryKey: ['claims', 'open'],
    queryFn: () => claimsApi.list({ status: 'O', size: 50 }),
  })

  const detailQuery = useQuery({
    queryKey: ['claims', 'detail', selectedClaimNbr],
    queryFn: (): Promise<ClaimDetail> => claimsApi.getByClaimNbr(selectedClaimNbr),
    enabled: Boolean(selectedClaimNbr),
  })

  const openReserves: ClaimReserve[] = useMemo(
    () => detailQuery.data?.reserves?.filter((r) => r.reserveStatus === 'O') ?? [],
    [detailQuery.data?.reserves],
  )

  const handleInitiatePayment = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!selectedClaimNbr || !user?.sub) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      await claimsApi.initiatePayment(selectedClaimNbr, {
        reserveId: Number.parseInt(reserveId, 10),
        paymentAmt: Number.parseFloat(paymentAmt),
        adjusterId: user.sub,
      })
      await queryClient.invalidateQueries({ queryKey: ['claims', 'detail', selectedClaimNbr] })
      setPaymentAmt('')
      setReserveId('')
    } catch {
      setSubmitError('Payment initiation failed. Verify amount and reserve selection.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section aria-labelledby="claims-payments-heading">
      <h1 id="claims-payments-heading">Claims Payment Workspace</h1>
      <p>
        <Link to="/claims/fnol">Start FNOL</Link>
      </p>

      <Card header={<h2>Open claims</h2>}>
        {claimsQuery.isLoading ? (
          <Skeleton variant="text" lines={3} />
        ) : claimsQuery.error ? (
          <p role="alert">Unable to load claims.</p>
        ) : (
          <DataTable
            aria-label="Open claims"
            rows={claimsQuery.data?.content ?? []}
            columns={[
              { id: 'claimNbr', label: 'Claim', accessor: (r) => r.claimNbr, sortable: true },
              { id: 'polNbr', label: 'Policy', accessor: (r) => r.polNbr, sortable: true },
              { id: 'lossDate', label: 'Loss date', accessor: (r) => r.lossDate, sortable: true },
              {
                id: 'select',
                label: 'Action',
                accessor: () => '',
                render: (r) => (
                  <Button
                    size="sm"
                    variant={selectedClaimNbr === r.claimNbr ? 'primary' : 'secondary'}
                    onClick={() => setSelectedClaimNbr(r.claimNbr)}
                  >
                    {selectedClaimNbr === r.claimNbr ? 'Selected' : 'Select'}
                  </Button>
                ),
              },
            ]}
            getRowId={(r) => r.claimNbr}
            emptyMessage="No open claims."
          />
        )}
      </Card>

      {selectedClaimNbr ? (
        <Card header={<h2>Payment initiation — {selectedClaimNbr}</h2>}>
          {detailQuery.isLoading ? (
            <Skeleton variant="text" lines={4} />
          ) : detailQuery.error ? (
            <p role="alert">Unable to load claim detail.</p>
          ) : (
            <>
              <DataTable
                aria-label="Claim reserves"
                rows={openReserves}
                columns={[
                  { id: 'reserveId', label: 'Reserve ID', accessor: (r) => r.reserveId },
                  { id: 'reserveType', label: 'Type', accessor: (r) => r.reserveType },
                  {
                    id: 'approvedAmt',
                    label: 'Approved',
                    accessor: (r) => r.approvedAmt,
                    render: (r) => <MoneyDisplay value={r.approvedAmt} />,
                  },
                  {
                    id: 'paidToDate',
                    label: 'Paid',
                    accessor: (r) => r.paidToDate,
                    render: (r) => <MoneyDisplay value={r.paidToDate} />,
                  },
                ]}
                getRowId={(r) => String(r.reserveId)}
                emptyMessage="No open reserves."
              />

              <form onSubmit={handleInitiatePayment} aria-label="Initiate payment form" style={{ marginTop: '1rem' }}>
                <SelectReserve reserveId={reserveId} setReserveId={setReserveId} reserves={openReserves} />
                <Input
                  label="Payment amount"
                  name="paymentAmt"
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={paymentAmt}
                  onChange={(e) => setPaymentAmt(e.target.value)}
                  required
                />
                {submitError ? (
                  <p role="alert" style={{ color: 'var(--c-error, #da1e28)' }}>
                    {submitError}
                  </p>
                ) : null}
                <Button type="submit" loading={submitting} disabled={openReserves.length === 0}>
                  Initiate payment
                </Button>
              </form>

              {detailQuery.data?.payments?.length ? (
                <section aria-label="Recent payments" style={{ marginTop: '1rem' }}>
                  <h3>Recent payments</h3>
                  <DataTable
                    aria-label="Claim payments"
                    rows={detailQuery.data.payments}
                    columns={[
                      { id: 'paymentId', label: 'ID', accessor: (r) => r.paymentId },
                      {
                        id: 'paymentAmt',
                        label: 'Amount',
                        accessor: (r) => r.paymentAmt,
                        render: (r) => <MoneyDisplay value={r.paymentAmt} />,
                      },
                      { id: 'paymentStatus', label: 'Status', accessor: (r) => r.paymentStatus },
                    ]}
                    getRowId={(r) => String(r.paymentId)}
                    emptyMessage="No payments."
                  />
                </section>
              ) : null}
            </>
          )}
        </Card>
      ) : null}
    </section>
  )
}

function SelectReserve({
  reserveId,
  setReserveId,
  reserves,
}: {
  reserveId: string
  setReserveId: (value: string) => void
  reserves: ClaimReserve[]
}) {
  return (
    <label>
      Reserve
      <select
        value={reserveId}
        onChange={(e) => setReserveId(e.target.value)}
        required
        aria-label="Reserve"
        style={{ display: 'block', marginBottom: '0.75rem' }}
      >
        <option value="">Select reserve…</option>
        {reserves.map((r) => (
          <option key={r.reserveId} value={String(r.reserveId)}>
            {r.reserveType} — {r.reserveId}
          </option>
        ))}
      </select>
    </label>
  )
}
