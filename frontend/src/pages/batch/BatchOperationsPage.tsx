import { useState } from 'react'
import batchFixture from '../../../fixtures/batch/jobs.json'
import { Badge, BlueprintCard, Button, DataTable, Modal } from '@/components/ui'

type BatchJob = (typeof batchFixture.jobs)[number]

const CHUNK_DOTS = Array.from({ length: 20 }, (_, i) => ({
  filled: i < 13,
  failed: i === 13,
}))

const STRUCTURED_LOG = `{"ts":"2026-08-10T03:27:41Z","level":"ERROR","job":"audit-archive-job","chunk":14,"actor":"svc-audit-archive-job","resource":"audit_log","operation":"archive_copy","error":"deadlock detected","retry":3,"action":"chunk rolled back"}
{"ts":"2026-08-10T03:27:42Z","level":"ERROR","job":"audit-archive-job","event":"error_threshold_exceeded","threshold":2,"count":3}`

function statusBadge(status: string): 'Active' | 'Pending' | 'Inactive' {
  if (status === 'Failed') return 'Pending'
  if (status === 'Parallel run') return 'Inactive'
  return 'Active'
}

const DEFAULT_JOB = batchFixture.jobs.find((j) => j.canRestart) ?? batchFixture.jobs[0]

function JobDetailPanel({
  job,
  onRestart,
}: {
  job: BatchJob
  onRestart: () => void
}) {
  return (
    <BlueprintCard kicker={`${job.name} — run detail`} elevation="md">
      <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>{job.detail}</p>
      {'showActor' in job && job.showActor ? (
        <p style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)' }}>{job.actorLine}</p>
      ) : null}
      {job.canRestart ? (
        <>
          <div style={{ display: 'flex', gap: 2, margin: 'var(--pcis-space-3) 0' }}>
            {CHUNK_DOTS.map((dot, i) => (
              <div
                key={i}
                style={{
                  width: 10,
                  height: 10,
                  background: dot.failed
                    ? 'var(--pcis-color-primary-900)'
                    : dot.filled
                      ? 'var(--pcis-color-primary-600)'
                      : 'var(--pcis-color-neutral-300)',
                }}
              />
            ))}
          </div>
          <div style={{ fontSize: 'var(--pcis-font-size-xs)', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 4, marginBottom: 'var(--pcis-space-2)' }}>
            <div>
              Restart point <strong>chunk 14, offset 13,000</strong>
            </div>
            <div>
              Chunk size <strong>1,000 (configurable)</strong>
            </div>
            <div>
              Skip policy <strong>skip 5, retry 3, backoff 2s</strong>
            </div>
            <div>
              Principal <strong>svc-audit-archive-job</strong>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 'var(--pcis-space-2)', flexWrap: 'wrap' }}>
            <Button variant="secondary">Skip &amp; continue</Button>
            <Button variant="primary" onClick={onRestart}>
              Restart from last committed chunk
            </Button>
            <Button variant="ghost">Download structured log</Button>
          </div>
          <div className="mono" style={{ marginTop: 'var(--pcis-space-4)', fontSize: 10, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--pcis-color-primary-600)' }}>
            Structured error log
          </div>
          <pre
            style={{
              background: 'var(--pcis-color-neutral-100)',
              border: '1px solid var(--pcis-color-border)',
              padding: 'var(--pcis-space-3)',
              fontSize: 11,
              fontFamily: 'var(--pcis-font-mono)',
              whiteSpace: 'pre-wrap',
              marginTop: 6,
            }}
          >
            {STRUCTURED_LOG}
          </pre>
        </>
      ) : (
        <p style={{ fontSize: 'var(--pcis-font-size-xs)', color: 'var(--pcis-color-text-muted)', marginTop: 'var(--pcis-space-3)' }}>
          Window {job.window} · exit code {job.exit}
        </p>
      )}
    </BlueprintCard>
  )
}

export function BatchOperationsPage() {
  const [selectedJob, setSelectedJob] = useState<BatchJob>(DEFAULT_JOB)
  const [restartOpen, setRestartOpen] = useState(false)

  return (
    <section aria-labelledby="batch-heading">
      <h1 id="batch-heading">Batch operations</h1>
      <p className="wf-page-lede">
        Nightly window jobs — scheduler health, chunk restart points, and structured error logs for failed runs.
      </p>

      <BlueprintCard kicker={batchFixture.windowLabel.toUpperCase()} style={{ marginBottom: 'var(--pcis-space-4)' }}>
        <p style={{ fontSize: 'var(--pcis-font-size-sm)', margin: 0 }}>
          Scheduler healthy · alerting on · audit-write failure alert armed
        </p>
        <div className="progress-bar">
          <div className="progress-bar__fill" style={{ width: `${batchFixture.windowUsedPct}%` }} />
        </div>
      </BlueprintCard>

      <div className="wf-batch-layout">
        <DataTable
          aria-label="Batch jobs"
          rows={batchFixture.jobs}
          columns={[
            { id: 'name', label: 'Job', accessor: (r) => r.name },
            { id: 'started', label: 'Started', accessor: (r) => r.started, render: (r) => <span className="mono">{r.started}</span> },
            { id: 'duration', label: 'Duration', accessor: (r) => r.duration, render: (r) => <span className="mono">{r.duration}</span> },
            { id: 'read', label: 'Read', accessor: (r) => r.read, render: (r) => <span className="mono">{r.read}</span> },
            { id: 'written', label: 'Written', accessor: (r) => r.written, render: (r) => <span className="mono">{r.written}</span> },
            { id: 'skipped', label: 'Skipped', accessor: (r) => r.skipped, render: (r) => <span className="mono">{r.skipped}</span> },
            {
              id: 'status',
              label: 'Status',
              accessor: (r) => r.status,
              render: (r) => <Badge status={statusBadge(r.status)}>{r.status}</Badge>,
            },
            { id: 'exit', label: 'Exit', accessor: (r) => r.exit, render: (r) => <span className="mono">{r.exit}</span> },
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
                    fontWeight: selectedJob.key === r.key ? 600 : 400,
                  }}
                  onClick={() => setSelectedJob(r)}
                >
                  {selectedJob.key === r.key ? 'Selected' : 'Details →'}
                </button>
              ),
            },
          ]}
          getRowId={(r) => r.key}
          emptyMessage="No batch jobs."
        />

        <JobDetailPanel job={selectedJob} onRestart={() => setRestartOpen(true)} />
      </div>

      <Modal
        open={restartOpen}
        title={`Restart ${selectedJob.name}?`}
        onClose={() => setRestartOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setRestartOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" onClick={() => setRestartOpen(false)}>
              Restart job
            </Button>
          </>
        }
      >
        <p>Resumes from the last committed chunk. No row already archived and verified will be re-processed or duplicated.</p>
      </Modal>
    </section>
  )
}
