import { apiClient } from './client'
import type { components } from './generated/claims-svc'

export type Claim = components['schemas']['Claim']
export type ClaimDetail = components['schemas']['ClaimDetail']
export type ClaimReserve = components['schemas']['ClaimReserve']
export type ClaimPayment = components['schemas']['ClaimPayment']
export type Approval = components['schemas']['Approval']
export type ClaimPage = components['schemas']['ClaimPage']
export type InitiatePaymentRequest = components['schemas']['InitiatePaymentRequest']

export interface ListClaimsParams {
  status?: 'O' | 'C' | 'D'
  polNbr?: string
  page?: number
  size?: number
}

/**
 * Typed client module for the claims-svc domain.
 * POST /payments is NOT retried on transient errors (non-idempotent mutation).
 */
export const claimsApi = {
  async list(params?: ListClaimsParams): Promise<ClaimPage> {
    const search = new URLSearchParams()
    if (params?.status) search.set('status', params.status)
    if (params?.polNbr) search.set('polNbr', params.polNbr)
    if (params?.page !== undefined) search.set('page', String(params.page))
    if (params?.size !== undefined) search.set('size', String(params.size))
    const qs = search.toString()
    return apiClient.get<ClaimPage>(`/v1/claims${qs ? `?${qs}` : ''}`)
  },

  async getByClaimNbr(claimNbr: string): Promise<ClaimDetail> {
    return apiClient.get<ClaimDetail>(`/v1/claims/${encodeURIComponent(claimNbr)}`)
  },

  async initiatePayment(
    claimNbr: string,
    request: InitiatePaymentRequest,
  ): Promise<ClaimPayment> {
    return apiClient.post<ClaimPayment>(
      `/v1/claims/${encodeURIComponent(claimNbr)}/payments`,
      request,
    )
  },
}
