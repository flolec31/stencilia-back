package com.stencilia.stenciliaback.appointment

import com.stencilia.stenciliaback.appointment.entity.Appointment
import com.stencilia.stenciliaback.auth.Role
import com.stencilia.stenciliaback.auth.service.JwtService
import com.stencilia.stenciliaback.client.entity.TattooClient
import com.stencilia.stenciliaback.discussion.entity.Message
import com.stencilia.stenciliaback.discussion.entity.MessageSender
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit

class AppointmentInboxListTest : AbstractAppointmentIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService

    private fun verified(
        artist: com.stencilia.stenciliaback.artist.entity.TattooArtist,
        client: TattooClient,
        verifiedAt: Instant,
        new: Boolean = true,
        archived: Boolean = false,
    ): Appointment {
        val appointment = createAppointment(artist = artist, client = client, verifiedAt = verifiedAt, archived = archived)
        if (!new) {
            appointment.new = false
            appointmentRepository.save(appointment)
        }
        return appointment
    }

    private fun unread(appointment: Appointment) {
        messageRepository.save(Message(appointment = appointment, sender = MessageSender.CLIENT, content = "hi"))
    }

    @Test
    fun `inbox orders new first, then unread, then most recent, and excludes archived`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        val client = createClient()
        val base = Instant.now()

        // index 0 = most recent; 28 and 29 (the two oldest) are new; the rest are not new.
        val reqs = (0 until 30).map { i ->
            verified(artist, client, verifiedAt = base.minus(i.toLong(), ChronoUnit.SECONDS), new = i >= 28)
        }
        // the single oldest non-new request (index 27) carries an unread client message
        unread(reqs[27])
        // a couple of archived requests must never appear in the inbox
        verified(artist, client, verifiedAt = base, archived = true)
        verified(artist, client, verifiedAt = base, archived = true)

        // Expected page-0 order: new block [28, 29], then unread non-new [27],
        // then remaining non-new by verifiedAt DESC [0, 1, ... , 16]
        mockMvc.perform(get("/appointment/inbox").param("page", "0").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(20))
            .andExpect(jsonPath("$.total").value(30))
            .andExpect(jsonPath("$.content[*].archived", everyItem(`is`(false))))
            .andExpect(jsonPath("$.content[0].id").value(reqs[28].id.toString()))
            .andExpect(jsonPath("$.content[1].id").value(reqs[29].id.toString()))
            .andExpect(jsonPath("$.content[2].id").value(reqs[27].id.toString()))
            .andExpect(jsonPath("$.content[3].id").value(reqs[0].id.toString()))
            .andExpect(jsonPath("$.content[19].id").value(reqs[16].id.toString()))
    }

    @Test
    fun `inbox places a new-and-unread request once, at the top of the new block`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        val client = createClient()
        val base = Instant.now()

        val newNoUnread = verified(artist, client, verifiedAt = base, new = true)
        val newAndUnread = verified(artist, client, verifiedAt = base.minus(1, ChronoUnit.SECONDS), new = true)
        unread(newAndUnread)
        verified(artist, client, verifiedAt = base.minus(2, ChronoUnit.SECONDS), new = false)

        // new-and-unread sorts ahead of new-without-unread (hasUnread DESC within the new block)
        mockMvc.perform(get("/appointment/inbox").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.content[0].id").value(newAndUnread.id.toString()))
            .andExpect(jsonPath("$.content[1].id").value(newNoUnread.id.toString()))
    }

    @Test
    fun `filter=new returns only new requests across the whole set`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        val client = createClient()
        val base = Instant.now()

        repeat(3) { verified(artist, client, verifiedAt = base.minus(it.toLong(), ChronoUnit.SECONDS), new = true) }
        repeat(5) { verified(artist, client, verifiedAt = base.minus((10 + it).toLong(), ChronoUnit.SECONDS), new = false) }

        mockMvc.perform(get("/appointment/inbox").param("filter", "NEW").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.content[*].new", everyItem(`is`(true))))
    }

    @Test
    fun `filter=unread returns only requests with an unread client message`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        val client = createClient()
        val base = Instant.now()

        val withUnread = (0 until 2).map {
            verified(artist, client, verifiedAt = base.minus(it.toLong(), ChronoUnit.SECONDS), new = false).also { a -> unread(a) }
        }
        repeat(4) { verified(artist, client, verifiedAt = base.minus((10 + it).toLong(), ChronoUnit.SECONDS), new = false) }

        mockMvc.perform(get("/appointment/inbox").param("filter", "UNREAD").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.content[*].unreadMessages", everyItem(`is`(true))))
            .andExpect(jsonPath("$.content[0].id").value(withUnread[0].id.toString()))
    }

    @Test
    fun `filter=new and unread returns the union with no duplicates`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        val client = createClient()
        val base = Instant.now()

        val newOnly = verified(artist, client, verifiedAt = base, new = true)
        val unreadOnly = verified(artist, client, verifiedAt = base.minus(1, ChronoUnit.SECONDS), new = false).also { unread(it) }
        val both = verified(artist, client, verifiedAt = base.minus(2, ChronoUnit.SECONDS), new = true).also { unread(it) }
        val neither = verified(artist, client, verifiedAt = base.minus(3, ChronoUnit.SECONDS), new = false)

        mockMvc.perform(
            get("/appointment/inbox").param("filter", "NEW", "UNREAD").header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.content[?(@.id=='${neither.id}')]").isEmpty)
            .andExpect(jsonPath("$.content[?(@.id=='${newOnly.id}')]").exists())
            .andExpect(jsonPath("$.content[?(@.id=='${unreadOnly.id}')]").exists())
            .andExpect(jsonPath("$.content[?(@.id=='${both.id}')]").exists())
    }

    @Test
    fun `inbox with no filter returns all non-archived requests, new and not`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        val client = createClient()
        val base = Instant.now()

        verified(artist, client, verifiedAt = base, new = true)
        verified(artist, client, verifiedAt = base.minus(1, ChronoUnit.SECONDS), new = false)
        verified(artist, client, verifiedAt = base.minus(2, ChronoUnit.SECONDS), new = false)
        verified(artist, client, verifiedAt = base.minus(3, ChronoUnit.SECONDS), archived = true)

        mockMvc.perform(get("/appointment/inbox").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.content[*].new", org.hamcrest.Matchers.hasItem(true)))
            .andExpect(jsonPath("$.content[*].new", org.hamcrest.Matchers.hasItem(false)))
    }

    @Test
    fun `inbox rejects an unknown filter value with 400`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)

        mockMvc.perform(get("/appointment/inbox").param("filter", "bogus").header("Authorization", "Bearer $token"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `inbox accepts a lowercase filter value`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        val client = createClient()
        verified(artist, client, verifiedAt = Instant.now(), new = true)
        verified(artist, client, verifiedAt = Instant.now().minus(1, ChronoUnit.SECONDS), new = false)

        mockMvc.perform(get("/appointment/inbox").param("filter", "new").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.content[*].new", everyItem(`is`(true))))
    }

    @Test
    fun `inbox rejects a negative page with 400`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)

        mockMvc.perform(get("/appointment/inbox").param("page", "-1").header("Authorization", "Bearer $token"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `inbox pages are stable under an id tiebreak when verifiedAt is equal`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        val client = createClient()
        val sharedInstant = Instant.now()

        // 21 requests sharing one verifiedAt and one new/unread state -> order is purely id ASC
        val ids = (0 until 21)
            .map { verified(artist, client, verifiedAt = sharedInstant, new = true).id.toString() }
            .sorted()

        val page0 = (0 until 20).map { ids[it] }
        mockMvc.perform(get("/appointment/inbox").param("page", "0").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(20))
            .andExpect(jsonPath("$.content[0].id").value(page0[0]))
            .andExpect(jsonPath("$.content[19].id").value(page0[19]))

        mockMvc.perform(get("/appointment/inbox").param("page", "1").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(ids[20]))
    }
}
