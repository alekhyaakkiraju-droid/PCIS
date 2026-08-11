import type { AuthSession } from '../../src/auth/types'
import sessionAdjuster from '../../fixtures/auth/session-adjuster.json'
import sessionCsr from '../../fixtures/auth/session-csr.json'
import sessionSupervisor from '../../fixtures/auth/session-supervisor.json'
import sessionUnauthenticated from '../../fixtures/auth/session-unauthenticated.json'

export const authFixtures = {
  adjuster: sessionAdjuster as AuthSession,
  csr: sessionCsr as AuthSession,
  supervisor: sessionSupervisor as AuthSession,
  unauthenticated: sessionUnauthenticated as AuthSession,
}
