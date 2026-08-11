import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { customerFixtures } from '../../test-fixtures/tableData'
import { DataTable } from './DataTable'

const columns = [
  { id: 'id', label: 'Customer ID', accessor: (r: (typeof customerFixtures)[number]) => r.id, sortable: true },
  { id: 'name', label: 'Name', accessor: (r: (typeof customerFixtures)[number]) => r.name, sortable: true },
]

describe('DataTable', () => {
  it('renders rows, sorts, and paginates', async () => {
    const user = userEvent.setup()
    render(
      <DataTable
        aria-label="Customers"
        rows={customerFixtures}
        columns={columns}
        getRowId={(r) => r.id}
        pageSize={5}
      />,
    )
    expect(screen.getByRole('table', { name: 'Customers' })).toBeInTheDocument()
    expect(screen.getByText('C-1001')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Name/ }))
    expect(screen.getByRole('columnheader', { name: /Name/ })).toHaveAttribute('aria-sort', 'ascending')
    await user.click(screen.getByRole('button', { name: 'Next' }))
    expect(screen.getByText(/Page 2 of/)).toBeInTheDocument()
  })

  it('shows accessible empty state', () => {
    render(
      <DataTable
        rows={[]}
        columns={columns}
        getRowId={() => 'x'}
        emptyMessage="No customers found."
      />,
    )
    expect(screen.getByRole('status')).toHaveTextContent('No customers found.')
  })

  it('has no a11y violations', async () => {
    const { container } = render(
      <DataTable
        rows={customerFixtures.slice(0, 3)}
        columns={columns}
        getRowId={(r) => r.id}
      />,
    )
    expect(await axe(container)).toHaveNoViolations()
  })
})
