package com.stencilia.stenciliaback.metrics.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.metrics")
data class MetricsProperties(
    val username: String = "",
    val password: String = "",
)
