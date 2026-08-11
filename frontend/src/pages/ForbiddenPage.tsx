import { MENUMD1_ERROR_91 } from '../auth/errors'

export function ForbiddenPage() {
  return (
    <section aria-labelledby="forbidden-heading" role="alert">
      <h1 id="forbidden-heading">Access denied</h1>
      <p>
        <strong>{MENUMD1_ERROR_91.code}</strong>: {MENUMD1_ERROR_91.message}
      </p>
      <p>
        Contact your supervisor if you believe you should have access to this menu option.
      </p>
    </section>
  )
}
