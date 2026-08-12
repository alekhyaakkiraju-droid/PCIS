import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import auditFixture from '../../../fixtures/customer-360/audit.json'
import billingFixture from '../../../fixtures/customer-360/billing.json'
import claimsFixture from '../../../fixtures/customer-360/claims.json'
import overviewFixture from '../../../fixtures/customer-360/overview.json'
import overview19284Fixture from '../../../fixtures/customer-360/overview-19284.json'
import policiesFixture from '../../../fixtures/customer-360/policies.json'
import profileFixture from '../../../fixtures/customer-360/profile.json'
import profile19284Fixture from '../../../fixtures/customer-360/profile-19284.json'
import { customerApi } from '@/api/customer-api'
import { formatClaimNbr } from '@/api/claims-fixture-fallback'
import { shouldUseCustomerFixtureFallback } from '@/api/customer-fixture-fallback'
import type {
  AuditEvent,
  Customer360BillingSection,
  Customer360ClaimsSection,
  Customer360PolicySection,
} from '@/api/customer360-types'
import type { Customer } from '@/api/customer-api'
import { maskPiiValue } from '@/hooks/useMaskedField'
import { Badge, BlueprintCard, DataTable, MoneyDisplay, Skeleton, Tabs, UnmaskModal, Avatar, Alert, Button, Modal, Input } from '@/components/ui'
import type { TabItem } from '@/components/ui'

type TabPanelProps = {
  loading: boolean
  error: Error | null
  children: ReactNode
}

function TabPanel({ loading, error, children }: TabPanelProps) {
  if (loading) return <Skeleton variant="text" lines={4} />
  if (error) {
    return (
      <p role="alert" style={{ color: 'var(--pcis-token-error)' }}>
        {error.message}
      </p>
    )
  }
  return <>{children}</>
}

function ContactMaskedFields({
  phone,
  email,
  maskPhone,
  maskEmail,
}: {
  phone?: string | null
  email?: string | null
  maskPhone: boolean
  maskEmail: boolean
}) {
  return (
    <div>
      <div>Phone: {maskPhone ? maskPiiValue(phone, 'phone') : (phone ?? '—')}</div>
      <div>Email: {maskEmail ? maskPiiValue(email, 'email') : (email ?? '—')}</div>
    </div>
  )
}

function OverviewTab({ custId, compact = false }: { custId: number; compact?: boolean }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['customer360', custId, 'overview'],
    queryFn: async () => {
      try {
        const response = await customerApi.get360(custId)
        return {
          custId: response.custId,
          custName: response.profile.data?.custName ?? 'Unknown',
          custStatus: response.profile.data?.custStatus ?? 'A',
          activePolicies: response.policies.data?.activeCount ?? 0,
          premiumInForce:
            response.policies.data?.items.reduce((sum, item) => sum + item.premium, 0) ?? 0,
          openClaims: response.claims.data?.openClaimCount ?? 0,
          balanceDue: response.billing.data?.balanceDue ?? 0,
        }
      } catch (err) {
        if (shouldUseCustomerFixtureFallback(err)) {
          return custId === 19284
            ? { ...(overview19284Fixture as typeof overviewFixture), custId }
            : { ...(overviewFixture as typeof overviewFixture), custId }
        }
        throw err
      }
    },
  })

  if (compact) {
    if (isLoading) return <Skeleton variant="text" lines={1} />
    if (error || !data) return null
    return (
      <>
        <div><div className="wf-stat-label">Policies in force</div><div className="wf-stat-value">{data.activePolicies}</div></div>
        <div><div className="wf-stat-label">Annual premium</div><div className="wf-stat-value mono"><MoneyDisplay value={data.premiumInForce} /></div></div>
        <div><div className="wf-stat-label">Open claims</div><div className="wf-stat-value">{data.openClaims}</div></div>
        <div><div className="wf-stat-label">Balance due</div><div className="wf-stat-value mono"><MoneyDisplay value={data.balanceDue} /></div></div>
      </>
    )
  }

  return (
    <TabPanel loading={isLoading} error={error}>
      {data ? (
        <dl style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1rem' }}>
          <div>
            <dt>Customer</dt>
            <dd>{data.custName}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>
              <Badge status="Active">{data.custStatus === 'A' ? 'Active' : data.custStatus}</Badge>
            </dd>
          </div>
          <div>
            <dt>Active policies</dt>
            <dd>{data.activePolicies}</dd>
          </div>
          <div>
            <dt>Premium in force</dt>
            <dd>
              <MoneyDisplay value={data.premiumInForce} />
            </dd>
          </div>
          <div>
            <dt>Open claims</dt>
            <dd>{data.openClaims}</dd>
          </div>
          <div>
            <dt>Balance due</dt>
            <dd>
              <MoneyDisplay value={data.balanceDue} />
            </dd>
          </div>
        </dl>
      ) : null}
    </TabPanel>
  )
}

