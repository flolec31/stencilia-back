package com.stencilia.stenciliaback.metrics

import com.stencilia.stenciliaback.AbstractIntegrationTest
import com.stencilia.stenciliaback.artist.entity.TattooArtist
import com.stencilia.stenciliaback.artist.repository.TattooArtistRepository
import com.stencilia.stenciliaback.auth.Role
import com.stencilia.stenciliaback.auth.service.JwtService
import com.stencilia.stenciliaback.metrics.repository.PageVisitCountRepository
import com.stencilia.stenciliaback.metrics.service.PageVisitService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class PageVisitCounterTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var artistRepository: TattooArtistRepository
    @Autowired lateinit var pageVisitCountRepository: PageVisitCountRepository
    @Autowired lateinit var pageVisitService: PageVisitService

    @BeforeEach @AfterEach
    fun cleanup() {
        pageVisitCountRepository.deleteAll()
        artistRepository.deleteAll()
    }

    private fun saveArtist(slug: String, email: String = "$slug@test.com"): TattooArtist =
        artistRepository.save(
            TattooArtist(
                id = UUID.randomUUID(), email = email, password = "hashed",
                artistName = slug, slug = slug, city = "c", countryCode = "FR",
                registeredAt = Instant.now(), activatedAt = Instant.now(),
            )
        )

    private fun countFor(id: UUID): Long = pageVisitCountRepository.findById(id).map { it.count }.orElse(0)

    @Test
    fun `anonymous visit increments the counter`() {
        val artist = saveArtist("ink")

        mockMvc.perform(get("/artists/ink")).andExpect(status().isOk)
        assertEquals(1, countFor(artist.id))

        mockMvc.perform(get("/artists/ink")).andExpect(status().isOk)
        assertEquals(2, countFor(artist.id))
    }

    @Test
    fun `an authenticated artist visiting the public slug still increments`() {
        val artist = saveArtist("ink")
        mockMvc.perform(get("/artists/ink")).andExpect(status().isOk)

        // The backend counts every hit to the public slug URL; identity isn't inspected (the FE does
        // not send the JWT here, and an artist's own "my page" will use an authed path — front #11).
        val ownToken = jwtService.generateAccessToken(artist.id, Role.ARTIST)
        mockMvc.perform(get("/artists/ink").header("Authorization", "Bearer $ownToken")).andExpect(status().isOk)

        assertEquals(2, countFor(artist.id))
    }

    @Test
    fun `visit response sets no cookie`() {
        saveArtist("ink")
        mockMvc.perform(get("/artists/ink"))
            .andExpect(status().isOk)
            .andExpect(header().doesNotExist("Set-Cookie"))
    }

    @Test
    fun `concurrent visits all count`() {
        val artist = saveArtist("ink")
        val threads = 10
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        repeat(threads) {
            pool.submit {
                start.await()
                pageVisitService.recordVisit(artist.id)
                done.countDown()
            }
        }
        start.countDown()
        done.await()
        pool.shutdown()

        assertEquals(threads.toLong(), countFor(artist.id))
    }
}
