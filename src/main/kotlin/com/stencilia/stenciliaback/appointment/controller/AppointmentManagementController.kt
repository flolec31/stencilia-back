package com.stencilia.stenciliaback.appointment.controller

import com.stencilia.stenciliaback.appointment.dto.AppointmentDetailsResponseDto
import com.stencilia.stenciliaback.appointment.dto.AppointmentItemResponseDto
import com.stencilia.stenciliaback.appointment.service.AppointmentManagementService
import com.stencilia.stenciliaback.common.dto.PagedResponseDto
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/appointment")
@Validated
class AppointmentManagementController(
    private val appointmentManagementService: AppointmentManagementService
) {

    @GetMapping("/inbox")
    fun getInbox(
        authentication: JwtAuthenticationToken,
        @RequestParam(defaultValue = "false") new: Boolean,
        @RequestParam(defaultValue = "false") unread: Boolean,
        @RequestParam(defaultValue = "0") @PositiveOrZero page: Int,
    ): PagedResponseDto<AppointmentItemResponseDto> {
        val artistId = UUID.fromString(authentication.token.subject)
        val appointments = appointmentManagementService.getInbox(artistId, new, unread, page)
        return PagedResponseDto(
            content = appointments.content,
            total = appointments.totalElements,
            page = appointments.number,
            pageCount = appointments.totalPages
        )
    }

    @GetMapping("/archive")
    fun getArchive(
        authentication: JwtAuthenticationToken,
        @RequestParam(defaultValue = "0") @PositiveOrZero page: Int,
    ): PagedResponseDto<AppointmentItemResponseDto> {
        val artistId = UUID.fromString(authentication.token.subject)
        val appointments = appointmentManagementService.getArchive(artistId, page)
        return PagedResponseDto(
            content = appointments.content,
            total = appointments.totalElements,
            page = appointments.number,
            pageCount = appointments.totalPages
        )
    }

    @GetMapping("/{appointmentId}")
    fun getAppointment(
        authentication: JwtAuthenticationToken,
        @PathVariable appointmentId: UUID,
    ): AppointmentDetailsResponseDto {
        val artistId = UUID.fromString(authentication.token.subject)
        return appointmentManagementService.getAppointmentDetails(artistId, appointmentId)
    }

    @PostMapping("/{appointmentId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun archiveAppointment(
        authentication: JwtAuthenticationToken,
        @PathVariable appointmentId: UUID,
    ) {
        val artistId = UUID.fromString(authentication.token.subject)
        appointmentManagementService.archiveAppointment(artistId, appointmentId)
    }

    @PostMapping("/{appointmentId}/unarchive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unarchiveAppointment(
        authentication: JwtAuthenticationToken,
        @PathVariable appointmentId: UUID,
    ) {
        val artistId = UUID.fromString(authentication.token.subject)
        appointmentManagementService.unarchiveAppointment(artistId, appointmentId)
    }

    @PostMapping("/{appointmentId}/mark-new")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAppointmentAsNew(
        authentication: JwtAuthenticationToken,
        @PathVariable appointmentId: UUID,
    ) {
        val artistId = UUID.fromString(authentication.token.subject)
        appointmentManagementService.markAppointmentAsNew(artistId, appointmentId)
    }
}
