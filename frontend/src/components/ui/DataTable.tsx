import { useMemo, useState, type ReactNode } from 'react'
import { Button } from './Button'
import styles from './DataTable.module.css'

export interface DataTableColumn<T> {
  id: string
  label: string
  accessor: (row: T) => string | number | null | undefined
  sortable?: boolean
  render?: (row: T) => ReactNode
}

export interface DataTableProps<T> {
  columns: DataTableColumn<T>[]
  rows: T[]
  getRowId: (row: T) => string
  pageSize?: number
  emptyMessage?: string
  selectable?: boolean
  'aria-label'?: string
}

type SortDir = 'asc' | 'desc'

export function DataTable<T>({
  columns,
  rows,
  getRowId,
  pageSize = 5,
  emptyMessage = 'No records found.',
  selectable = false,
  'aria-label': ariaLabel = 'Data table',
}: DataTableProps<T>) {
  const [sortId, setSortId] = useState<string | null>(null)
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<Set<string>>(new Set())

  const sorted = useMemo(() => {
    if (!sortId) return rows
    const col = columns.find((c) => c.id === sortId)
    if (!col) return rows
    const copy = [...rows]
    copy.sort((a, b) => {
      const av = col.accessor(a)
      const bv = col.accessor(b)
      const aNull = av === null || av === undefined
      const bNull = bv === null || bv === undefined
      if (aNull && bNull) return 0
      if (aNull) return 1
      if (bNull) return -1
      if (typeof av === 'number' && typeof bv === 'number') {
        return sortDir === 'asc' ? av - bv : bv - av
      }
      const cmp = String(av).localeCompare(String(bv), undefined, { numeric: true })
      return sortDir === 'asc' ? cmp : -cmp
    })
    return copy
  }, [rows, columns, sortId, sortDir])

  const pageCount = Math.max(1, Math.ceil(sorted.length / pageSize))
  const safePage = Math.min(page, pageCount - 1)
  const pageRows = sorted.slice(safePage * pageSize, safePage * pageSize + pageSize)

  const toggleSort = (columnId: string) => {
    if (sortId === columnId) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortId(columnId)
      setSortDir('asc')
    }
    setPage(0)
  }

  const toggleRow = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  if (rows.length === 0) {
    return (
      <div className={styles.wrapper} role="status">
        <p className={styles.empty}>{emptyMessage}</p>
      </div>
    )
  }

  return (
    <div className={styles.wrapper}>
      <table className={styles.table} aria-label={ariaLabel}>
        <thead>
          <tr>
            {selectable ? <th className={styles.th} scope="col">Select</th> : null}
            {columns.map((col) => (
              <th key={col.id} className={styles.th} scope="col" aria-sort={
                sortId === col.id ? (sortDir === 'asc' ? 'ascending' : 'descending') : 'none'
              }>
                {col.sortable ? (
                  <button
                    type="button"
                    className={styles.sortButton}
                    onClick={() => toggleSort(col.id)}
                  >
                    {col.label}
                    {sortId === col.id ? (sortDir === 'asc' ? ' ▲' : ' ▼') : ''}
                  </button>
                ) : (
                  col.label
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {pageRows.map((row) => {
            const id = getRowId(row)
            return (
              <tr key={id} className={selected.has(id) ? styles.selectedRow : undefined}>
                {selectable ? (
                  <td className={styles.td}>
                    <input
                      type="checkbox"
                      checked={selected.has(id)}
                      onChange={() => toggleRow(id)}
                      aria-label={`Select row ${id}`}
                    />
                  </td>
                ) : null}
                {columns.map((col) => (
                  <td key={col.id} className={styles.td}>
                    {col.render ? col.render(row) : String(col.accessor(row) ?? '')}
                  </td>
                ))}
              </tr>
            )
          })}
        </tbody>
      </table>
      <div className={styles.pagination}>
        <p className={styles.pageMeta}>
          Showing {safePage * pageSize + 1}–
          {Math.min((safePage + 1) * pageSize, sorted.length)} of {sorted.length}
        </p>
        <div className={styles.pageControls}>
          <Button
            size="sm"
            variant="secondary"
            disabled={safePage === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Previous
          </Button>
          <span className={styles.pageMeta}>
            Page {safePage + 1} of {pageCount}
          </span>
          <Button
            size="sm"
            variant="secondary"
            disabled={safePage >= pageCount - 1}
            onClick={() => setPage((p) => Math.min(pageCount - 1, p + 1))}
          >
            Next
          </Button>
        </div>
      </div>
    </div>
  )
}
