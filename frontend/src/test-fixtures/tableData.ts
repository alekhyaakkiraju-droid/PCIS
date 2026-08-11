import customers from '../../fixtures/table-data/customers.json'
import claims from '../../fixtures/table-data/claims.json'
import policies from '../../fixtures/table-data/policies.json'
import billing from '../../fixtures/table-data/billing.json'

export type CustomerFixture = (typeof customers)[number]
export type ClaimFixture = (typeof claims)[number]
export type PolicyFixture = (typeof policies)[number]
export type BillingFixture = (typeof billing)[number]

export const customerFixtures = customers as CustomerFixture[]
export const claimFixtures = claims as ClaimFixture[]
export const policyFixtures = policies as PolicyFixture[]
export const billingFixtures = billing as BillingFixture[]
