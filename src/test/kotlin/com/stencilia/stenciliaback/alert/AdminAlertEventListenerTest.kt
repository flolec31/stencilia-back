package com.stencilia.stenciliaback.alert

import com.stencilia.stenciliaback.alert.event.AdminAlertEventListener
import com.stencilia.stenciliaback.alert.repository.TestAccountRepository
import com.stencilia.stenciliaback.alert.service.AdminAlertService
import com.stencilia.stenciliaback.appointment.entity.Appointment
import com.stencilia.stenciliaback.appointment.entity.TattooStyle
import com.stencilia.stenciliaback.appointment.event.AppointmentRequestVerifiedEvent
import com.stencilia.stenciliaback.artist.entity.TattooArtist
import com.stencilia.stenciliaback.auth.artist.event.ArtistVerifiedEvent
import com.stencilia.stenciliaback.client.entity.TattooClient
import com.stencilia.stenciliaback.support.entity.SupportMessage
import com.stencilia.stenciliaback.support.entity.SupportMessageType
import com.stencilia.stenciliaback.support.event.SupportMessageSubmittedEvent
import java.time.Instant
import java.util.UUID
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

        verify(alertService).send("🎨 New artist signed up: jane-ink")
    }

    @Test
    fun `appointment request verified fires new-request message`() {
        whenever(testAccountRepository.existsByEmailIgnoreCase(any())).thenReturn(false)

        listener.on(AppointmentRequestVerifiedEvent(appointment(slug = "jane-ink", clientEmail = "c@x.com")))

        verify(alertService).send("📋 New tattoo request sent to jane-ink")
    }

    @Test
    fun `support submitted fires support message with type`() {
        whenever(testAccountRepository.existsByEmailIgnoreCase(any())).thenReturn(false)

        listener.on(SupportMessageSubmittedEvent(supportMessage(slug = "jane-ink", type = SupportMessageType.BUG)))

        verify(alertService).send("🛟 Support (BUG) from jane-ink")
    }

    @Test
    fun `test-account email is filtered for each event using the event's own email`() {
        whenever(testAccountRepository.existsByEmailIgnoreCase(any())).thenReturn(true)

        listener.on(ArtistVerifiedEvent("jane-ink", "jane@x.com"))
        listener.on(AppointmentRequestVerifiedEvent(appointment(slug = "jane-ink", clientEmail = "client@x.com")))
        listener.on(SupportMessageSubmittedEvent(supportMessage(slug = "jane-ink", artistEmail = "support@x.com")))

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

    @Test
    fun `listener swallows a test-account lookup failure`() {
        whenever(testAccountRepository.existsByEmailIgnoreCase(any())).thenThrow(RuntimeException("db down"))

        assertDoesNotThrow { listener.on(ArtistVerifiedEvent("jane-ink", "jane@x.com")) }
    }

    @Test
    fun `appointment request with a test-account artist sends no alert even when the client is real`() {
        whenever(testAccountRepository.existsByEmailIgnoreCase("client@x.com")).thenReturn(false)
        whenever(testAccountRepository.existsByEmailIgnoreCase("artist-test@x.com")).thenReturn(true)

        listener.on(
            AppointmentRequestVerifiedEvent(
                appointment(slug = "jane-ink", clientEmail = "client@x.com", artistEmail = "artist-test@x.com")
            )
        )

        verifyNoInteractions(alertService)
    }

    private fun artist(slug: String, email: String) = TattooArtist(
        id = UUID.randomUUID(),
        email = email,
        password = "hashed",
        artistName = "Jane Ink",
        slug = slug,
        city = "Paris",
        countryCode = "FR",
        registeredAt = Instant.now(),
        activatedAt = Instant.now(),
    )

    private fun appointment(slug: String, clientEmail: String, artistEmail: String = "artist@x.com") = Appointment(
        artist = artist(slug, artistEmail),
        client = TattooClient(email = clientEmail, firstName = "Jane", lastName = "Doe"),
        tattooDescription = "A dragon",
        tattooPlacement = "forearm",
        tattooSize = "10x10cm",
        firstTattoo = false,
        coverUp = false,
        color = false,
        style = TattooStyle.REALISM,
    )

    private fun supportMessage(
        slug: String,
        artistEmail: String = "artist@x.com",
        type: SupportMessageType = SupportMessageType.BUG,
    ) = SupportMessage(
        artist = artist(slug, artistEmail),
        type = type,
        message = "Something is broken",
        createdAt = Instant.now(),
    )
}
