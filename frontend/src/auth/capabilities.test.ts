import { describe, expect, it } from 'vitest'
import {
  canAccessNavItem,
  hasAnyRole,
  hasPermission,
  permissionsForRoles,
} from './capabilities'
import { isRouteAllowedForRoles } from '@/layout/nav-config'

describe('capabilities', () => {
  it('grants claims write to adjusters only', () => {
    expect(hasPermission(['CLAIMS_ADJUSTER'], 'claims:write')).toBe(true)
    expect(hasPermission(['CLAIMS_ADJUSTER'], 'claims:approve')).toBe(false)
    expect(hasPermission(['CSR'], 'claims:write')).toBe(false)
  })

  it('grants claims approve to supervisors', () => {
    expect(hasPermission(['CLAIMS_SUPERVISOR'], 'claims:approve')).toBe(true)
  })

  it('grants billing to finance and compliance', () => {
    expect(hasPermission(['FINANCE'], 'billing:write')).toBe(true)
    expect(hasPermission(['FINANCE'], 'batch:read')).toBe(false)
    expect(hasPermission(['COMPLIANCE'], 'billing:read')).toBe(true)
  })

  it('grants batch to batch service account', () => {
    expect(hasPermission(['BATCH_SVC'], 'batch:write')).toBe(true)
    expect(hasPermission(['BATCH_SVC'], 'billing:read')).toBe(false)
  })

  it('unions permissions across multiple roles', () => {
    const perms = permissionsForRoles(['CLAIMS_ADJUSTER', 'CSR'])
    expect(perms.has('claims:write')).toBe(true)
    expect(perms.has('customer:write')).toBe(true)
  })
})

describe('route matrix', () => {
  it('allows CSR on customer 360 only', () => {
    expect(isRouteAllowedForRoles('/customers', ['CSR'])).toBe(true)
    expect(isRouteAllowedForRoles('/claims', ['CSR'])).toBe(false)
  })

  it('allows finance on billing but not batch or admin', () => {
    expect(isRouteAllowedForRoles('/billing', ['FINANCE'])).toBe(true)
    expect(isRouteAllowedForRoles('/batch', ['FINANCE'])).toBe(false)
    expect(isRouteAllowedForRoles('/admin', ['FINANCE'])).toBe(false)
  })

  it('allows batch operator on batch only', () => {
    expect(isRouteAllowedForRoles('/batch', ['BATCH_SVC'])).toBe(true)
    expect(isRouteAllowedForRoles('/billing', ['BATCH_SVC'])).toBe(false)
  })

  it('allows supervisor on all operational screens except admin', () => {
    const paths = ['/claims', '/customers', '/policies', '/billing', '/batch']
    for (const path of paths) {
      expect(isRouteAllowedForRoles(path, ['CLAIMS_SUPERVISOR'])).toBe(true)
    }
    expect(isRouteAllowedForRoles('/admin', ['CLAIMS_SUPERVISOR'])).toBe(false)
  })

  it('matches nav item access to route access', () => {
    const billing = { to: '/billing', label: 'Billing', menuOption: 'BIL', roles: ['COMPLIANCE', 'CLAIMS_SUPERVISOR', 'FINANCE'] as const }
    expect(canAccessNavItem(['FINANCE'], billing)).toBe(true)
    expect(hasAnyRole(['UNDERWRITER'], billing.roles)).toBe(false)
  })
})
