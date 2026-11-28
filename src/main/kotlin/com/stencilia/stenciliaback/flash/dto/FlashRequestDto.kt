package com.stencilia.stenciliaback.flash.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FlashRequestDto(
    @field:NotBlank @field:Size(max = 1024) val imageKey: String,
    @field:Size(max = 255) val caption: String? = null,
)
