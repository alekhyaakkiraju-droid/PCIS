import { useState } from 'react'
import {
  Accordion,
  Alert,
  Avatar,
  Badge,
  BlueprintCard,
  Breadcrumbs,
  Button,
  DataTable,
  Dropdown,
  Input,
  Modal,
  MoneyDisplay,
  Select,
  Tabs,
  Tag,
  ThemeToggle,
} from '@/components/ui'
import { designTokens } from '@/components/ui/tokens'
import styles from './DesignSystemPage.module.css'

const CORE_SWATCHES = [
  { name: 'Primary', var: '--pcis-token-primary' },
  { name: 'Secondary', var: '--pcis-token-secondary' },
  { name: 'Accent', var: '--pcis-token-accent' },
  { name: 'Background', var: '--pcis-token-background' },
  { name: 'Surface', var: '--pcis-token-surface' },
  { name: 'Border', var: '--pcis-token-border' },
]

const SEMANTIC_SWATCHES = [
  { name: 'Success', var: '--pcis-token-success' },
  { name: 'Warning', var: '--pcis-token-warning' },
  { name: 'Error', var: '--pcis-token-error' },
  { name: 'Info', var: '--pcis-token-info' },
  { name: 'Text primary', var: '--pcis-token-text-primary' },
  { name: 'Text muted', var: '--pcis-token-text-muted' },
]

const SPACING = Object.entries(designTokens.spacing)

function Section({ id, title, children }: { id: string; title: string; children: React.ReactNode }) {
  return (
    <section id={id} className={styles.section} aria-labelledby={`${id}-heading`}>
      <h2 id={`${id}-heading`} className={styles.sectionTitle}>
        {title}
      </h2>
      {children}
    </section>
  )
}

function SwatchGrid({ swatches }: { swatches: { name: string; var: string }[] }) {
  return (
    <div className={styles.swatchGrid}>
      {swatches.map((sw) => (
        <div key={sw.name} className={styles.swatch}>
          <div className={styles.swatchColor} style={{ background: `var(${sw.var})` }} />
          <span className={styles.swatchLabel}>{sw.name}</span>
        </div>
      ))}
    </div>
  )
}

