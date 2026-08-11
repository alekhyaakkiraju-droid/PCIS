import agingFixture from '../../fixtures/billing/aging.json'
import installmentsFixture from '../../fixtures/billing/installments.json'
import { apiClient } from './client'

export type Installment = {
  id: string
  policyId: string
  dueDate: string
  amount: number
  status: string
}

export type AgingBucket = {
  bucket: string
  invoiceCount: number
  amountDue: number
}

async function loadInstallments(): Promise<Installment[]> {
  try {
    return await apiClient.get<Installment[]>('/v1/billing/installments')
  } catch {
    return installmentsFixture as Installment[]
  }
}

async function loadAging(): Promise<AgingBucket[]> {
  try {
    return await apiClient.get<AgingBucket[]>('/v1/billing/aging')
  } catch {
    return agingFixture as AgingBucket[]
  }
}

export const billingApi = {
  listInstallments: loadInstallments,
  listAging: loadAging,
}
