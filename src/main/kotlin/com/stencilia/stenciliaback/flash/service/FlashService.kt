package com.stencilia.stenciliaback.flash.service

import com.stencilia.stenciliaback.flash.dto.FlashResponseDto
import com.stencilia.stenciliaback.storage.dto.ImageUploadResponseDto
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

interface FlashService {
    fun getFlashes(artistId: UUID): List<FlashResponseDto>
    fun uploadImage(artistId: UUID, image: MultipartFile): ImageUploadResponseDto
    fun create(artistId: UUID, imageKey: String, caption: String?): FlashResponseDto
    fun updateCaption(artistId: UUID, flashId: UUID, caption: String?): FlashResponseDto
    fun delete(artistId: UUID, flashId: UUID)
    fun reorder(artistId: UUID, flashIds: List<UUID>)

    companion object {
        const val MAX_FLASHES = 50
    }
}
