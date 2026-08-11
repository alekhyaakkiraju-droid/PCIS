import { useNavigate } from 'react-router'
import { Button } from '@/components/ui'

const DEMO_CUSTOMERS = [
  { custId: 19284, custName: 'Marta Field', custStatus: 'A' },
  { custId: 100001, custName: 'Alice Johnson', custStatus: 'A' },
]

export function CustomerSearch() {
  const navigate = useNavigate()

  return (
    <div>
      <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginBottom: 'var(--pcis-space-4)' }}>
        Select a demo customer from the wireframe:
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--pcis-space-2)' }}>
        {DEMO_CUSTOMERS.map((customer) => (
          <Button
            key={customer.custId}
            variant="secondary"
            onClick={() => navigate(`/customers/${customer.custId}`)}
            style={{ justifyContent: 'flex-start' }}
          >
            {customer.custName} · CUS-{String(customer.custId).padStart(7, '0')}
          </Button>
        ))}
      </div>
    </div>
  )
}
