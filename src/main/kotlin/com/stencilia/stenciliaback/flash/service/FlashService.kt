package com.stencilia.stenciliaback.flash.service

import com.stencilia.stenciliaback.flash.dto.FlashResponseDto
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

interface FlashService {
    fun getFlashes(artistId: UUID): List<FlashResponseDto>
    fun create(artistId: UUID, image: MultipartFile, caption: String?): FlashResponseDto
    fun updateCaption(artistId: UUID, flashId: UUID, caption: String?): FlashResponseDto
    fun delete(artistId: UUID, flashId: UUID)
    fun reorder(artistId: UUID, flashIds: List<UUID>)

    companion object {
        const val MAX_FLASHES = 50
    }
}
