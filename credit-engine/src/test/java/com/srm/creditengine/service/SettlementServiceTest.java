package com.srm.creditengine.service;

import com.srm.creditengine.config.PricingProperties;
import com.srm.creditengine.domain.entity.ExchangeRate;
import com.srm.creditengine.domain.entity.Receivable;
import com.srm.creditengine.domain.entity.Settlement;
import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.domain.enums.ReceivableStatus;
import com.srm.creditengine.domain.enums.ReceivableType;
import com.srm.creditengine.dto.request.SettlementRequest;
import com.srm.creditengine.exception.ReceivableAlreadySettledException;
import com.srm.creditengine.exception.ReceivableNotFoundException;
import com.srm.creditengine.pricing.ChequePreDatadoStrategy;
import com.srm.creditengine.pricing.DuplicataMercantilStrategy;
import com.srm.creditengine.pricing.PricingStrategyFactory;
import com.srm.creditengine.repository.ReceivableRepository;
import com.srm.creditengine.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SettlementServiceTest {

    private SettlementService settlementService;
    private ReceivableRepository receivableRepository;
    private SettlementRepository settlementRepository;
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        receivableRepository = mock(ReceivableRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        exchangeRateService  = mock(ExchangeRateService.class);

        PricingProperties properties = new PricingProperties();
        properties.setBaseRate(new BigDecimal("0.01"));

        PricingStrategyFactory factory = new PricingStrategyFactory(List.of(
                new DuplicataMercantilStrategy(),
                new ChequePreDatadoStrategy()
        ));

        PricingService pricingService = new PricingService(factory, properties);

        settlementService = new SettlementService(
                receivableRepository,
                settlementRepository,
                pricingService,
                exchangeRateService
        );
    }

    // =========================================================================
    // GOLDEN CASE C3 — conversão cross-currency USD
    // =========================================================================

    @Nested
    @DisplayName("Golden Case C3 — cross-currency USD")
    class GoldenCaseC3 {

        @Test
        @DisplayName("C3: Duplicata R$ 100.000, 3 meses, USD/BRL 5.4321 → US$ 17.094,67")
        void c3_duplicata_usd() {
            Receivable receivable = buildReceivable(
                    new BigDecimal("100000.00"), 3,
                    ReceivableType.DUPLICATA_MERCANTIL, PaymentCurrency.USD);

            ExchangeRate rate = ExchangeRate.builder()
                    .currencyPair("USD/BRL")
                    .rate(new BigDecimal("5.4321"))
                    .effectiveAt(LocalDateTime.now().minusMinutes(1))
                    .build();

            when(settlementRepository.findByIdempotencyKey(anyString()))
                    .thenReturn(Optional.empty());
            when(receivableRepository.findById(1L))
                    .thenReturn(Optional.of(receivable));
            when(settlementRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(receivableRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(exchangeRateService.getLatestRateAt(anyString(), any()))
                    .thenReturn(rate);

            SettlementService.SettlementResult result = settlementService.settle(
                    new SettlementRequest(1L, PaymentCurrency.USD),
                    "test-key-c3"
            );

            assertThat(result.created()).isTrue();
            assertThat(result.settlement().getFinalAmount())
                    .as("Golden case C3: US$ 17.094,67")
                    .isEqualByComparingTo(new BigDecimal("17094.67"));
            assertThat(result.settlement().getPresentValueBrl())
                    .as("VP em BRL deve ser R$ 92.859,94 antes da conversão")
                    .isEqualByComparingTo(new BigDecimal("92859.94"));
            assertThat(result.settlement().getExchangeRateUsed())
                    .isEqualByComparingTo(new BigDecimal("5.4321"));
        }
    }

    // =========================================================================
    // IDEMPOTÊNCIA
    // =========================================================================

    @Nested
    @DisplayName("Idempotência")
    class Idempotency {

        @Test
        @DisplayName("Segunda chamada com mesma idempotency-key retorna resultado existente sem reprocessar")
        void segunda_chamada_retorna_existente() {
            Settlement existing = mock(Settlement.class);

            when(settlementRepository.findByIdempotencyKey("key-existente"))
                    .thenReturn(Optional.of(existing));

            SettlementService.SettlementResult result = settlementService.settle(
                    new SettlementRequest(1L, PaymentCurrency.BRL),
                    "key-existente"
            );

            assertThat(result.created()).isFalse();
            assertThat(result.settlement()).isSameAs(existing);

            // Não deve consultar o banco de recebíveis nem salvar nada
            verifyNoInteractions(receivableRepository);
            verify(settlementRepository, never()).save(any());
        }
    }

    // =========================================================================
    // VALIDAÇÕES DE NEGÓCIO
    // =========================================================================

    @Nested
    @DisplayName("Validações de negócio")
    class BusinessValidations {

        @Test
        @DisplayName("Recebível inexistente lança ReceivableNotFoundException")
        void receivable_nao_encontrado() {
            when(settlementRepository.findByIdempotencyKey(anyString()))
                    .thenReturn(Optional.empty());
            when(receivableRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> settlementService.settle(
                    new SettlementRequest(99L, PaymentCurrency.BRL), "key-1"))
                    .isInstanceOf(ReceivableNotFoundException.class);
        }

        @Test
        @DisplayName("Recebível já liquidado lança ReceivableAlreadySettledException")
        void receivable_ja_liquidado() {
            Receivable settled = buildReceivable(
                    new BigDecimal("10000.00"), 1,
                    ReceivableType.DUPLICATA_MERCANTIL, PaymentCurrency.BRL);
            settled.settle(); // marca como SETTLED

            when(settlementRepository.findByIdempotencyKey(anyString()))
                    .thenReturn(Optional.empty());
            when(receivableRepository.findById(1L))
                    .thenReturn(Optional.of(settled));

            assertThatThrownBy(() -> settlementService.settle(
                    new SettlementRequest(1L, PaymentCurrency.BRL), "key-2"))
                    .isInstanceOf(ReceivableAlreadySettledException.class);
        }

        @Test
        @DisplayName("Liquidação BRL não consulta taxa de câmbio")
        void liquidacao_brl_nao_consulta_cambio() {
            Receivable receivable = buildReceivable(
                    new BigDecimal("25000.00"), 2,
                    ReceivableType.CHEQUE_PRE_DATADO, PaymentCurrency.BRL);

            when(settlementRepository.findByIdempotencyKey(anyString()))
                    .thenReturn(Optional.empty());
            when(receivableRepository.findById(1L))
                    .thenReturn(Optional.of(receivable));
            when(settlementRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(receivableRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            settlementService.settle(
                    new SettlementRequest(1L, PaymentCurrency.BRL), "key-3");

            verifyNoInteractions(exchangeRateService);
        }

        @Test
        @DisplayName("Liquidação BRL — deságio e VP corretos (golden case C2)")
        void liquidacao_brl_golden_case_c2() {
            Receivable receivable = buildReceivable(
                    new BigDecimal("25000.00"), 2,
                    ReceivableType.CHEQUE_PRE_DATADO, PaymentCurrency.BRL);

            when(settlementRepository.findByIdempotencyKey(anyString()))
                    .thenReturn(Optional.empty());
            when(receivableRepository.findById(1L))
                    .thenReturn(Optional.of(receivable));
            when(settlementRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(receivableRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            SettlementService.SettlementResult result = settlementService.settle(
                    new SettlementRequest(1L, PaymentCurrency.BRL), "key-c2");

            assertThat(result.settlement().getFinalAmount())
                    .isEqualByComparingTo(new BigDecimal("23337.77"));
            assertThat(result.settlement().getDiscountAmount())
                    .isEqualByComparingTo(new BigDecimal("1662.23"));
            assertThat(result.settlement().getExchangeRateUsed()).isNull();
        }
    }

    // =========================================================================
    // helpers
    // =========================================================================

    private Receivable buildReceivable(BigDecimal faceValue, int term,
                                       ReceivableType type, PaymentCurrency currency) {
        return Receivable.builder()
                .id(1L)
                .cedente("Empresa Teste")
                .documentNumber("DOC-001")
                .type(type)
                .faceValue(faceValue)
                .termInMonths(term)
                .dueDate(LocalDate.now().plusMonths(term))
                .paymentCurrency(currency)
                .status(ReceivableStatus.PENDING)
                .build();
    }
}
