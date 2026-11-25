package com.stencilia.stenciliaback.auth.artist.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequestDto(
    @field:NotBlank @field:Size(max = 72) val currentPassword: String,
    @field:NotBlank @field:Size(min = 8, max = 72) val newPassword: String,
)
