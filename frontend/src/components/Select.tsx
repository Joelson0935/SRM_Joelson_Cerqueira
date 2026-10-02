import { forwardRef } from 'react'
import type { SelectHTMLAttributes } from 'react'
import styles from './Field.module.css'

export interface SelectOption {
  value: string
  label: string
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  options: SelectOption[]
  invalid?: boolean
  /** Texto de uma opção vazia inicial (ex: "Selecione..."). */
  placeholder?: string
}

/**
 * Select base. Usa forwardRef para integrar com o register() do react-hook-form.
 */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ options, invalid = false, placeholder, className, ...rest }, ref) => {
    const classes = [styles.control, invalid && styles.invalid, className]
      .filter(Boolean)
      .join(' ')
    return (
      <select ref={ref} className={classes} aria-invalid={invalid} {...rest}>
        {placeholder !== undefined && (
          <option value="">{placeholder}</option>
        )}
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    )
  },
)
Select.displayName = 'Select'
