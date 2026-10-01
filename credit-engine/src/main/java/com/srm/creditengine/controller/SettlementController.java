package com.srm.creditengine.controller;

import com.srm.creditengine.dto.request.SettlementRequest;
import com.srm.creditengine.dto.response.SettlementResponse;
import com.srm.creditengine.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint de liquidação de recebíveis.
 *
 * POST /api/settlements
 *
 * Header obrigatório: Idempotency-Key (UUID gerado pelo cliente)
 * Retorna 201 Created para nova liquidação.
 * Retorna 200 OK para retentativa com a mesma Idempotency-Key (idempotente).
 */
@RestController
@RequestMapping("/api/settlements")
@Tag(name = "Settlements", description = "Liquidação de recebíveis")
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

        // 201 Created para nova liquidação, 200 OK para retentativa idempotente
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
