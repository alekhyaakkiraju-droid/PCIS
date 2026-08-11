import type { ButtonHTMLAttributes, ReactNode } from 'react'
import styles from './Button.module.css'

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'ghost'
export type ButtonSize = 'sm' | 'md' | 'lg'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  loading?: boolean
  children: ReactNode
}

export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  children,
  className,
  type = 'button',
  title,
  ...rest
}: ButtonProps) {
  const isDisabled = disabled || loading
  const classes = [
    styles.button,
    styles[variant],
    styles[size],
    className,
  ]
    .filter(Boolean)
    .join(' ')

  const label = typeof children === 'string' ? children : undefined

  return (
    <button
      type={type}
      className={classes}
      disabled={isDisabled}
      aria-busy={loading || undefined}
      aria-disabled={isDisabled || undefined}
      title={title ?? (label && label.length > 32 ? label : undefined)}
      {...rest}
    >
      {loading ? <span className={styles.spinner} aria-hidden="true" /> : null}
      <span>{children}</span>
    </button>
  )
}
