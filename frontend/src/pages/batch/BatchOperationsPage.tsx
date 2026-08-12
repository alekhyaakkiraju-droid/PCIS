import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { batchStatusApi, type BatchJobRun } from '@/api/batch-status-api'
import { Badge, BlueprintCard, Button, DataTable } from '@/components/ui'

/**
 * Static per-job metadata — legacy COBOL program ID, configured cadence, and whether a real
 * on-demand trigger endpoint exists. Sourced from each batch module's own JobConfig/application.yaml,
 * not derived from run history (Spring Batch execution tables don't carry this — it's config, not
 * runtime state). `triggerable` jobs live in billing-svc, a persistent server with its Job beans
 * always registered — see BatchTriggerController. The rest are one-shot CLI jars with no running
 * process to send a trigger request to, so they can't be triggered without a bigger infra change.
 */
const JOB_META: Record<string, { legacyProgram: string; schedule: string; triggerable?: boolean }> = {
  auditArchiveJob: { legacyProgram: 'AUD002B', schedule: 'Daily' },
  auditPurgeJob: { legacyProgram: '—', schedule: 'Daily' },
  claimPaymentJob: { legacyProgram: 'CLM006B', schedule: 'Daily' },
  policyRenewalJob: { legacyProgram: 'POL006B', schedule: 'Monthly' },
  billingGenerationJob: { legacyProgram: 'BIL003B', schedule: 'Daily', triggerable: true },
  commissionCalculationJob: { legacyProgram: 'CMM001B', schedule: 'Daily', triggerable: true },
  delinquencyAgingJob: { legacyProgram: 'PRM005B', schedule: 'Daily', triggerable: true },
  reconciliationJob: { legacyProgram: '—', schedule: 'Daily' },
  domainRollbackJob: { legacyProgram: '—', schedule: 'Manual' },
}