export function DesignSystemPage() {
  const [modalOpen, setModalOpen] = useState(false)
  const [toggleOn, setToggleOn] = useState(true)

  return (
    <div className={styles.page}>
      <header className={styles.hero}>
        <div>
          <p className={styles.kicker}>PCIS Design System</p>
          <h1 id="design-system-heading">Component library &amp; tokens</h1>
          <p className={styles.lede}>
            IBM Plex Sans / IBM Plex Mono · Carbon-inspired palette · WCAG 2.1 AA target
          </p>
        </div>
        <div className={styles.heroActions}>
          <ThemeToggle />
          <span className={styles.wcagBadge}>WCAG 2.1 AA</span>
        </div>
      </header>

      <Section id="colors" title="Color palette">
        <BlueprintCard kicker="Brand & surfaces" style={{ marginBottom: 'var(--pcis-space-4)' }}>
          <SwatchGrid swatches={CORE_SWATCHES} />
        </BlueprintCard>
        <BlueprintCard kicker="Semantic states">
          <SwatchGrid swatches={SEMANTIC_SWATCHES} />
        </BlueprintCard>
      </Section>

      <Section id="typography" title="Typography">
        <div className={styles.typeGrid}>
          <BlueprintCard kicker="IBM Plex Sans — hierarchy">
            <p style={{ fontSize: 'var(--pcis-font-size-3xl)', fontWeight: 700, margin: 0 }}>3xl 32px — Claim CLM-0004821</p>
            <p style={{ fontSize: 'var(--pcis-font-size-2xl)', fontWeight: 600, margin: '8px 0 0' }}>2xl 24px — Page title</p>
            <p style={{ fontSize: 'var(--pcis-font-size-xl)', fontWeight: 600, margin: '8px 0 0' }}>xl 20px — Section title</p>
            <p style={{ fontSize: 'var(--pcis-font-size-lg)', fontWeight: 500, margin: '8px 0 0' }}>lg 16px — Card heading</p>
            <p style={{ fontSize: 'var(--pcis-font-size-md)', margin: '8px 0 0' }}>md 14px — Body copy and form values</p>
            <p style={{ fontSize: 'var(--pcis-font-size-sm)', margin: '8px 0 0', color: 'var(--pcis-color-text-muted)' }}>
              sm 12px — Dense table rows (5250 subfile style)
            </p>
            <p style={{ fontSize: 'var(--pcis-font-size-xs)', margin: '8px 0 0', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
              xs 11px — Labels & metadata
            </p>
          </BlueprintCard>
          <BlueprintCard kicker="IBM Plex Mono — numeric & identifiers">
            <p className="mono" style={{ fontSize: 'var(--pcis-font-size-md)', margin: 0 }}>
              48,000.00 · CLM-0004821 · POL-000004821
            </p>
            <p style={{ fontSize: 'var(--pcis-font-size-xs)', marginTop: 'var(--pcis-space-3)', color: 'var(--pcis-color-text-muted)' }}>
              {designTokens.typography.usage}
            </p>
          </BlueprintCard>
        </div>
      </Section>

      <Section id="components" title="Components">
        <BlueprintCard kicker="Buttons" style={{ marginBottom: 'var(--pcis-space-4)' }}>
          <div className={styles.row}>
            <Button variant="primary">Submit payment</Button>
            <Button variant="secondary">Secondary</Button>
            <Button variant="ghost">Ghost</Button>
            <Button variant="danger">Deny request</Button>
            <Button variant="primary" disabled>
              Disabled
            </Button>
            <Button variant="primary" loading>
              Reconciling…
            </Button>
          </div>
        </BlueprintCard>

        <BlueprintCard kicker="Form inputs" style={{ marginBottom: 'var(--pcis-space-4)' }}>
          <div className={styles.formGrid}>
            <Input label="Policy number" name="pol" placeholder="POL-000004821" />
            <Select
              label="Billing frequency"
              name="freq"
              options={[
                { value: 'M', label: 'Monthly (M)' },
                { value: 'Q', label: 'Quarterly (Q)' },
              ]}
            />
            <Input
              label="Initial reserve"
              name="reserve"
              defaultValue="-1200.00"
              validationState="error"
              errorMessage="Reserve must be greater than 0.00"
            />
            <Input label="Adjuster (read-only)" name="adj" defaultValue="K. Alvarez" readOnly />
          </div>
          <div className={styles.formExtras}>
            <label className={styles.checkLabel}>
              <input type="checkbox" defaultChecked /> Mandatory coverage
            </label>
            <fieldset className={styles.radioGroup}>
              <legend className="visually-hidden">Decision</legend>
              <label><input type="radio" name="decision" defaultChecked /> Approve</label>
              <label><input type="radio" name="decision" /> Deny</label>
            </fieldset>
            <label className={styles.toggleLabel}>
              <input type="checkbox" role="switch" checked={toggleOn} onChange={(e) => setToggleOn(e.target.checked)} />
              Mask restricted fields in audit
            </label>
          </div>
        </BlueprintCard>

        <BlueprintCard kicker="Badges, tags & avatars" style={{ marginBottom: 'var(--pcis-space-4)' }}>
          <div className={styles.row}>
            <Badge status="Active">FNOL</Badge>
            <Badge status="Approved">APPROVED</Badge>
            <Badge status="Pending">PENDING</Badge>
            <Badge status="Denied">DENIED</Badge>
            <Badge status="Neutral">DRAFT</Badge>
            <Tag>Claims</Tag>
            <Tag variant="accent">Restricted tier</Tag>
            <Avatar initials="AD" label="Adjuster" />
            <Avatar initials="MA" label="Marta Field" />
          </div>
        </BlueprintCard>

        <BlueprintCard kicker="Dense data table" style={{ marginBottom: 'var(--pcis-space-4)' }}>
          <DataTable
            aria-label="Sample claims"
            rows={[
              { claim: 'CLM-0004821', policy: 'POL-0004821', type: 'FIR', status: 'AP', reserve: 48000, paid: 12000 },
              { claim: 'CLM-0004804', policy: 'POL-0007712', type: 'WAT', status: 'PD', reserve: 9250, paid: 0 },
            ]}
            columns={[
              { id: 'claim', label: 'Claim', accessor: (r) => r.claim, render: (r) => <span className="mono">{r.claim}</span> },
              { id: 'policy', label: 'Policy', accessor: (r) => r.policy, render: (r) => <span className="mono">{r.policy}</span> },
              { id: 'type', label: 'Type', accessor: (r) => r.type },
              { id: 'status', label: 'Status', accessor: (r) => r.status, render: (r) => <Badge status="Active">{r.status}</Badge> },
              { id: 'reserve', label: 'Reserve', accessor: (r) => r.reserve, render: (r) => <MoneyDisplay value={r.reserve} className="mono" /> },
              { id: 'paid', label: 'Paid to date', accessor: (r) => r.paid, render: (r) => <MoneyDisplay value={r.paid} className="mono" /> },
            ]}
            getRowId={(r) => r.claim}
            emptyMessage="No rows."
          />
        </BlueprintCard>

        <BlueprintCard kicker="Navigation, tabs & alerts" style={{ marginBottom: 'var(--pcis-space-4)' }}>
          <Breadcrumbs items={[{ label: 'Claims', to: '/claims' }, { label: 'Workspace' }, { label: 'CLM-0004821' }]} />
          <div style={{ marginTop: 'var(--pcis-space-4)' }}>
            <Tabs
              aria-label="Claim workspace"
              items={[
                { id: 'reserves', label: 'Reserves', content: <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>Reserve ledger panel</p> },
                { id: 'payments', label: 'Payments', content: <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>Payment authority panel</p> },
                { id: 'notes', label: 'Notes', content: <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>Case notes panel</p> },
              ]}
              defaultTabId="reserves"
            />
          </div>
          <div className={styles.alertStack}>
            <Alert variant="info">Rating snapshot persisted for audit replay.</Alert>
            <Alert variant="success" title="Parallel run">Reconciliation matched 12,478 of 12,480 rows.</Alert>
            <Alert variant="warning">Payment exceeds reinsurance referral threshold.</Alert>
            <Alert variant="error" title="Authorization denied" role="alert">
              AUTHORITY_LIMIT_EXCEEDED — cumulative payout exceeds adjuster limit.
            </Alert>
          </div>
        </BlueprintCard>

        <BlueprintCard kicker="Dropdown, modal & accordion">
          <div className={styles.row} style={{ marginBottom: 'var(--pcis-space-4)' }}>
            <Dropdown
              label="Actions"
              items={[
                { id: 'export', label: 'Export evidence pack' },
                { id: 'rerun', label: 'Re-run comparison' },
              ]}
            />
            <Button variant="secondary" onClick={() => setModalOpen(true)}>
              Open confirmation modal
            </Button>
          </div>
          <Accordion
            items={[
              {
                id: 'vars',
                title: 'Externalized variables',
                defaultOpen: true,
                content: (
                  <ul className={styles.plainList}>
                    <li><span className="mono">audit.retention.days</span> → 365</li>
                    <li><span className="mono">billing.leadDays</span> → 15</li>
                  </ul>
                ),
              },
              {
                id: 'tiers',
                title: 'Data classification tiers',
                content: <p>Public · Internal · Restricted · PCI</p>,
              },
            ]}
          />
          <Modal open={modalOpen} title="Confirm action" onClose={() => setModalOpen(false)}>
            <p style={{ fontSize: 'var(--pcis-font-size-sm)' }}>Save tunable changes and create a new version?</p>
            <div className={styles.row} style={{ marginTop: 'var(--pcis-space-4)', justifyContent: 'flex-end' }}>
              <Button variant="ghost" onClick={() => setModalOpen(false)}>Cancel</Button>
              <Button variant="primary" onClick={() => setModalOpen(false)}>Save &amp; version</Button>
            </div>
          </Modal>
        </BlueprintCard>
      </Section>

      <Section id="spacing" title="Spacing scale">
        <BlueprintCard>
          <div className={styles.spacingList}>
            {SPACING.map(([key, px]) => (
              <div key={key} className={styles.spacingRow}>
                <div className={styles.spacingBar} style={{ width: px }} />
                <span className="mono">{key}</span>
                <span>{px}</span>
              </div>
            ))}
          </div>
        </BlueprintCard>
      </Section>

      <Section id="elevation" title="Shadows & elevation">
        <div className={styles.shadowGrid}>
          {(['sm', 'md', 'lg', 'xl'] as const).map((size) => (
            <BlueprintCard key={size} elevation={size} style={{ fontSize: 'var(--pcis-font-size-sm)' }}>
              shadow-{size}
            </BlueprintCard>
          ))}
        </div>
      </Section>

      <Section id="radius" title="Border radius">
        <div className={styles.radiusGrid}>
          {Object.entries(designTokens.border_radius).map(([name, value]) => (
            <div key={name} className={styles.radiusSample} style={{ borderRadius: value }}>
              <span>{name}</span>
              <span className="mono">{value}</span>
            </div>
          ))}
        </div>
      </Section>
    </div>
  )
}
