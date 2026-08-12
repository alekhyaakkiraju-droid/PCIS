import { apiClient } from './client'
import { normalizeClaimNbr } from './claims-fixture-fallback'
import type { components } from './generated/claims-svc'

export type Claim = components['schemas']['Claim']
export type ClaimDetail = components['schemas']['ClaimDetail'] & {
  adjusterId?: string | null
  adjusterName?: string | null
  reserveRemaining?: number | null
  reserveLedger?: ReserveLedgerEntry[]
}
export type ClaimReserve = components['schemas']['ClaimReserve']
export type ClaimPayment = components['schemas']['ClaimPayment']
export type Approval = components['schemas']['Approval']
export type ClaimNote = components['schemas']['ClaimNote']
export type InitiatePaymentRequest = components['schemas']['InitiatePaymentRequest']

export type ClaimListItem = {
  claimNbr: string
  polNbr: string
  custId: number
  lossDate: string
  claimType: string
  claimStatus: string
  reserveRemaining?: number | null
  totalApprovedAmt?: number | null
  totalPaidToDate?: number | null
  adjusterId?: string | null
  adjusterName?: string | null
  pendingApproval?: boolean
}

export type ReserveLedgerEntry = {
  ledgerId: number
  claimNbr: string
  reserveId?: number | null
  eventDate: string
  reason: string
  amount: number
  balanceAfter: number
  actorId: string
  eventType: string
}

export type CreateClaimRequest = {
  claimNbr?: string
  polNbr: string
  custId: number
  lossDate: string
  claimType: string
  description?: string
  initialReserveType?: string
  initialReserveAmt?: number
}

export type CreateReserveRequest = {
  reserveType: string
  approvedAmt: number
  reason?: string
}

export type CreateApprovalRequest = {
  reserveId: number
}

export type UpdateClaimRequest = {
  claimStatus: string
  lossDate?: string
  claimType?: string
}

export interface ListClaimsParams {
  status?: 'O' | 'C' | 'D'
  view?: 'open' | 'pending' | 'closed' | 'escalated'
}

export type PolicySnapshot = {
  policyNumber: string
  customerId: number
  policyType: string
  status: string
  effectiveDate: string
  expirationDate: string
}

function claimPath(claimNbr: string): string {
  return encodeURIComponent(normalizeClaimNbr(claimNbr))
}

export const claimsApi = {
  async list(params?: ListClaimsParams): Promise<ClaimListItem[]> {
    const search = new URLSearchParams()
    if (params?.status) search.set('status', params.status)
    if (params?.view) search.set('view', params.view)
    const qs = search.toString()
    return apiClient.get<ClaimListItem[]>(`/v1/claims${qs ? `?${qs}` : ''}`)
  },

  async getByClaimNbr(claimNbr: string): Promise<ClaimDetail> {
    return apiClient.get<ClaimDetail>(`/v1/claims/${claimPath(claimNbr)}`)
  },

  async create(data: CreateClaimRequest): Promise<Claim> {
    return apiClient.post<Claim>('/v1/claims', data)
  },

  async update(claimNbr: string, data: UpdateClaimRequest, version?: number): Promise<ClaimDetail> {
    const headers: Record<string, string> = {}
    if (version != null) {
      headers['If-Match'] = String(version)
    }
    return apiClient.put<ClaimDetail>(`/v1/claims/${claimPath(claimNbr)}`, data, {
      headers,
    })
  },

  async lookupPolicy(polNbr: string): Promise<PolicySnapshot> {
    return apiClient.get<PolicySnapshot>(`/v1/policies/${encodeURIComponent(polNbr)}`)
  },

  async listReserves(claimNbr: string): Promise<ClaimReserve[]> {
    return apiClient.get<ClaimReserve[]>(`/v1/claims/${claimPath(claimNbr)}/reserves`)
  },

  async createReserve(claimNbr: string, data: CreateReserveRequest): Promise<ClaimReserve> {
    return apiClient.post<ClaimReserve>(`/v1/claims/${claimPath(claimNbr)}/reserves`, data)
  },

  async listPayments(claimNbr: string): Promise<ClaimPayment[]> {
    return apiClient.get<ClaimPayment[]>(`/v1/claims/${claimPath(claimNbr)}/payments`)
  },

  async initiatePayment(claimNbr: string, request: InitiatePaymentRequest): Promise<ClaimPayment> {
    return apiClient.post<ClaimPayment>(`/v1/claims/${claimPath(claimNbr)}/payments`, request)
  },

  async listApprovals(claimNbr: string): Promise<Approval[]> {
    return apiClient.get<Approval[]>(`/v1/claims/${claimPath(claimNbr)}/approvals`)
  },

  async createApproval(claimNbr: string, request: CreateApprovalRequest): Promise<Approval> {
    return apiClient.post<Approval>(`/v1/claims/${claimPath(claimNbr)}/approvals`, request)
  },

  async createNote(claimNbr: string, noteText: string): Promise<ClaimNote> {
    return apiClient.post<ClaimNote>(`/v1/claims/${claimPath(claimNbr)}/notes`, {
      noteText,
    })
  },
}