function ProfileTab({
  custId,
  maskTax,
  maskEmail,
  maskPhone,
}: {
  custId: number
  maskTax: boolean
  maskEmail: boolean
  maskPhone: boolean
}) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['customer360', custId, 'profile'],
    queryFn: async (): Promise<Customer> => {
      try {
        return await customerApi.getById(custId)
      } catch (err) {
        if (shouldUseCustomerFixtureFallback(err)) {
          return custId === 19284 ? (profile19284Fixture as Customer) : (profileFixture as Customer)
        }
        throw err
      }
    },
  })

  return (
    <TabPanel loading={isLoading} error={error}>
      {data ? (
        <>
          <div
            className="card-kicker"
            style={{
              marginBottom: 8,
              fontSize: 10,
              letterSpacing: '0.1em',
              textTransform: 'uppercase',
              color: 'var(--pcis-color-primary-600)',
            }}
          >
            Identity
          </div>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
              gap: 'var(--pcis-space-4)',
              marginBottom: 'var(--pcis-space-6)',
            }}
          >
            <div>
              <div style={{ fontSize: 11, color: 'var(--pcis-color-text-muted)' }}>Customer type</div>
              <div style={{ fontSize: 14 }}>{data.custType === 'I' ? 'Individual (I)' : 'Business (B)'}</div>
            </div>
            <div>
              <div style={{ fontSize: 11, color: 'var(--pcis-color-text-muted)' }}>Tax ID</div>
              <div style={{ fontSize: 14 }}>{maskTax ? maskPiiValue(data.taxId, 'taxId') : (data.taxId ?? '—')}</div>
            </div>
            <div>
              <div style={{ fontSize: 11, color: 'var(--pcis-color-text-muted)' }}>Status</div>
              <div style={{ fontSize: 14 }}>
                <Badge status="Active">{data.custStatus === 'A' ? 'Active' : data.custStatus}</Badge>
              </div>
            </div>
            <div>
              <div style={{ fontSize: 11, color: 'var(--pcis-color-text-muted)' }}>Email</div>
              <div style={{ fontSize: 14 }}>
                {maskEmail ? maskPiiValue(data.contacts?.[0]?.emailAddr, 'email') : (data.contacts?.[0]?.emailAddr ?? '—')}
              </div>
            </div>
            <div>
              <div style={{ fontSize: 11, color: 'var(--pcis-color-text-muted)' }}>Phone</div>
              <div style={{ fontSize: 14 }}>
                {maskPhone ? maskPiiValue(data.contacts?.[0]?.phoneNbr, 'phone') : (data.contacts?.[0]?.phoneNbr ?? '—')}
              </div>
            </div>
          </div>
          {data.addresses?.length ? (
            <section aria-label="Addresses">
              <div
                className="card-kicker"
                style={{
                  marginBottom: 8,
                  fontSize: 10,
                  letterSpacing: '0.1em',
                  textTransform: 'uppercase',
                  color: 'var(--pcis-color-primary-600)',
                }}
              >
                Addresses
              </div>
              <DataTable
                aria-label="Customer addresses"
                rows={data.addresses}
                columns={[
                  { id: 'type', label: 'Type', accessor: (r) => r.addrType ?? 'PRM', render: (r) => (r.addrType === 'PRM' ? 'Mailing' : r.addrType ?? '—') },
                  { id: 'line1', label: 'Line 1', accessor: (r) => r.addressLine1 },
                  { id: 'city', label: 'City', accessor: (r) => r.city },
                  { id: 'state', label: 'State', accessor: (r) => r.stateCode },
                  { id: 'zip', label: 'Zip', accessor: (r) => r.zipCode },
                  {
                    id: 'primary',
                    label: 'Primary',
                    accessor: (r) => r.addrType,
                    render: (r) => (r.addrType === 'PRM' ? <Badge status="Active">Yes</Badge> : 'No'),
                  },
                ]}
                getRowId={(r) => String(r.addrId ?? r.addressLine1)}
                emptyMessage="No addresses."
              />
            </section>
          ) : null}
          {data.contacts?.length ? (
            <section aria-label="Contacts" style={{ marginTop: 'var(--pcis-space-6)' }}>
              <div
                className="card-kicker"
                style={{
                  marginBottom: 8,
                  fontSize: 10,
                  letterSpacing: '0.1em',
                  textTransform: 'uppercase',
                  color: 'var(--pcis-color-primary-600)',
                }}
              >
                Contacts
              </div>
              <ul style={{ margin: 0, padding: 0, listStyle: 'none' }}>
                {data.contacts.map((contact) => (
                  <li key={contact.contactId ?? `${contact.firstName}-${contact.lastName}`} style={{ marginBottom: 'var(--pcis-space-3)' }}>
                    <strong>
                      {contact.firstName} {contact.lastName}
                    </strong>
                    <ContactMaskedFields
                      phone={contact.phoneNbr}
                      email={contact.emailAddr}
                      maskPhone={maskPhone}
                      maskEmail={maskEmail}
                    />
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      ) : null}
    </TabPanel>
  )
}

function PoliciesTab({ custId }: { custId: number }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['customer360', custId, 'policies'],
    queryFn: async (): Promise<Customer360PolicySection> => {
      try {
        const response = await customerApi.get360(custId)
        if (response.policies.status === 'AVAILABLE' && response.policies.data) {
          return response.policies.data
        }
        throw new Error(response.policies.message ?? 'Policies unavailable')
      } catch (err) {
        if (shouldUseCustomerFixtureFallback(err)) {
          return policiesFixture as Customer360PolicySection
        }
        throw err
      }
    },
  })

  return (
    <TabPanel loading={isLoading} error={error}>
      {data ? (
        <DataTable
          aria-label="Customer policies"
          rows={data.items}
          columns={[
            {
              id: 'policyId',
              label: 'Policy',
              accessor: (r) => r.policyId,
              sortable: true,
              render: (r) => (
                <Link to={`/policies?mode=inquiry&highlightPolicy=${encodeURIComponent(r.policyId)}`}>{r.policyId}</Link>
              ),
            },
            { id: 'policyType', label: 'Type', accessor: (r) => r.policyType, sortable: true },
            { id: 'status', label: 'Status', accessor: (r) => r.status, sortable: true },
            {
              id: 'premium',
              label: 'Premium',
              accessor: (r) => r.premium,
              sortable: true,
              render: (r) => <MoneyDisplay value={r.premium} />,
            },
          ]}
          getRowId={(r) => r.policyId}
          emptyMessage="No policies found."
        />
      ) : null}
    </TabPanel>
  )
}

