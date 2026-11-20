package com.stencilia.stenciliaback.alert.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "test_account")
class TestAccount(
    @Id var email: String,
)
