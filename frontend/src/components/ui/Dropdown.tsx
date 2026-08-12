import { useEffect, useId, useRef, useState, type ReactNode } from 'react'
import styles from './Dropdown.module.css'

export type DropdownItem = {
  id: string
  label: string
  onSelect?: () => void
  disabled?: boolean
}

export type DropdownProps = {
  label: string
  items: DropdownItem[]
  children?: ReactNode
}

export function Dropdown({ label, items }: DropdownProps) {
  const menuId = useId()
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onDoc = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onDoc)
    return () => document.removeEventListener('mousedown', onDoc)
  }, [open])

  return (
    <div className={styles.root} ref={rootRef}>
      <button
        type="button"
        className={styles.trigger}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        onClick={() => setOpen((v) => !v)}
      >
        {label} ▾
      </button>
      {open ? (
        <ul id={menuId} role="menu" className={styles.menu}>
          {items.map((item) => (
            <li key={item.id} role="none">
              <button
                type="button"
                role="menuitem"
                className={styles.item}
                disabled={item.disabled}
                onClick={() => {
                  item.onSelect?.()
                  setOpen(false)
                }}
              >
                {item.label}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}
