import type { HTMLAttributes } from 'react'
import styles from './Badge.module.css'

export type ClaimStatus = 'Open' | 'Approved' | 'Denied' | 'Settled' | 'Closed'
export type PolicyStatus = 'Active' | 'Renewal' | 'Cancelled' | 'Expired'
export type BillingStatus = 'Open' | 'Paid' | 'Overdue' | 'Cancelled'
export type BadgeStatus = ClaimStatus | PolicyStatus | BillingStatus | 'Neutral'

const statusClass: Record<string, string | undefined> = {
  Open: styles.open,
  Approved: styles.approved,
  Denied: styles.denied,
  Settled: styles.settled,
  Closed: styles.closed,
  Active: styles.active,
  Renewal: styles.renewal,
  Cancelled: styles.cancelled,
  Expired: styles.expired,
  Paid: styles.paid,
  Overdue: styles.overdue,
  Neutral: styles.neutral,
}

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  status?: BadgeStatus
  children?: string
}

export function Badge({ status = 'Neutral', children, className, title, ...rest }: BadgeProps) {
  const label = children ?? status
  const classes = [styles.badge, statusClass[status] ?? styles.neutral, className]
    .filter(Boolean)
    .join(' ')

  return (
    <span
      className={classes}
      title={title ?? (label.length > 24 ? label : undefined)}
      {...rest}
    >
      {label}
    </span>
  )
}
