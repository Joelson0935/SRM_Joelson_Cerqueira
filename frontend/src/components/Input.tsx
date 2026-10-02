import { forwardRef } from 'react'
import type { InputHTMLAttributes } from 'react'
import styles from './Field.module.css'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean
}

/**
 * Input base. Usa forwardRef para integrar com o register() do react-hook-form.
 * `invalid` aplica estilo de erro e marca aria-invalid para acessibilidade.
 */
export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ invalid = false, className, ...rest }, ref) => {
    const classes = [styles.control, invalid && styles.invalid, className]
      .filter(Boolean)
      .join(' ')
    return (
      <input ref={ref} className={classes} aria-invalid={invalid} {...rest} />
    )
  },
)
Input.displayName = 'Input'
