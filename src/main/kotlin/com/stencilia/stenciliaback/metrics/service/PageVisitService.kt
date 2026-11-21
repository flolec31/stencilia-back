package com.stencilia.stenciliaback.metrics.service

import java.util.UUID

interface PageVisitService {
    /** Records a public-page visit for the given artist. */
    fun recordVisit(artistId: UUID)
}
