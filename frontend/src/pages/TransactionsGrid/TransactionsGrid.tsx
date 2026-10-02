import { useState } from 'react'

import { Badge, Button, Field, Input, Select, Table } from '../../components'
import type { Column, SelectOption } from '../../components'
import { useListSettlementsQuery } from '../../features/settlements/settlementsApi'
import { PaymentCurrency, RECEIVABLE_TYPE_LABELS } from '../../types'
import type { SettlementFilters, SettlementResponse } from '../../types'
import { extractApiError } from '../../utils/apiError'
import { formatCurrency, formatDateTime, formatNumber } from '../../utils/format'
import styles from './TransactionsGrid.module.css'

const PAGE_SIZE = 10
const SORT = 'settledAt,desc'

const CURRENCY_OPTIONS: SelectOption[] = [
  { value: PaymentCurrency.BRL, label: 'BRL' },
  { value: PaymentCurrency.USD, label: 'USD' },
]

// Estado dos campos de filtro (controlados). Separado dos filtros efetivamente
// aplicados para que a query só mude ao clicar em "Filtrar".
interface FilterForm {
  from: string
  to: string
  cedente: string
  currency: string
}

const EMPTY_FILTERS: FilterForm = { from: '', to: '', cedente: '', currency: '' }

/**
 * Grid de Transações — extrato de liquidações com filtros combinados e
 * paginação server-side. Nunca carrega a tabela inteira: cada página é uma
 * query ao backend.
 */
export function TransactionsGrid() {
  const [form, setForm] = useState<FilterForm>(EMPTY_FILTERS)
  const [applied, setApplied] = useState<SettlementFilters>({})
  const [page, setPage] = useState(0)

  const { data, isFetching, isError, error } = useListSettlementsQuery({
    ...applied,
    page,
    size: PAGE_SIZE,
    sort: SORT,
  })

  const updateField = (field: keyof FilterForm, value: string) =>
    setForm((prev) => ({ ...prev, [field]: value }))

  const applyFilters = () => {
    const next: SettlementFilters = {}
    if (form.from) next.from = form.from
    if (form.to) next.to = form.to
    if (form.cedente.trim()) next.cedente = form.cedente.trim()
    if (form.currency) next.currency = form.currency as PaymentCurrency
    setApplied(next)
    setPage(0) // volta para a primeira página ao refiltrar
  }

  const clearFilters = () => {
    setForm(EMPTY_FILTERS)
    setApplied({})
    setPage(0)
  }

  const columns: Column<SettlementResponse>[] = [
    { header: '#', render: (r) => r.id },
    { header: 'Recebível', render: (r) => `#${r.receivableId}` },
    { header: 'Tipo', render: (r) => RECEIVABLE_TYPE_LABELS[r.receivableType] },
    {
      header: 'Valor de face',
      align: 'right',
      render: (r) => formatCurrency(r.faceValue, PaymentCurrency.BRL),
    },
    {
      header: 'VP (BRL)',
      align: 'right',
      render: (r) => formatCurrency(r.presentValueBrl, PaymentCurrency.BRL),
    },
    {
      header: 'Valor final',
      align: 'right',
      render: (r) => formatCurrency(r.finalAmount, r.paymentCurrency),
    },
    {
      header: 'Moeda',
      align: 'center',
      render: (r) => (
        <Badge variant={r.paymentCurrency === 'USD' ? 'info' : 'neutral'}>
          {r.paymentCurrency}
        </Badge>
      ),
    },
    {
      header: 'Câmbio',
      align: 'right',
      render: (r) =>
        r.exchangeRateUsed != null ? formatNumber(r.exchangeRateUsed, 4) : '—',
    },
    { header: 'Liquidado em', render: (r) => formatDateTime(r.settledAt) },
  ]

  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0
  const isFirst = data?.first ?? true
  const isLast = data?.last ?? true

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>Extrato de liquidações</h2>

      {/* Filtros */}
      <div className={styles.filters}>
        <Field htmlFor="f-from" label="De">
          <Input
            id="f-from"
            type="datetime-local"
            value={form.from}
            onChange={(e) => updateField('from', e.target.value)}
          />
        </Field>
        <Field htmlFor="f-to" label="Até">
          <Input
            id="f-to"
            type="datetime-local"
            value={form.to}
            onChange={(e) => updateField('to', e.target.value)}
          />
        </Field>
        <Field htmlFor="f-cedente" label="Cedente">
          <Input
            id="f-cedente"
            placeholder="Nome do cedente"
            value={form.cedente}
            onChange={(e) => updateField('cedente', e.target.value)}
          />
        </Field>
        <Field htmlFor="f-currency" label="Moeda">
          <Select
            id="f-currency"
            options={CURRENCY_OPTIONS}
            placeholder="Todas"
            value={form.currency}
            onChange={(e) => updateField('currency', e.target.value)}
          />
        </Field>
        <div className={styles.filterActions}>
          <Button type="button" onClick={applyFilters}>
            Filtrar
          </Button>
          <Button type="button" variant="secondary" onClick={clearFilters}>
            Limpar
          </Button>
        </div>
      </div>

      {isError && (
        <p className={styles.errorBox}>
          {extractApiError(error)?.message ?? 'Erro ao carregar o extrato.'}
        </p>
      )}

      {/* Tabela */}
      <Table
        columns={columns}
        rows={data?.content ?? []}
        rowKey={(r) => r.id}
        loading={isFetching}
        emptyMessage="Nenhuma liquidação encontrada para os filtros aplicados."
      />

      {/* Paginação */}
      <div className={styles.pagination}>
        <span className={styles.pageInfo}>
          {totalElements} liquidação(ões) · Página {totalPages === 0 ? 0 : page + 1} de {totalPages}
        </span>
        <div className={styles.pageButtons}>
          <Button
            type="button"
            variant="secondary"
            disabled={isFirst || isFetching}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Anterior
          </Button>
          <Button
            type="button"
            variant="secondary"
            disabled={isLast || isFetching}
            onClick={() => setPage((p) => p + 1)}
          >
            Próxima
          </Button>
        </div>
      </div>
    </div>
  )
}
