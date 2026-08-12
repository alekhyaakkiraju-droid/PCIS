import { useId, useState, type ReactNode } from 'react'
import styles from './Accordion.module.css'

export type AccordionItem = {
  id: string
  title: string
  content: ReactNode
  defaultOpen?: boolean
}

export type AccordionProps = {
  items: AccordionItem[]
}

export function Accordion({ items }: AccordionProps) {
  const baseId = useId()
  const [openIds, setOpenIds] = useState<Set<string>>(
    () => new Set(items.filter((i) => i.defaultOpen).map((i) => i.id)),
  )

  const toggle = (id: string) => {
    setOpenIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return (
    <div className={styles.root}>
      {items.map((item) => {
        const open = openIds.has(item.id)
        const panelId = `${baseId}-panel-${item.id}`
        const headerId = `${baseId}-header-${item.id}`
        return (
          <div key={item.id} className={styles.item}>
            <h3 className={styles.heading}>
              <button
                type="button"
                id={headerId}
                className={styles.trigger}
                aria-expanded={open}
                aria-controls={panelId}
                onClick={() => toggle(item.id)}
              >
                <span>{item.title}</span>
                <span className={styles.chevron} aria-hidden="true">
                  {open ? '▾' : '▸'}
                </span>
              </button>
            </h3>
            <div
              id={panelId}
              role="region"
              aria-labelledby={headerId}
              hidden={!open}
              className={styles.panel}
            >
              {item.content}
            </div>
          </div>
        )
      })}
    </div>
  )
}
