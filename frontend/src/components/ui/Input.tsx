import { useId, type InputHTMLAttributes } from 'react'
import styles from './Field.module.css'

export type InputValidationState = 'default' | 'error' | 'success'

export interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  label: string
  hint?: string
  errorMessage?: string
  validationState?: InputValidationState
}

export function Input({
  label,
  hint,
  errorMessage,
  validationState = 'default',
  id,
  required,
  className,
  type = 'text',
  ...rest
}: InputProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const hintId = `${inputId}-hint`
  const errorId = `${inputId}-error`
  const describedBy = [
    errorMessage ? errorId : null,
    hint && !errorMessage ? hintId : null,
  ]
    .filter(Boolean)
    .join(' ')

  const stateClass =
    validationState === 'error' || errorMessage
      ? styles.error
      : validationState === 'success'
        ? styles.success
        : ''

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={inputId}>
        {label}
        {required ? (
          <span className={styles.required} aria-hidden="true">
            *
          </span>
        ) : null}
      </label>
      <input
        id={inputId}
        type={type}
        className={[styles.control, stateClass, className].filter(Boolean).join(' ')}
        required={required}
        aria-invalid={Boolean(errorMessage) || validationState === 'error' || undefined}
        aria-describedby={describedBy || undefined}
        {...rest}
      />
      {errorMessage ? (
        <p id={errorId} className={`${styles.hint} ${styles.hintError}`} role="alert">
          {errorMessage}
        </p>
      ) : null}
      {hint && !errorMessage ? (
        <p id={hintId} className={`${styles.hint} ${validationState === 'success' ? styles.hintSuccess : ''}`}>
          {hint}
        </p>
      ) : null}
    </div>
  )
}
