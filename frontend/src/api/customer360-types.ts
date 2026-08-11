import type { Customer } from './customer-api'

export type SectionStatus = 'AVAILABLE' | 'UNAVAILABLE' | 'ERROR'

export type SectionWrapper<T> = {
  status: SectionStatus
  data: T | null
  message: string | null
}

export type Customer360PolicyItem = {
  policyId: string
  policyType: string
  status: string
  premium: number
}

export type Customer360PolicySection = {
  activeCount: number
  items: Customer360PolicyItem[]
}

export type Customer360BillingSection = {
  balanceDue: number
  openInvoiceCount: number
}

export type Customer360ClaimItem = {
  claimId: string
  status: string
  reserveAmount: number
}

export type Customer360ClaimsSection = {
  openClaimCount: number
  items: Customer360ClaimItem[]
}

export type Customer360Response = {
  custId: number
  profile: SectionWrapper<Customer>
  policies: SectionWrapper<Customer360PolicySection>
  billing: SectionWrapper<Customer360BillingSection>
  claims: SectionWrapper<Customer360ClaimsSection>
}

export type AuditEvent = {
  id: string
  timestamp: string
  action: string
  actor: string
  detail: string
}
