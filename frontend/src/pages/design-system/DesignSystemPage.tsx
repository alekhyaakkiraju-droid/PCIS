import { Button, BlueprintCard, Badge, Input, Select, DataTable, MoneyDisplay } from '@/components/ui'

const NEUTRAL_RAMP = [100, 200, 300, 400, 500, 600, 700, 800, 900].map((step) => ({
  step,
  color: `var(--pcis-color-neutral-${step})`,
}))

const ACCENT_RAMP = [100, 200, 300, 400, 500, 600, 700, 800, 900].map((step) => ({
  step,
  color: `var(--pcis-color-primary-${step})`,
}))

const SPACING_SCALE = [
  { label: 'space-1', px: '4px' },
  { label: 'space-2', px: '8px' },
  { label: 'space-3', px: '12px' },
  { label: 'space-4', px: '16px' },
  { label: 'space-6', px: '24px' },
  { label: 'space-8', px: '32px' },
]

function ColorRamp({ prefix, swatches }: { prefix: 'neutral' | 'accent'; swatches: { step: number; color: string }[] }) {
  const label =
    prefix === 'neutral'
      ? 'Color — mono steel-blue scheme (neutral)'
      : 'Color — mono steel-blue scheme (accent)'
  return (
    <div style={{ marginBottom: 'var(--pcis-space-6)' }}>
      <div
        className="card-kicker"
        style={{
          marginBottom: 8,
          fontSize: 10,
          letterSpacing: '0.1em',
          textTransform: 'uppercase',
          color: 'var(--pcis-color-primary-600)',
        }}
      >
        {label}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(9, 1fr)', gap: 6 }}>
        {swatches.map((sw) => (
          <div key={sw.step}>
            <div style={{ height: 52, background: sw.color, border: '1px solid var(--pcis-color-border)' }} />
            <div style={{ fontSize: 10, marginTop: 4 }}>
              {prefix}-{sw.step}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export function DesignSystemPage() {
  return (
    <section aria-labelledby="design-system-heading" style={{ maxWidth: 1100 }}>
      <h1 id="design-system-heading">Design System</h1>
      <p style={{ color: 'var(--pcis-color-text-muted)', marginBottom: 'var(--pcis-space-6)' }}>
        Color — mono steel-blue scheme · Typography — IBM Plex Sans / IBM Plex Mono
      </p>

      <ColorRamp prefix="neutral" swatches={NEUTRAL_RAMP} />
      <ColorRamp prefix="accent" swatches={ACCENT_RAMP} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--pcis-space-8)', marginBottom: 'var(--pcis-space-8)' }}>
        <div>
          <div className="card-kicker" style={{ marginBottom: 8, fontSize: 10, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--pcis-color-primary-600)' }}>
            Typography — IBM Plex Sans / IBM Plex Mono
          </div>
          <div style={{ fontWeight: 700, fontSize: 32, margin: 0 }}>Aa — 3xl 32px</div>
          <div style={{ fontWeight: 700, fontSize: 24, margin: '4px 0 0' }}>Aa — 2xl 24px</div>
          <div style={{ fontWeight: 600, fontSize: 20, margin: '4px 0 0' }}>Aa — xl 20px</div>
          <div style={{ fontWeight: 600, fontSize: 16, margin: '4px 0 0' }}>Aa — lg 16px</div>
          <p style={{ fontSize: 14, margin: 'var(--pcis-space-2) 0', fontWeight: 400 }}>
            Body md 14px — the quick brown fox settles a claim.
          </p>
          <p style={{ fontSize: 12, margin: 0, opacity: 0.8, fontFamily: 'var(--pcis-font-mono)' }}>
            sm 12px mono — default density for data grids (5250 subfile parity); mono is mandatory for money &amp; identifiers.
          </p>
          <p style={{ fontSize: 11, margin: '4px 0 0', opacity: 0.6 }}>xs 11px</p>
        </div>
        <div>
          <div className="card-kicker" style={{ marginBottom: 8, fontSize: 10, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--pcis-color-primary-600)' }}>
            Spacing &amp; elevation
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {SPACING_SCALE.map((sp) => (
              <div key={sp.label} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div style={{ height: 10, background: 'var(--pcis-color-primary-300)', width: sp.px }} />
                <span style={{ fontSize: 12 }}>{sp.label}</span>
              </div>
            ))}
          </div>
          <div style={{ display: 'flex', gap: 'var(--pcis-space-4)', marginTop: 'var(--pcis-space-4)' }}>
            <BlueprintCard elevation="sm" style={{ fontSize: 12 }}>shadow-sm</BlueprintCard>
            <BlueprintCard elevation="md" style={{ fontSize: 12 }}>shadow-md</BlueprintCard>
            <BlueprintCard elevation="lg" style={{ fontSize: 12 }}>shadow-lg</BlueprintCard>
          </div>
        </div>
      </div>

      <BlueprintCard kicker="Components" style={{ marginBottom: 'var(--pcis-space-6)' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--pcis-space-3)', alignItems: 'center', marginBottom: 'var(--pcis-space-4)' }}>
          <Button variant="primary">Primary</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="primary" disabled>
            Disabled
          </Button>
          <Badge status="Active">Accent tag</Badge>
          <Badge status="Pending">Neutral tag</Badge>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--pcis-space-4)', maxWidth: 640 }}>
          <Input label="Text field" name="demo" placeholder="Placeholder…" />
          <Select
            label="Select"
            name="demoSelect"
            options={[
              { value: 'a', label: 'Option A' },
              { value: 'b', label: 'Option B' },
            ]}
          />
        </div>
      </BlueprintCard>

      <BlueprintCard kicker="Dense data table — mono for money & IDs" style={{ maxWidth: 640 }}>
        <DataTable
          aria-label="Design system sample table"
          rows={[
            { claim: 'CLM-0004821', policy: 'POL-0004821', type: 'FIR', status: 'AP', reserve: 48000 },
            { claim: 'CLM-0004822', policy: 'POL-0007712', type: 'WAT', status: 'PD', reserve: 9250 },
          ]}
          columns={[
            { id: 'claim', label: 'Claim', accessor: (r) => r.claim, render: (r) => <span className="mono">{r.claim}</span> },
            { id: 'policy', label: 'Policy', accessor: (r) => r.policy, render: (r) => <span className="mono">{r.policy}</span> },
            { id: 'type', label: 'Type', accessor: (r) => r.type },
            { id: 'status', label: 'Status', accessor: (r) => r.status, render: (r) => <Badge status="Active">{r.status}</Badge> },
            { id: 'reserve', label: 'Reserve', accessor: (r) => r.reserve, render: (r) => <MoneyDisplay value={r.reserve} className="mono" /> },
          ]}
          getRowId={(r) => r.claim}
          emptyMessage="No rows."
        />
      </BlueprintCard>
    </section>
  )
}
