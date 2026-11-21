package com.stencilia.stenciliaback.common

import com.stencilia.stenciliaback.alert.service.AdminAlertService
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus

class UnhandledExceptionAdviceTest {

    private val adminAlertService = mock<AdminAlertService>()
    private val request = mock<HttpServletRequest>()

    @Test
    fun `maps an unmapped exception to 500 and alerts with method, path and exception type`() {
        whenever(request.method).thenReturn("GET")
        whenever(request.requestURI).thenReturn("/artists/jane-ink")
        val advice = UnhandledExceptionAdvice(adminAlertService)

        val response = advice.handleUnhandled(RuntimeException("boom"), request)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        verify(adminAlertService).send("💥 500 on GET /artists/jane-ink: RuntimeException")
    }
}
