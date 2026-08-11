import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import styles from './Toast.module.css'

export type ToastTone = 'success' | 'error' | 'warning' | 'info'

export interface ToastMessage {
  id: string
  message: string
  tone?: ToastTone
  durationMs?: number
}

interface ToastContextValue {
  push: (toast: Omit<ToastMessage, 'id'> & { id?: string }) => string
  dismiss: (id: string) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

let toastSeq = 0

export function ToastProvider({
  children,
  defaultDurationMs = 5000,
}: {
  children: ReactNode
  defaultDurationMs?: number
}) {
  const [toasts, setToasts] = useState<ToastMessage[]>([])

  const dismiss = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const push = useCallback(
    (toast: Omit<ToastMessage, 'id'> & { id?: string }) => {
      const id = toast.id ?? `toast-${++toastSeq}`
      setToasts((prev) => [
        ...prev,
        {
          id,
          message: toast.message,
          tone: toast.tone ?? 'info',
          durationMs: toast.durationMs ?? defaultDurationMs,
        },
      ])
      return id
    },
    [defaultDurationMs],
  )

  const value = useMemo(() => ({ push, dismiss }), [push, dismiss])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className={styles.region} aria-live="polite" aria-relevant="additions text">
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} onDismiss={dismiss} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}

function ToastItem({
  toast,
  onDismiss,
}: {
  toast: ToastMessage
  onDismiss: (id: string) => void
}) {
  useEffect(() => {
    if (!toast.durationMs || toast.durationMs <= 0) return
    const timer = window.setTimeout(() => onDismiss(toast.id), toast.durationMs)
    return () => window.clearTimeout(timer)
  }, [toast, onDismiss])

  return (
    <div
      role="status"
      className={[styles.toast, styles[toast.tone ?? 'info']].join(' ')}
    >
      <span>{toast.message}</span>
      <button
        type="button"
        className={styles.dismiss}
        aria-label="Dismiss notification"
        onClick={() => onDismiss(toast.id)}
      >
        ×
      </button>
    </div>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return ctx
}
