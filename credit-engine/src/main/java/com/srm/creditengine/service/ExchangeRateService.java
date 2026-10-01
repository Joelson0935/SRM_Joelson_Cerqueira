package com.srm.creditengine.service;

import com.srm.creditengine.domain.entity.ExchangeRate;
import com.srm.creditengine.dto.request.ExchangeRateRequest;
import com.srm.creditengine.exception.ExchangeRateNotFoundException;
import com.srm.creditengine.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Gerencia as taxas de câmbio do sistema.
 *
 * Premissa (SPEC.md §1.3): a taxa vigente é a mais recente com
 * effectiveAt <= momento da consulta. Registros são imutáveis —
 * para atualizar a taxa, cadastra-se um novo registro.
 */
@Service
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    /**
     * Cadastra uma nova taxa de câmbio.
     * Se effectiveAt não for informado, usa o momento atual.
     */
    @Transactional
    public ExchangeRate create(ExchangeRateRequest request) {
        LocalDateTime effectiveAt = request.effectiveAt() != null
                ? request.effectiveAt()
                : LocalDateTime.now();

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .currencyPair(request.currencyPair().toUpperCase())
                .rate(request.rate())
                .effectiveAt(effectiveAt)
                .build();

        return exchangeRateRepository.save(exchangeRate);
    }

    /**
     * Retorna a taxa vigente para o par de moedas no momento atual.
     *
     * @throws ExchangeRateNotFoundException se não houver taxa cadastrada
     */
    @Transactional(readOnly = true)
    public ExchangeRate getLatestRate(String currencyPair) {
        return getLatestRateAt(currencyPair, LocalDateTime.now());
    }

    /**
     * Retorna a taxa vigente para o par de moedas em um momento específico.
     * Usado pelo SettlementService para gravar a taxa exata da liquidação.
     *
     * @throws ExchangeRateNotFoundException se não houver taxa vigente no momento informado
     */
    @Transactional(readOnly = true)
    public ExchangeRate getLatestRateAt(String currencyPair, LocalDateTime referenceTime) {
        return exchangeRateRepository
                .findLatestByCurrencyPairAt(currencyPair.toUpperCase(), referenceTime)
                .orElseThrow(() -> new ExchangeRateNotFoundException(currencyPair));
    }

    /**
     * Retorna apenas o valor (BigDecimal) da taxa vigente.
     * Atalho conveniente para o motor de precificação.
     */
    @Transactional(readOnly = true)
    public BigDecimal getLatestRateValue(String currencyPair) {
        return getLatestRate(currencyPair).getRate();
    }
}
