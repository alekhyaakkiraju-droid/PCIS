import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import inquiryFixture from '../../../fixtures/claims/inquiry.json'
import { claimsApi, type ClaimListItem } from '@/api/claims-api'
import {
  custNameForId,
  formatClaimNbr,
  shouldUseClaimsFixtureFallback,
} from '@/api/claims-fixture-fallback'
import { Badge, DataTable, Input, formatMoney } from '@/components/ui'

type InquiryTab = 'open' | 'pending' | 'closed' | 'escalated'

type InquiryRow = {
  id: string
  name: string
  lossDate: string
  adjuster: string
  reserve: string
  status: string
  tagClass: string
}

function tagStatus(tagClass: string): 'Active' | 'Pending' | 'Inactive' {
  if (tagClass === 'outline') return 'Pending'
  if (tagClass === 'neutral') return 'Inactive'
  return 'Active'
}

function claimToRow(claim: ClaimListItem, tab: InquiryTab): InquiryRow {
  const reserve =
    claim.reserveRemaining != null ? formatMoney(claim.reserveRemaining) : '—'
  const statusLabel =
    claim.claimStatus === 'C'
      ? 'Closed — paid'
      : claim.pendingApproval || tab === 'pending'
        ? 'Pending approval'
        : 'Open'
  return {
    id: formatClaimNbr(claim.claimNbr),
    name: custNameForId(claim.custId),
    lossDate: claim.lossDate,
    adjuster: claim.adjusterName ?? 'K. Alvarez',
    reserve,
    status: statusLabel,
    tagClass:
      claim.claimStatus === 'C'
        ? 'accent'
        : claim.pendingApproval || tab === 'pending'
          ? 'neutral'
          : 'outline',
  }
}

function fixtureRows(tab: InquiryTab, filter: string): InquiryRow[] {
  const source = (inquiryFixture as Record<InquiryTab, InquiryRow[]>)[tab] ?? []
  if (!filter.trim()) return source
  const q = filter.toLowerCase()
  return source.filter(
    (row) =>
      row.id.toLowerCase().includes(q) ||
      row.name.toLowerCase().includes(q) ||
      row.adjuster.toLowerCase().includes(q),
  )
}

export function ClaimInquiryPage() {
  const [searchParams] = useSearchParams()
  const highlightClaim = searchParams.get('claimNbr')
  const [tab, setTab] = useState<InquiryTab>('open')
  const [filter, setFilter] = useState('')

  const { data, isLoading, error } = useQuery({
    queryKey: ['claims-inquiry', tab],
    queryFn: async () => {
      try {
        if (tab === 'escalated') return [] as ClaimListItem[]
        return await claimsApi.list({ view: tab })
      } catch (err) {
        if (shouldUseClaimsFixtureFallback(err)) {
          return null
        }
        throw err
      }
    },
  })

  const rows = useMemo(() => {
    if (data === null) {
      return fixtureRows(tab, filter)
    }
    const mapped = (data ?? []).map((claim) => claimToRow(claim, tab))
    if (!filter.trim()) return mapped
    const q = filter.toLowerCase()
    return mapped.filter(
      (row) =>
        row.id.toLowerCase().includes(q) ||
        row.name.toLowerCase().includes(q) ||
        row.adjuster.toLowerCase().includes(q),
    )
  }, [data, filter, tab])

  const tabs: { id: InquiryTab; label: string }[] = [
    { id: 'open', label: 'Open' },
    { id: 'pending', label: 'Pending approval' },
    { id: 'closed', label: 'Closed' },
    { id: 'escalated', label: 'Escalated (SIU)' },
  ]

  return (
    <section aria-labelledby="claim-inquiry-heading">
      <h1 id="claim-inquiry-heading" className="visually-hidden">
        Claim Inquiry
      </h1>

      <div style={{ display: 'flex', gap: 'var(--pcis-space-3)', marginBottom: 'var(--pcis-space-4)', alignItems: 'center' }}>
        <Input
          label="Filter claims"
          name="inquiryFilter"
          placeholder="Filter by claimant, policy, adjuster…"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="inquiry-filter"
        />
      </div>

      {highlightClaim ? (
        <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginBottom: 'var(--pcis-space-3)' }}>
          Highlighting claim <strong>{highlightClaim}</strong> — open Payment workspace from the table.
        </p>
      ) : null}

      {error ? (
        <p role="alert" style={{ color: 'var(--c-error, #da1e28)', marginBottom: 'var(--pcis-space-3)' }}>
          {(error as Error).message}
        </p>
      ) : null}

      <div role="tablist" aria-label="Claim inquiry views" style={{ display: 'flex', gap: 'var(--pcis-space-2)', borderBottom: '1px solid var(--pcis-color-border)', marginBottom: 'var(--pcis-space-4)' }}>
        {tabs.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={tab === t.id}
            className={tab === t.id ? 'page-stepper__active' : undefined}
            style={{ background: 'none', border: 'none', borderBottom: tab === t.id ? '2px solid var(--pcis-color-primary-600)' : '2px solid transparent', padding: 'var(--pcis-space-2) var(--pcis-space-4)', cursor: 'pointer', font: 'inherit', textTransform: 'uppercase', fontSize: '13px', letterSpacing: '0.03em' }}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {isLoading ? <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>Loading claims…</p> : null}

      {!isLoading && rows.length === 0 ? (
        <p className="empty-state">
          No qualifying records for this filter — 0 selected, 0 processed. This is a well-formed empty state, not an error.
        </p>
      ) : null}

      {!isLoading && rows.length > 0 ? (
        <DataTable
          aria-label="Claim inquiry results"
          rows={rows}
          columns={[
            { id: 'id', label: 'Claim', accessor: (r) => r.id, render: (r) => <span className="mono">{r.id}</span> },
            { id: 'name', label: 'Insured', accessor: (r) => r.name },
            { id: 'lossDate', label: 'Loss date', accessor: (r) => r.lossDate },
            { id: 'adjuster', label: 'Adjuster', accessor: (r) => r.adjuster },
            { id: 'reserve', label: 'Reserve', accessor: (r) => r.reserve, render: (r) => <span className="mono">{r.reserve}</span> },
            { id: 'status', label: 'Status', accessor: (r) => r.status, render: (r) => <Badge status={tagStatus(r.tagClass)}>{r.status}</Badge> },
            {
              id: 'action',
              label: '',
              accessor: () => '',
              render: (r) => (
                <Link to={`/claims/payments?claimNbr=${encodeURIComponent(r.id)}`} style={{ fontSize: 'var(--pcis-font-size-sm)' }}>
                  Open →
                </Link>
              ),
            },
          ]}
          getRowId={(r) => r.id}
          emptyMessage="No claims match this filter."
        />
      ) : null}
    </section>
  )
}
