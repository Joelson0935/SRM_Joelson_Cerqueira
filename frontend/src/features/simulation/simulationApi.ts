import { api } from '../../services/api'
import type { PricingSimulationResponse, SimulationRequest } from '../../types'

/**
 * Endpoint de simulação (POST /api/simulate).
 *
 * Simulação não persiste nada — é usada no painel do operador para exibir o VP
 * em tempo real. Por isso não provê nem invalida tags de cache.
 */
export const simulationApi = api.injectEndpoints({
  endpoints: (builder) => ({
    simulate: builder.mutation<PricingSimulationResponse, SimulationRequest>({
      query: (body) => ({
        url: '/simulate',
        method: 'POST',
        body,
      }),
    }),
  }),
})

export const { useSimulateMutation } = simulationApi
