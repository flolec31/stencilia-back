package com.stencilia.stenciliaback.artist.controller

import com.stencilia.stenciliaback.artist.dto.PhotoUploadResponseDto
import com.stencilia.stenciliaback.artist.dto.ProfileResponseDto
import com.stencilia.stenciliaback.artist.dto.SlugAvailabilityResponseDto
import com.stencilia.stenciliaback.artist.dto.UpdateProfileRequestDto
import com.stencilia.stenciliaback.artist.exception.SlugAlreadyTakenException
import com.stencilia.stenciliaback.artist.service.TattooArtistService
import com.stencilia.stenciliaback.metrics.service.PageVisitService
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/artists")
@Validated
class TattooArtistController(
    private val tattooArtistService: TattooArtistService,
    private val pageVisitService: PageVisitService,
) {

    @GetMapping("/slug-available")
    fun isSlugAvailable(
        @RequestParam @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,28}[a-z0-9]$") slug: String
    ): SlugAvailabilityResponseDto =
        SlugAvailabilityResponseDto(available = !tattooArtistService.isSlugTaken(slug))

    @PatchMapping("/me")
    fun updateProfile(
        @Valid @RequestBody request: UpdateProfileRequestDto,
        authentication: JwtAuthenticationToken
    ): ProfileResponseDto {
        val artistId = UUID.fromString(authentication.token.subject)
        if (request.slug != null && tattooArtistService.existsBySlugAndIdNot(request.slug, artistId)) {
            throw SlugAlreadyTakenException()
        }
        return tattooArtistService.updateProfile(artistId, request)
    }

    @GetMapping("/me")
    fun getProfile(
        authentication: JwtAuthenticationToken
    ): ProfileResponseDto {
        val artistId = UUID.fromString(authentication.token.subject)
        return tattooArtistService.getProfile(artistId)
    }

    @GetMapping("/{slug}")
    fun getPublicProfile(@PathVariable slug: String): ProfileResponseDto {
        val profile = tattooArtistService.getProfileBySlug(slug)
        // Best-effort: counting a visit must never break the public page load.
        runCatching { pageVisitService.recordVisit(slug, currentArtistId()) }
        return profile
    }

    private fun currentArtistId(): UUID? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication is JwtAuthenticationToken) {
            runCatching { UUID.fromString(authentication.token.subject) }.getOrNull()
        } else {
            null
        }
    }

    @DeleteMapping("/me/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePhoto(authentication: JwtAuthenticationToken) {
        val artistId = UUID.fromString(authentication.token.subject)
        tattooArtistService.deletePhoto(artistId)
    }

    @PostMapping("/me/photo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadPhoto(
        @RequestParam("photo") photo: MultipartFile,
        authentication: JwtAuthenticationToken
    ): PhotoUploadResponseDto {
        val artistId = UUID.fromString(authentication.token.subject)
        val photoUrl = tattooArtistService.updatePhoto(artistId, photo)
        return PhotoUploadResponseDto(photoUrl = photoUrl)
    }
}