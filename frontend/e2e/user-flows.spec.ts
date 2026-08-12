import { test, expect } from '@playwright/test'

const adjusterSession = {
  authenticated: true,
  user: {
    sub: 'adjuster-alice',
    name: 'Alice Adjuster',
    email: 'alice.adjuster@pcis.example.com',
    roles: ['CLAIMS_ADJUSTER'],
    authority_limit: 25000,
  },
}

const complianceSession = {
  authenticated: true,
  user: {
    sub: 'compliance-sam',
    name: 'Sam Compliance',
    email: 'sam.compliance@pcis.example.com',
    roles: ['COMPLIANCE'],
  },
}

async function mockAdjuster(page: import('@playwright/test').Page) {
  await page.route('/api/auth/session', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(adjusterSession),
    }),
  )
}

async function mockCompliance(page: import('@playwright/test').Page) {
  await page.route('/api/auth/session', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(complianceSession),
    }),
  )
}

test.describe('Wireframe user flows', () => {
  test('dashboard → FNOL → claim inquiry navigation', async ({ page }) => {
    await mockAdjuster(page)
    await page.goto('/')
    await expect(page.getByRole('heading', { name: /Good morning/i })).toBeVisible()
    await page.getByRole('link', { name: 'FNOL Intake', exact: true }).click()
    await expect(page.getByText('Policy lookup')).toBeVisible()
    await page.getByRole('link', { name: 'Claim Inquiry', exact: true }).click()
    await expect(page.getByRole('tab', { name: 'Open' })).toBeVisible()
  })

  test('FNOL form validates and shows register action', async ({ page }) => {
    await mockAdjuster(page)
    await page.goto('/claims/fnol')
    await expect(page.getByRole('button', { name: 'Register claim' })).toBeVisible()
    await expect(page.getByLabel('Policy number')).toHaveValue('POL000003001')
    await page.getByLabel('Loss narrative (first case note)').fill('Pipe burst in kitchen.')
    await expect(page.getByText('In force — Homeowners HO-3')).toBeVisible()
  })

  test('payment workspace shows authority panel tabs', async ({ page }) => {
    await mockAdjuster(page)
    await page.goto('/claims/payments')
    await expect(page.getByText('Reserve remaining')).toBeVisible()
    await expect(page.getByRole('tab', { name: 'Payments' })).toBeVisible()
    await expect(page.getByRole('tab', { name: 'Approvals' })).toBeVisible()
  })

  test('billing reconciliation sign-off flow', async ({ page }) => {
    await mockCompliance(page)
    await page.goto('/billing')
    await expect(page.getByText('Rows compared')).toBeVisible()
    await page.getByRole('button', { name: 'Approve cutover gate' }).click()
    await expect(page.getByRole('button', { name: 'Signed off ✓' })).toBeDisabled()
  })

  test('batch operations job detail drawer', async ({ page }) => {
    await mockCompliance(page)
    await page.goto('/batch')
    await page.getByRole('button', { name: 'Details →' }).first().click()
    await expect(page.getByText(/run detail/i)).toBeVisible()
  })

  test('admin tunables edit panel opens', async ({ page }) => {
    await mockCompliance(page)
    await page.goto('/admin')
    await page.getByRole('button', { name: 'Edit →' }).first().click()
    await expect(page.getByLabel('New value')).toBeVisible()
  })
})
