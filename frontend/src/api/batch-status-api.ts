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

export const batchStatusApi = {
  async listRuns(): Promise<BatchJobRun[]> {
    return apiClient.get<BatchJobRun[]>('/v1/admin/batch/runs')
  },

  async triggerRun(jobName: string): Promise<TriggerRunResult> {
    return apiClient.post<TriggerRunResult>(`/v1/billing/batch/${encodeURIComponent(jobName)}/run`)
  },
}
