import { Link, useLocation } from 'react-router'
import { useCapabilities } from '@/auth/useCapabilities'
import { MENUMD1_ERROR_91 } from '@/auth/errors'
import { formatRoleList } from '@/demo/demo-role'
import { BlueprintCard, Button } from '@/components/ui'

export function ForbiddenPage() {
  const { pathname } = useLocation()
  const { roles, requiredRolesForPath } = useCapabilities()

  const roleLabel =
    roles[0]?.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()) ?? 'Guest'
  const required = requiredRolesForPath(pathname)
  const deniedPermLabel = required ? formatRoleList(required) : 'any authenticated role'

  return (
    <section aria-labelledby="forbidden-heading" role="alert">
      <BlueprintCard style={{ padding: 'var(--pcis-space-6)', maxWidth: 560 }}>
        <h1
          id="forbidden-heading"
          style={{
            fontSize: '1.375rem',
            fontWeight: 600,
            color: 'var(--pcis-color-primary-800)',
            margin: 0,
          }}
        >
          403 — Access denied
        </h1>
        <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginTop: 'var(--pcis-space-2)' }}>
          Role <strong>{roleLabel}</strong> has no permission for this screen. Required:{' '}
          <strong>{deniedPermLabel}</strong>.
        </p>
        <p
          className="mono"
          style={{
            fontSize: 'var(--pcis-font-size-xs)',
            marginTop: 'var(--pcis-space-3)',
            color: 'var(--pcis-color-text-muted)',
          }}
        >
          reason_code: NO_PERMISSION_GRANT · deny-by-default (BR-09) · attempt logged ·{' '}
          {MENUMD1_ERROR_91.code}
        </p>
        <p style={{ fontSize: 'var(--pcis-font-size-sm)', marginTop: 'var(--pcis-space-3)' }}>
          {MENUMD1_ERROR_91.message}
        </p>
        <Link to="/" style={{ display: 'inline-block', marginTop: 'var(--pcis-space-4)' }}>
          <Button variant="secondary">Back to dashboard</Button>
        </Link>
      </BlueprintCard>
    </section>
  )
}
