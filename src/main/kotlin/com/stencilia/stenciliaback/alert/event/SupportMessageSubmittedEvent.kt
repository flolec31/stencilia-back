package com.stencilia.stenciliaback.alert.event

import com.stencilia.stenciliaback.support.entity.SupportMessageType

data class SupportMessageSubmittedEvent(
    val slug: String,
    val type: SupportMessageType,
    val artistEmail: String,
)
