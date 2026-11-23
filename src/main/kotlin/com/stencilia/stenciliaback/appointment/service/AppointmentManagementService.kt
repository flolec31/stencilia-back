package com.stencilia.stenciliaback.appointment.service

import com.stencilia.stenciliaback.appointment.dto.AppointmentDetailsResponseDto
import com.stencilia.stenciliaback.appointment.dto.AppointmentItemResponseDto
import com.stencilia.stenciliaback.appointment.dto.InboxFilter
import org.springframework.data.domain.Page
import java.util.*

interface AppointmentManagementService {
    fun getInbox(artistId: UUID, filters: Set<InboxFilter>, page: Int): Page<AppointmentItemResponseDto>
    fun getArchive(artistId: UUID, page: Int): Page<AppointmentItemResponseDto>
    fun getAppointmentDetails(artistId: UUID, appointmentId: UUID): AppointmentDetailsResponseDto
    fun archiveAppointment(artistId: UUID, appointmentId: UUID)
    fun unarchiveAppointment(artistId: UUID, appointmentId: UUID)
    fun markAppointmentAsNew(artistId: UUID, appointmentId: UUID)
}
