package com.stencilia.stenciliaback.auth.artist.event

data class PasswordChangedEvent(
    val to: String,
    val artistName: String,
)
