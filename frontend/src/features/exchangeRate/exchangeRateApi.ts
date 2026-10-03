import { api } from '../../services/api'
import type { ExchangeRateRequest, ExchangeRateResponse } from '../../types'

/**
 * Endpoints de taxas de câmbio.
 *
 * POST /api/exchange-rates          → cadastra nova taxa
 * GET  /api/exchange-rates/latest?pair=USD/BRL → taxa vigente para o par
 *
 * O par ("USD/BRL") vai como query param — não no path — porque a barra
 * codificada (%2F) no path é bloqueada pelo Tomcat (400 Bad Request).
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
      query: (pair) => ({
        url: '/exchange-rates/latest',
        params: { pair },
      }),
      providesTags: ['ExchangeRate'],
    }),
  }),
})

export const { useCreateExchangeRateMutation, useGetLatestRateQuery } =
  exchangeRateApi
