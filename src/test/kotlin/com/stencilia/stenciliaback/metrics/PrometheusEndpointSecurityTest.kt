package com.stencilia.stenciliaback.metrics

import com.stencilia.stenciliaback.AbstractIntegrationTest
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@TestPropertySource(properties = ["app.metrics.username=scraper", "app.metrics.password=s3cret"])
class PrometheusEndpointSecurityTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `prometheus endpoint rejects a request with no credentials`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `prometheus endpoint rejects wrong credentials`() {
        mockMvc.perform(get("/actuator/prometheus").with(httpBasic("scraper", "wrong")))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `prometheus endpoint serves metrics with valid credentials`() {
        mockMvc.perform(get("/actuator/prometheus").with(httpBasic("scraper", "s3cret")))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("jvm_")))
    }

    @Test
    fun `health endpoint stays public`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }
}
