import type { ReactNode } from 'react'
import styles from './Alert.module.css'

export type AlertVariant = 'info' | 'success' | 'warning' | 'error'

export type AlertProps = {
  variant?: AlertVariant
  title?: string
  children: ReactNode
  role?: 'alert' | 'status'
}

const icons: Record<AlertVariant, string> = {
  info: 'ℹ',
  success: '✓',
  warning: '⚠',
  error: '✕',
}

export function Alert({ variant = 'info', title, children, role = 'status' }: AlertProps) {
  return (
    <div className={`${styles.alert} ${styles[variant]}`} role={role}>
      <span className={styles.icon} aria-hidden="true">
        {icons[variant]}
      </span>
      <div className={styles.body}>
        {title ? <strong className={styles.title}>{title}</strong> : null}
        <div>{children}</div>
      </div>
    </div>
  )
}
