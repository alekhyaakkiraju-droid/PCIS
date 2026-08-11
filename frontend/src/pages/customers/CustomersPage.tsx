import { useParams, useSearchParams, Link } from 'react-router'
import { Customer360Page } from './Customer360Page'
import { CustomerSearch } from './CustomerSearch'
import { DuplicateResolutionBanner } from './DuplicateResolutionBanner'
import { BlueprintCard } from '@/components/ui'

export function CustomersPage() {
  const { customerId } = useParams()
  const [searchParams] = useSearchParams()
  const parsedId = customerId ? Number.parseInt(customerId, 10) : NaN
  const showDuplicateScenario = searchParams.get('scenario') === 'duplicate'

  if (customerId && Number.isFinite(parsedId)) {
    return <Customer360Page customerId={parsedId} />
  }

  return (
    <section aria-labelledby="customers-heading">
      <h1 id="customers-heading">Customer 360</h1>
      {showDuplicateScenario ? (
        <DuplicateResolutionBanner
          onUseExisting={() => {
            window.location.href = '/customers/19284'
          }}
        />
      ) : null}
      <BlueprintCard kicker="Search" title="Find a customer" style={{ maxWidth: 720, marginBottom: 'var(--pcis-space-6)' }}>
        <CustomerSearch />
      </BlueprintCard>
      {!showDuplicateScenario ? (
        <p style={{ fontSize: 'var(--pcis-font-size-sm)', color: 'var(--pcis-color-text-muted)' }}>
          Demo:{' '}
          <Link to="/customers?scenario=duplicate">duplicate tax ID resolution flow</Link>
        </p>
      ) : null}
    </section>
  )
}
