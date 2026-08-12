import type { HTMLAttributes } from 'react'
import styles from './Tag.module.css'

export type TagProps = HTMLAttributes<HTMLSpanElement> & {
  variant?: 'neutral' | 'accent'
}

export function Tag({ variant = 'neutral', children, className, ...rest }: TagProps) {
  const classes = [styles.tag, styles[variant], className].filter(Boolean).join(' ')
  return (
    <span className={classes} {...rest}>
      {children}
    </span>
  )
}
