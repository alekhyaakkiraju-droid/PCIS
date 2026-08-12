import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { batchStatusApi, type BatchJobRun } from '@/api/batch-status-api'
import { Badge, BlueprintCard, Button, DataTable } from '@/components/ui'

function statusBadge(status: string): 'Active' | 'Pending' | 'Inactive' {
  if (status === 'FAILED' || status === 'UNKNOWN') return 'Pending'
  if (status === 'STARTED' || status === 'STARTING') return 'Inactive'
  return 'Active'
}

function formatTimestamp(value: string | null): string {
  if (!value) return '—'
  return new Date(value).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

function formatDuration(start: string | null, end: string | null): string {
  if (!start || !end) return '—'
  const ms = new Date(end).getTime() - new Date(start).getTime()
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

function JobDetailPanel({ job }: { job: BatchJobRun }) {
  return (
    <BlueprintCard kicker={`${job.jobName} — run detail (execution #${job.jobExecutionId})`} elevation="md">
      <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>
        Domain <strong>{job.domain}</strong> · Started {formatTimestamp(job.startTime)} · Duration{' '}
        {formatDuration(job.startTime, job.endTime)}
      </p>
      <DataTable
        aria-label="Step executions"
        rows={job.steps}
        columns={[
          { id: 'stepName', label: 'Step', accessor: (r) => r.stepName },
          {
            id: 'status',
            label: 'Status',
            accessor: (r) => r.status,
            render: (r) => <Badge status={statusBadge(r.status)}>{r.status}</Badge>,
          },
          { id: 'read', label: 'Read', accessor: (r) => r.readCount, render: (r) => <span className="mono">{r.readCount}</span> },
          { id: 'written', label: 'Written', accessor: (r) => r.writeCount, render: (r) => <span className="mono">{r.writeCount}</span> },
          { id: 'skipped', label: 'Skipped', accessor: (r) => r.skipCount, render: (r) => <span className="mono">{r.skipCount}</span> },
          { id: 'exit', label: 'Exit', accessor: (r) => r.exitCode ?? '—' },
        ]}
        getRowId={(r) => r.stepName}
        emptyMessage="No steps recorded."
      />
      <div style={{ display: 'flex', gap: 'var(--pcis-space-2)', flexWrap: 'wrap', marginTop: 'var(--pcis-space-4)' }}>
        <Button variant="secondary" disabled title="Interactive restart is not wired up yet">
          Skip &amp; continue
        </Button>
        <Button variant="primary" disabled title="Interactive restart is not wired up yet">
          Restart from last committed chunk
        </Button>
      </div>
    </BlueprintCard>
  )
}

export function BatchOperationsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['batch-runs'],
    queryFn: () => batchStatusApi.listRuns(),
  })

  const runs = data ?? []
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const rowKey = (r: BatchJobRun) => `${r.domain}-${r.jobExecutionId}`
  const selectedJob = runs.find((r) => rowKey(r) === selectedKey) ?? runs[0]

  return (
    <section aria-labelledby="batch-heading">
      <h1 id="batch-heading">Batch operations</h1>
      <p className="wf-page-lede">
        Real Spring Batch run history for the converted COBOL programs — read from each job's own execution
        metadata.
      </p>

      {isLoading ? (
        <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>Loading batch run history…</p>
      ) : error ? (
        <p role="alert">Unable to load batch run history.</p>
      ) : runs.length === 0 ? (
        <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>No batch runs recorded yet.</p>
      ) : (
        <div className="wf-batch-layout">
          <DataTable
            aria-label="Batch jobs"
            rows={runs}
            columns={[
              { id: 'name', label: 'Job', accessor: (r) => r.jobName },
              { id: 'domain', label: 'Domain', accessor: (r) => r.domain },
              { id: 'started', label: 'Started', accessor: (r) => r.startTime, render: (r) => <span className="mono">{formatTimestamp(r.startTime)}</span> },
              {
                id: 'duration',
                label: 'Duration',
                accessor: (r) => r.startTime,
                render: (r) => <span className="mono">{formatDuration(r.startTime, r.endTime)}</span>,
              },
              { id: 'read', label: 'Read', accessor: (r) => r.readCount, render: (r) => <span className="mono">{r.readCount}</span> },
              { id: 'written', label: 'Written', accessor: (r) => r.writeCount, render: (r) => <span className="mono">{r.writeCount}</span> },
              { id: 'skipped', label: 'Skipped', accessor: (r) => r.skipCount, render: (r) => <span className="mono">{r.skipCount}</span> },
              {
                id: 'status',
                label: 'Status',
                accessor: (r) => r.status,
                render: (r) => <Badge status={statusBadge(r.status)}>{r.status}</Badge>,
              },
              { id: 'exit', label: 'Exit', accessor: (r) => r.exitCode ?? '—' },
              {
                id: 'details',
                label: '',
                accessor: () => '',
                render: (r) => (
                  <button
                    type="button"
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'var(--pcis-color-primary-700)',
                      cursor: 'pointer',
                      fontSize: 'var(--pcis-font-size-sm)',
                      fontWeight: selectedJob && rowKey(selectedJob) === rowKey(r) ? 600 : 400,
                    }}
                    onClick={() => setSelectedKey(rowKey(r))}
                  >
                    {selectedJob && rowKey(selectedJob) === rowKey(r) ? 'Selected' : 'Details →'}
                  </button>
                ),
              },
            ]}
            getRowId={(r) => `${r.domain}-${r.jobExecutionId}`}
            emptyMessage="No batch jobs."
          />

          {selectedJob ? <JobDetailPanel job={selectedJob} /> : null}
        </div>
      )}
    </section>
  )
}
