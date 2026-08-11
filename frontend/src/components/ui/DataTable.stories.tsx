import type { Meta, StoryObj } from '@storybook/react'
import { Badge } from './Badge'
import { DataTable } from './DataTable'
import { MoneyDisplay } from './MoneyDisplay'
import { claimFixtures, customerFixtures } from '../../test-fixtures/tableData'
import type { ClaimStatus } from './Badge'

const meta: Meta<typeof DataTable> = {
  title: 'UI/DataTable',
  component: DataTable,
  tags: ['autodocs'],
}

export default meta
type Story = StoryObj<typeof DataTable>

export const Customers: Story = {
  args: {
    'aria-label': 'Customers',
    pageSize: 5,
    getRowId: (row: (typeof customerFixtures)[number]) => row.id,
    rows: customerFixtures,
    columns: [
      { id: 'id', label: 'Customer ID', accessor: (r) => r.id, sortable: true },
      { id: 'name', label: 'Name', accessor: (r) => r.name, sortable: true },
      { id: 'state', label: 'State', accessor: (r) => r.state, sortable: true },
      { id: 'status', label: 'Status', accessor: (r) => r.status },
    ],
  },
}

export const Claims: Story = {
  args: {
    'aria-label': 'Claims',
    selectable: true,
    pageSize: 5,
    getRowId: (row: (typeof claimFixtures)[number]) => row.id,
    rows: claimFixtures,
    columns: [
      { id: 'id', label: 'Claim ID', accessor: (r) => r.id, sortable: true },
      {
        id: 'status',
        label: 'Status',
        accessor: (r) => r.status,
        render: (r) => <Badge status={r.status as ClaimStatus} />,
      },
      { id: 'lossDate', label: 'Loss date', accessor: (r) => r.lossDate, sortable: true },
      {
        id: 'reserve',
        label: 'Reserve',
        accessor: (r) => r.reserve,
        sortable: true,
        render: (r) => <MoneyDisplay value={r.reserve} />,
      },
    ],
  },
}

export const Empty: Story = {
  args: {
    rows: [],
    getRowId: () => 'x',
    columns: [{ id: 'id', label: 'ID', accessor: () => '' }],
    emptyMessage: 'No claims match your filters.',
  },
}
