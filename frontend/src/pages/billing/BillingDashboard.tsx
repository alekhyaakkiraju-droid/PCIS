import { useQuery } from '@tanstack/react-query'
import { billingApi } from '@/api/billing-api'
import { Card, DataTable, MoneyDisplay, Skeleton, Tabs } from '@/components/ui'
import type { TabItem } from '@/components/ui'

function InstallmentsView() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['billing', 'installments'],
    queryFn: () => billingApi.listInstallments(),
  })

  if (isLoading) return <Skeleton variant="text" lines={4} />
  if (error) return <p role="alert">Unable to load installments.</p>

  return (
    <DataTable
      aria-label="Billing installments"
      rows={data ?? []}
      columns={[
        { id: 'id', label: 'Installment', accessor: (r) => r.id, sortable: true },
        { id: 'policyId', label: 'Policy', accessor: (r) => r.policyId, sortable: true },
        { id: 'dueDate', label: 'Due date', accessor: (r) => r.dueDate, sortable: true },
        {
          id: 'amount',
          label: 'Amount',
          accessor: (r) => r.amount,
          render: (r) => <MoneyDisplay value={r.amount} />,
        },
        { id: 'status', label: 'Status', accessor: (r) => r.status, sortable: true },
      ]}
      getRowId={(r) => r.id}
      emptyMessage="No installments."
    />
  )
}

function AgingView() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['billing', 'aging'],
    queryFn: () => billingApi.listAging(),
  })

  if (isLoading) return <Skeleton variant="text" lines={4} />
  if (error) return <p role="alert">Unable to load aging report.</p>

  return (
    <DataTable
      aria-label="Aging buckets"
      rows={data ?? []}
      columns={[
        { id: 'bucket', label: 'Bucket', accessor: (r) => r.bucket, sortable: true },
        { id: 'invoiceCount', label: 'Invoices', accessor: (r) => r.invoiceCount, sortable: true },
        {
          id: 'amountDue',
          label: 'Amount due',
          accessor: (r) => r.amountDue,
          render: (r) => <MoneyDisplay value={r.amountDue} />,
        },
      ]}
      getRowId={(r) => r.bucket}
      emptyMessage="No aging data."
    />
  )
}

export function BillingDashboard() {
  const tabs: TabItem[] = [
    { id: 'installments', label: 'Installments', content: <InstallmentsView /> },
    { id: 'aging', label: 'Aging', content: <AgingView /> },
  ]

  return (
    <section aria-labelledby="billing-heading">
      <h1 id="billing-heading">Billing Dashboard</h1>
      <Card>
        <Tabs items={tabs} aria-label="Billing views" defaultTabId="installments" />
      </Card>
    </section>
  )
}
