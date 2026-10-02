import type { ReactNode } from 'react'
import styles from './Field.module.css'

interface FieldProps {
  /** id do controle para associar o <label> (htmlFor). */
  htmlFor: string
  label: string
  required?: boolean
  error?: string
  children: ReactNode
}

/**
 * Compõe label + controle + mensagem de erro com espaçamento padronizado.
 * Associa o label ao controle via htmlFor para acessibilidade.
 */
export function Field({
  htmlFor,
  label,
  required = false,
  error,
  children,
}: FieldProps) {
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={htmlFor}>
        {label}
        {required && <span className={styles.required}>*</span>}
      </label>
      {children}
      {error && <span className={styles.error}>{error}</span>}
    </div>
  )
}
