package com.stencilia.stenciliaback.alert.event

import com.stencilia.stenciliaback.alert.repository.TestAccountRepository
import com.stencilia.stenciliaback.alert.service.AdminAlertService
import com.stencilia.stenciliaback.appointment.event.AppointmentRequestVerifiedEvent
import com.stencilia.stenciliaback.auth.artist.event.ArtistVerifiedEvent
import com.stencilia.stenciliaback.support.event.SupportMessageSubmittedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AdminAlertEventListener(
    private val adminAlertService: AdminAlertService,
    private val testAccountRepository: TestAccountRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ArtistVerifiedEvent) {
        alert(listOf(event.email), "🎨 New artist signed up — ${event.slug}")
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: AppointmentRequestVerifiedEvent) {
        val appointment = event.appointment
        // BR-5: a test-account artist OR client suppresses the alert.
        alert(
            listOf(appointment.client.email, appointment.artist.email),
            "📋 New tattoo request — ${appointment.artist.slug}",
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: SupportMessageSubmittedEvent) {
        val message = event.supportMessage
        alert(listOf(message.artist.email), "🛟 Support (${message.type}) — ${message.artist.slug}")
    }

    // Everything is wrapped so neither the test-account lookup nor the send can break the
    // user flow this AFTER_COMMIT listener runs after (architecture: alerting never blocks a flow).
    private fun alert(emails: List<String>, message: String) {
        runCatching {
            if (emails.any { testAccountRepository.existsByEmailIgnoreCase(it) }) return
            adminAlertService.send(message)
        }.onFailure { logger.warn("Failed to dispatch admin alert", it) }
    }
}
