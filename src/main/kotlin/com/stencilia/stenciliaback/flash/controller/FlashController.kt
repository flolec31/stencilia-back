package com.stencilia.stenciliaback.flash.controller

import com.stencilia.stenciliaback.flash.dto.FlashCaptionRequestDto
import com.stencilia.stenciliaback.flash.dto.FlashOrderRequestDto
import com.stencilia.stenciliaback.flash.dto.FlashResponseDto
import com.stencilia.stenciliaback.flash.service.FlashService
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import com.stencilia.stenciliaback.security.subjectId
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/artists/me/flashes")
@Validated
class FlashController(
    private val flashService: FlashService,
) {

    @GetMapping
    fun getFlashes(authentication: JwtAuthenticationToken): List<FlashResponseDto> =
        flashService.getFlashes(authentication.subjectId)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        authentication: JwtAuthenticationToken,
        @RequestParam("image") image: MultipartFile,
        @RequestParam(required = false) @Size(max = 255) caption: String?,
    ): FlashResponseDto =
        flashService.create(authentication.subjectId, image, caption)

    @PatchMapping("/{flashId}")
    fun updateCaption(
        authentication: JwtAuthenticationToken,
        @PathVariable flashId: UUID,
        @Valid @RequestBody request: FlashCaptionRequestDto,
    ): FlashResponseDto =
        flashService.updateCaption(authentication.subjectId, flashId, request.caption)

    @DeleteMapping("/{flashId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        authentication: JwtAuthenticationToken,
        @PathVariable flashId: UUID,
    ) = flashService.delete(authentication.subjectId, flashId)

    @PutMapping("/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reorder(
        authentication: JwtAuthenticationToken,
        @Valid @RequestBody request: FlashOrderRequestDto,
    ) = flashService.reorder(authentication.subjectId, request.flashIds)
}
