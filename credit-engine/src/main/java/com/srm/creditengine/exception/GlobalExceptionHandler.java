package com.srm.creditengine.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
/**
 * Handler global de exceções — ponto único de mapeamento de exceção → HTTP.
 *
 * Princípios:
 * - Nenhuma exceção é engolida silenciosamente.
 * - Nenhum erro retorna 200 OK.
 * - Stack traces nunca vazam para o cliente — só são logados internamente.
 * - Erros de validação retornam 422 com lista de campos inválidos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================================
    // 404 — Recurso não encontrado
    // =========================================================================

    @ExceptionHandler(ReceivableNotFoundException.class)
    public ResponseEntity<ApiError> handleReceivableNotFound(ReceivableNotFoundException ex) {
        log.warn("Receivable not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("RECEIVABLE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ExchangeRateNotFoundException.class)
    public ResponseEntity<ApiError> handleExchangeRateNotFound(ExchangeRateNotFoundException ex) {
        log.warn("Exchange rate not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("EXCHANGE_RATE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(SettlementNotFoundException.class)
    public ResponseEntity<ApiError> handleSettlementNotFound(SettlementNotFoundException ex) {
        log.warn("Settlement not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("SETTLEMENT_NOT_FOUND", ex.getMessage()));
    }

    // =========================================================================
    // 409 — Conflito de estado
    // =========================================================================

    @ExceptionHandler(ReceivableAlreadySettledException.class)
    public ResponseEntity<ApiError> handleAlreadySettled(ReceivableAlreadySettledException ex) {
        log.warn("Attempt to settle already settled receivable: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiError.of("RECEIVABLE_ALREADY_SETTLED", ex.getMessage()));
    }

    // =========================================================================
    // 422 — Erro de validação de input (Bean Validation)
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationError(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        log.warn("Validation failed: {} field error(s)", fieldErrors.size());
        return ResponseEntity
                .status(422)
                .body(ApiError.of(
                        "VALIDATION_ERROR",
                        "Request validation failed. Check the 'details' field for specifics.",
                        fieldErrors));
    }

    // =========================================================================
    // 400 — Bad request
    // =========================================================================

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Missing required header: {}", ex.getHeaderName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        "MISSING_HEADER",
                        "Required header is missing: " + ex.getHeaderName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format(
                "Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        log.warn("Type mismatch: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("INVALID_PARAMETER", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("INVALID_REQUEST", ex.getMessage()));
    }

    // =========================================================================
    // 500 — Erro interno (fallback)
    // Stack trace logado internamente, nunca exposto ao cliente.
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred. Please try again later."));
    }
}
