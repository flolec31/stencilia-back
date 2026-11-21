package com.stencilia.stenciliaback.metrics

import com.stencilia.stenciliaback.AbstractIntegrationTest
import com.stencilia.stenciliaback.metrics.repository.MetricsRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Review Focus: a failing metric query must not 500 the scrape — the endpoint stays up. */
@TestPropertySource(properties = ["app.metrics.username=scraper", "app.metrics.password=s3cret"])
class MetricsResilienceTest : AbstractIntegrationTest() {

    @Autowired lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var metricsRepository: MetricsRepository

    @Test
    fun `scrape still succeeds when a metric query throws`() {
        whenever(metricsRepository.countRegisteredArtists()).thenThrow(RuntimeException("db down"))

        mockMvc.perform(get("/actuator/prometheus").with(httpBasic("scraper", "s3cret")))
            .andExpect(status().isOk)
    }
}
