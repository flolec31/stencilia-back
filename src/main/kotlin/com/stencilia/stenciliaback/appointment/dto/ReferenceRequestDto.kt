package com.stencilia.stenciliaback.appointment.dto

import com.stencilia.stenciliaback.appointment.entity.Appointment
import com.stencilia.stenciliaback.appointment.entity.Reference
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ReferenceRequestDto(
    @field:NotBlank @field:Size(max = 1024) val key: String,
    @field:Size(max = 2000) val comment: String? = null,
) {
    fun toEntity(appointment: Appointment): Reference =
        Reference(
            key = key,
            comment = comment,
            appointment = appointment,
        )
}