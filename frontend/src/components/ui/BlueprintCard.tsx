import type { HTMLAttributes, ReactNode } from 'react'
import styles from './BlueprintCard.module.css'

export type BlueprintCardElevation = 'sm' | 'md' | 'lg' | 'none'

export interface BlueprintCardProps extends HTMLAttributes<HTMLElement> {
  kicker?: ReactNode
  title?: ReactNode
  meta?: ReactNode
  elevation?: BlueprintCardElevation
  dashed?: boolean
  children?: ReactNode
}

export function BlueprintCard({
  kicker,
  title,
  meta,
  elevation = 'sm',
  dashed = false,
  children,
  className,
  ...rest
}: BlueprintCardProps) {
  const elevClass =
    elevation === 'md'
      ? styles.elevMd
      : elevation === 'lg'
        ? styles.elevLg
        : elevation === 'sm'
          ? styles.elevSm
          : ''

  const classes = [styles.card, elevClass, dashed ? styles.dashed : '', className]
    .filter(Boolean)
    .join(' ')

  return (
    <article className={classes} {...rest}>
      {kicker ? <div className={styles.kicker}>{kicker}</div> : null}
      {title ? <h2 className={styles.title}>{title}</h2> : null}
      {meta ? <div className={styles.meta}>{meta}</div> : null}
      {children ? <div className={styles.body}>{children}</div> : null}
    </article>
  )
}
