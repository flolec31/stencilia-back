package com.stencilia.stenciliaback.metrics

import com.stencilia.stenciliaback.AbstractIntegrationTest
import com.stencilia.stenciliaback.artist.entity.TattooArtist
import com.stencilia.stenciliaback.artist.repository.TattooArtistRepository
import com.stencilia.stenciliaback.metrics.service.PageVisitService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

/** Review Focus: a visit-counter failure must never break the public page load. */
class PageVisitResilienceTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var artistRepository: TattooArtistRepository

    @MockitoBean lateinit var pageVisitService: PageVisitService

    @BeforeEach @AfterEach
    fun cleanup() = artistRepository.deleteAll().let {}

    @Test
    fun `public page still loads when recording a visit throws`() {
        artistRepository.save(
            TattooArtist(
                id = UUID.randomUUID(), email = "ink@test.com", password = "hashed",
                artistName = "ink", slug = "ink", city = "c", countryCode = "FR",
                registeredAt = Instant.now(), activatedAt = Instant.now(),
            )
        )
        whenever(pageVisitService.recordVisit(any())).thenThrow(RuntimeException("boom"))

        mockMvc.perform(get("/artists/ink")).andExpect(status().isOk)
    }
}
