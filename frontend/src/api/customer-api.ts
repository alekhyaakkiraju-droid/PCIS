import { apiClient } from './client'
import type { Customer360Response } from './customer360-types'
import type { components } from './generated/customer-svc'

export type Customer = components['schemas']['Customer']
export type CustomerAddress = components['schemas']['CustomerAddress']
export type CustomerContact = components['schemas']['CustomerContact']
export type CreateCustomerRequest = components['schemas']['CreateCustomerRequest']
export type UpdateCustomerRequest = components['schemas']['UpdateCustomerRequest']
export type DuplicateOverrideRequest = components['schemas']['DuplicateOverrideRequest']
export type DuplicateCheckResponse = components['schemas']['DuplicateCheckResponse']

/**
 * Typed client module for the customer-svc domain.
 * All calls go through the shared ApiClient singleton for consistent
 * auth, correlation, retry, and error handling.
 */
export const customerApi = {
  async search(query: string): Promise<Customer[]> {
    const params = new URLSearchParams({ q: query })
    return apiClient.get<Customer[]>(`/v1/customers/search?${params.toString()}`)
  },

  async getById(id: number): Promise<Customer> {
    return apiClient.get<Customer>(`/v1/customers/${id}`)
  },

  async create(data: CreateCustomerRequest): Promise<Customer> {
    return apiClient.post<Customer>('/v1/customers', data)
  },

  async update(id: number, data: UpdateCustomerRequest): Promise<Customer> {
    return apiClient.put<Customer>(`/v1/customers/${id}`, data)
  },

  async duplicateCheck(id: number): Promise<DuplicateCheckResponse> {
    return apiClient.get<DuplicateCheckResponse>(`/v1/customers/${id}/duplicate-check`)
  },

  async createWithDuplicateOverride(data: DuplicateOverrideRequest): Promise<Customer> {
    return apiClient.post<Customer>('/v1/customers/duplicate-overrides', data)
  },

  async get360(id: number): Promise<Customer360Response> {
    return apiClient.get<Customer360Response>(`/v1/customers/${id}/360`)
  },
}
