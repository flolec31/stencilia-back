package com.stencilia.stenciliaback.alert.service.implementation

import com.stencilia.stenciliaback.alert.config.TelegramConfig
import com.stencilia.stenciliaback.alert.service.AdminAlertService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class AdminAlertServiceImpl(
    private val telegramConfig: TelegramConfig,
    private val restClient: RestClient = RestClient.create(),
) : AdminAlertService {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    override fun send(text: String) {
        if (telegramConfig.botToken.isBlank() || telegramConfig.chatId.isBlank()) return
        try {
            restClient.post()
                .uri("https://api.telegram.org/bot${telegramConfig.botToken}/sendMessage")
                .body(mapOf("chat_id" to telegramConfig.chatId, "text" to text))
                .retrieve()
                .toBodilessEntity()
        } catch (e: Exception) {
            logger.warn("Failed to send admin alert to Telegram", e)
        }
    }
}
