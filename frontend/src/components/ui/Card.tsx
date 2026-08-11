import type { HTMLAttributes, KeyboardEvent, MouseEvent, ReactNode } from 'react'
import { Skeleton } from './Skeleton'
import styles from './Card.module.css'

export interface CardProps extends HTMLAttributes<HTMLElement> {
  header?: ReactNode
  footer?: ReactNode
  loading?: boolean
  interactive?: boolean
  children?: ReactNode
}

export function Card({
  header,
  footer,
  loading = false,
  interactive = false,
  children,
  className,
  onClick,
  onKeyDown,
  ...rest
}: CardProps) {
  const classes = [styles.card, interactive ? styles.interactive : '', className]
    .filter(Boolean)
    .join(' ')

  const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    onKeyDown?.(event)
    if (!interactive || !onClick) return
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onClick(event as unknown as MouseEvent<HTMLElement>)
    }
  }

  return (
    <article
      className={classes}
      tabIndex={interactive ? 0 : undefined}
      role={interactive ? 'button' : undefined}
      onClick={onClick}
      onKeyDown={handleKeyDown}
      {...rest}
    >
      {header ? <header className={styles.header}>{header}</header> : null}
      <div className={styles.body}>
        {loading ? <Skeleton variant="text" lines={3} /> : children}
      </div>
      {footer && !loading ? <footer className={styles.footer}>{footer}</footer> : null}
    </article>
  )
}
