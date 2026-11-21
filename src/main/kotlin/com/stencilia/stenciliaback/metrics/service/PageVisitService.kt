package com.stencilia.stenciliaback.metrics.service

interface PageVisitService {
    /** Records a public-page visit for [slug]'s artist. */
    fun recordVisit(slug: String)
}
