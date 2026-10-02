import { useDispatch, useSelector } from 'react-redux'
import type { AppDispatch, RootState } from './store'

/**
 * Hooks tipados do Redux — use estes em vez de useDispatch/useSelector crus,
 * garantindo tipagem de RootState e AppDispatch em toda a aplicação.
 */
export const useAppDispatch = useDispatch.withTypes<AppDispatch>()
export const useAppSelector = useSelector.withTypes<RootState>()
