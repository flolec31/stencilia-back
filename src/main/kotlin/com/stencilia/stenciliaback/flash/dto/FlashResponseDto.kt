package com.stencilia.stenciliaback.flash.dto

import com.stencilia.stenciliaback.flash.entity.Flash
import java.util.UUID

data class FlashResponseDto(
    val id: UUID,
    val imageUrl: String,
    val caption: String?,
) {
    constructor(flash: Flash, imageUrl: String) : this(
        id = flash.id,
        imageUrl = imageUrl,
        caption = flash.caption,
    )
}
