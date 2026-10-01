package com.srm.creditengine.pricing;

import com.srm.creditengine.config.PricingProperties;
import com.srm.creditengine.domain.enums.ReceivableType;
import com.srm.creditengine.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários do motor de precificação.
 *
 * Os golden cases (C1, C2, C3) validam ao centavo os valores esperados
 * definidos no enunciado — qualquer desvio indica problema de precisão numérica.
 *
 * Premissas fixas dos golden cases (independentes do SPEC.md):
 *  - Taxa base: 1,00% a.m.
 *  - Prazo em meses inteiros
 *  - Arredondamento: half-even, 2 casas, somente no resultado final
 */
class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        PricingProperties properties = new PricingProperties();
        properties.setBaseRate(new BigDecimal("0.01")); // 1,00% a.m.

        PricingStrategyFactory factory = new PricingStrategyFactory(List.of(
                new DuplicataMercantilStrategy(),
                new ChequePreDatadoStrategy()
        ));

        pricingService = new PricingService(factory, properties);
    }

    // =========================================================================
    // GOLDEN CASES — aferição obrigatória ao centavo
    // =========================================================================

    @Nested
    @DisplayName("Golden Cases")
    class GoldenCases {

        @Test
        @DisplayName("C1 — Duplicata Mercantil, R$ 100.000,00, 3 meses, BRL → R$ 92.859,94")
        void c1_duplicata_3meses_brl() {
            BigDecimal faceValue = new BigDecimal("100000.00");

            PricingResult result = pricingService.calculate(faceValue, 3, ReceivableType.DUPLICATA_MERCANTIL);

            assertThat(result.presentValueBrl())
                    .as("Valor presente deve ser exatamente R$ 92.859,94")
                    .isEqualByComparingTo(new BigDecimal("92859.94"));

            assertThat(result.discountAmount())
                    .as("Deságio deve ser exatamente R$ 7.140,06")
                    .isEqualByComparingTo(new BigDecimal("7140.06"));
        }

        @Test
        @DisplayName("C2 — Cheque Pré-datado, R$ 25.000,00, 2 meses, BRL → R$ 23.337,77")
        void c2_cheque_2meses_brl() {
            BigDecimal faceValue = new BigDecimal("25000.00");

            PricingResult result = pricingService.calculate(faceValue, 2, ReceivableType.CHEQUE_PRE_DATADO);

            assertThat(result.presentValueBrl())
                    .as("Valor presente deve ser exatamente R$ 23.337,77")
                    .isEqualByComparingTo(new BigDecimal("23337.77"));

            assertThat(result.discountAmount())
                    .as("Deságio deve ser exatamente R$ 1.662,23")
                    .isEqualByComparingTo(new BigDecimal("1662.23"));
        }

        @Test
        @DisplayName("C3 — Duplicata Mercantil, R$ 100.000,00, 3 meses → VP BRL R$ 92.859,94 (conversão USD é responsabilidade do SettlementService)")
        void c3_duplicata_3meses_vpBrl() {
            // O PricingService só calcula o VP em BRL.
            // A conversão cross-currency (÷ 5,4321 → US$ 17.094,67) é feita pelo SettlementService.
            // Este teste valida que o VP em BRL está correto antes da conversão.
            BigDecimal faceValue = new BigDecimal("100000.00");

            PricingResult result = pricingService.calculate(faceValue, 3, ReceivableType.DUPLICATA_MERCANTIL);

            assertThat(result.presentValueBrl())
                    .isEqualByComparingTo(new BigDecimal("92859.94"));
        }
    }

    // =========================================================================
    // PARÂMETROS AUDITÁVEIS — values gravados no registro de liquidação
    // =========================================================================

    @Nested
    @DisplayName("Parâmetros auditáveis no resultado")
    class AuditParameters {

        @Test
        @DisplayName("Resultado deve conter baseRate, spread e prazo usados")
        void resultado_contem_parametros_de_auditoria() {
            PricingResult result = pricingService.calculate(
                    new BigDecimal("100000.00"), 3, ReceivableType.DUPLICATA_MERCANTIL);

            assertThat(result.baseRateUsed()).isEqualByComparingTo(new BigDecimal("0.01"));
            assertThat(result.spreadUsed()).isEqualByComparingTo(new BigDecimal("0.015"));
            assertThat(result.termInMonths()).isEqualTo(3);
        }
    }

    // =========================================================================
    // CASOS DE BORDA
    // =========================================================================

    @Nested
    @DisplayName("Casos de borda")
    class EdgeCases {

        @Test
        @DisplayName("Prazo de 1 mês — Duplicata Mercantil")
        void prazo_1_mes() {
            BigDecimal faceValue = new BigDecimal("10000.00");

            PricingResult result = pricingService.calculate(faceValue, 1, ReceivableType.DUPLICATA_MERCANTIL);

            // VP = 10000 / (1 + 0.01 + 0.015)^1 = 10000 / 1.025 = 9756.097560...
            // Arredondado HALF_EVEN 2 casas → 9756.10
            assertThat(result.presentValueBrl())
                    .isEqualByComparingTo(new BigDecimal("9756.10"));
        }

        @Test
        @DisplayName("Prazo de 1 mês — Cheque Pré-datado")
        void prazo_1_mes_cheque() {
            BigDecimal faceValue = new BigDecimal("10000.00");

            PricingResult result = pricingService.calculate(faceValue, 1, ReceivableType.CHEQUE_PRE_DATADO);

            // VP = 10000 / (1 + 0.01 + 0.025)^1 = 10000 / 1.035 = 9661.835748...
            // Arredondado HALF_EVEN 2 casas → 9661.84
            assertThat(result.presentValueBrl())
                    .isEqualByComparingTo(new BigDecimal("9661.84"));
        }

        @Test
        @DisplayName("Deságio é sempre positivo — VP < faceValue")
        void desagio_sempre_positivo() {
            PricingResult result = pricingService.calculate(
                    new BigDecimal("50000.00"), 6, ReceivableType.CHEQUE_PRE_DATADO);

            assertThat(result.discountAmount()).isPositive();
            assertThat(result.presentValueBrl()).isLessThan(new BigDecimal("50000.00"));
        }

        @Test
        @DisplayName("Tipo inválido lança IllegalArgumentException")
        void tipo_invalido_lanca_excecao() {
            // Simula um tipo sem strategy registrada removendo uma delas do factory
            PricingStrategyFactory factoryIncompleta = new PricingStrategyFactory(
                    List.of(new DuplicataMercantilStrategy())
            );
            PricingService serviceParcial = new PricingService(
                    factoryIncompleta, pricingService_properties());

            assertThatThrownBy(() ->
                    serviceParcial.calculate(
                            new BigDecimal("1000.00"), 1, ReceivableType.CHEQUE_PRE_DATADO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CHEQUE_PRE_DATADO");
        }

        @Test
        @DisplayName("Spread Duplicata é 0.015 e Cheque é 0.025")
        void spreads_corretos() {
            assertThat(new DuplicataMercantilStrategy().getSpread())
                    .isEqualByComparingTo(new BigDecimal("0.015"));
            assertThat(new ChequePreDatadoStrategy().getSpread())
                    .isEqualByComparingTo(new BigDecimal("0.025"));
        }
    }

    // helper para evitar duplicação
    private PricingProperties pricingService_properties() {
        PricingProperties p = new PricingProperties();
        p.setBaseRate(new BigDecimal("0.01"));
        return p;
    }
}
