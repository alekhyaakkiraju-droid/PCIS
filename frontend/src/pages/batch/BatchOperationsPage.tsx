import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { batchStatusApi, type BatchJobRun, type StreamRunResult } from '@/api/batch-status-api'
import { Badge, BlueprintCard, Button, DataTable } from '@/components/ui'

/**
 * Static per-job metadata — legacy COBOL program ID, configured cadence, and whether a real
 * on-demand trigger endpoint exists. Sourced from each batch module's own JobConfig/application.yaml,
 * not derived from run history (Spring Batch execution tables don't carry this — it's config, not
 * runtime state). `triggerable` jobs live in billing-svc, a persistent server with its Job beans
 * always registered — see BatchTriggerController. The rest are one-shot CLI jars with no running
 * process to send a trigger request to, so they can't be triggered without a bigger infra change.
 */
/** No legacy predecessor — introduced during modernization, not a COBOL conversion. */
const NEW_PROGRAM = 'New'

const JOB_META: Record<string, { legacyProgram: string; schedule: string; triggerable?: boolean }> = {
  auditArchiveJob: { legacyProgram: 'AUD002B', schedule: 'Daily' },
  auditPurgeJob: { legacyProgram: NEW_PROGRAM, schedule: 'Daily' },
  claimPaymentJob: { legacyProgram: 'CLM006B', schedule: 'Daily' },
  policyRenewalJob: { legacyProgram: 'POL006B', schedule: 'Monthly' },
  billingGenerationJob: { legacyProgram: 'BIL003B', schedule: 'Daily', triggerable: true },
  commissionCalculationJob: { legacyProgram: 'CMM001B', schedule: 'Daily', triggerable: true },
  delinquencyAgingJob: { legacyProgram: 'PRM005B', schedule: 'Daily', triggerable: true },
  reconciliationJob: { legacyProgram: NEW_PROGRAM, schedule: 'Daily' },
  domainRollbackJob: { legacyProgram: NEW_PROGRAM, schedule: 'Manual' },
}

function jobMeta(jobName: string) {
  return JOB_META[jobName] ?? { legacyProgram: '—', schedule: '—' }
}

