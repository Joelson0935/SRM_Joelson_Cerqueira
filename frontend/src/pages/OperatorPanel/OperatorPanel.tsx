import { useEffect, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { v4 as uuidv4 } from 'uuid'

import { Badge, Button, Field, Input, Select } from '../../components'
import type { SelectOption } from '../../components'
import { useCreateReceivableMutation } from '../../features/receivables/receivablesApi' 
import { useSimulateMutation } from '../../features/simulation/simulationApi'
import { useSettleMutation } from '../../features/settlements/settlementsApi'
import {
  PaymentCurrency,
  RECEIVABLE_STATUS_LABELS,
  RECEIVABLE_TYPE_LABELS,
  ReceivableType,
} from '../../types'
import type {
  ReceivableResponse,
  SettlementResponse,
  SimulationRequest,
} from '../../types'
import {
  formatCurrency,
  formatDateTime,
  formatPercent,
} from '../../utils/format'
import { extractApiError } from '../../utils/apiError'
import styles from './OperatorPanel.module.css'

interface FormValues {
  cedente: string
  documentNumber: string
  type: ReceivableType
  faceValue: string
  termInMonths: string
  dueDate: string
  paymentCurrency: PaymentCurrency
}

const TYPE_OPTIONS: SelectOption[] = [
  { value: ReceivableType.DUPLICATA_MERCANTIL, label: RECEIVABLE_TYPE_LABELS.DUPLICATA_MERCANTIL },
  { value: ReceivableType.CHEQUE_PRE_DATADO, label: RECEIVABLE_TYPE_LABELS.CHEQUE_PRE_DATADO },
]

const CURRENCY_OPTIONS: SelectOption[] = [
  { value: PaymentCurrency.BRL, label: 'BRL (Real)' },
  { value: PaymentCurrency.USD, label: 'USD (Dólar)' },
]

/**
 * Painel do Operador.
 *
 * Fluxo em duas operações distintas (opção B):
 *   1. Cadastrar recebível (POST /api/receivables)
 *   2. Liquidar o recebível criado (POST /api/settlements + Idempotency-Key)
 *
 * Enquanto o operador preenche tipo/valor/prazo/moeda, a simulação roda em
 * tempo real (debounce) e exibe o VP sem persistir nada.
 */
export function OperatorPanel() {
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    defaultValues: {
      cedente: '',
      documentNumber: '',
      type: ReceivableType.DUPLICATA_MERCANTIL,
      faceValue: '',
      termInMonths: '',
      dueDate: '',
      paymentCurrency: PaymentCurrency.BRL,
    },
  })

  const [simulate, simulation] = useSimulateMutation()
  const [createReceivable, creation] = useCreateReceivableMutation()
  const [settle, settlement] = useSettleMutation()

  // Recebível já cadastrado nesta sessão da tela (habilita o passo de liquidação).
  const [createdReceivable, setCreatedReceivable] =
    useState<ReceivableResponse | null>(null)
  const [settledResult, setSettledResult] =
    useState<SettlementResponse | null>(null)

  // Campos que alimentam a simulação em tempo real.
  const type = watch('type')
  const faceValue = watch('faceValue')
  const termInMonths = watch('termInMonths')
  const paymentCurrency = watch('paymentCurrency')

  // Debounce da simulação: 400ms após a última mudança nos campos relevantes.
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  useEffect(() => {
    const face = Number(faceValue)
    const term = Number(termInMonths)
    // Só simula com valores válidos — evita chamadas com formulário incompleto.
    if (!type || !paymentCurrency || !face || face <= 0 || !term || term <= 0) {
      return
    }
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      const body: SimulationRequest = {
        type,
        faceValue: face,
        termInMonths: term,
        paymentCurrency,
      }
      void simulate(body)
    }, 400)
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [type, faceValue, termInMonths, paymentCurrency, simulate])

  const onCreate = handleSubmit(async (values) => {
    setSettledResult(null)
    try {
      const created = await createReceivable({
        cedente: values.cedente,
        documentNumber: values.documentNumber,
        type: values.type,
        faceValue: Number(values.faceValue),
        termInMonths: Number(values.termInMonths),
        dueDate: values.dueDate,
        paymentCurrency: values.paymentCurrency,
      }).unwrap()
      setCreatedReceivable(created)
    } catch {
      // erro exibido via creation.error abaixo
    }
  })

  const onSettle = async () => {
    if (!createdReceivable) return
    try {
      const result = await settle({
        body: {
          receivableId: createdReceivable.id,
          currency: createdReceivable.paymentCurrency,
        },
        idempotencyKey: uuidv4(),
      }).unwrap()
      setSettledResult(result)
    } catch {
      // erro exibido via settlement.error abaixo
    }
  }

  const resetAll = () => {
    reset()
    setCreatedReceivable(null)
    setSettledResult(null)
  }

  const sim = simulation.data
  const simIsUsdWithoutRate =
    simulation.isError &&
    extractApiError(simulation.error)?.code === 'EXCHANGE_RATE_NOT_FOUND'

  return (
    <div className={styles.panel}>
      {/* ----------------------------------------------------------------- */}
      {/* Coluna 1 — formulário de cadastro                                  */}
      {/* ----------------------------------------------------------------- */}
      <section className={styles.card}>
        <h2 className={styles.cardTitle}>1. Cadastrar recebível</h2>

        <form onSubmit={onCreate} noValidate>
          <Field htmlFor="cedente" label="Cedente" required error={errors.cedente?.message}>
            <Input
              id="cedente"
              placeholder="Nome do cedente"
              invalid={!!errors.cedente}
              disabled={!!createdReceivable}
              {...register('cedente', { required: 'Cedente é obrigatório' })}
            />
          </Field>

          <Field htmlFor="documentNumber" label="Número do documento" required error={errors.documentNumber?.message}>
            <Input
              id="documentNumber"
              placeholder="Ex: DUP-2026-001"
              invalid={!!errors.documentNumber}
              disabled={!!createdReceivable}
              {...register('documentNumber', { required: 'Documento é obrigatório' })}
            />
          </Field>

          <div className={styles.row}>
            <Field htmlFor="type" label="Tipo" required error={errors.type?.message}>
              <Select
                id="type"
                options={TYPE_OPTIONS}
                invalid={!!errors.type}
                disabled={!!createdReceivable}
                {...register('type', { required: 'Tipo é obrigatório' })}
              />
            </Field>

            <Field htmlFor="paymentCurrency" label="Moeda" required error={errors.paymentCurrency?.message}>
              <Select
                id="paymentCurrency"
                options={CURRENCY_OPTIONS}
                invalid={!!errors.paymentCurrency}
                disabled={!!createdReceivable}
                {...register('paymentCurrency', { required: 'Moeda é obrigatória' })}
              />
            </Field>
          </div>

          <div className={styles.row}>
            <Field htmlFor="faceValue" label="Valor de face (BRL)" required error={errors.faceValue?.message}>
              <Input
                id="faceValue"
                type="number"
                step="0.01"
                min="0.01"
                placeholder="100000.00"
                invalid={!!errors.faceValue}
                disabled={!!createdReceivable}
                {...register('faceValue', {
                  required: 'Valor é obrigatório',
                  validate: (v) => Number(v) > 0 || 'Valor deve ser positivo',
                })}
              />
            </Field>

            <Field htmlFor="termInMonths" label="Prazo (meses)" required error={errors.termInMonths?.message}>
              <Input
                id="termInMonths"
                type="number"
                min="1"
                max="360"
                placeholder="3"
                invalid={!!errors.termInMonths}
                disabled={!!createdReceivable}
                {...register('termInMonths', {
                  required: 'Prazo é obrigatório',
                  validate: (v) => {
                    const n = Number(v)
                    if (n < 1) return 'Mínimo de 1 mês'
                    if (n > 360) return 'Máximo de 360 meses'
                    return true
                  },
                })}
              />
            </Field>
          </div>

          <Field htmlFor="dueDate" label="Data de vencimento" required error={errors.dueDate?.message}>
            <Input
              id="dueDate"
              type="date"
              invalid={!!errors.dueDate}
              disabled={!!createdReceivable}
              {...register('dueDate', { required: 'Vencimento é obrigatório' })}
            />
          </Field>

          {creation.isError && (
            <p className={styles.errorBox}>
              {extractApiError(creation.error)?.message ?? 'Erro ao cadastrar recebível.'}
            </p>
          )}

          <div className={styles.actions}>
            {!createdReceivable ? (
              <Button type="submit" loading={creation.isLoading}>
                Cadastrar recebível
              </Button>
            ) : (
              <Button type="button" variant="secondary" onClick={resetAll}>
                Novo recebível
              </Button>
            )}
          </div>
        </form>
      </section>

      {/* ----------------------------------------------------------------- */}
      {/* Coluna 2 — simulação em tempo real + liquidação                    */}
      {/* ----------------------------------------------------------------- */}
      <section className={styles.sideColumn}>
        {/* Simulação */}
        <div className={styles.card}>
          <h2 className={styles.cardTitle}>Simulação em tempo real</h2>
          {simIsUsdWithoutRate ? (
            <p className={styles.hint}>
              Não há taxa USD/BRL cadastrada. Cadastre uma taxa na aba Câmbio
              para simular em dólar.
            </p>
          ) : sim ? (
            <dl className={styles.resultGrid}>
              <div><dt>Valor presente (BRL)</dt><dd>{formatCurrency(sim.presentValueBrl, PaymentCurrency.BRL)}</dd></div>
              <div>
                <dt>Valor final ({sim.finalCurrency})</dt>
                <dd className={styles.highlight}>{formatCurrency(sim.finalAmount, sim.finalCurrency)}</dd>
              </div>
              <div><dt>Deságio</dt><dd>{formatCurrency(sim.discountAmount, PaymentCurrency.BRL)}</dd></div>
              <div><dt>Taxa base</dt><dd>{formatPercent(sim.baseRateUsed)}</dd></div>
              <div><dt>Spread</dt><dd>{formatPercent(sim.spreadUsed)}</dd></div>
              {sim.exchangeRateUsed != null && (
                <div><dt>Câmbio usado</dt><dd>{sim.exchangeRateUsed}</dd></div>
              )}
            </dl>
          ) : (
            <p className={styles.hint}>
              Preencha tipo, valor, prazo e moeda para ver a simulação.
            </p>
          )}
        </div>

        {/* Liquidação — habilitada após cadastrar o recebível */}
        {createdReceivable && (
          <div className={styles.card}>
            <h2 className={styles.cardTitle}>2. Liquidar recebível</h2>
            <div className={styles.receivableInfo}>
              <span>Recebível #{createdReceivable.id}</span>
              <Badge variant={createdReceivable.status === 'SETTLED' ? 'success' : 'warning'}>
                {RECEIVABLE_STATUS_LABELS[createdReceivable.status]}
              </Badge>
            </div>

            {settlement.isError && (
              <p className={styles.errorBox}>
                {extractApiError(settlement.error)?.message ?? 'Erro ao liquidar.'}
              </p>
            )}

            {settledResult ? (
              <div className={styles.successBox}>
                <p><strong>Liquidação confirmada</strong> (#{settledResult.id})</p>
                <dl className={styles.resultGrid}>
                  <div>
                    <dt>Valor final</dt>
                    <dd className={styles.highlight}>
                      {formatCurrency(settledResult.finalAmount, settledResult.paymentCurrency)}
                    </dd>
                  </div>
                  <div><dt>VP (BRL)</dt><dd>{formatCurrency(settledResult.presentValueBrl, PaymentCurrency.BRL)}</dd></div>
                  <div><dt>Deságio</dt><dd>{formatCurrency(settledResult.discountAmount, PaymentCurrency.BRL)}</dd></div>
                  <div><dt>Liquidado em</dt><dd>{formatDateTime(settledResult.settledAt)}</dd></div>
                </dl>
              </div>
            ) : (
              <div className={styles.actions}>
                <Button type="button" onClick={onSettle} loading={settlement.isLoading}>
                  Liquidar em {createdReceivable.paymentCurrency}
                </Button>
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  )
}