function BillingTab({ custId }: { custId: number }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['customer360', custId, 'billing'],
    queryFn: async (): Promise<Customer360BillingSection> => {
      try {
        const response = await customerApi.get360(custId)
        if (response.billing.status === 'AVAILABLE' && response.billing.data) {
          return response.billing.data
        }
        throw new Error(response.billing.message ?? 'Billing unavailable')
      } catch (err) {
        if (shouldUseCustomerFixtureFallback(err)) {
          return billingFixture as Customer360BillingSection
        }
        throw err
      }
    },
  })

  return (
    <TabPanel loading={isLoading} error={error}>
      {data ? (
        <dl>
          <div>
            <dt>Balance due</dt>
            <dd>
              <MoneyDisplay value={data.balanceDue} />
            </dd>
          </div>
          <div>
            <dt>Open invoices</dt>
            <dd>{data.openInvoiceCount}</dd>
          </div>
        </dl>
      ) : null}
    </TabPanel>
  )
}

function ClaimsTab({ custId }: { custId: number }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['customer360', custId, 'claims'],
    queryFn: async (): Promise<Customer360ClaimsSection> => {
      try {
        const response = await customerApi.get360(custId)
        if (response.claims.status === 'AVAILABLE' && response.claims.data) {
          return response.claims.data
        }
        throw new Error(response.claims.message ?? 'Claims unavailable')
      } catch (err) {
        if (shouldUseCustomerFixtureFallback(err)) {
          return claimsFixture as Customer360ClaimsSection
        }
        throw err
      }
    },
  })

  return (
    <TabPanel loading={isLoading} error={error}>
      {data ? (
        <DataTable
          aria-label="Customer claims"
          rows={data.items}
          columns={[
            {
              id: 'claimId',
              label: 'Claim',
              accessor: (r) => r.claimId,
              sortable: true,
              render: (r) => (
                <Link to={`/claims?claimNbr=${encodeURIComponent(formatClaimNbr(r.claimId))}`}>{formatClaimNbr(r.claimId)}</Link>
              ),
            },
            { id: 'status', label: 'Status', accessor: (r) => r.status, sortable: true },
            {
              id: 'reserveAmount',
              label: 'Reserve',
              accessor: (r) => r.reserveAmount,
              sortable: true,
              render: (r) => <MoneyDisplay value={r.reserveAmount} />,
            },
          ]}
          getRowId={(r) => r.claimId}
          emptyMessage="No claims found."
        />
      ) : null}
    </TabPanel>
  )
}

