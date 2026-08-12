import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { customerApi, type Customer } from '@/api/customer-api'
import { shouldUseCustomerFixtureFallback } from '@/api/customer-fixture-fallback'
import { Input } from '@/components/ui'

const DEMO_CUSTOMERS: Customer[] = [
  { custId: 19284, custName: 'Marta Field', custType: 'B', custStatus: 'A' },
  { custId: 100001, custName: 'Alice Johnson', custType: 'I', custStatus: 'A' },
]

export function CustomerSearch() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')

  const trimmed = query.trim()
  const { data, isLoading, error, isFetching } = useQuery({
    queryKey: ['customer-search', trimmed],
    queryFn: async () => {
      try {
        return trimmed.length === 0 ? await customerApi.list() : await customerApi.search(trimmed)
      } catch (err) {
        if (shouldUseCustomerFixtureFallback(err)) {
          const term = trimmed.toLowerCase()
          return term.length === 0
            ? DEMO_CUSTOMERS
            : DEMO_CUSTOMERS.filter(
                (customer) =>
                  customer.custName.toLowerCase().includes(term) ||
                  String(customer.custId).includes(term),
              )
        }
        throw err
      }
    },
  })

  const results = data ?? []

  return (
    <div>
      <Input
        label="Customer search"
        name="customerSearch"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="e.g. Marta Field or 19284"
      />
      {(isLoading || isFetching) ? (
        <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginTop: 'var(--pcis-space-3)' }}>
          Searching…
        </p>
      ) : null}
      {error ? (
        <p role="alert" style={{ color: 'var(--pcis-token-error)', marginTop: 'var(--pcis-space-3)' }}>
          {(error as Error).message}
        </p>
      ) : null}
      <p
        style={{
          fontSize: 'var(--pcis-font-size-sm)',
          marginTop: 'var(--pcis-space-4)',
          marginBottom: 'var(--pcis-space-2)',
        }}
      >
        {query.trim().length >= 2 ? 'Search results' : 'Recent customers'}
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-2)' }}>
        {results.map((customer) => (
          <button
            key={customer.custId}
            type="button"
            className="wf-demo-btn"
            onClick={() => navigate(`/customers/${customer.custId}`)}
          >
            {customer.custName} · CUS-{String(customer.custId).padStart(7, '0')}
          </button>
        ))}
        {!isLoading && results.length === 0 ? (
          <p style={{ fontSize: 'var(--pcis-font-size-sm)', color: 'var(--pcis-color-text-muted)' }}>
            No customers matched your search.
          </p>
        ) : null}
      </div>
    </div>
  )
}
