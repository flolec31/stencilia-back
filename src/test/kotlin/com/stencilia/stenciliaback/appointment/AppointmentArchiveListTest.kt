package com.stencilia.stenciliaback.appointment

import com.stencilia.stenciliaback.appointment.entity.Appointment
import com.stencilia.stenciliaback.auth.Role
import com.stencilia.stenciliaback.auth.service.JwtService
import com.stencilia.stenciliaback.client.entity.TattooClient
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

class AppointmentArchiveListTest : AbstractAppointmentIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService

    @Test
    fun `archive returns archived requests only, most recent first, in batches of 20`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        val client = createClient()
        val base = Instant.now()

        // 25 archived, distinct verifiedAt (index 0 = most recent)
        val archived: List<Appointment> = (0 until 25).map { i ->
            createAppointment(
                artist = artist,
                client = client,
                verifiedAt = base.minus(i.toLong(), ChronoUnit.SECONDS),
                archived = true,
            )
        }
        // 2 non-archived verified requests that must never appear in the archive
        val inboxOnly = (0 until 2).map {
            createAppointment(artist = artist, client = client, archived = false)
        }

        mockMvc.perform(get("/appointment/archive").param("page", "0").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(20))
            .andExpect(jsonPath("$.total").value(25))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.pageCount").value(2))
            .andExpect(jsonPath("$.content[0].id").value(archived[0].id.toString()))
            .andExpect(jsonPath("$.content[1].id").value(archived[1].id.toString()))
            .andExpect(jsonPath("$.content[*].archived", everyItem(`is`(true))))

        mockMvc.perform(get("/appointment/archive").param("page", "1").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.content[*].archived", everyItem(`is`(true))))
            .andExpect(jsonPath("$.content[4].id").value(archived[24].id.toString()))
    }

    @Test
    fun `archive rejects a negative page with 400`() {
        val artist = createActivatedArtist()
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)

        mockMvc.perform(get("/appointment/archive").param("page", "-1").header("Authorization", "Bearer $token"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `archive returns 401 when not authenticated`() {
        mockMvc.perform(get("/appointment/archive"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `archive does not return another artists requests`() {
        val artist = createActivatedArtist(slug = "my-artist")
        val other = createActivatedArtist(slug = "other-artist")
        val token = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        val client = createClient()
        createAppointment(artist = other, client = client, archived = true)

        mockMvc.perform(get("/appointment/archive").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isEmpty)
            .andExpect(jsonPath("$.total").value(0))
    }
}
