import type { PaymentCurrency } from '../types'

/**
 * Helpers de formatação para EXIBIÇÃO apenas.
 *
 * Nenhum cálculo financeiro acontece no cliente — estes helpers apenas formatam
 * valores já calculados pelo backend. Valores monetários chegam como number
 * (serialização de BigDecimal) e são formatados via Intl.NumberFormat.
 */

const CURRENCY_LOCALE: Record<PaymentCurrency, string> = {
  BRL: 'pt-BR',
  USD: 'en-US',
}

/** Formata um valor monetário na moeda informada (ex: R$ 92.859,94 / $17,094.67). */
export function formatCurrency(
  value: number | null | undefined,
  currency: PaymentCurrency,
): string {
  if (value === null || value === undefined) return '—'
  return new Intl.NumberFormat(CURRENCY_LOCALE[currency], {
    style: 'currency',
    currency,
  }).format(value)
}

/** Formata um número decimal com N casas (default 2), locale pt-BR. */
export function formatNumber(
  value: number | null | undefined,
  fractionDigits = 2,
): string {
  if (value === null || value === undefined) return '—'
  return new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  }).format(value)
}

/** Formata uma taxa decimal (ex: 0.015) como percentual (ex: 1,5000%). */
export function formatPercent(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return `${new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(value * 100)}%`
}

/** Formata um ISO date-time para dd/mm/aaaa hh:mm. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date)
}
