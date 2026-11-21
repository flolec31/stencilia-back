package com.stencilia.stenciliaback.metrics.service.implementation

import com.stencilia.stenciliaback.artist.repository.TattooArtistRepository
import com.stencilia.stenciliaback.metrics.repository.PageVisitCountRepository
import com.stencilia.stenciliaback.metrics.service.PageVisitService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PageVisitServiceImpl(
    private val artistRepository: TattooArtistRepository,
    private val pageVisitCountRepository: PageVisitCountRepository,
) : PageVisitService {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Own read-write transaction — the public profile read is read-only.
    @Transactional
    override fun recordVisit(slug: String) {
        try {
            val artist = artistRepository.findBySlugAndActivatedAtIsNotNull(slug).orElse(null) ?: return
            pageVisitCountRepository.increment(artist.id)
        } catch (e: Exception) {
            logger.warn("Failed to record page visit for slug {}", slug, e)
        }
    }
}