/** Program codes render as mono/code text; "New" (no legacy predecessor) renders as plain text. */
function LegacyProgramLabel({ value, mono }: { value: string; mono?: 'strong' | 'span' }) {
  if (value === NEW_PROGRAM) {
    return <span style={{ color: 'var(--pcis-color-text-muted)', fontStyle: 'italic' }}>{value}</span>
  }
  return mono === 'strong' ? <strong className="mono">{value}</strong> : <span className="mono">{value}</span>
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

function JobDetailPanel({ job, runCount }: { job: BatchJobRun; runCount: number }) {
  const [errorExpanded, setErrorExpanded] = useState(false)
  const meta = jobMeta(job.jobName)

  return (
    <BlueprintCard kicker={`${job.jobName} — run detail (execution #${job.jobExecutionId})`} elevation="md">
      <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>
        Domain <strong>{job.domain}</strong> · Legacy program <LegacyProgramLabel value={meta.legacyProgram} mono="strong" /> ·
        Started {formatTimestamp(job.startTime)} · Duration {formatDuration(job.startTime, job.endTime)} ·
        Runs recorded <strong className="mono">{runCount}</strong>
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
              {`Failed at ${formatTimestamp(job.endTime)}\n\n${job.exitMessage}`}
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

/** Terminal-styled live log — lines are appended as the SSE stream delivers them. */
function TerminalConsole({
  jobName,
  lines,
  running,
}: {
  jobName: string
  lines: string[]
  running: boolean
}) {
  const bodyRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (bodyRef.current) {
      bodyRef.current.scrollTop = bodyRef.current.scrollHeight
    }
  }, [lines])

  return (
    <div style={{ borderRadius: 'var(--pcis-radius-md)', overflow: 'hidden', border: '1px solid #000' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '6px 12px', background: '#2d2d2d' }}>
        <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#ff5f56', display: 'inline-block' }} />
        <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#ffbd2e', display: 'inline-block' }} />
        <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#27c93f', display: 'inline-block' }} />
        <span className="mono" style={{ color: '#ccc', fontSize: 'var(--pcis-font-size-xs)', marginLeft: 8 }}>
          {jobName}
          {running ? ' — running' : ''}
        </span>
      </div>
      <div
        ref={bodyRef}
        className="mono"
        style={{
          background: '#1e1e1e',
          color: '#4ade80',
          fontSize: 'var(--pcis-font-size-xs)',
          padding: 'var(--pcis-space-3)',
          maxHeight: 220,
          overflowY: 'auto',
          whiteSpace: 'pre-wrap',
        }}
      >
        {lines.length === 0 && running ? <div style={{ color: '#888' }}>Starting job…</div> : null}
        {lines.map((line, index) => (
          <div key={index}>{line}</div>
        ))}
        {running ? <span aria-hidden style={{ opacity: 0.7 }}>▋</span> : null}
      </div>
    </div>
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
  // Keyed by jobName (not executionId) so selection survives a fresh trigger — triggering
  // a job creates a new execution id for its "latest run" row; keying by executionId would
  // make the previously-selected row vanish from latestRuns and silently fall back to row 0.
  const rowKey = (r: BatchJobRun) => r.jobName

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
  const isTriggerable = Boolean(selectedJob && jobMeta(selectedJob.jobName).triggerable)

  // Live-console state: which job the console belongs to, lines as they stream in, whether the
  // run is still in flight, and the final result/error once the stream closes.
  const [consoleJobName, setConsoleJobName] = useState<string | null>(null)
  const [consoleLines, setConsoleLines] = useState<string[]>([])
  const [consoleRunning, setConsoleRunning] = useState(false)
  const [consoleResult, setConsoleResult] = useState<StreamRunResult | null>(null)
  const [consoleError, setConsoleError] = useState<string | null>(null)
  const closeStreamRef = useRef<(() => void) | null>(null)

  useEffect(() => () => closeStreamRef.current?.(), [])

  const resetConsole = () => {
    closeStreamRef.current?.()
    closeStreamRef.current = null
    setConsoleJobName(null)
    setConsoleLines([])
    setConsoleRunning(false)
    setConsoleResult(null)
    setConsoleError(null)
  }

  // Selecting a different row must drop any previous run's log/result — otherwise a stale
  // panel from a different job lingers below the newly-selected row, looking irrelevant.
  const selectRow = (key: string) => {
    setSelectedKey(key)
    resetConsole()
  }

  const handleTriggerRun = () => {
    if (!selectedJob) return
    const jobName = selectedJob.jobName
    closeStreamRef.current?.()
    setConsoleJobName(jobName)
    setConsoleLines([])
    setConsoleRunning(true)
    setConsoleResult(null)
    setConsoleError(null)
    closeStreamRef.current = batchStatusApi.streamTriggerRun(jobName, {
      onLog: (line) => setConsoleLines((prev) => [...prev, line]),
      onResult: (result) => {
        setConsoleResult(result)
        setConsoleRunning(false)
        queryClient.invalidateQueries({ queryKey: ['batch-runs'] })
      },
      onFailed: (message) => {
        setConsoleError(message)
        setConsoleRunning(false)
      },
    })
  }

  // Extra guard: only ever render the console underneath the job it actually belongs to.
  const showConsole = consoleJobName !== null && consoleJobName === selectedJob?.jobName

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
                disabled={!isTriggerable || consoleRunning}
                loading={consoleRunning}
                onClick={handleTriggerRun}
                title={
                  isTriggerable
                    ? `Run ${selectedJob?.jobName} now`
                    : 'Select a billing job below — the other domains run as one-shot jobs with no live server to trigger'
                }
              >
                Trigger Run
              </Button>
            </div>
            {showConsole ? (
              <div style={{ marginBottom: 'var(--pcis-space-4)' }}>
                <TerminalConsole jobName={consoleJobName!} lines={consoleLines} running={consoleRunning} />
                {consoleError ? (
                  <p role="alert" style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-token-error)', marginTop: 'var(--pcis-space-2)' }}>
                    {consoleError}
                  </p>
                ) : null}
                {consoleResult ? (
                  <BlueprintCard
                    kicker={`Execution #${consoleResult.jobExecutionId} — ${consoleResult.status}`}
                    elevation="sm"
                    style={{ marginTop: 'var(--pcis-space-3)' }}
                  >
                    {consoleResult.createdRecords.length > 0 ? (
                      <div>
                        <div style={{ fontSize: 'var(--pcis-font-size-xs)', fontWeight: 600, marginBottom: 4 }}>
                          Records created ({consoleResult.createdRecords.length})
                        </div>
                        <DataTable
                          aria-label="Records created by this run"
                          rows={consoleResult.createdRecords}
                          columns={Object.keys(consoleResult.createdRecords[0]).map((key) => ({
                            id: key,
                            label: key,
                            accessor: (r: Record<string, unknown>) => String(r[key] ?? ''),
                          }))}
                          getRowId={(r) => JSON.stringify(r)}
                          emptyMessage="No records."
                        />
                      </div>
                    ) : (
                      <p style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)', margin: 0 }}>
                        No new records — this run found nothing eligible to write (billing jobs are idempotent, so re-running against the same seed data typically produces zero new rows after the first successful pass).
                      </p>
                    )}
                  </BlueprintCard>
                ) : null}
              </div>
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
                render: (r) => <LegacyProgramLabel value={jobMeta(r.jobName).legacyProgram} />,
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
                      onClick={() => selectRow(rowKey(r))}
                    >
                      Details
                    </button>
                  ),
              },
            ]}
            getRowId={rowKey}
            highlightRowId={selectedJob ? rowKey(selectedJob) : undefined}
            emptyMessage="No batch jobs."
          />
          </div>

          {selectedJob ? (
            <div style={{ marginTop: 'var(--pcis-space-6)' }}>
              <JobDetailPanel
                key={selectedJob.jobName}
                job={selectedJob}
                runCount={runs.filter((r) => r.jobName === selectedJob.jobName).length}
              />
            </div>
          ) : null}
        </div>
      )}
    </section>
  )
}
