package com.stencilia.stenciliaback.appointment.event

import com.stencilia.stenciliaback.appointment.entity.Appointment

data class AppointmentRequestVerifiedEvent(val appointment: Appointment)
