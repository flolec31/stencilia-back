package com.stencilia.stenciliaback.flash.controller

import com.stencilia.stenciliaback.flash.dto.FlashCaptionRequestDto
import com.stencilia.stenciliaback.flash.dto.FlashOrderRequestDto
import com.stencilia.stenciliaback.flash.dto.FlashRequestDto
import com.stencilia.stenciliaback.flash.dto.FlashResponseDto
import com.stencilia.stenciliaback.flash.service.FlashService
import com.stencilia.stenciliaback.storage.dto.ImageUploadResponseDto
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/artists/me/flashes")
class FlashController(
    private val flashService: FlashService,
) {

    @GetMapping
    fun getFlashes(authentication: JwtAuthenticationToken): List<FlashResponseDto> =
        flashService.getFlashes(artistId(authentication))

    @PostMapping("/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadImage(
        authentication: JwtAuthenticationToken,
        @RequestParam("image") image: MultipartFile,
    ): ImageUploadResponseDto =
        flashService.uploadImage(artistId(authentication), image)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        authentication: JwtAuthenticationToken,
        @Valid @RequestBody request: FlashRequestDto,
    ): FlashResponseDto =
        flashService.create(artistId(authentication), request.imageKey, request.caption)

    @PatchMapping("/{flashId}")
    fun updateCaption(
        authentication: JwtAuthenticationToken,
        @PathVariable flashId: UUID,
        @Valid @RequestBody request: FlashCaptionRequestDto,
    ): FlashResponseDto =
        flashService.updateCaption(artistId(authentication), flashId, request.caption)

    @DeleteMapping("/{flashId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        authentication: JwtAuthenticationToken,
        @PathVariable flashId: UUID,
    ) = flashService.delete(artistId(authentication), flashId)

    @PutMapping("/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reorder(
        authentication: JwtAuthenticationToken,
        @Valid @RequestBody request: FlashOrderRequestDto,
    ) = flashService.reorder(artistId(authentication), request.flashIds)

    private fun artistId(authentication: JwtAuthenticationToken): UUID =
        UUID.fromString(authentication.token.subject)
}
