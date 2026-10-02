import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'

/**
 * apiSlice base do RTK Query.
 *
 * baseUrl '/api' funciona em dev (proxy do Vite → localhost:8080) e em produção
 * (frontend servido atrás do mesmo host/reverse-proxy do backend).
 *
 * Os endpoints concretos são injetados por feature via `api.injectEndpoints`
 * (padrão code-splitting do RTK Query) — mantém cada feature coesa e evita um
 * arquivo monolítico de endpoints.
 *
 * tagTypes habilitam invalidação de cache: ao criar um recebível/liquidação/taxa,
 * as listas correspondentes são refeitas automaticamente.
 */
export const api = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
  tagTypes: ['Receivable', 'Settlement', 'ExchangeRate'],
  endpoints: () => ({}),
})
