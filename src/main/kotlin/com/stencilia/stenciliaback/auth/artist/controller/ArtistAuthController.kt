package com.stencilia.stenciliaback.auth.artist.controller

import com.stencilia.stenciliaback.auth.artist.dto.*
import com.stencilia.stenciliaback.auth.dto.LoginResponseDto
import com.stencilia.stenciliaback.auth.artist.service.AuthService
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import com.stencilia.stenciliaback.security.subjectId
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth/artist")
@Validated
class ArtistAuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun register(@Valid @RequestBody request: RegisterRequestDto) =
        authService.register(request)

    @GetMapping("/verify")
    fun verify(@RequestParam token: String): LoginResponseDto =
        authService.verify(token)

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resendVerification(@RequestParam @Size(max = 254) email: String) =
        authService.resendVerification(email)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequestDto): LoginResponseDto =
        authService.login(request.email, request.password)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequestDto): LoginResponseDto =
        authService.refresh(request.refreshToken)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: LogoutRequestDto) =
        authService.logout(request.refreshToken)

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun forgotPassword(@RequestParam @Size(max = 254) email: String) =
        authService.forgotPassword(email)

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequestDto): LoginResponseDto =
        authService.resetPassword(request)

    @PostMapping("/change-password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequestDto,
        authentication: JwtAuthenticationToken,
    ): LoginResponseDto {
        val artistId = authentication.subjectId
        return authService.changePassword(artistId, request)
    }
}