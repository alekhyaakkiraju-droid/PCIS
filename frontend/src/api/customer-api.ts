import { apiClient } from './client'
import type { Customer360Response } from './customer360-types'
import type { components } from './generated/customer-svc'

export type Customer = components['schemas']['Customer']
export type CustomerAddress = components['schemas']['CustomerAddress']
export type CustomerContact = components['schemas']['CustomerContact']
export type CustomerPage = components['schemas']['CustomerPage']
export type CreateCustomerRequest = components['schemas']['CreateCustomerRequest']

export interface ListCustomersParams {
  q?: string
  page?: number
  size?: number
}

/**
 * Typed client module for the customer-svc domain.
 * All calls go through the shared ApiClient singleton for consistent
 * auth, correlation, retry, and error handling.
 */
export const customerApi = {
  async list(params?: ListCustomersParams): Promise<CustomerPage> {
    const search = new URLSearchParams()
    if (params?.q) search.set('q', params.q)
    if (params?.page !== undefined) search.set('page', String(params.page))
    if (params?.size !== undefined) search.set('size', String(params.size))
    const qs = search.toString()
    return apiClient.get<CustomerPage>(`/v1/customers${qs ? `?${qs}` : ''}`)
  },

  async getById(id: number): Promise<Customer> {
    return apiClient.get<Customer>(`/v1/customers/${id}`)
  },

  async create(data: CreateCustomerRequest): Promise<Customer> {
    return apiClient.post<Customer>('/v1/customers', data)
  },

  async get360(id: number): Promise<Customer360Response> {
    return apiClient.get<Customer360Response>(`/v1/customers/${id}/360`)
  },
}
