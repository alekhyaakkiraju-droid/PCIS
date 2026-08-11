import {
  useId,
  useState,
  type KeyboardEvent,
  type ReactNode,
} from 'react'
import styles from './Tabs.module.css'

export interface TabItem {
  id: string
  label: string
  content: ReactNode
  disabled?: boolean
}

export interface TabsProps {
  items: TabItem[]
  defaultTabId?: string
  'aria-label'?: string
}

export function Tabs({ items, defaultTabId, 'aria-label': ariaLabel = 'Tabs' }: TabsProps) {
  const baseId = useId()
  const initial =
    defaultTabId && items.some((t) => t.id === defaultTabId && !t.disabled)
      ? defaultTabId
      : (items.find((t) => !t.disabled)?.id ?? items[0]?.id ?? '')
  const [activeId, setActiveId] = useState(initial)

  const enabled = items.filter((t) => !t.disabled)

  const onKeyDown = (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (enabled.length === 0) return
    const currentEnabledIndex = enabled.findIndex((t) => t.id === items[index]?.id)
    let nextIndex = currentEnabledIndex

    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault()
      nextIndex = (currentEnabledIndex + 1) % enabled.length
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault()
      nextIndex = (currentEnabledIndex - 1 + enabled.length) % enabled.length
    } else if (event.key === 'Home') {
      event.preventDefault()
      nextIndex = 0
    } else if (event.key === 'End') {
      event.preventDefault()
      nextIndex = enabled.length - 1
    } else {
      return
    }

    const next = enabled[nextIndex]
    if (!next) return
    setActiveId(next.id)
    document.getElementById(`${baseId}-tab-${next.id}`)?.focus()
  }

  return (
    <div className={styles.root}>
      <div role="tablist" aria-label={ariaLabel} className={styles.list}>
        {items.map((tab, index) => {
          const selected = tab.id === activeId
          return (
            <button
              key={tab.id}
              id={`${baseId}-tab-${tab.id}`}
              role="tab"
              type="button"
              className={styles.tab}
              aria-selected={selected}
              aria-controls={`${baseId}-panel-${tab.id}`}
              tabIndex={selected ? 0 : -1}
              disabled={tab.disabled}
              onClick={() => setActiveId(tab.id)}
              onKeyDown={(e) => onKeyDown(e, index)}
            >
              {tab.label}
            </button>
          )
        })}
      </div>
      {items.map((tab) => {
        const selected = tab.id === activeId
        return (
          <div
            key={tab.id}
            id={`${baseId}-panel-${tab.id}`}
            role="tabpanel"
            aria-labelledby={`${baseId}-tab-${tab.id}`}
            hidden={!selected}
            className={styles.panel}
            tabIndex={0}
          >
            {selected ? tab.content : null}
          </div>
        )
      })}
    </div>
  )
}
