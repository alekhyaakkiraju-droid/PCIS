import { useState } from 'react'
import { useNavigate } from 'react-router'
import { customerApi } from '@/api/customer-api'
import { Button, Card, DataTable, Input, Skeleton } from '@/components/ui'
import type { Customer } from '@/api/customer-api'

const searchColumns = [
  {
    id: 'custId',
    label: 'Customer ID',
    accessor: (row: Customer) => row.custId,
    sortable: true,
  },
  {
    id: 'custName',
    label: 'Name',
    accessor: (row: Customer) => row.custName,
    sortable: true,
  },
  {
    id: 'custStatus',
    label: 'Status',
    accessor: (row: Customer) => row.custStatus,
    sortable: true,
  },
]

export function CustomerSearch() {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [results, setResults] = useState<Customer[]>([])

  const handleSearch = async (event: React.FormEvent) => {
    event.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const page = await customerApi.list({ q: query.trim() || undefined, size: 20 })
      setResults(page.content)
    } catch {
      setError('Unable to search customers. Please try again.')
      setResults([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card header={<h2>Customer Search</h2>}>
      <form onSubmit={handleSearch} aria-label="Customer search form">
        <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1rem' }}>
          <Input
            label="Search"
            name="q"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Name, ID, or tax ID"
          />
          <Button type="submit" loading={loading} style={{ alignSelf: 'flex-end' }}>
            Search
          </Button>
        </div>
      </form>

      {error ? (
        <p role="alert" style={{ color: 'var(--c-error, #da1e28)' }}>
          {error}
        </p>
      ) : null}

      {loading ? (
        <Skeleton variant="text" lines={4} />
      ) : (
        <DataTable
          aria-label="Customer search results"
          rows={results}
          columns={searchColumns}
          getRowId={(row) => String(row.custId)}
          emptyMessage="Enter a search term to find customers."
          pageSize={10}
        />
      )}

      {results.length > 0 ? (
        <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {results.slice(0, 5).map((customer) => (
            <Button
              key={customer.custId}
              variant="secondary"
              size="sm"
              onClick={() => navigate(`/customers/${customer.custId}`)}
            >
              View {customer.custName}
            </Button>
          ))}
        </div>
      ) : null}
    </Card>
  )
}
