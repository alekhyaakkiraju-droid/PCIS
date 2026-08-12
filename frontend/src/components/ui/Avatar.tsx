import styles from './Avatar.module.css'

export type AvatarProps = {
  initials: string
  label?: string
  size?: 'sm' | 'md'
}

export function Avatar({ initials, label, size = 'md' }: AvatarProps) {
  return (
    <span
      className={`${styles.avatar} ${styles[size]}`}
      title={label}
      aria-label={label ?? initials}
      role="img"
    >
      {initials.slice(0, 2).toUpperCase()}
    </span>
  )
}
