package com.stencilia.stenciliaback.common

import com.stencilia.stenciliaback.alert.service.AdminAlertService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Lowest-precedence catch-all: fires a Telegram alert on every server 5xx (BR-1c of #87).
 *
 * It is consulted only after every specific advice, and extends [ResponseEntityExceptionHandler]
 * so Spring's framework exceptions (404/405/415/400) keep their proper 4xx status and never reach
 * the generic handler below — so bot/user errors raise no alert (BR-4). Test accounts are not
 * filtered here (BR-5). The email-failure alert lives at its own source (ResendEmailClient), and
 * EmailDeliveryException has a specific 503 handler, so the two never double-alert.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class UnhandledExceptionAdvice(
    private val adminAlertService: AdminAlertService,
) : ResponseEntityExceptionHandler() {

    @ExceptionHandler(Exception::class)
    fun handleUnhandled(e: Exception, request: HttpServletRequest): ResponseEntity<Map<String, String?>> {
        // BR-3: endpoint = method + path only (no query string, which may carry a token); error type = exception class.
        adminAlertService.send("💥 500 ${request.method} ${request.requestURI} — ${e.javaClass.simpleName}")
        logger.warn("⚠ Unhandled server exception ⚠", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("error" to "Internal server error"))
    }
}
