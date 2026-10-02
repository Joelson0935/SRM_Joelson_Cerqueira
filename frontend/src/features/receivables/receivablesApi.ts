import { api } from '../../services/api'
import type {
  Page,
  ReceivableRequest,
  ReceivableResponse,
} from '../../types'

/**
 * Endpoints de recebíveis (POST/GET /api/receivables).
 *
 * createReceivable invalida a tag 'Receivable' para refazer listagens.
 */
export const receivablesApi = api.injectEndpoints({
  endpoints: (builder) => ({
    createReceivable: builder.mutation<ReceivableResponse, ReceivableRequest>({
      query: (body) => ({
        url: '/receivables',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Receivable'],
    }),

    listReceivables: builder.query<
      Page<ReceivableResponse>,
      { page?: number; size?: number; sort?: string } | void
    >({
      query: (params) => ({
        url: '/receivables',
        params: params ?? undefined,
      }),
      providesTags: ['Receivable'],
    }),

    getReceivable: builder.query<ReceivableResponse, number>({
      query: (id) => `/receivables/${id}`,
      providesTags: ['Receivable'],
    }),
  }),
})

export const {
  useCreateReceivableMutation,
  useListReceivablesQuery,
  useGetReceivableQuery,
} = receivablesApi
