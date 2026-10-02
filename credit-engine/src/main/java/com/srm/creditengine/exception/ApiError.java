package com.srm.creditengine.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estrutura padronizada para todas as respostas de erro da API.
 *
 * code      → código de erro legível por máquina (ex: "RECEIVABLE_NOT_FOUND")
 * message   → mensagem legível por humano
 * timestamp → momento do erro
 * details   → lista de erros de validação (campo + mensagem), presente apenas em 422
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        LocalDateTime timestamp,
        List<FieldError> details
) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, LocalDateTime.now(), null);
    }

    public static ApiError of(String code, String message, List<FieldError> details) {
        return new ApiError(code, message, LocalDateTime.now(), details);
    }

    /**
     * Detalhe de um erro de validação de campo.
     */
    public record FieldError(String field, String message) {}
}
