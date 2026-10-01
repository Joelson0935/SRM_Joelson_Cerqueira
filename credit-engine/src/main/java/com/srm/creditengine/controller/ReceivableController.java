package com.srm.creditengine.controller;

import com.srm.creditengine.dto.request.ReceivableRequest;
import com.srm.creditengine.dto.response.ReceivableResponse;
import com.srm.creditengine.service.ReceivableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de gestão de recebíveis.
 *
 * POST /api/receivables       → cadastra recebível
 * GET  /api/receivables       → lista com paginação server-side
 * GET  /api/receivables/{id}  → busca por ID
 */
@RestController
@RequestMapping("/api/receivables")
@Tag(name = "Receivables", description = "Gestão de recebíveis")
public class ReceivableController {

    private final ReceivableService receivableService;

    public ReceivableController(ReceivableService receivableService) {
        this.receivableService = receivableService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar recebível")
    public ReceivableResponse create(@Valid @RequestBody ReceivableRequest request) {
        return ReceivableResponse.from(receivableService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar recebíveis", description = "Paginação server-side. Params: page, size, sort.")
    public Page<ReceivableResponse> findAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return receivableService.findAll(pageable).map(ReceivableResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar recebível por ID")
    public ReceivableResponse findById(@PathVariable Long id) {
        return ReceivableResponse.from(receivableService.findById(id));
    }
}
