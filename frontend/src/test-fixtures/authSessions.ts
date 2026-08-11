import type { AuthSession } from '../../src/auth/types'
import sessionAdjuster from '../../fixtures/auth/session-adjuster.json'
import sessionCsr from '../../fixtures/auth/session-csr.json'
import sessionSupervisor from '../../fixtures/auth/session-supervisor.json'
import sessionUnderwriter from '../../fixtures/auth/session-underwriter.json'
import sessionFinance from '../../fixtures/auth/session-finance.json'
import sessionCompliance from '../../fixtures/auth/session-compliance.json'
import sessionUnauthenticated from '../../fixtures/auth/session-unauthenticated.json'

export const authFixtures = {
  adjuster: sessionAdjuster as AuthSession,
  csr: sessionCsr as AuthSession,
  supervisor: sessionSupervisor as AuthSession,
  underwriter: sessionUnderwriter as AuthSession,
  finance: sessionFinance as AuthSession,
  compliance: sessionCompliance as AuthSession,
  unauthenticated: sessionUnauthenticated as AuthSession,
}
