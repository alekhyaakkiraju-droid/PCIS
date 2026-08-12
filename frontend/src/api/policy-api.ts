import { apiClient } from './client'

export type PolicyCoverage = {
  coverageId: string
  coverageType: string
  coverageLimit: number
  deductibleAmount: number
  premiumAmount: number
}

export type PolicyBillingPlan = {
  billingFrequency: string
  installmentCount: number
  installmentFee: number
}

export type PolicyHistory = {
  eventCode: string
  eventDate: string
  eventDescription: string
}

export type Policy = {
  policyNumber: string
  customerId: number
  agentId: string
  policyType: string
  status: string
  effectiveDate: string
  expirationDate: string
  annualPremium: number
  coverages: PolicyCoverage[]
  billingPlan: PolicyBillingPlan
  history: PolicyHistory[]
  createdAt: string
  updatedAt: string
}

export type PolicyPage = {
  content: Policy[]
  page: {
    number: number
    size: number
    totalElements: number
    totalPages: number
  }
}

export type PolicyEndorseRequest = {
  endorsementType: string
  effectiveDate: string
  coverageChanges: Array<{
    coverageType: string
    coverageLimit: number
    deductibleAmount: number
    premiumAmount: number
  }>
  reason: string
}

export type PolicyCancelRequest = {
  cancellationDate: string
  reason: string
}

export type PolicyCreateRequest = {
  customerId: number
  agentId: string
  policyType: string
  annualPremium: number
  effectiveDate: string
  expirationDate: string
  coverages: Array<{
    coverageType: string
    coverageLimit: number
    premiumAmount: number
  }>
  billingPlan: {
    billingFrequency: string
    installmentCount: number
  }
}

export interface ListPoliciesParams {
  customerId?: number
  status?: string
  page?: number
  size?: number
}

export const policyApi = {
  async create(request: PolicyCreateRequest): Promise<Policy> {
    return apiClient.post<Policy>('/v1/policies', request)
  },

  async list(params?: ListPoliciesParams): Promise<PolicyPage> {
    const search = new URLSearchParams()
    if (params?.customerId !== undefined) search.set('customerId', String(params.customerId))
    if (params?.status) search.set('status', params.status)
    if (params?.page !== undefined) search.set('page', String(params.page))
    if (params?.size !== undefined) search.set('size', String(params.size))
    const qs = search.toString()
    return apiClient.get<PolicyPage>(`/v1/policies${qs ? `?${qs}` : ''}`)
  },

  async getByNumber(policyNumber: string): Promise<Policy> {
    return apiClient.get<Policy>(`/v1/policies/${encodeURIComponent(policyNumber)}`)
  },

  async endorse(policyNumber: string, request: PolicyEndorseRequest): Promise<Policy> {
    return apiClient.put<Policy>(
      `/v1/policies/${encodeURIComponent(policyNumber)}/endorse`,
      request,
    )
  },

  async cancel(policyNumber: string, request: PolicyCancelRequest): Promise<Policy> {
    return apiClient.post<Policy>(
      `/v1/policies/${encodeURIComponent(policyNumber)}/cancel`,
      request,
    )
  },
}
