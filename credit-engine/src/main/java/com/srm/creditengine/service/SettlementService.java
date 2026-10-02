package com.srm.creditengine.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.srm.creditengine.domain.entity.ExchangeRate;
import com.srm.creditengine.domain.entity.Receivable;
import com.srm.creditengine.domain.entity.Settlement;
import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.dto.request.SettlementRequest;
import com.srm.creditengine.exception.ReceivableAlreadySettledException;
import com.srm.creditengine.exception.ReceivableNotFoundException;
import com.srm.creditengine.pricing.PricingResult;
import com.srm.creditengine.repository.ReceivableRepository;
import com.srm.creditengine.repository.SettlementRepository;

/**
 * Orquestra o fluxo completo de liquidação de um recebível.
 *
 * Garantias implementadas:
 *
 * 1. IDEMPOTÊNCIA: se a mesma idempotency-key chegar novamente (retry de rede,
 *    duplo clique), retorna o resultado original sem reprocessar.
 *
 * 2. ACID: o INSERT em settlements e o UPDATE em receivables ocorrem na mesma
 *    transação — ou ambos persistem, ou nenhum persiste.
 *
 * 3. UNICIDADE: a constraint uq_settlements_receivable_id no banco garante
 *    que um recebível nunca seja liquidado duas vezes, mesmo em race conditions.
 *
 * 4. AUDITORIA: todos os parâmetros do cálculo (taxa base, spread, câmbio,
 *    prazo) são gravados no registro de liquidação no momento da operação.
 */
@Service
public class SettlementService {

    private static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_EVEN);
    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    // Par de moedas padrão para conversão USD
    private static final String USD_BRL_PAIR = "USD/BRL";

    private final ReceivableRepository receivableRepository;
    private final SettlementRepository settlementRepository;
    private final PricingService pricingService;
    private final ExchangeRateService exchangeRateService;

    public SettlementService(ReceivableRepository receivableRepository,
                             SettlementRepository settlementRepository,
                             PricingService pricingService,
                             ExchangeRateService exchangeRateService) {
        this.receivableRepository = receivableRepository;
        this.settlementRepository = settlementRepository;
        this.pricingService = pricingService;
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * Processa a liquidação de um recebível.
     *
     * @param request        dados da liquidação (receivableId, currency)
     * @param idempotencyKey chave única fornecida pelo cliente (header Idempotency-Key)
     * @return par contendo o Settlement e um flag indicando se foi criado agora (true) ou já existia (false)
     */
    @Transactional
    public SettlementResult settle(SettlementRequest request, String idempotencyKey) {

        // 1. IDEMPOTÊNCIA: verifica se já existe liquidação para esta key
        Optional<Settlement> existing = settlementRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return new SettlementResult(existing.get(), false);
        }

        // 2. Busca o recebível — lança 404 se não encontrado
        Receivable receivable = receivableRepository.findById(request.receivableId())
                .orElseThrow(() -> new ReceivableNotFoundException(request.receivableId()));

        // 3. Valida que o recebível ainda não foi liquidado — lança 409 se já foi
        if (receivable.getStatus() == com.srm.creditengine.domain.enums.ReceivableStatus.SETTLED) {
            throw new ReceivableAlreadySettledException(receivable.getId());
        }

        // 4. Calcula o valor presente em BRL via motor de precificação
        PricingResult pricing = pricingService.calculate(
                receivable.getFaceValue(),
                receivable.getTermInMonths(),
                receivable.getType()
        );

        // 5. Resolve o valor final e taxa de câmbio (se cross-currency)
        LocalDateTime settlementTime = LocalDateTime.now();
        BigDecimal finalAmount;
        BigDecimal exchangeRateUsed = null;

        if (request.currency() == PaymentCurrency.USD) {
            // Busca taxa vigente no momento da liquidação — gravada para auditoria
            ExchangeRate exchangeRate = exchangeRateService
                    .getLatestRateAt(USD_BRL_PAIR, settlementTime);
            exchangeRateUsed = exchangeRate.getRate();

            // Conversão: VP em BRL já arredondado ÷ taxa → USD, arredondado HALF_EVEN
            // Conforme SPEC.md §2.3: converte o VP BRL já arredondado
            finalAmount = pricing.presentValueBrl()
                    .divide(exchangeRateUsed, MATH_CONTEXT)
                    .setScale(MONETARY_SCALE, ROUNDING_MODE);
        } else {
            finalAmount = pricing.presentValueBrl();
        }

        // 6. Persiste o registro de liquidação (imutável)
        Settlement settlement = Settlement.builder()
                .receivable(receivable)
                .receivableType(receivable.getType())
                .faceValue(receivable.getFaceValue())
                .presentValueBrl(pricing.presentValueBrl())
                .finalAmount(finalAmount)
                .discountAmount(pricing.discountAmount())
                .paymentCurrency(request.currency())
                .baseRateUsed(pricing.baseRateUsed())
                .spreadUsed(pricing.spreadUsed())
                .termInMonthsUsed(pricing.termInMonths())
                .exchangeRateUsed(exchangeRateUsed)
                .idempotencyKey(idempotencyKey)
                .settledAt(settlementTime)
                .build();

        Settlement saved = settlementRepository.save(settlement);

        // 7. Atualiza status do recebível para SETTLED
        // Ambas as operações estão na mesma @Transactional — ACID garantido
        receivable.settle();
        receivableRepository.save(receivable);

        return new SettlementResult(saved, true);
    }

    /**
     * Extrato analítico de liquidações com filtros opcionais e paginação server-side.
     * Nunca carrega toda a tabela em memória — a query paginada vai direto ao banco.
     *
     * @param from     início do período (inclusivo), null = sem limite inferior
     * @param to       fim do período (inclusivo), null = sem limite superior
     * @param cedente  nome do cedente, null = todos
     * @param currency moeda de pagamento, null = todas
     * @param pageable configuração de paginação e ordenação
     */
    @Transactional(readOnly = true)
    public Page<Settlement> findByFilters(LocalDateTime from,
                                          LocalDateTime to,
                                          String cedente,
                                          PaymentCurrency currency,
                                          Pageable pageable) {
        return settlementRepository.findByFilters(from, to, cedente, currency, pageable);
    }

    /**
     * Busca uma liquidação pelo ID.
     */
    @Transactional(readOnly = true)
    public Settlement findById(Long id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + id));
    }

    /**
     * Resultado do processo de liquidação.
     *
     * @param settlement o registro de liquidação (novo ou existente)
     * @param created    true se foi criado agora, false se já existia (idempotente)
     */
    public record SettlementResult(Settlement settlement, boolean created) {}
}
