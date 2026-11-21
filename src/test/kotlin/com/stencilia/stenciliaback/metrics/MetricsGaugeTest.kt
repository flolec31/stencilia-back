package com.stencilia.stenciliaback.metrics

import com.stencilia.stenciliaback.AbstractIntegrationTest
import com.stencilia.stenciliaback.alert.entity.TestAccount
import com.stencilia.stenciliaback.alert.repository.TestAccountRepository
import com.stencilia.stenciliaback.appointment.entity.Appointment
import com.stencilia.stenciliaback.appointment.entity.TattooStyle
import com.stencilia.stenciliaback.appointment.repository.AppointmentRepository
import com.stencilia.stenciliaback.artist.entity.TattooArtist
import com.stencilia.stenciliaback.artist.repository.TattooArtistRepository
import com.stencilia.stenciliaback.client.entity.TattooClient
import com.stencilia.stenciliaback.client.repository.TattooClientRepository
import com.stencilia.stenciliaback.metrics.repository.PageVisitCountRepository
import com.stencilia.stenciliaback.metrics.service.PageVisitService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@TestPropertySource(properties = ["app.metrics.username=scraper", "app.metrics.password=s3cret"])
class MetricsGaugeTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var artistRepository: TattooArtistRepository
    @Autowired lateinit var clientRepository: TattooClientRepository
    @Autowired lateinit var appointmentRepository: AppointmentRepository
    @Autowired lateinit var pageVisitCountRepository: PageVisitCountRepository
    @Autowired lateinit var pageVisitService: PageVisitService
    @Autowired lateinit var testAccountRepository: TestAccountRepository

    @BeforeEach @AfterEach
    fun cleanup() {
        appointmentRepository.deleteAll()
        pageVisitCountRepository.deleteAll()
        clientRepository.deleteAll()
        artistRepository.deleteAll()
        testAccountRepository.deleteAll()
    }

    private fun artist(slug: String, email: String = "$slug@test.com", activated: Boolean = true) =
        artistRepository.save(
            TattooArtist(
                id = UUID.randomUUID(), email = email, password = "h", artistName = slug, slug = slug,
                city = "c", countryCode = "FR", registeredAt = Instant.now(),
                activatedAt = if (activated) Instant.now() else null,
            )
        )

    private fun client(email: String) =
        clientRepository.save(TattooClient(id = UUID.randomUUID(), email = email, firstName = "F", lastName = "L"))

    private fun appointment(
        artist: TattooArtist,
        client: TattooClient,
        verifiedAt: Instant? = null,
        submittedAt: Instant = Instant.now(),
        isNew: Boolean = true,
    ) = appointmentRepository.save(
        Appointment(
            id = UUID.randomUUID(), artist = artist, client = client,
            tattooDescription = "d", tattooPlacement = "arm", tattooSize = "S",
            firstTattoo = false, coverUp = false, color = false, style = TattooStyle.BLACKWORK,
            submittedAt = submittedAt, verifiedAt = verifiedAt, new = isNew, archived = false,
        )
    )

    private fun gauge(name: String): Double {
        val body = mockMvc.perform(get("/actuator/prometheus").with(httpBasic("scraper", "s3cret")))
            .andExpect(status().isOk).andReturn().response.contentAsString
        val match = Regex("^" + Regex.escape(name) + "(\\{[^}]*\\})? ([0-9eE.+-]+)$", RegexOption.MULTILINE)
            .find(body) ?: error("metric $name not found in scrape")
        return match.groupValues[2].toDouble()
    }

    @Test
    fun `registered artists counts a verified artist`() {
        artist("ink")
        assertEquals(1.0, gauge("stencilia_artists_registered"))
    }

    @Test
    fun `counted requests exclude self-requests`() {
        val a = artist("ink", "ink@test.com")
        appointment(a, client("ink@test.com"), verifiedAt = Instant.now()) // self-request
        appointment(a, client("real@test.com"), verifiedAt = Instant.now()) // real
        assertEquals(1.0, gauge("stencilia_requests_counted"))
    }

    @Test
    fun `active artists respects the 30-day boundary`() {
        val old = artist("old", "old@test.com")
        appointment(old, client("c1@test.com"), verifiedAt = Instant.now().minus(31, ChronoUnit.DAYS))
        val fresh = artist("new", "new@test.com")
        appointment(fresh, client("c2@test.com"), verifiedAt = Instant.now().minus(29, ChronoUnit.DAYS))
        assertEquals(1.0, gauge("stencilia_artists_active"))
    }

    @Test
    fun `lost unvalidated respects the 24h boundary`() {
        val a = artist("ink")
        appointment(a, client("c1@test.com"), verifiedAt = null, submittedAt = Instant.now().minus(25, ChronoUnit.HOURS))
        appointment(a, client("c2@test.com"), verifiedAt = null, submittedAt = Instant.now().minus(23, ChronoUnit.HOURS))
        assertEquals(1.0, gauge("stencilia_requests_lost_unvalidated"))
    }

    @Test
    fun `lost unopened counts validated-but-unopened and clears once opened`() {
        val a = artist("ink")
        val appt = appointment(a, client("c@test.com"), verifiedAt = Instant.now().minus(8, ChronoUnit.DAYS), isNew = true)
        assertEquals(1.0, gauge("stencilia_requests_lost_unopened"))

        appt.new = false
        appointmentRepository.save(appt)
        assertEquals(0.0, gauge("stencilia_requests_lost_unopened"))
    }

    @Test
    fun `visits total reflects the page-visit counter`() {
        val a = artist("ink")
        pageVisitService.recordVisit(a.id)
        pageVisitService.recordVisit(a.id)
        assertEquals(2.0, gauge("stencilia_visits"))
    }

    @Test
    fun `submitted and validated count the full funnel including self-requests`() {
        val a = artist("ink", "ink@test.com")
        appointment(a, client("ink@test.com"), verifiedAt = Instant.now())   // self, validated
        appointment(a, client("real@test.com"), verifiedAt = Instant.now())  // real, validated
        assertEquals(2.0, gauge("stencilia_requests_submitted"))
        assertEquals(2.0, gauge("stencilia_requests_validated"))
        assertEquals(1.0, gauge("stencilia_requests_counted"))
    }

    @Test
    fun `excluded accounts leave every metric unchanged`() {
        val a = artist("ink", "ink@test.com")
        appointment(a, client("real@test.com"), verifiedAt = Instant.now())
        pageVisitService.recordVisit(a.id)
        testAccountRepository.save(TestAccount(email = "ink@test.com"))

        assertEquals(0.0, gauge("stencilia_artists_registered"))
        assertEquals(0.0, gauge("stencilia_requests_counted"))
        assertEquals(0.0, gauge("stencilia_requests_submitted"))
        assertEquals(0.0, gauge("stencilia_requests_validated"))
        assertEquals(0.0, gauge("stencilia_visits"))
    }

    @Test
    fun `all gauges read zero on an empty database`() {
        listOf(
            "stencilia_artists_registered", "stencilia_requests_counted", "stencilia_artists_active",
            "stencilia_requests_lost_unvalidated", "stencilia_requests_lost_unopened",
            "stencilia_visits", "stencilia_requests_submitted", "stencilia_requests_validated",
        ).forEach { assertEquals(0.0, gauge(it), "expected $it to be 0 on empty DB") }
    }
}
