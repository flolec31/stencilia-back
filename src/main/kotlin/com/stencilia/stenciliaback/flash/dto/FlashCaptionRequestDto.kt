package com.stencilia.stenciliaback.flash.dto

import jakarta.validation.constraints.Size

data class FlashCaptionRequestDto(
    @field:Size(max = 255) val caption: String? = null,
)
