import { api } from '../../services/api'
import type { ExchangeRateRequest, ExchangeRateResponse } from '../../types'

/**
 * Endpoints de taxas de câmbio.
 *
 * POST /api/exchange-rates           → cadastra nova taxa
 * GET  /api/exchange-rates/latest/{pair} → taxa vigente para o par
 *
 * O par vem no formato "USD/BRL". Como a URL contém '/', ele é codificado para
 * não quebrar o path (USD%2FBRL).
 */
export const exchangeRateApi = api.injectEndpoints({
  endpoints: (builder) => ({
    createExchangeRate: builder.mutation<
      ExchangeRateResponse,
      ExchangeRateRequest
    >({
      query: (body) => ({
        url: '/exchange-rates',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['ExchangeRate'],
    }),

    getLatestRate: builder.query<ExchangeRateResponse, string>({
      query: (currencyPair) =>
        `/exchange-rates/latest/${encodeURIComponent(currencyPair)}`,
      providesTags: ['ExchangeRate'],
    }),
  }),
})

export const { useCreateExchangeRateMutation, useGetLatestRateQuery } =
  exchangeRateApi
