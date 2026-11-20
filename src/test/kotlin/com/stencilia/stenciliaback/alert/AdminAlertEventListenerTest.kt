package com.stencilia.stenciliaback.alert

import com.stencilia.stenciliaback.alert.event.AdminAlertEventListener
import com.stencilia.stenciliaback.alert.event.ArtistVerifiedEvent
import com.stencilia.stenciliaback.alert.event.RequestValidatedEvent
import com.stencilia.stenciliaback.alert.event.SupportMessageSubmittedEvent
import com.stencilia.stenciliaback.alert.repository.TestAccountRepository
import com.stencilia.stenciliaback.alert.service.AdminAlertService
import com.stencilia.stenciliaback.support.entity.SupportMessageType
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class AdminAlertEventListenerTest {

    private val alertService = mock<AdminAlertService>()
    private val testAccountRepository = mock<TestAccountRepository>()
    private val listener = AdminAlertEventListener(alertService, testAccountRepository)

    @Test
    fun `artist verified fires sign-up message`() {
        whenever(testAccountRepository.existsByEmailIgnoreCase(any())).thenReturn(false)

        listener.on(ArtistVerifiedEvent("jane-ink", "jane@x.com"))

        verify(alertService).send("🎨 New artist signed up — jane-ink")
    }

    @Test
    fun `request validated fires new-request message`() {
        whenever(testAccountRepository.existsByEmailIgnoreCase(any())).thenReturn(false)

        listener.on(RequestValidatedEvent("jane-ink", "c@x.com"))

        verify(alertService).send("📋 New tattoo request — jane-ink")
    }

    @Test
    fun `support fires support message with type`() {
        whenever(testAccountRepository.existsByEmailIgnoreCase(any())).thenReturn(false)

        listener.on(SupportMessageSubmittedEvent("jane-ink", SupportMessageType.BUG, "jane@x.com"))

        verify(alertService).send("🛟 Support (BUG) — jane-ink")
    }

    @Test
    fun `test-account email is filtered for each event using the event's own email`() {
        whenever(testAccountRepository.existsByEmailIgnoreCase(any())).thenReturn(true)

        listener.on(ArtistVerifiedEvent("jane-ink", "jane@x.com"))
        listener.on(RequestValidatedEvent("jane-ink", "client@x.com"))
        listener.on(SupportMessageSubmittedEvent("jane-ink", SupportMessageType.HELP, "support@x.com"))

        verify(testAccountRepository).existsByEmailIgnoreCase("jane@x.com")
        verify(testAccountRepository).existsByEmailIgnoreCase("client@x.com")
        verify(testAccountRepository).existsByEmailIgnoreCase("support@x.com")
        verifyNoInteractions(alertService)
    }

    @Test
    fun `listener swallows a send failure`() {
        whenever(testAccountRepository.existsByEmailIgnoreCase(any())).thenReturn(false)
        whenever(alertService.send(any())).thenThrow(RuntimeException("boom"))

        assertDoesNotThrow { listener.on(ArtistVerifiedEvent("jane-ink", "jane@x.com")) }
    }
}
