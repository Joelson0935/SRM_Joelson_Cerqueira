import type { ButtonHTMLAttributes, ReactNode } from 'react'
import styles from './Button.module.css'

type Variant = 'primary' | 'secondary' | 'danger'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  /** Quando true, desabilita o botão e exibe texto de carregamento. */
  loading?: boolean
  children: ReactNode
}

/**
 * Botão base da aplicação. Repassa todas as props nativas de <button>.
 * Em estado `loading` fica desabilitado e exibe "Processando...".
 */
export function Button({
  variant = 'primary',
  loading = false,
  disabled,
  children,
  className,
  ...rest
}: ButtonProps) {
  const classes = [styles.button, styles[variant], className]
    .filter(Boolean)
    .join(' ')

  return (
    <button
      className={classes}
      disabled={disabled || loading}
      aria-busy={loading}
      {...rest}
    >
      {loading ? 'Processando...' : children}
    </button>
  )
}
