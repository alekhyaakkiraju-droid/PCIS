import { apiClient } from './client'

export type Tunable = {
  key: string
  domain: string
  valueType: string
  valueText: string | null
  numericValue: number | null
  minValue: number | null
  maxValue: number | null
  unit: string | null
  description: string | null
  effectiveFrom: string
  effectiveTo: string | null
  version: number
}

export type TunablePage = {
  content: Tunable[]
  totalElements: number
}

export type UpdateTunableRequest = {
  numericValue?: number
  valueText?: string
  effectiveFrom: string
  expectedVersion: number
  changeReason: string
}

export const adminApi = {
  async listTunables(): Promise<TunablePage> {
    return apiClient.get<TunablePage>('/v1/admin/tunables?size=100')
  },

  async updateTunable(key: string, request: UpdateTunableRequest): Promise<Tunable> {
    return apiClient.put<Tunable>(`/v1/admin/tunables/${encodeURIComponent(key)}`, request)
  },
}
