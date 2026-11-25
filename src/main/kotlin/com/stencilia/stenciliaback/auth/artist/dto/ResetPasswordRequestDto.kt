package com.stencilia.stenciliaback.auth.artist.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ResetPasswordRequestDto(
    @field:NotBlank @field:Size(max = 255) val token: String,
    @field:NotBlank @field:Size(min = 8, max = 72) val newPassword: String,
)