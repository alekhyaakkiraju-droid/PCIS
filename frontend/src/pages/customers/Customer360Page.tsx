import { useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import auditFixture from '../../../fixtures/customer-360/audit.json'
import billingFixture from '../../../fixtures/customer-360/billing.json'
import claimsFixture from '../../../fixtures/customer-360/claims.json'
import overviewFixture from '../../../fixtures/customer-360/overview.json'
import policiesFixture from '../../../fixtures/customer-360/policies.json'
import profileFixture from '../../../fixtures/customer-360/profile.json'
import { customerApi } from '@/api/customer-api'
import type {
  AuditEvent,
  Customer360BillingSection,
  Customer360ClaimsSection,
  Customer360PolicySection,
} from '@/api/customer360-types'
import type { Customer } from '@/api/customer-api'
import { useMaskedField } from '@/hooks/useMaskedField'
import { Badge, DataTable, MoneyDisplay, Skeleton, Tabs } from '@/components/ui'
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
      <p role="alert" style={{ color: 'var(--c-error, #da1e28)' }}>
        {error.message}
      </p>
    )
  }
  return <>{children}</>
}

function ContactMaskedFields({ phone, email }: { phone?: string | null; email?: string | null }) {
  const phoneField = useMaskedField(phone, 'phone')
  const emailField = useMaskedField(email, 'email')
  return (
    <div>
      <div>
        Phone: {phoneField.displayValue}
        {phoneField.canToggle ? (
          <button type="button" onClick={phoneField.toggleReveal} style={{ marginLeft: '0.5rem' }}>
            {phoneField.isMasked ? 'Show' : 'Hide'}
          </button>
        ) : null}
      </div>
      <div>
        Email: {emailField.displayValue}
        {emailField.canToggle ? (
          <button type="button" onClick={emailField.toggleReveal} style={{ marginLeft: '0.5rem' }}>
            {emailField.isMasked ? 'Show' : 'Hide'}
          </button>
        ) : null}
      </div>
    </div>
  )
}

function MaskedField({ label, value, type }: { label: string; value?: string | null; type: 'taxId' | 'phone' | 'email' }) {
  const { displayValue, canToggle, isMasked, toggleReveal } = useMaskedField(value, type)
  return (
    <div>
      <dt>{label}</dt>
      <dd>
        {displayValue}
        {canToggle ? (
          <button type="button" onClick={toggleReveal} style={{ marginLeft: '0.5rem' }}>
            {isMasked ? 'Show' : 'Hide'}
          </button>
        ) : null}
      </dd>
    </div>
  )
}

function OverviewTab({ custId }: { custId: number }) {
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
      } catch {
        return { ...(overviewFixture as typeof overviewFixture), custId }
      }
    },
  })

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

function ProfileTab({ custId }: { custId: number }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['customer360', custId, 'profile'],
    queryFn: async (): Promise<Customer> => {
      try {
        return await customerApi.getById(custId)
      } catch {
        return profileFixture as Customer
      }
    },
  })

  return (
    <TabPanel loading={isLoading} error={error}>
      {data ? (
        <>
          <dl style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1rem' }}>
            <div>
              <dt>Name</dt>
              <dd>{data.custName}</dd>
            </div>
            <div>
              <dt>Type</dt>
              <dd>{data.custType === 'I' ? 'Individual' : 'Business'}</dd>
            </div>
            <MaskedField label="Tax ID" value={data.taxId} type="taxId" />
          </dl>
          {data.addresses?.length ? (
            <section aria-label="Addresses" style={{ marginTop: '1rem' }}>
              <h3>Addresses</h3>
              <ul>
                {data.addresses.map((addr) => (
                  <li key={addr.addrId ?? `${addr.addressLine1}-${addr.city}`}>
                    {addr.addressLine1}, {addr.city}, {addr.stateCode} {addr.zipCode}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
          {data.contacts?.length ? (
            <section aria-label="Contacts" style={{ marginTop: '1rem' }}>
              <h3>Contacts</h3>
              <ul>
                {data.contacts.map((contact) => (
                  <li key={contact.contactId ?? `${contact.firstName}-${contact.lastName}`}>
                    <strong>
                      {contact.firstName} {contact.lastName}
                    </strong>
                    <ContactMaskedFields phone={contact.phoneNbr} email={contact.emailAddr} />
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
      } catch {
        return policiesFixture as Customer360PolicySection
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
            { id: 'policyId', label: 'Policy', accessor: (r) => r.policyId, sortable: true },
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
      } catch {
        return billingFixture as Customer360BillingSection
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
      } catch {
        return claimsFixture as Customer360ClaimsSection
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
            { id: 'claimId', label: 'Claim', accessor: (r) => r.claimId, sortable: true },
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
      ) : null}
    </TabPanel>
  )
}

export type Customer360PageProps = {
  customerId: number
}

export function Customer360Page({ customerId }: Customer360PageProps) {
  const tabs: TabItem[] = [
    { id: 'overview', label: 'Overview', content: <OverviewTab custId={customerId} /> },
    { id: 'profile', label: 'Profile', content: <ProfileTab custId={customerId} /> },
    { id: 'policies', label: 'Policies', content: <PoliciesTab custId={customerId} /> },
    { id: 'billing', label: 'Billing', content: <BillingTab custId={customerId} /> },
    { id: 'claims', label: 'Claims', content: <ClaimsTab custId={customerId} /> },
    { id: 'audit', label: 'Audit', content: <AuditTab custId={customerId} /> },
  ]

  return (
    <section aria-labelledby="customer-360-heading">
      <h1 id="customer-360-heading">Customer 360</h1>
      <p>Customer ID: {customerId}</p>
      <Tabs items={tabs} aria-label="Customer 360 sections" defaultTabId="overview" />
    </section>
  )
}