function AuditTab({ custId }: { custId: number }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['customer360', custId, 'audit'],
    queryFn: async (): Promise<AuditEvent[]> => {
      await new Promise((resolve) => setTimeout(resolve, 10))
      return auditFixture as AuditEvent[]
    },
  })

  return (
    <TabPanel loading={isLoading} error={error}>
      {data ? (
        <>
          <p style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)', marginBottom: 'var(--pcis-space-2)' }}>
            audit-svc does not yet expose a per-customer read endpoint, so this tab is not wired to a live log.
          </p>
          <DataTable
          aria-label="Customer audit trail"
          rows={data}
          columns={[
            { id: 'timestamp', label: 'When', accessor: (r) => r.timestamp, sortable: true },
            { id: 'action', label: 'Action', accessor: (r) => r.action, sortable: true },
            { id: 'actor', label: 'Actor', accessor: (r) => r.actor, sortable: true },
            { id: 'detail', label: 'Detail', accessor: (r) => r.detail },
          ]}
          getRowId={(r) => r.id}
          emptyMessage="No audit events."
          />
        </>
      ) : null}
    </TabPanel>
  )
}

export type Customer360PageProps = {
  customerId: number
}

export function Customer360Page({ customerId }: Customer360PageProps) {
  const [maskAll, setMaskAll] = useState(true)
  const maskTax = maskAll
  const maskEmail = maskAll
  const maskPhone = maskAll
  const [unmaskOpen, setUnmaskOpen] = useState(false)
  const [unmaskReason, setUnmaskReason] = useState<string | null>(null)

  const duplicateCheckQuery = useQuery({
    queryKey: ['customer', customerId, 'duplicate-check'],
    queryFn: () => customerApi.duplicateCheck(customerId),
    retry: false,
  })

  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['customer360', customerId, 'profile-header'],
    queryFn: async (): Promise<Customer> => {
      try {
        return await customerApi.getById(customerId)
      } catch (err) {
        if (shouldUseCustomerFixtureFallback(err)) {
          return customerId === 19284
            ? (profile19284Fixture as Customer)
            : (profileFixture as Customer)
        }
        throw err
      }
    },
  })

  const { data: aggregate } = useQuery({
    queryKey: ['customer360', customerId, 'aggregate'],
    queryFn: () => customerApi.get360(customerId),
    retry: false,
  })

  const policyCount = aggregate?.policies.data?.activeCount
  const claimCount = aggregate?.claims.data?.openClaimCount

  const tabs: TabItem[] = [
    {
      id: 'profile',
      label: 'Profile',
      content: <ProfileTab custId={customerId} maskTax={maskTax} maskEmail={maskEmail} maskPhone={maskPhone} />,
    },
    { id: 'policies', label: policyCount ? `Policies (${policyCount})` : 'Policies', content: <PoliciesTab custId={customerId} /> },
    { id: 'billing', label: 'Billing', content: <BillingTab custId={customerId} /> },
    { id: 'claims', label: claimCount ? `Claims (${claimCount})` : 'Claims', content: <ClaimsTab custId={customerId} /> },
    { id: 'audit', label: 'Audit trail', content: <AuditTab custId={customerId} /> },
  ]

  const initials = (profile?.custName ?? 'CU')
    .split(/\s+/)
    .map((p) => p[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  const queryClient = useQueryClient()
  const [editOpen, setEditOpen] = useState(false)
  const [editName, setEditName] = useState('')

  const updateMutation = useMutation({
    mutationFn: () => customerApi.update(customerId, { custName: editName }),
    onSuccess: () => {
      setEditOpen(false)
      queryClient.invalidateQueries({ queryKey: ['customer360', customerId] })
    },
  })

  return (
    <section aria-labelledby="customer-360-heading">
      <UnmaskModal
        open={unmaskOpen}
        onClose={() => setUnmaskOpen(false)}
        onConfirm={(justification) => {
          setMaskAll(false)
          setUnmaskReason(justification)
          setUnmaskOpen(false)
        }}
      />

      <div className="wf-header-row">
        <div style={{ display: 'flex', gap: 'var(--pcis-space-4)', alignItems: 'flex-start' }}>
          <Avatar initials={initials} label={profile?.custName ?? 'Customer'} size="md" />
          <div>
            <div className="wf-entity-kicker">Customer CUS-{String(customerId).padStart(7, '0')}</div>
            <h1 id="customer-360-heading" className="wf-entity-title">
              {profileLoading ? '…' : (profile?.custName ?? 'Customer 360')}
            </h1>
            <div style={{ display: 'flex', gap: 'var(--pcis-space-2)', alignItems: 'center', marginTop: 'var(--pcis-space-2)' }}>
              <Badge status="Active">{profile?.custStatus === 'A' ? 'Active' : profile?.custStatus === 'I' ? 'Inactive' : profile?.custStatus === 'S' ? 'Suspended' : '—'}</Badge>
              <Badge status="Denied" title="CUSTOMER_T is classified Restricted tier — Tax ID masked to last 4 digits">Restricted</Badge>
            </div>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 'var(--pcis-space-2)' }}>
          <Link to={`/policies?custId=${customerId}`}>
            <Button variant="ghost">New quote</Button>
          </Link>
          <Button
            variant="primary"
            onClick={() => {
              setEditName(profile?.custName ?? '')
              setEditOpen(true)
            }}
          >
            Edit customer
          </Button>
        </div>
      </div>

      <Modal
        open={editOpen}
        title="Edit customer"
        onClose={() => setEditOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setEditOpen(false)}>Cancel</Button>
            <Button variant="primary" onClick={() => updateMutation.mutate()} loading={updateMutation.isPending}>
              Save
            </Button>
          </>
        }
      >
        <Input label="Customer name" name="editName" value={editName} onChange={(e) => setEditName(e.target.value)} />
        {updateMutation.error ? <p role="alert">Update failed.</p> : null}
      </Modal>

      {duplicateCheckQuery.data?.duplicateFound && duplicateCheckQuery.data.existingCustomer ? (
        <Alert variant="warning" title="Possible duplicate tax ID" role="alert">
          Blocking step for new record creation — review candidate CUS-
          {String(duplicateCheckQuery.data.existingCustomer.custId).padStart(7, '0')} (
          {duplicateCheckQuery.data.existingCustomer.custName}).
        </Alert>
      ) : null}

      <div className="wf-stats-bar">
        <OverviewTab custId={customerId} compact />
      </div>

      <label style={{ display: 'inline-flex', alignItems: 'center', gap: 'var(--pcis-space-2)', fontSize: 'var(--pcis-font-size-sm)', marginBottom: 'var(--pcis-space-4)' }}>
        <input
          type="checkbox"
          role="switch"
          checked={!maskAll}
          onChange={(e) => {
            if (e.target.checked) setUnmaskOpen(true)
            else setMaskAll(true)
          }}
        />
        Unmask restricted fields
      </label>
      {!maskAll && unmaskReason ? (
        <p style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)', marginTop: -8, marginBottom: 'var(--pcis-space-4)' }}>
          Unmasked — reason logged for this session: "{unmaskReason}"
        </p>
      ) : null}

      <Tabs items={tabs} aria-label="Customer 360 sections" defaultTabId="profile" />

      <Alert variant="info" title="Classification notice">
        Restricted-tier values are masked in profile views. Unmasking requires a recorded justification.
      </Alert>
    </section>
  )
}
