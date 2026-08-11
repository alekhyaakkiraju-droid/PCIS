import { test, expect } from '@playwright/test'

/**
 * E2E auth flow tests against the PCIS SPA.
 *
 * The BFF endpoints (/api/auth/*) are intercepted by Playwright's route API so
 * these tests run without a live Keycloak instance. The OIDC redirect itself is
 * also intercepted: when the browser navigates to the Keycloak authorization
 * URL we capture the redirect_uri and state, then immediately redirect back
 * with a synthetic authorization code — simulating a successful login without
 * contacting the real IdP.
 */

const KEYCLOAK_BASE = 'http://localhost:8180/realms/pcis'
const AUTH_ENDPOINT = `${KEYCLOAK_BASE}/protocol/openid-connect/auth`
const LOGOUT_ENDPOINT = `${KEYCLOAK_BASE}/protocol/openid-connect/logout`

function adjusterSession() {
  return {
    authenticated: true,
    user: {
      sub: 'adjuster-alice',
      name: 'Alice Adjuster',
      email: 'alice.adjuster@pcis.example.com',
      roles: ['CLAIMS_ADJUSTER'],
      authority_limit: 25000,
    },
  }
}

function csrSession() {
  return {
    authenticated: true,
    user: {
      sub: 'csr-carol',
      name: 'Carol CSR',
      email: 'carol.csr@pcis.example.com',
      roles: ['CSR'],
    },
  }
}

test.describe('OIDC PKCE login flow', () => {
  test('unauthenticated access to /claims triggers OIDC redirect', async ({ page }) => {
    // Start unauthenticated — no session cookie
    await page.route('/api/auth/session', (route) =>
      route.fulfill({ status: 401, contentType: 'application/json', body: '{"authenticated":false,"user":null}' }),
    )

    // Intercept the Keycloak authorization redirect and bounce it back with a code
    await page.route(`${AUTH_ENDPOINT}**`, async (route) => {
      const url = new URL(route.request().url())
      const redirectUri = url.searchParams.get('redirect_uri') ?? ''
      const state = url.searchParams.get('state') ?? ''

      const callbackUrl = new URL(redirectUri)
      callbackUrl.searchParams.set('code', 'synthetic-auth-code')
      callbackUrl.searchParams.set('state', state)

      await route.fulfill({ status: 302, headers: { Location: callbackUrl.toString() } })
    })

    // After callback, BFF establishes session and returns adjuster identity
    let callbackCalled = false
    await page.route('/api/auth/callback', (route) => {
      callbackCalled = true
      return route.fulfill({ status: 204 })
    })

    let sessionCallCount = 0
    await page.route('/api/auth/session', (route) => {
      sessionCallCount++
      const body = sessionCallCount > 1 ? adjusterSession() : { authenticated: false, user: null }
      return route.fulfill({
        status: sessionCallCount > 1 ? 200 : 401,
        contentType: 'application/json',
        body: JSON.stringify(body),
      })
    })

    await page.goto('/claims')

    // Wait for the callback to complete and the app to settle on /claims
    await expect(page).toHaveURL(/\/claims/, { timeout: 10_000 })
    expect(callbackCalled).toBe(true)
  })

  test('authenticated adjuster sees Claims in sidebar but not Billing', async ({ page }) => {
    await page.route('/api/auth/session', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(adjusterSession()),
      }),
    )

    await page.goto('/')
    await expect(page.getByRole('navigation', { name: 'Primary' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Claims' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Billing' })).not.toBeVisible()
    await expect(page.getByText('Alice Adjuster')).toBeVisible()
  })

  test('CSR sees Customers and Billing but not Claims', async ({ page }) => {
    await page.route('/api/auth/session', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(csrSession()),
      }),
    )

    await page.goto('/')
    await expect(page.getByRole('link', { name: 'Customers' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Billing' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Claims' })).not.toBeVisible()
  })

  test('accessing /claims as CSR renders 403 Forbidden page', async ({ page }) => {
    await page.route('/api/auth/session', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(csrSession()),
      }),
    )

    await page.goto('/claims')
    await expect(page.getByRole('alert')).toBeVisible()
    await expect(page.getByText(/MENUMD1-91/)).toBeVisible()
    await expect(page.getByText(/not authorized/i)).toBeVisible()
  })

  test('logout clears session and redirects to Keycloak end_session', async ({ page }) => {
    await page.route('/api/auth/session', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(adjusterSession()),
      }),
    )

    let logoutCalled = false
    await page.route('/api/auth/logout', (route) => {
      logoutCalled = true
      return route.fulfill({ status: 204 })
    })

    // Allow the Keycloak end_session redirect without following it
    await page.route(`${LOGOUT_ENDPOINT}**`, (route) =>
      route.fulfill({ status: 200, body: 'logged out' }),
    )

    await page.goto('/')
    await expect(page.getByText('Alice Adjuster')).toBeVisible()

    await page.getByRole('button', { name: 'Sign out' }).click()
    expect(logoutCalled).toBe(true)
  })
})
