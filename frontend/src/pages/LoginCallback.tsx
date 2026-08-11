import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { consumeReturnUrl, useAuth } from '../auth/AuthContext'
import { establishSession } from '../auth/session-api'

export function LoginCallback() {
  const { refreshSession } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function completeLogin() {
      try {
        const code = searchParams.get('code')
        const state = searchParams.get('state')

        if (!code) {
          throw new Error('Missing authorization code')
        }

        await establishSession(code, state ?? '')
        await refreshSession()

        const returnUrl = state && state.startsWith('/') ? state : consumeReturnUrl()
        navigate(returnUrl, { replace: true })
      } catch (cause) {
        setError(cause instanceof Error ? cause.message : 'Sign-in failed')
      }
    }

    void completeLogin()
  }, [navigate, refreshSession, searchParams])

  if (error) {
    return (
      <section role="alert">
        <h1>Sign-in failed</h1>
        <p>{error}</p>
      </section>
    )
  }

  return <p role="status">Completing sign-in…</p>
}
