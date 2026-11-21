package com.stencilia.stenciliaback.alert

import com.stencilia.stenciliaback.alert.config.TelegramConfig
import com.stencilia.stenciliaback.alert.service.implementation.AdminAlertServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import java.util.concurrent.atomic.AtomicBoolean

class AdminAlertServiceImplTest {

    @Test
    fun `send swallows transport failure`() {
        val throwing = RestClient.builder()
            .requestInterceptor { _, _, _ -> throw RuntimeException("boom") }
            .build()
        val service = AdminAlertServiceImpl(
            TelegramConfig(botToken = "token", chatId = "123"),
            throwing,
        )

        assertDoesNotThrow { service.send("x") }
    }

    @Test
    fun `send with blank token does nothing and does not throw`() {
        val called = AtomicBoolean(false)
        val tracking = RestClient.builder()
            .requestInterceptor { _, _, _ -> called.set(true); throw RuntimeException("should not be called") }
            .build()
        val service = AdminAlertServiceImpl(
            TelegramConfig(botToken = "", chatId = ""),
            tracking,
        )

        assertDoesNotThrow { service.send("x") }
        assertThat(called.get()).isFalse()
    }
}
