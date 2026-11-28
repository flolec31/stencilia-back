package com.stencilia.stenciliaback.flash.dto

import com.stencilia.stenciliaback.flash.service.FlashService
import jakarta.validation.constraints.Size
import java.util.UUID

data class FlashOrderRequestDto(
    @field:Size(max = FlashService.MAX_FLASHES) val flashIds: List<UUID>,
)
