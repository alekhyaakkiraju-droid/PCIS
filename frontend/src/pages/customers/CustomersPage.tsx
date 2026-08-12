import { useParams, useSearchParams, Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { Customer360Page } from './Customer360Page'
import { CustomerSearch } from './CustomerSearch'
import { DuplicateResolutionBanner } from './DuplicateResolutionBanner'
import { customerApi, type CreateCustomerRequest } from '@/api/customer-api'
import { HttpError } from '@/api/types'
import { shouldUseCustomerFixtureFallback } from '@/api/customer-fixture-fallback'
import { BlueprintCard } from '@/components/ui'

const DUPLICATE_SCENARIO_CUSTOMER: CreateCustomerRequest = {
  taxId: '512444821',
  custName: 'Field Holdings LLC',
  custType: 'B',
}

type DuplicateScenarioState = {
  matchedCustomerId: number
  matchedCustomerName: string
}

export function CustomersPage() {
  const { customerId } = useParams()
  const [searchParams] = useSearchParams()
  const parsedId = customerId ? Number.parseInt(customerId, 10) : NaN
  const showDuplicateScenario = searchParams.get('scenario') === 'duplicate'

  const { data: duplicateState, isLoading: duplicateLoading } = useQuery({
    queryKey: ['customer-duplicate-scenario'],
    enabled: showDuplicateScenario,
    retry: false,
    queryFn: async (): Promise<DuplicateScenarioState> => {
      try {
        await customerApi.create(DUPLICATE_SCENARIO_CUSTOMER)
        throw new Error('Expected duplicate tax ID conflict')
      } catch (error) {
        if (error instanceof HttpError && error.status === 409) {
          const existingCustId = Number(error.problem?.existingCustId)
          const existingCustName = String(error.problem?.existingCustName ?? 'Existing customer')
          if (Number.isFinite(existingCustId)) {
            return { matchedCustomerId: existingCustId, matchedCustomerName: existingCustName }
          }
        }
        if (shouldUseCustomerFixtureFallback(error)) {
          return { matchedCustomerId: 19284, matchedCustomerName: 'Marta Field' }
        }
        throw error
      }
    },
  })

  if (customerId && Number.isFinite(parsedId)) {
    return <Customer360Page customerId={parsedId} />
  }

  return (
    <section aria-labelledby="customers-heading" className="wf-customer-search-page">
      <h1 id="customers-heading">Customer 360</h1>
      {showDuplicateScenario ? (
        duplicateLoading ? (
          <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginBottom: 'var(--pcis-space-4)' }}>
            Checking duplicate tax ID…
          </p>
        ) : duplicateState ? (
          <DuplicateResolutionBanner
            pendingCustomer={DUPLICATE_SCENARIO_CUSTOMER}
            matchedCustomerId={duplicateState.matchedCustomerId}
            matchedCustomerName={duplicateState.matchedCustomerName}
          />
        ) : null
      ) : null}
      <BlueprintCard kicker="Search" title="Find a customer" className="wf-search-card">
        <p className="wf-page-lede" style={{ marginTop: 0 }}>
          Search by name, customer ID, or tax ID
        </p>
        <CustomerSearch />
      </BlueprintCard>
      {!showDuplicateScenario ? (
        <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>
          Demo:{' '}
          <Link to="/customers?scenario=duplicate">duplicate tax ID resolution flow</Link>
        </p>
      ) : null}
    </section>
  )
}
