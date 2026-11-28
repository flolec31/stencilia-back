package com.stencilia.stenciliaback.security

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.UUID

/** Id of the authenticated artist or client, carried as the JWT subject. */
val JwtAuthenticationToken.subjectId: UUID
    get() = UUID.fromString(token.subject)
