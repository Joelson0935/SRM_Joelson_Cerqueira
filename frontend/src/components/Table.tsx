import type { ReactNode } from 'react'
import styles from './Table.module.css'

export interface Column<T> {
  /** Cabeçalho da coluna. */
  header: string
  /** Renderiza a célula a partir da linha. */
  render: (row: T) => ReactNode
  /** Alinhamento do conteúdo da célula. */
  align?: 'left' | 'right' | 'center'
}

interface TableProps<T> {
  columns: Column<T>[]
  rows: T[]
  /** Extrai uma key estável por linha. */
  rowKey: (row: T) => string | number
  loading?: boolean
  emptyMessage?: string
}

/**
 * Tabela genérica tipada. Define colunas declarativamente (header + render) e
 * trata estados de loading e vazio. Sem lógica de negócio.
 */
export function Table<T>({
  columns,
  rows,
  rowKey,
  loading = false,
  emptyMessage = 'Nenhum registro encontrado.',
}: TableProps<T>) {
  return (
    <div className={styles.wrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            {columns.map((col, i) => (
              <th
                key={i}
                className={styles.th}
                style={{ textAlign: col.align ?? 'left' }}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td className={styles.state} colSpan={columns.length}>
                Carregando...
              </td>
            </tr>
          ) : rows.length === 0 ? (
            <tr>
              <td className={styles.state} colSpan={columns.length}>
                {emptyMessage}
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={rowKey(row)} className={styles.tr}>
                {columns.map((col, i) => (
                  <td
                    key={i}
                    className={styles.td}
                    style={{ textAlign: col.align ?? 'left' }}
                  >
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
