package com.stencilia.stenciliaback.metrics.service

import java.util.UUID

interface PageVisitService {
    /** Records a public-page visit for the artist owning [slug], unless the viewer is that artist. Best-effort. */
    fun recordVisit(slug: String, viewerArtistId: UUID?)
}
