package com.srm.creditengine.controller;

import com.srm.creditengine.domain.enums.PaymentCurrency;
import com.srm.creditengine.dto.request.SettlementRequest;
import com.srm.creditengine.dto.response.SettlementResponse;
import com.srm.creditengine.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Endpoints de liquidação e extrato.
 *
 * POST /api/settlements                  → liquida recebível (idempotente via header)
 * GET  /api/settlements                  → extrato com filtros e paginação server-side
 * GET  /api/settlements/{id}             → detalhe de uma liquidação
 */
@RestController
@RequestMapping("/api/settlements")
@Tag(name = "Settlements", description = "Liquidação e extrato de recebíveis")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    @Operation(
            summary = "Liquidar recebível",
            description = """
                    Processa a liquidação de um recebível.

                    **Idempotência:** envie um UUID único no header `Idempotency-Key`.
                    Retentativas com a mesma key retornam o resultado original (HTTP 200)
                    sem gerar uma segunda liquidação.

                    Retorna 201 para nova liquidação, 200 para retentativa idempotente.
                    """
    )
    public ResponseEntity<SettlementResponse> settle(
            @Parameter(description = "UUID único por tentativa de liquidação", required = true)
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody SettlementRequest request) {

        SettlementService.SettlementResult result =
                settlementService.settle(request, idempotencyKey);

        SettlementResponse response = SettlementResponse.from(result.settlement());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Extrato de liquidações",
            description = """
                    Retorna o histórico de liquidações com filtros opcionais.
                    Paginação server-side — nunca carrega toda a tabela em memória.

                    Query params: `from`, `to` (ISO-8601), `cedente`, `currency` (BRL|USD), `page`, `size`, `sort`.
                    """
    )
    public Page<SettlementResponse> findAll(
            @Parameter(description = "Início do período (ex: 2026-01-01T00:00:00)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "Fim do período (ex: 2026-12-31T23:59:59)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

            @Parameter(description = "Nome do cedente")
            @RequestParam(required = false) String cedente,

            @Parameter(description = "Moeda de pagamento: BRL ou USD")
            @RequestParam(required = false) PaymentCurrency currency,

            @PageableDefault(size = 20, sort = "settledAt") Pageable pageable) {

        return settlementService
                .findByFilters(from, to, cedente, currency, pageable)
                .map(SettlementResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma liquidação")
    public SettlementResponse findById(@PathVariable Long id) {
        return SettlementResponse.from(settlementService.findById(id));
    }
}
