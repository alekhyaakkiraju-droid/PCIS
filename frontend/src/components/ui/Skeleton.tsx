import type { CSSProperties, HTMLAttributes } from 'react'
import styles from './Skeleton.module.css'

export type SkeletonVariant = 'rectangle' | 'circle' | 'text'

export interface SkeletonProps extends HTMLAttributes<HTMLDivElement> {
  variant?: SkeletonVariant
  width?: string | number
  height?: string | number
  lines?: number
}

export function Skeleton({
  variant = 'rectangle',
  width,
  height,
  lines = 1,
  className,
  style,
  ...rest
}: SkeletonProps) {
  const dimStyle: CSSProperties = {
    ...style,
    ...(width !== undefined ? { inlineSize: width } : null),
    ...(height !== undefined ? { blockSize: height } : null),
  }

  if (variant === 'text') {
    return (
      <div
        className={className}
        role="status"
        aria-busy="true"
        aria-label="Loading"
        {...rest}
      >
        {Array.from({ length: lines }, (_, i) => (
          <span key={i} className={`${styles.skeleton} ${styles.textLine}`} aria-hidden="true" />
        ))}
      </div>
    )
  }

  const shape = variant === 'circle' ? styles.circle : styles.rectangle

  return (
    <div
      className={[styles.skeleton, shape, className].filter(Boolean).join(' ')}
      style={dimStyle}
      role="status"
      aria-busy="true"
      aria-label="Loading"
      {...rest}
    />
  )
}
