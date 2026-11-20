package com.stencilia.stenciliaback.alert.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.telegram")
data class TelegramConfig(
    val botToken: String = "",
    val chatId: String = "",
)
