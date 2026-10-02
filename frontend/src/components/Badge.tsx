import type { ReactNode } from 'react'
import styles from './Badge.module.css'

type Variant = 'neutral' | 'success' | 'warning' | 'info'

interface BadgeProps {
  variant?: Variant
  children: ReactNode
}

/** Rótulo colorido, apenas visual (status, moeda, etc.). */
export function Badge({ variant = 'neutral', children }: BadgeProps) {
  return (
    <span className={`${styles.badge} ${styles[variant]}`}>{children}</span>
  )
}