function jobMeta(jobName: string) {
  return JOB_META[jobName] ?? { legacyProgram: '—', schedule: '—' }
}

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
  const [errorExpanded, setErrorExpanded] = useState(false)
  const meta = jobMeta(job.jobName)

  return (
    <BlueprintCard kicker={`${job.jobName} — run detail (execution #${job.jobExecutionId})`} elevation="md">
      <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>
        Domain <strong>{job.domain}</strong> · Legacy program <strong className="mono">{meta.legacyProgram}</strong> ·
        Started {formatTimestamp(job.startTime)} · Duration {formatDuration(job.startTime, job.endTime)}
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
          { id: 'errors', label: 'Errors', accessor: (r) => r.skipCount, render: (r) => <span className="mono">{r.skipCount}</span> },
          { id: 'exit', label: 'Exit', accessor: (r) => r.exitCode ?? '—' },
        ]}
        getRowId={(r) => r.stepName}
        emptyMessage="No steps recorded."
      />

      {job.exitMessage ? (
        <div style={{ marginTop: 'var(--pcis-space-4)' }}>
          <button
            type="button"
            onClick={() => setErrorExpanded((v) => !v)}
            style={{ background: 'none', border: 'none', color: 'var(--pcis-token-error)', cursor: 'pointer', padding: 0, fontSize: 'var(--pcis-font-size-sm)', fontWeight: 600 }}
          >
            {errorExpanded ? '▾' : '▸'} Error detail
          </button>
          {errorExpanded ? (
            <pre
              className="mono"
              style={{
                marginTop: 'var(--pcis-space-2)',
                padding: 'var(--pcis-space-3)',
                background: 'var(--pcis-color-error-50)',
                color: 'var(--pcis-color-error-700)',
                fontSize: 'var(--pcis-font-size-xs)',
                overflowX: 'auto',
                whiteSpace: 'pre-wrap',
                maxHeight: 260,
                overflowY: 'auto',
              }}
            >
              {job.exitMessage}
            </pre>
          ) : null}
        </div>
      ) : null}

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
  const queryClient = useQueryClient()
  const { data, isLoading, error } = useQuery({
    queryKey: ['batch-runs'],
    queryFn: () => batchStatusApi.listRuns(),
  })

  const runs = data ?? []
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const rowKey = (r: BatchJobRun) => `${r.domain}-${r.jobExecutionId}`

  // The table shows one row per job — its latest execution. Full history stays
  // available per-job via Details (which already surfaces logs/steps for that run).
  const latestRuns = useMemo(() => {
    const seen = new Set<string>()
    const latest: BatchJobRun[] = []
    for (const run of runs) {
      if (seen.has(run.jobName)) continue
      seen.add(run.jobName)
      latest.push(run)
    }
    return latest
  }, [runs])

  const selectedJob = latestRuns.find((r) => rowKey(r) === selectedKey) ?? latestRuns[0]

  const triggerMutation = useMutation({
    mutationFn: (jobName: string) => batchStatusApi.triggerRun(jobName),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['batch-runs'] }),
  })
  const isTriggerable = Boolean(selectedJob && jobMeta(selectedJob.jobName).triggerable)

  const jobsTracked = Object.keys(JOB_META).length
  const completedCount = runs.filter((r) => r.status === 'COMPLETED').length
  const totalErrors = runs.reduce((sum, r) => sum + r.skipCount, 0)
  const failedCount = runs.filter((r) => r.status === 'FAILED' || r.status === 'UNKNOWN').length
  const mostRecent = runs[0]

  return (
    <section aria-labelledby="batch-heading">
      <h1 id="batch-heading">Batch Operations Console</h1>
      <p className="wf-page-lede">
        Real Spring Batch run history for the converted COBOL programs — read directly from each job's own
        execution metadata, across every modernized domain.
      </p>

      {!isLoading && !error && runs.length > 0 ? (
        <div className="wf-kpi-grid" style={{ marginBottom: 'var(--pcis-space-6)' }}>
          <div className="wf-kpi-card">
            <div className="wf-stat-label">Jobs tracked</div>
            <div className="wf-kpi-value">{jobsTracked}</div>
            <div className="wf-kpi-sub">Across audit, claims, policy, billing, reconciliation</div>
          </div>
          <div className="wf-kpi-card">
            <div className="wf-stat-label">Last run duration</div>
            <div className="wf-kpi-value">{formatDuration(mostRecent?.startTime ?? null, mostRecent?.endTime ?? null)}</div>
            <div className="wf-kpi-sub">{mostRecent?.jobName ?? '—'}</div>
          </div>
          <div className="wf-kpi-card">
            <div className="wf-stat-label">Jobs completed</div>
            <div className="wf-kpi-value">{completedCount} / {runs.length}</div>
            <div className="wf-kpi-sub">{failedCount > 0 ? `${failedCount} failed` : 'None failed'}</div>
          </div>
          <div className="wf-kpi-card">
            <div className="wf-stat-label">Total errors</div>
            <div className={`wf-kpi-value${totalErrors > 0 ? ' wf-kpi-sub--warn' : ''}`}>{totalErrors}</div>
            <div className="wf-kpi-sub">Skipped records across all runs</div>
          </div>
        </div>
      ) : null}

      {isLoading ? (
        <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>Loading batch run history…</p>
      ) : error ? (
        <p role="alert">Unable to load batch run history.</p>
      ) : runs.length === 0 ? (
        <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>No batch runs recorded yet.</p>
      ) : (
        <div>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--pcis-space-3)' }}>
              <h2 style={{ fontSize: 'var(--pcis-font-size-xs)', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--pcis-color-text-muted)', margin: 0 }}>
                Job Status
              </h2>
              <Button
                variant="primary"
                size="sm"
                disabled={!isTriggerable || triggerMutation.isPending}
                loading={triggerMutation.isPending}
                onClick={() => selectedJob && triggerMutation.mutate(selectedJob.jobName)}
                title={
                  isTriggerable
                    ? `Run ${selectedJob?.jobName} now`
                    : 'Select a billing job below — the other domains run as one-shot jobs with no live server to trigger'
                }
              >
                Trigger Run
              </Button>
            </div>
            {triggerMutation.data ? (
              <BlueprintCard kicker={`Execution #${triggerMutation.data.jobExecutionId} — ${triggerMutation.data.status}`} elevation="sm" style={{ marginBottom: 'var(--pcis-space-4)' }}>
                <div className="mono" style={{ fontSize: 'var(--pcis-font-size-xs)' }}>
                  <div style={{ fontWeight: 600, marginBottom: 4 }}>Log</div>
                  <pre style={{ margin: 0, whiteSpace: 'pre-wrap', maxHeight: 160, overflowY: 'auto' }}>
                    {triggerMutation.data.logLines.join('\n')}
                  </pre>
                </div>
                {triggerMutation.data.createdRecords.length > 0 ? (
                  <div style={{ marginTop: 'var(--pcis-space-3)' }}>
                    <div style={{ fontSize: 'var(--pcis-font-size-xs)', fontWeight: 600, marginBottom: 4 }}>
                      Records created ({triggerMutation.data.createdRecords.length})
                    </div>
                    <DataTable
                      aria-label="Records created by this run"
                      rows={triggerMutation.data.createdRecords}
                      columns={Object.keys(triggerMutation.data.createdRecords[0]).map((key) => ({
                        id: key,
                        label: key,
                        accessor: (r: Record<string, unknown>) => String(r[key] ?? ''),
                      }))}
                      getRowId={(r) => JSON.stringify(r)}
                      emptyMessage="No records."
                    />
                  </div>
                ) : (
                  <p style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)', marginTop: 'var(--pcis-space-2)' }}>
                    No new records — this run found nothing eligible to write (billing jobs are idempotent, so re-running against the same seed data typically produces zero new rows after the first successful pass).
                  </p>
                )}
              </BlueprintCard>
            ) : null}
            {triggerMutation.isError ? (
              <p role="alert" style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-token-error)', marginTop: -8, marginBottom: 'var(--pcis-space-3)' }}>
                Unable to trigger run.
              </p>
            ) : null}
          <DataTable
            aria-label="Batch jobs"
            rows={latestRuns}
            columns={[
              { id: 'name', label: 'Job', accessor: (r) => r.jobName },
              {
                id: 'legacy',
                label: 'Legacy Program',
                accessor: (r) => jobMeta(r.jobName).legacyProgram,
                render: (r) => <span className="mono">{jobMeta(r.jobName).legacyProgram}</span>,
              },
              { id: 'domain', label: 'Domain', accessor: (r) => r.domain },
              { id: 'schedule', label: 'Schedule', accessor: (r) => jobMeta(r.jobName).schedule },
              { id: 'started', label: 'Started', accessor: (r) => r.startTime, render: (r) => <span className="mono">{formatTimestamp(r.startTime)}</span> },
              {
                id: 'duration',
                label: 'Duration',
                accessor: (r) => r.startTime,
                render: (r) => <span className="mono">{formatDuration(r.startTime, r.endTime)}</span>,
              },
              { id: 'read', label: 'Read', accessor: (r) => r.readCount, render: (r) => <span className="mono">{r.readCount}</span> },
              { id: 'written', label: 'Written', accessor: (r) => r.writeCount, render: (r) => <span className="mono">{r.writeCount}</span> },
              { id: 'errors', label: 'Errors', accessor: (r) => r.skipCount, render: (r) => <span className="mono">{r.skipCount}</span> },
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
                render: (r) =>
                  selectedJob && rowKey(selectedJob) === rowKey(r) ? (
                    <Badge status="Active">Selected</Badge>
                  ) : (
                    <button
                      type="button"
                      style={{
                        background: 'none',
                        border: 'none',
                        color: 'var(--pcis-color-primary-700)',
                        cursor: 'pointer',
                        fontSize: 'var(--pcis-font-size-sm)',
                        textDecoration: 'underline',
                      }}
                      onClick={() => setSelectedKey(rowKey(r))}
                    >
                      Details
                    </button>
                  ),
              },
            ]}
            getRowId={(r) => `${r.domain}-${r.jobExecutionId}`}
            highlightRowId={selectedJob ? rowKey(selectedJob) : undefined}
            emptyMessage="No batch jobs."
          />
          </div>

          {selectedJob ? (
            <div style={{ marginTop: 'var(--pcis-space-6)' }}>
              <JobDetailPanel job={selectedJob} />
            </div>
          ) : null}
        </div>
      )}
    </section>
  )
}
