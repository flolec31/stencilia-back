package com.stencilia.stenciliaback.metrics

import com.stencilia.stenciliaback.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Review Focus: with no `app.metrics` credential configured, the endpoint is locked, not open. */
class PrometheusEndpointLockedByDefaultTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `prometheus endpoint is locked when no credential is configured`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized)
    }
}
