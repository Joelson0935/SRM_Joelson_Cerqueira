import { useForm } from 'react-hook-form'

import { Button, Field, Input, Select } from '../../components'
import type { SelectOption } from '../../components'
import {
  useCreateExchangeRateMutation,
  useGetLatestRateQuery,
} from '../../features/exchangeRate/exchangeRateApi'
import type { ExchangeRateRequest } from '../../types'
import { extractApiError } from '../../utils/apiError'
import { formatDateTime, formatNumber } from '../../utils/format'
import styles from './ExchangeRatePage.module.css'

// O backend trabalha apenas com BRL e USD — par fixo USD/BRL.
const USD_BRL = 'USD/BRL'
const PAIR_OPTIONS: SelectOption[] = [{ value: USD_BRL, label: 'USD/BRL' }]

interface FormValues {
  currencyPair: string
  rate: string
  effectiveAt: string
}

/**
 * Cadastro de Taxa de Câmbio.
 *
 * Cadastra uma nova taxa (POST /api/exchange-rates) e exibe a taxa vigente
 * (GET /api/exchange-rates/latest/USD/BRL). Como o cadastro invalida a tag
 * 'ExchangeRate', o cartão de taxa vigente é refeito automaticamente.
 */
export function ExchangeRatePage() {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    defaultValues: { currencyPair: USD_BRL, rate: '', effectiveAt: '' },
  })

  const [createRate, creation] = useCreateExchangeRateMutation()
  const latest = useGetLatestRateQuery(USD_BRL)

  const latestError = extractApiError(latest.error)
  const noRateYet =
    latest.isError && latestError?.code === 'EXCHANGE_RATE_NOT_FOUND'

  const onSubmit = handleSubmit(async (values) => {
    const body: ExchangeRateRequest = {
      currencyPair: values.currencyPair,
      rate: Number(values.rate),
      // Campo opcional: só envia se preenchido; senão o backend usa now().
      ...(values.effectiveAt ? { effectiveAt: values.effectiveAt } : {}),
    }
    try {
      await createRate(body).unwrap()
      reset({ currencyPair: USD_BRL, rate: '', effectiveAt: '' })
    } catch {
      // erro exibido via creation.error abaixo
    }
  })

  return (
    <div className={styles.page}>
      {/* Formulário de cadastro */}
      <section className={styles.card}>
        <h2 className={styles.cardTitle}>Cadastrar taxa de câmbio</h2>

        <form onSubmit={onSubmit} noValidate>
          <Field htmlFor="currencyPair" label="Par de moedas" required>
            <Select
              id="currencyPair"
              options={PAIR_OPTIONS}
              {...register('currencyPair', { required: true })}
            />
          </Field>

          <Field htmlFor="rate" label="Taxa (BRL por 1 USD)" required error={errors.rate?.message}>
            <Input
              id="rate"
              type="number"
              step="0.0001"
              min="0.0001"
              placeholder="5.4321"
              invalid={!!errors.rate}
              {...register('rate', {
                required: 'Taxa é obrigatória',
                validate: (v) => Number(v) > 0 || 'Taxa deve ser positiva',
              })}
            />
          </Field>

          <Field htmlFor="effectiveAt" label="Vigente a partir de (opcional)">
            <Input id="effectiveAt" type="datetime-local" {...register('effectiveAt')} />
          </Field>

          {creation.isError && (
            <p className={styles.errorBox}>
              {extractApiError(creation.error)?.message ?? 'Erro ao cadastrar taxa.'}
            </p>
          )}
          {creation.isSuccess && (
            <p className={styles.successBox}>Taxa cadastrada com sucesso.</p>
          )}

          <div className={styles.actions}>
            <Button type="submit" loading={creation.isLoading}>
              Cadastrar taxa
            </Button>
          </div>
        </form>
      </section>

      {/* Taxa vigente */}
      <section className={styles.card}>
        <h2 className={styles.cardTitle}>Taxa vigente — USD/BRL</h2>

        {latest.isFetching ? (
          <p className={styles.hint}>Carregando...</p>
        ) : noRateYet ? (
          <p className={styles.hint}>
            Nenhuma taxa cadastrada ainda. Cadastre a primeira taxa ao lado.
          </p>
        ) : latest.isError ? (
          <p className={styles.errorBox}>
            {latestError?.message ?? 'Erro ao carregar a taxa vigente.'}
          </p>
        ) : latest.data ? (
          <dl className={styles.rateGrid}>
            <div>
              <dt>Taxa</dt>
              <dd className={styles.highlight}>
                {formatNumber(latest.data.rate, 4)}
              </dd>
            </div>
            <div>
              <dt>Vigente desde</dt>
              <dd>{formatDateTime(latest.data.effectiveAt)}</dd>
            </div>
            <div>
              <dt>Cadastrada em</dt>
              <dd>{formatDateTime(latest.data.createdAt)}</dd>
            </div>
          </dl>
        ) : null}
      </section>
    </div>
  )
}
