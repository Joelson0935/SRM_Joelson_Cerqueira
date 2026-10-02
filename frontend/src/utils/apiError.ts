import type { FetchBaseQueryError } from '@reduxjs/toolkit/query'
import type { ApiError } from '../types'

/**
 * Extrai o corpo ApiError padronizado do backend de dentro de um erro do
 * RTK Query. Retorna null quando o erro não tem o formato esperado (ex: erro
 * de rede, timeout).
 */
export function extractApiError(
  error: FetchBaseQueryError | ApiError | unknown,
): ApiError | null {
  if (!error || typeof error !== 'object') return null

  // Erro do RTK Query: { status, data }
  if ('data' in error) {
    const data = (error as FetchBaseQueryError).data
    if (data && typeof data === 'object' && 'code' in data && 'message' in data) {
      return data as ApiError
    }
    return null
  }

  // Já é um ApiError
  if ('code' in error && 'message' in error) {
    return error as ApiError
  }

  return null
}
