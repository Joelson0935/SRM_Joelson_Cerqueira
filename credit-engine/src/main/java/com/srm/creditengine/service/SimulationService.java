package com.srm.creditengine.service;

import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.dto.request.SimulationRequest;
import com.srm.creditengine.dto.response.PricingSimulationResponse;
import com.srm.creditengine.pricing.PricingResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Simula o valor presente de um recebível sem persistir nada.
 * Usado pelo painel do operador para exibir o VP em tempo real.
 */
@Service
public class SimulationService {

    private static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_EVEN);
    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private static final String USD_BRL_PAIR = "USD/BRL";

    private final PricingService pricingService;
    private final ExchangeRateService exchangeRateService;

    public SimulationService(PricingService pricingService,
                             ExchangeRateService exchangeRateService) {
        this.pricingService = pricingService;
        this.exchangeRateService = exchangeRateService;
    }

    /**
     * Calcula o valor presente sem persistir — apenas para visualização.
     * Para USD, busca a taxa vigente atual (não a da data de liquidação futura).
     */
    public PricingSimulationResponse simulate(SimulationRequest request) {
        PricingResult pricing = pricingService.calculate(
                request.faceValue(),
                request.termInMonths(),
                request.type()
        );

        BigDecimal finalAmount;
        BigDecimal exchangeRateUsed = null;

        if (request.paymentCurrency() == PaymentCurrency.USD) {
            exchangeRateUsed = exchangeRateService
                    .getLatestRateAt(USD_BRL_PAIR, LocalDateTime.now())
                    .getRate();

            finalAmount = pricing.presentValueBrl()
                    .divide(exchangeRateUsed, MATH_CONTEXT)
                    .setScale(MONETARY_SCALE, ROUNDING_MODE);
        } else {
            finalAmount = pricing.presentValueBrl();
        }

        return new PricingSimulationResponse(
                request.type(),
                request.faceValue(),
                request.termInMonths(),
                request.paymentCurrency(),
                pricing.presentValueBrl(),
                finalAmount,
                request.paymentCurrency(),
                pricing.discountAmount(),
                pricing.baseRateUsed(),
                pricing.spreadUsed(),
                exchangeRateUsed
        );
    }
}
