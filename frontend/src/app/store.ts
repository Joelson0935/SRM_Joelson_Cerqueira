import { configureStore } from '@reduxjs/toolkit'
import { api } from '../services/api'

/**
 * Store Redux da aplicação.
 *
 * Todo o estado de servidor (recebíveis, liquidações, taxas) é gerenciado pelo
 * cache do RTK Query sob o reducer `api`. Não há slices de estado manual porque
 * não temos estado global de UI que justifique — filtros de tela ficam locais
 * aos componentes.
 */
export const store = configureStore({
  reducer: {
    [api.reducerPath]: api.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(api.middleware),
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
