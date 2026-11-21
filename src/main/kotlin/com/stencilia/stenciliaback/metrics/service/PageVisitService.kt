package com.stencilia.stenciliaback.metrics.service

import java.util.UUID

interface PageVisitService {
    /** Records a public-page visit for [slug]'s artist, unless the viewer is that artist. */
    fun recordVisit(slug: String, viewerArtistId: UUID?)
}
