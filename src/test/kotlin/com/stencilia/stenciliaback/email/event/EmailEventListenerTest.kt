package com.stencilia.stenciliaback.email.event

import com.stencilia.stenciliaback.appointment.entity.Appointment
import com.stencilia.stenciliaback.appointment.entity.TattooStyle
import com.stencilia.stenciliaback.appointment.event.AppointmentLinksRequested
import com.stencilia.stenciliaback.appointment.event.AppointmentRequestSubmittedEvent
import com.stencilia.stenciliaback.appointment.event.AppointmentRequestVerifiedEvent
import com.stencilia.stenciliaback.artist.entity.TattooArtist
import com.stencilia.stenciliaback.auth.artist.event.ArtistVerificationRequestedEvent
import com.stencilia.stenciliaback.auth.artist.event.PasswordResetRequestedEvent
import com.stencilia.stenciliaback.client.entity.TattooClient
import com.stencilia.stenciliaback.email.service.EmailService
import com.stencilia.stenciliaback.support.entity.SupportMessage
import com.stencilia.stenciliaback.support.entity.SupportMessageType
import com.stencilia.stenciliaback.support.event.SupportMessageSubmittedEvent
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class EmailEventListenerTest {

    @Mock
    private lateinit var emailService: EmailService

    @InjectMocks
    private lateinit var listener: EmailEventListener

    @Test
    fun `on ArtistVerificationRequestedEvent delegates to email service`() {
        listener.on(ArtistVerificationRequestedEvent(to = "user@example.com", artistName = "Test Artist", token = "abc123"))

        verify(emailService).sendArtistVerificationEmail("user@example.com", "Test Artist", "abc123")
    }

    @Test
    fun `on PasswordResetRequestedEvent delegates to email service`() {
        listener.on(PasswordResetRequestedEvent(to = "user@example.com", artistName = "Test Artist", token = "reset-token-123"))

        verify(emailService).sendPasswordResetEmail("user@example.com", "Test Artist", "reset-token-123")
    }

    @Test
    fun `on AppointmentRequestSubmittedEvent delegates to email service`() {
        val appointment = buildAppointment()

        listener.on(AppointmentRequestSubmittedEvent(appointment))

        verify(emailService).sendAppointmentVerificationEmail(appointment)
    }

    @Test
    fun `on AppointmentRequestVerifiedEvent sends artist notification and client confirmation`() {
        val appointment = buildAppointment()

        listener.on(AppointmentRequestVerifiedEvent(appointment))

        verify(emailService).sendAppointmentNotificationEmail(appointment)
        verify(emailService).sendAppointmentRequestConfirmationEmail(appointment)
    }

    @Test
    fun `on AppointmentLinksRequested delegates to email service`() {
        val appointments = listOf(buildAppointment())

        listener.on(AppointmentLinksRequested(appointments))

        verify(emailService).sendAppointmentLinksEmail(appointments)
    }

    @Test
    fun `on SupportMessageSubmittedEvent sends support notification and artist confirmation`() {
        val message = SupportMessage(
            artist = buildArtist(),
            type = SupportMessageType.BUG,
            message = "Something is broken",
            createdAt = Instant.now(),
        )

        listener.on(SupportMessageSubmittedEvent(message))

        verify(emailService).sendSupportMessageReceivedEmail(message)
        verify(emailService).sendSupportMessageConfirmationEmail("artist@test.com", "Test Artist")
    }

    private fun buildArtist() = TattooArtist(
        id = UUID.randomUUID(),
        email = "artist@test.com",
        password = "hashed",
        artistName = "Test Artist",
        slug = "test-artist",
        city = "Test City",
        countryCode = "FR",
        registeredAt = Instant.now(),
        activatedAt = Instant.now()
    )

    private fun buildAppointment(): Appointment {
        val client = TattooClient(email = "client@test.com", firstName = "Jane", lastName = "Doe")
        return Appointment(
            artist = buildArtist(),
            client = client,
            tattooDescription = "A beautiful dragon tattoo",
            tattooPlacement = "forearm",
            tattooSize = "10x10cm",
            firstTattoo = false,
            coverUp = false,
            color = false,
            style = TattooStyle.REALISM
        )
    }
}
