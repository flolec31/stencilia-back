package com.stencilia.stenciliaback.auth.artist.event

data class ArtistVerificationRequestedEvent(
    val to: String,
    val artistName: String,
    val token: String,
)
