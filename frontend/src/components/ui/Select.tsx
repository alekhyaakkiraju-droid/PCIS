import { useId, type SelectHTMLAttributes } from 'react'
import styles from './Field.module.css'

export interface SelectOption {
  value: string
  label: string
  disabled?: boolean
}

export interface SelectOptionGroup {
  label: string
  options: SelectOption[]
}

export interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  label: string
  options?: SelectOption[]
  optionGroups?: SelectOptionGroup[]
  placeholder?: string
  hint?: string
  errorMessage?: string
}

export function Select({
  label,
  options = [],
  optionGroups,
  placeholder,
  hint,
  errorMessage,
  id,
  required,
  className,
  ...rest
}: SelectProps) {
  const generatedId = useId()
  const selectId = id ?? generatedId
  const hintId = `${selectId}-hint`
  const errorId = `${selectId}-error`
  const describedBy = [
    errorMessage ? errorId : null,
    hint && !errorMessage ? hintId : null,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={selectId}>
        {label}
        {required ? (
          <span className={styles.required} aria-hidden="true">
            *
          </span>
        ) : null}
      </label>
      <select
        id={selectId}
        className={[styles.control, errorMessage ? styles.error : '', className]
          .filter(Boolean)
          .join(' ')}
        required={required}
        aria-invalid={Boolean(errorMessage) || undefined}
        aria-describedby={describedBy || undefined}
        {...rest}
      >
        {placeholder ? (
          <option value="" disabled>
            {placeholder}
          </option>
        ) : null}
        {optionGroups
          ? optionGroups.map((group) => (
              <optgroup key={group.label} label={group.label}>
                {group.options.map((opt) => (
                  <option key={opt.value} value={opt.value} disabled={opt.disabled}>
                    {opt.label}
                  </option>
                ))}
              </optgroup>
            ))
          : options.map((opt) => (
              <option key={opt.value} value={opt.value} disabled={opt.disabled}>
                {opt.label}
              </option>
            ))}
      </select>
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
