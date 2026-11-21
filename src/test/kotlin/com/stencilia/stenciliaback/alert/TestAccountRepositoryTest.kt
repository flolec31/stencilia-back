package com.stencilia.stenciliaback.alert

import com.stencilia.stenciliaback.AbstractIntegrationTest
import com.stencilia.stenciliaback.alert.entity.TestAccount
import com.stencilia.stenciliaback.alert.repository.TestAccountRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TestAccountRepositoryTest : AbstractIntegrationTest() {

    @Autowired lateinit var testAccountRepository: TestAccountRepository

    @BeforeEach
    @AfterEach
    fun cleanup() {
        testAccountRepository.deleteAll()
    }

    @Test
    fun `save then existsByEmailIgnoreCase returns true for exact match`() {
        testAccountRepository.save(TestAccount("me@test.com"))

        assertThat(testAccountRepository.existsByEmailIgnoreCase("me@test.com")).isTrue()
    }

    @Test
    fun `existsByEmailIgnoreCase is case-insensitive`() {
        testAccountRepository.save(TestAccount("me@test.com"))

        assertThat(testAccountRepository.existsByEmailIgnoreCase("ME@Test.com")).isTrue()
    }

    @Test
    fun `existsByEmailIgnoreCase returns false for unknown email`() {
        testAccountRepository.save(TestAccount("me@test.com"))

        assertThat(testAccountRepository.existsByEmailIgnoreCase("other@test.com")).isFalse()
    }
}
