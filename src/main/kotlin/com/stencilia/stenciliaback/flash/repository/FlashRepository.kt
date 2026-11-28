package com.stencilia.stenciliaback.flash.repository

import com.stencilia.stenciliaback.flash.entity.Flash
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FlashRepository : JpaRepository<Flash, UUID> {
    fun findByArtistIdOrderByPositionAsc(artistId: UUID): List<Flash>
    fun findFirstByArtistIdOrderByPositionAsc(artistId: UUID): Flash?
    fun findByIdAndArtistId(id: UUID, artistId: UUID): Flash?
    fun countByArtistId(artistId: UUID): Long
    fun existsByImageKey(imageKey: String): Boolean
}
