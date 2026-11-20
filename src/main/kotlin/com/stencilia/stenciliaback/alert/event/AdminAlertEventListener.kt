package com.stencilia.stenciliaback.alert.event

import com.stencilia.stenciliaback.alert.repository.TestAccountRepository
import com.stencilia.stenciliaback.alert.service.AdminAlertService
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
        alert(event.email, "🎨 New artist signed up — ${event.slug}")
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: RequestValidatedEvent) {
        alert(event.clientEmail, "📋 New tattoo request — ${event.slug}")
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: SupportMessageSubmittedEvent) {
        alert(event.artistEmail, "🛟 Support (${event.type}) — ${event.slug}")
    }

    private fun alert(email: String, message: String) {
        if (testAccountRepository.existsByEmailIgnoreCase(email)) return
        runCatching { adminAlertService.send(message) }
            .onFailure { logger.warn("Failed to dispatch admin alert", it) }
    }
}
