package com.stencilia.stenciliaback.common

import com.stencilia.stenciliaback.AbstractIntegrationTest
import com.stencilia.stenciliaback.alert.service.AdminAlertService
import com.stencilia.stenciliaback.artist.service.TattooArtistService
import com.stencilia.stenciliaback.storage.exception.ImageUploadFailedException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Every server 5xx alerts the admin; user/bot errors (framework 4xx) do not.
 * Drives a real 500 through the public GET /artists/{slug} by stubbing its service to throw.
 * The 4xx cases use permitAll prefixes so the framework exception (not the security layer)
 * decides the status, exercising the ResponseEntityExceptionHandler branches the advice relies on.
 */
class BreakAlertIntegrationTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var adminAlertService: AdminAlertService
    @MockitoBean lateinit var tattooArtistService: TattooArtistService

    @Test
    fun `a 5xx from an endpoint alerts the admin with method, path and exception type`() {
        whenever(tattooArtistService.getProfileBySlug("jane-ink")).thenThrow(RuntimeException("boom"))

        mockMvc.perform(get("/artists/jane-ink")).andExpect(status().isInternalServerError)

        verify(adminAlertService).send("💥 500 on GET /artists/jane-ink: RuntimeException")
    }

    @Test
    fun `a server-side failure that has its own 5xx exception type still alerts via the catch-all`() {
        whenever(tattooArtistService.getProfileBySlug("jane-ink"))
            .thenThrow(ImageUploadFailedException(RuntimeException("storage down")))

        mockMvc.perform(get("/artists/jane-ink")).andExpect(status().isInternalServerError)

        verify(adminAlertService).send("💥 500 on GET /artists/jane-ink: ImageUploadFailedException")
    }

    @Test
    fun `a scanned unauthenticated URL (401) does not alert`() {
        // A bot hitting a random path matches no permitAll rule, so Spring Security answers 401
        // before the dispatcher — it never reaches the advice.
        mockMvc.perform(get("/some-random-scan")).andExpect(status().isUnauthorized)

        verifyNoInteractions(adminAlertService)
    }

    @Test
    fun `an unknown path under a public prefix (404) does not alert`() {
        mockMvc.perform(get("/auth/artist/does-not-exist")).andExpect(status().isNotFound)

        verifyNoInteractions(adminAlertService)
    }

    @Test
    fun `a wrong method on a public endpoint (405) does not alert`() {
        // /auth/artist/login is POST-only; GET is permitted by security but unmapped → 405.
        mockMvc.perform(get("/auth/artist/login")).andExpect(status().isMethodNotAllowed)

        verifyNoInteractions(adminAlertService)
    }

    @Test
    fun `a malformed body (400) does not alert`() {
        mockMvc.perform(
            post("/auth/artist/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ not json")
        ).andExpect(status().isBadRequest)

        verifyNoInteractions(adminAlertService)
    }
}
