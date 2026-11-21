package com.stencilia.stenciliaback.metrics.service.implementation

import com.stencilia.stenciliaback.metrics.repository.PageVisitCountRepository
import com.stencilia.stenciliaback.metrics.service.PageVisitService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PageVisitServiceImpl(
    private val pageVisitCountRepository: PageVisitCountRepository,
) : PageVisitService {

    private val logger = LoggerFactory.getLogger(javaClass)

    // REQUIRES_NEW: its own read-write transaction, since the caller's profile read is read-only.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun recordVisit(artistId: UUID) {
        try {
            pageVisitCountRepository.increment(artistId)
        } catch (e: Exception) {
            logger.warn("Failed to record page visit for artist {}", artistId, e)
        }
    }
}
