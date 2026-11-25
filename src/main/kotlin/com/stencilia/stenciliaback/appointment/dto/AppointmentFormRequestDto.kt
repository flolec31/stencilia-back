package com.stencilia.stenciliaback.appointment.dto

import com.stencilia.stenciliaback.appointment.entity.Appointment
import com.stencilia.stenciliaback.appointment.entity.TattooStyle
import com.stencilia.stenciliaback.artist.entity.TattooArtist
import com.stencilia.stenciliaback.client.entity.TattooClient
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AppointmentFormRequestDto(
    @field:NotBlank @field:Email @field:Size(max = 254) val email: String,
    @field:NotBlank @field:Size(max = 100) val firstName: String,
    @field:NotBlank @field:Size(max = 100) val lastName: String,
    @field:NotBlank @field:Size(max = 2000) val description: String,
    @field:NotBlank @field:Size(max = 255) val placement: String,
    @field:NotBlank @field:Size(max = 255) val size: String,
    val firstTattoo: Boolean,
    val coverUp: Boolean,
    val color: Boolean,
    val style: TattooStyle,
    @field:Valid @field:Size(max = 10) val references: List<ReferenceRequestDto> = emptyList()
) {

    @Suppress("unused")
    @AssertTrue(message = "If the tattoo is a cover, a photo is needed")
    fun isAtLeastOneReferenceIfCover(): Boolean = !coverUp || references.isNotEmpty()

    fun toEntity(
        artist: TattooArtist,
        client: TattooClient
    ) : Appointment = Appointment(
        artist = artist,
        client = client,
        tattooDescription = this.description,
        tattooPlacement = this.placement,
        tattooSize = this.size,
        firstTattoo = this.firstTattoo,
        coverUp = this.coverUp,
        color = this.color,
        style = this.style
    )

}