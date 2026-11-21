package com.stencilia.stenciliaback.email.client

import com.resend.Resend
import com.resend.services.emails.Emails
import com.stencilia.stenciliaback.alert.service.AdminAlertService
import com.stencilia.stenciliaback.email.exception.EmailDeliveryException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ResendEmailClientTest {

    private val resend = mock<Resend>()
    private val emails = mock<Emails>()
    private val adminAlertService = mock<AdminAlertService>()

    @Test
    fun `a send failure alerts the admin with the template id then rethrows EmailDeliveryException`() {
        whenever(resend.emails()).thenReturn(emails)
        whenever(emails.send(any())).thenThrow(RuntimeException("resend down"))
        val client = ResendEmailClient(resend, "from@stencilia.me", adminAlertService)

        assertThrows<EmailDeliveryException> {
            client.sendEmail("client@x.com", "verify-artist-signup", mapOf("LINK" to "l"))
        }

        verify(adminAlertService).send("✉️ Email failed to send — type: verify-artist-signup")
        // BR-3: the alert carries the template id only, never the recipient address.
        verify(adminAlertService, never()).send(argThat { contains("client@x.com") })
    }
}
