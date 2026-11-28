package com.stencilia.stenciliaback.support.controller

import com.stencilia.stenciliaback.storage.dto.ImageUploadResponseDto
import com.stencilia.stenciliaback.support.dto.SupportMessageRequestDto
import com.stencilia.stenciliaback.support.service.SupportMessageService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import com.stencilia.stenciliaback.security.subjectId
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/support")
class SupportMessageController(
    private val supportMessageService: SupportMessageService
) {

    @PostMapping("/screenshot", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadScreenshot(
        @RequestParam("image") photo: MultipartFile,
        authentication: JwtAuthenticationToken
    ): ImageUploadResponseDto {
        val artistId = authentication.subjectId
        return supportMessageService.uploadScreenshot(artistId, photo)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun submit(
        @Valid @RequestBody request: SupportMessageRequestDto,
        authentication: JwtAuthenticationToken
    ) {
        val artistId = authentication.subjectId
        supportMessageService.submit(artistId, request)
    }
}
