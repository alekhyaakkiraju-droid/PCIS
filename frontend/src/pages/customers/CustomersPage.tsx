import { useParams } from 'react-router'
import { Customer360Page } from './Customer360Page'
import { CustomerSearch } from './CustomerSearch'

export function CustomersPage() {
  const { customerId } = useParams()
  const parsedId = customerId ? Number.parseInt(customerId, 10) : NaN

  if (customerId && Number.isFinite(parsedId)) {
    return <Customer360Page customerId={parsedId} />
  }

  return (
    <section>
      <h1>Customers</h1>
      <CustomerSearch />
    </section>
  )
}
