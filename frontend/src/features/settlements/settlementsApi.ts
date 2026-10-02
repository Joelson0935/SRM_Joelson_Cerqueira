import { api } from '../../services/api'
import type {
  Page,
  SettlementFilters,
  SettlementRequest,
  SettlementResponse,
} from '../../types'

/**
 * Endpoints de liquidação e extrato (POST/GET /api/settlements).
 *
 * A liquidação envia o header Idempotency-Key (UUID gerado pelo cliente) —
 * retentativas com a mesma key retornam o mesmo resultado sem gerar nova
 * liquidação. Criar uma liquidação invalida 'Settlement' (atualiza o extrato)
 * e 'Receivable' (o recebível passa a SETTLED).
 */
export const settlementsApi = api.injectEndpoints({
  endpoints: (builder) => ({
    settle: builder.mutation<
      SettlementResponse,
      { body: SettlementRequest; idempotencyKey: string }
    >({
      query: ({ body, idempotencyKey }) => ({
        url: '/settlements',
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey },
        body,
      }),
      invalidatesTags: ['Settlement', 'Receivable'],
    }),

    listSettlements: builder.query<
      Page<SettlementResponse>,
      SettlementFilters | void
    >({
      query: (filters) => {
        // Remove chaves vazias/undefined para não enviar params desnecessários.
        const params: Record<string, string | number> = {}
        if (filters) {
          for (const [key, value] of Object.entries(filters)) {
            if (value !== undefined && value !== null && value !== '') {
              params[key] = value as string | number
            }
          }
        }
        return { url: '/settlements', params }
      },
      providesTags: ['Settlement'],
    }),

    getSettlement: builder.query<SettlementResponse, number>({
      query: (id) => `/settlements/${id}`,
      providesTags: ['Settlement'],
    }),
  }),
})

export const {
  useSettleMutation,
  useListSettlementsQuery,
  useGetSettlementQuery,
} = settlementsApi
