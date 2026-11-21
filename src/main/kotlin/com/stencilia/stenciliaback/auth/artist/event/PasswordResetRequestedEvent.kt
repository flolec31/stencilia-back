package com.stencilia.stenciliaback.auth.artist.event

data class PasswordResetRequestedEvent(
    val to: String,
    val artistName: String,
    val token: String,
)
