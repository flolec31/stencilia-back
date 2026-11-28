package com.stencilia.stenciliaback.flash.service.implementation

import com.stencilia.stenciliaback.artist.service.TattooArtistService
import com.stencilia.stenciliaback.flash.dto.FlashResponseDto
import com.stencilia.stenciliaback.flash.entity.Flash
import com.stencilia.stenciliaback.flash.exception.*
import com.stencilia.stenciliaback.flash.repository.FlashRepository
import com.stencilia.stenciliaback.flash.service.FlashService
import com.stencilia.stenciliaback.flash.service.FlashService.Companion.MAX_FLASHES
import com.stencilia.stenciliaback.storage.service.ImageUploadService
import com.stencilia.stenciliaback.storage.service.StorageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
@Transactional(readOnly = true)
class FlashServiceImpl(
    private val tattooArtistService: TattooArtistService,
    private val flashRepository: FlashRepository,
    private val imageUploadService: ImageUploadService,
    private val storageService: StorageService,
) : FlashService {

    override fun getFlashes(artistId: UUID): List<FlashResponseDto> =
        flashRepository.findByArtistIdOrderByPositionAsc(artistId)
            .map { FlashResponseDto(it, storageService.getSignedUrl(it.imageKey)) }

    @Transactional
    override fun create(artistId: UUID, image: MultipartFile, caption: String?): FlashResponseDto {
        val artist = tattooArtistService.findById(artistId)
        if (flashRepository.countByArtistId(artistId) >= MAX_FLASHES) throw FlashLimitReachedException()
        val upload = imageUploadService.uploadFlashImage(artistId, image)
        val firstPosition = flashRepository.findFirstByArtistIdOrderByPositionAsc(artistId)?.position ?: 1
        val flash = flashRepository.save(
            Flash(
                artist = artist,
                imageKey = upload.key,
                caption = caption?.takeIf { it.isNotBlank() },
                position = firstPosition - 1,
            )
        )
        return FlashResponseDto(flash, upload.url)
    }

    @Transactional
    override fun updateCaption(artistId: UUID, flashId: UUID, caption: String?): FlashResponseDto {
        val flash = findOwnedFlash(artistId, flashId)
        flash.caption = caption?.takeIf { it.isNotBlank() }
        return FlashResponseDto(flash, storageService.getSignedUrl(flash.imageKey))
    }

    @Transactional
    override fun delete(artistId: UUID, flashId: UUID) {
        val flash = findOwnedFlash(artistId, flashId)
        flashRepository.delete(flash)
        storageService.delete(flash.imageKey)
    }

    @Transactional
    override fun reorder(artistId: UUID, flashIds: List<UUID>) {
        val flashes = flashRepository.findByArtistIdOrderByPositionAsc(artistId)
        val byId = flashes.associateBy { it.id }
        if (flashIds.size != flashes.size || flashIds.toSet() != byId.keys) throw InvalidFlashOrderException()
        flashIds.forEachIndexed { index, id -> byId.getValue(id).position = index }
    }

    private fun findOwnedFlash(artistId: UUID, flashId: UUID): Flash =
        flashRepository.findByIdAndArtistId(flashId, artistId) ?: throw FlashNotFoundException()
}
