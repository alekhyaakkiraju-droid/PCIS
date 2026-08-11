import { test, expect } from '@playwright/test'

/**
 * Optional Playwright smoke for key routes.
 * Full axe coverage runs in vitest via `npm run test:a11y`.
 */
test.describe('route smoke', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('/api/auth/session', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: true,
          user: {
            sub: 'csr-carol',
            name: 'Carol CSR',
            email: 'carol.csr@pcis.example.com',
            roles: ['CSR', 'FINANCE'],
          },
        }),
      }),
    )
  })

  test('billing dashboard renders', async ({ page }) => {
    await page.route('/api/auth/session', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: true,
          user: {
            sub: 'compliance-sam',
            name: 'Sam Compliance',
            email: 'sam@pcis.example.com',
            roles: ['COMPLIANCE'],
          },
        }),
      }),
    )
    await page.goto('/billing')
    await expect(page.getByText('Rows compared')).toBeVisible()
  })

  test('FNOL wizard renders', async ({ page }) => {
    await page.route('/api/auth/session', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          authenticated: true,
          user: {
            sub: 'adjuster-alice',
            name: 'Alice Adjuster',
            email: 'alice.adjuster@pcis.example.com',
            roles: ['CLAIMS_ADJUSTER'],
          },
        }),
      }),
    )
    await page.goto('/claims/fnol')
    await expect(page.getByRole('button', { name: 'Register claim' })).toBeVisible()
  })
})
