/**
 * Tipos TypeScript alinhados aos DTOs do backend (SRM Credit Engine).
 *
 * Convenção de valores monetários: o backend serializa BigDecimal como número
 * JSON. No frontend representamos como `number` apenas para transporte/exibição —
 * nenhum cálculo financeiro é feito no cliente (o motor de precificação é do
 * backend). A formatação para exibição usa Intl.NumberFormat.
 */

// =============================================================================
// Enums (espelham com.srm.creditengine.domain.enums)
// =============================================================================

export const ReceivableType = {
  DUPLICATA_MERCANTIL: 'DUPLICATA_MERCANTIL',
  CHEQUE_PRE_DATADO: 'CHEQUE_PRE_DATADO',
} as const
export type ReceivableType = (typeof ReceivableType)[keyof typeof ReceivableType]

export const PaymentCurrency = {
  BRL: 'BRL',
  USD: 'USD',
} as const
export type PaymentCurrency = (typeof PaymentCurrency)[keyof typeof PaymentCurrency]

export const ReceivableStatus = {
  PENDING: 'PENDING',
  SETTLED: 'SETTLED',
} as const
export type ReceivableStatus = (typeof ReceivableStatus)[keyof typeof ReceivableStatus]

// Rótulos legíveis para exibição na UI.
export const RECEIVABLE_TYPE_LABELS: Record<ReceivableType, string> = {
  DUPLICATA_MERCANTIL: 'Duplicata Mercantil',
  CHEQUE_PRE_DATADO: 'Cheque Pré-datado',
}

export const RECEIVABLE_STATUS_LABELS: Record<ReceivableStatus, string> = {
  PENDING: 'Pendente',
  SETTLED: 'Liquidado',
}

// =============================================================================
// Receivable (ReceivableRequest / ReceivableResponse)
// =============================================================================

export interface ReceivableRequest {
  cedente: string
  documentNumber: string
  type: ReceivableType
  faceValue: number
  termInMonths: number
  dueDate: string // ISO date (YYYY-MM-DD)
  paymentCurrency: PaymentCurrency
}

export interface ReceivableResponse {
  id: number
  cedente: string
  documentNumber: string
  type: ReceivableType
  faceValue: number
  termInMonths: number
  dueDate: string
  paymentCurrency: PaymentCurrency
  status: ReceivableStatus
  createdAt: string // ISO date-time
}

// =============================================================================
// Simulation (SimulationRequest / PricingSimulationResponse)
// =============================================================================

export interface SimulationRequest {
  type: ReceivableType
  faceValue: number
  termInMonths: number
  paymentCurrency: PaymentCurrency
}

export interface PricingSimulationResponse {
  receivableType: ReceivableType
  faceValue: number
  termInMonths: number
  paymentCurrency: PaymentCurrency
  presentValueBrl: number
  finalAmount: number
  finalCurrency: PaymentCurrency
  discountAmount: number
  baseRateUsed: number
  spreadUsed: number
  exchangeRateUsed: number | null // null quando BRL
}

// =============================================================================
// Settlement (SettlementRequest / SettlementResponse)
// =============================================================================

export interface SettlementRequest {
  receivableId: number
  currency: PaymentCurrency
}

export interface SettlementResponse {
  id: number
  receivableId: number
  receivableType: ReceivableType
  faceValue: number
  presentValueBrl: number
  finalAmount: number
  discountAmount: number
  paymentCurrency: PaymentCurrency
  baseRateUsed: number
  spreadUsed: number
  termInMonthsUsed: number
  exchangeRateUsed: number | null
  idempotencyKey: string
  settledAt: string // ISO date-time
}

// Filtros aceitos pelo extrato de liquidações (GET /api/settlements).
export interface SettlementFilters {
  from?: string // ISO date-time
  to?: string // ISO date-time
  cedente?: string
  currency?: PaymentCurrency
  page?: number
  size?: number
  sort?: string
}

// =============================================================================
// ExchangeRate (ExchangeRateRequest / ExchangeRateResponse)
// =============================================================================

export interface ExchangeRateRequest {
  currencyPair: string // formato "USD/BRL"
  rate: number
  effectiveAt?: string // ISO date-time; default = now no backend
}

export interface ExchangeRateResponse {
  id: number
  currencyPair: string
  rate: number
  effectiveAt: string
  createdAt: string
}

// =============================================================================
// Paginação (Spring Data Page<T>) e erro padronizado (ApiError)
// =============================================================================

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number // página atual (0-based)
  size: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

export interface ApiFieldError {
  field: string
  message: string
}

export interface ApiError {
  code: string
  message: string
  timestamp: string
  details?: ApiFieldError[]
}
