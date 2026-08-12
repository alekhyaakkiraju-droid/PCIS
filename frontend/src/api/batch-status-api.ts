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
  readCount: number
  writeCount: number
  skipCount: number
  steps: BatchStepRun[]
}

export const batchStatusApi = {
  async listRuns(): Promise<BatchJobRun[]> {
    return apiClient.get<BatchJobRun[]>('/v1/admin/batch/runs')
  },
}
