import {
  useId,
  useState,
  type ChangeEvent,
  type TextareaHTMLAttributes,
} from 'react'
import styles from './Field.module.css'

export interface TextAreaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
  hint?: string
  errorMessage?: string
  showCount?: boolean
  maxLength?: number
}

function initialLength(value: TextAreaProps['value'], defaultValue: TextAreaProps['defaultValue']) {
  if (typeof value === 'string') return value.length
  if (typeof defaultValue === 'string') return defaultValue.length
  return 0
}

export function TextArea({
  label,
  hint,
  errorMessage,
  showCount = false,
  maxLength,
  id,
  required,
  className,
  value,
  defaultValue,
  onChange,
  ...rest
}: TextAreaProps) {
  const generatedId = useId()
  const areaId = id ?? generatedId
  const hintId = `${areaId}-hint`
  const errorId = `${areaId}-error`
  const countId = `${areaId}-count`
  const [uncontrolledLength, setUncontrolledLength] = useState(() =>
    initialLength(value, defaultValue),
  )
  const currentLength =
    typeof value === 'string' ? value.length : uncontrolledLength

  const describedBy = [
    errorMessage ? errorId : null,
    hint && !errorMessage ? hintId : null,
    showCount && maxLength ? countId : null,
  ]
    .filter(Boolean)
    .join(' ')

  const handleChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
    if (typeof value !== 'string') {
      setUncontrolledLength(event.target.value.length)
    }
    onChange?.(event)
  }

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={areaId}>
        {label}
        {required ? (
          <span className={styles.required} aria-hidden="true">
            *
          </span>
        ) : null}
      </label>
      <textarea
        id={areaId}
        className={[styles.control, styles.textarea, errorMessage ? styles.error : '', className]
          .filter(Boolean)
          .join(' ')}
        required={required}
        maxLength={maxLength}
        value={value}
        defaultValue={defaultValue}
        onChange={handleChange}
        aria-invalid={Boolean(errorMessage) || undefined}
        aria-describedby={describedBy || undefined}
        {...rest}
      />
      {showCount && maxLength ? (
        <p id={countId} className={styles.charCount}>
          {currentLength}/{maxLength}
        </p>
      ) : null}
      {errorMessage ? (
        <p id={errorId} className={`${styles.hint} ${styles.hintError}`} role="alert">
          {errorMessage}
        </p>
      ) : null}
      {hint && !errorMessage ? (
        <p id={hintId} className={styles.hint}>
          {hint}
        </p>
      ) : null}
    </div>
  )
}
