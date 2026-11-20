package com.stencilia.stenciliaback.email.event

import com.stencilia.stenciliaback.appointment.event.AppointmentRequestSubmittedEvent
import com.stencilia.stenciliaback.appointment.event.AppointmentLinksRequested
import com.stencilia.stenciliaback.appointment.event.AppointmentRequestVerifiedEvent
import com.stencilia.stenciliaback.auth.artist.event.ArtistVerificationRequestedEvent
import com.stencilia.stenciliaback.auth.artist.event.PasswordChangedEvent
import com.stencilia.stenciliaback.auth.artist.event.PasswordResetRequestedEvent
import com.stencilia.stenciliaback.auth.client.event.ClientAccessCodeRequested
import com.stencilia.stenciliaback.discussion.event.ArtistMessagePostedEvent
import com.stencilia.stenciliaback.discussion.event.ClientMessagePostedEvent
import com.stencilia.stenciliaback.email.service.EmailService
import com.stencilia.stenciliaback.support.event.SupportMessageSubmittedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EmailEventListener(
    private val emailService: EmailService
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ArtistVerificationRequestedEvent) {
        emailService.sendArtistVerificationEmail(event.to, event.artistName, event.token)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PasswordResetRequestedEvent) {
        emailService.sendPasswordResetEmail(event.to, event.artistName, event.token)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PasswordChangedEvent) {
        emailService.sendPasswordChangedEmail(event.to, event.artistName)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: AppointmentRequestSubmittedEvent) {
        emailService.sendAppointmentVerificationEmail(event.appointment)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: AppointmentRequestVerifiedEvent) {
        emailService.sendAppointmentNotificationEmail(event.appointment)
        emailService.sendAppointmentRequestConfirmationEmail(event.appointment)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: AppointmentLinksRequested) {
        emailService.sendAppointmentLinksEmail(event.appointments)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ArtistMessagePostedEvent) {
        emailService.sendNewMessageEmailToClient(event.appointment)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ClientMessagePostedEvent) {
        emailService.sendNewMessageEmailToArtist(event.appointment)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: SupportMessageSubmittedEvent) {
        emailService.sendSupportMessageReceivedEmail(event.supportMessage)
        emailService.sendSupportMessageConfirmationEmail(
            event.supportMessage.artist.email,
            event.supportMessage.artist.artistName,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ClientAccessCodeRequested) {
        emailService.sendClientAccessCodeEmail(event.to, event.clientFirstName, event.code)
    }
}
