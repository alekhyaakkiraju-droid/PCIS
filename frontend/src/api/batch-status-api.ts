import { apiClient } from './client'

export type BatchStepRun = {
  stepName: string
  status: string
  readCount: number
  writeCount: number
  skipCount: number
  exitCode: string | null
}

export type BatchJobRun = {
  jobName: string
  domain: string
  jobExecutionId: number
  startTime: string | null
  endTime: string | null
  status: string
  exitCode: string | null
  exitMessage: string | null
  readCount: number
  writeCount: number
  skipCount: number
  steps: BatchStepRun[]
}

export type TriggerRunResult = {
  jobName: string
  jobExecutionId: number
  status: string
  exitCode: string | null
  logLines: string[]
  createdRecords: Record<string, unknown>[]
}

/** Final payload from the live SSE stream — log lines arrive separately, one "log" event per line. */
export type StreamRunResult = {
  jobName: string
  jobExecutionId: number
  status: string
  exitCode: string | null
  createdRecords: Record<string, unknown>[]
}

export interface StreamTriggerHandlers {
  onLog: (line: string) => void
  onResult: (result: StreamRunResult) => void
  onFailed: (message: string) => void
}

function apiBaseUrl(): string {
  const envBase = typeof import.meta !== 'undefined' ? import.meta.env?.VITE_API_BASE_URL : undefined
  return envBase ?? '/api'
}

export const batchStatusApi = {
  async listRuns(): Promise<BatchJobRun[]> {
    return apiClient.get<BatchJobRun[]>('/v1/admin/batch/runs')
  },

  async triggerRun(jobName: string): Promise<TriggerRunResult> {
    return apiClient.post<TriggerRunResult>(`/v1/billing/batch/${encodeURIComponent(jobName)}/run`)
  },

  /**
   * Opens a live Server-Sent Events connection to the job's run so the console can render log
   * lines as the job actually emits them, instead of waiting for the whole run to finish and
   * showing a static block of text. Returns a cleanup function that closes the connection.
   */
  streamTriggerRun(jobName: string, handlers: StreamTriggerHandlers): () => void {
    const url = `${apiBaseUrl()}/v1/billing/batch/${encodeURIComponent(jobName)}/run/stream`
    const source = new EventSource(url, { withCredentials: true })

    source.addEventListener('log', (event) => {
      handlers.onLog((event as MessageEvent).data)
    })
    source.addEventListener('result', (event) => {
      handlers.onResult(JSON.parse((event as MessageEvent).data) as StreamRunResult)
      source.close()
    })
    source.addEventListener('failed', (event) => {
      handlers.onFailed((event as MessageEvent).data)
      source.close()
    })
    source.onerror = () => {
      if (source.readyState === EventSource.CLOSED) return
      handlers.onFailed('Connection to the job stream was lost.')
      source.close()
    }

    return () => source.close()
  },
}
