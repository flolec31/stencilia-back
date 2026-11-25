package com.stencilia.stenciliaback.appointment.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AppointmentLinksRequestDto(
    @field:NotBlank @field:Email @field:Size(max = 254) val email: String
)
