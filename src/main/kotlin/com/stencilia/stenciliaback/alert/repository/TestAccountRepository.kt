package com.stencilia.stenciliaback.alert.repository

import com.stencilia.stenciliaback.alert.entity.TestAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TestAccountRepository : JpaRepository<TestAccount, String> {
    fun existsByEmailIgnoreCase(email: String): Boolean
}
