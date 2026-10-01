package com.srm.creditengine.controller;

import com.srm.creditengine.dto.request.ExchangeRateRequest;
import com.srm.creditengine.dto.response.ExchangeRateResponse;
import com.srm.creditengine.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de gestão de taxas de câmbio.
 *
 * POST /api/exchange-rates        → cadastra nova taxa
 * GET  /api/exchange-rates/latest/{pair} → retorna taxa vigente atual
 */
@RestController
@RequestMapping("/api/exchange-rates")
@Tag(name = "Exchange Rates", description = "Gestão de taxas de câmbio")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastrar taxa de câmbio",
            description = "Registra uma nova taxa de câmbio vigente. Para atualizar a taxa, cadastre um novo registro com effectiveAt mais recente."
    )
    public ExchangeRateResponse create(@Valid @RequestBody ExchangeRateRequest request) {
        return ExchangeRateResponse.from(exchangeRateService.create(request));
    }

    @GetMapping("/latest/{currencyPair}")
    @Operation(
            summary = "Consultar taxa vigente",
            description = "Retorna a taxa de câmbio mais recente vigente no momento da consulta para o par informado (ex: USD/BRL)."
    )
    public ExchangeRateResponse getLatest(@PathVariable String currencyPair) {
        return ExchangeRateResponse.from(
                exchangeRateService.getLatestRate(currencyPair));
    }
}
