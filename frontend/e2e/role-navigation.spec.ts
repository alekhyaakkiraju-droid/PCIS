import { test, expect } from '@playwright/test'

const ALL_NAV = [
  'Dashboard',
  'Design System',
  'FNOL Intake',
  'Claim Inquiry',
  'Payment & Authority',
  'Customer 360',
  'Policy Issuance',
  'Billing Reconciliation',
  'Batch Operations',
  'Admin & Compliance',
]

type RoleCase = {
  name: string
  session: object
  allowed: Record<string, boolean>
}

const roles: RoleCase[] = [
  {
    name: 'Claims Adjuster',
    session: {
      authenticated: true,
      user: {
        sub: 'adjuster-alice',
        name: 'Alice Adjuster',
        email: 'alice@pcis.example.com',
        roles: ['CLAIMS_ADJUSTER'],
        authority_limit: 25000,
      },
    },
    allowed: {
      '/': true,
      '/design-system': true,
      '/claims/fnol': true,
      '/claims': true,
      '/claims/payments': true,
      '/customers': false,
      '/policies': false,
      '/billing': false,
      '/batch': false,
      '/admin': false,
    },
  },
  {
    name: 'Claims Supervisor',
    session: {
      authenticated: true,
      user: {
        sub: 'supervisor-mike',
        name: 'Mike Supervisor',
        email: 'mike@pcis.example.com',
        roles: ['CLAIMS_SUPERVISOR'],
      },
    },
    allowed: {
      '/': true,
      '/design-system': true,
      '/claims/fnol': true,
      '/claims': true,
      '/claims/payments': true,
      '/customers': true,
      '/policies': true,
      '/billing': true,
      '/batch': true,
      '/admin': false,
    },
  },
  {
    name: 'CSR',
    session: {
      authenticated: true,
      user: {
        sub: 'csr-carol',
        name: 'Carol CSR',
        email: 'carol@pcis.example.com',
        roles: ['CSR'],
      },
    },
    allowed: {
      '/': true,
      '/design-system': true,
      '/claims/fnol': false,
      '/claims': false,
      '/claims/payments': false,
      '/customers': true,
      '/policies': false,
      '/billing': false,
      '/batch': false,
      '/admin': false,
    },
  },
  {
    name: 'Underwriter',
    session: {
      authenticated: true,
      user: {
        sub: 'uw-dana',
        name: 'Dana Underwriter',
        email: 'dana@pcis.example.com',
        roles: ['UNDERWRITER'],
      },
    },
    allowed: {
      '/': true,
      '/design-system': true,
      '/claims/fnol': false,
      '/claims': false,
      '/claims/payments': false,
      '/customers': false,
      '/policies': true,
      '/billing': false,
      '/batch': false,
      '/admin': false,
    },
  },
  {
    name: 'Compliance',
    session: {
      authenticated: true,
      user: {
        sub: 'compliance-sam',
        name: 'Sam Compliance',
        email: 'sam@pcis.example.com',
        roles: ['COMPLIANCE'],
      },
    },
    allowed: {
      '/': true,
      '/design-system': true,
      '/claims/fnol': false,
      '/claims': false,
      '/claims/payments': false,
      '/customers': false,
      '/policies': false,
      '/billing': true,
      '/batch': true,
      '/admin': true,
    },
  },
]

async function mockSession(page: import('@playwright/test').Page, session: object) {
  await page.route('/api/auth/session', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(session),
    }),
  )
}

test.describe('Wireframe role navigation matrix', () => {
  for (const roleCase of roles) {
    test(`${roleCase.name} sees full sidebar`, async ({ page }) => {
      await mockSession(page, roleCase.session)
      await page.goto('/')
      for (const label of ALL_NAV) {
        await expect(page.getByRole('link', { name: label, exact: true })).toBeVisible()
      }
    })

    for (const [path, allowed] of Object.entries(roleCase.allowed)) {
      test(`${roleCase.name} ${allowed ? 'can access' : 'denied on'} ${path}`, async ({ page }) => {
        await mockSession(page, roleCase.session)
        await page.goto(path)
        await expect(page.getByRole('navigation', { name: 'Primary' })).toBeVisible()
        if (allowed) {
          await expect(page.getByRole('alert')).not.toBeVisible()
        } else {
          await expect(page.getByText(/403 — Access denied/)).toBeVisible()
        }
      })
    }
  }
})

test.describe('Wireframe interactive flows', () => {
  test('adjuster payment authority check and audit rollback', async ({ page }) => {
    await mockSession(page, roles[0].session)
    await page.goto('/claims/payments')
    await page.getByRole('button', { name: 'Submit payment request' }).click()
    await expect(page.getByText(/Denied —/)).toBeVisible()
    await page.getByRole('button', { name: /Simulate audit-outbox failure/ }).click()
    await expect(page.getByText(/ROLLED BACK/)).toBeVisible()
  })

  test('supervisor can open customer 360 Marta Field', async ({ page }) => {
    await mockSession(page, roles[1].session)
    await page.goto('/customers/19284')
    await expect(page.getByRole('heading', { name: 'Marta Field' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Request unmask →' })).toBeVisible()
  })

  test('compliance billing sign-off', async ({ page }) => {
    await mockSession(page, roles[4].session)
    await page.goto('/billing')
    await page.getByRole('button', { name: 'Approve cutover gate' }).click()
    await expect(page.getByRole('button', { name: 'Signed off ✓' })).toBeVisible()
  })

  test('FNOL registers claim on valid loss date', async ({ page }) => {
    await mockSession(page, roles[0].session)
    await page.goto('/claims/fnol')
    await page.getByLabel('Loss narrative (first case note)').fill('Pipe burst in kitchen ceiling.')
    await page.getByRole('button', { name: 'Register claim' }).click()
    await expect(page.getByText(/registered — reserve/)).toBeVisible()
  })
})
