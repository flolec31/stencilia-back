package com.stencilia.stenciliaback.metrics.repository

import com.stencilia.stenciliaback.artist.entity.TattooArtist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * On-demand metric count queries. Bound to [TattooArtist] only to satisfy Spring Data; each query
 * names its own target entity.
 */
@Repository
interface MetricsRepository : JpaRepository<TattooArtist, UUID> {

    @Query(
        "SELECT COUNT(a) FROM TattooArtist a WHERE a.activatedAt IS NOT NULL " +
            "AND NOT EXISTS (SELECT 1 FROM TestAccount t WHERE LOWER(t.email) = LOWER(a.email))",
    )
    fun countRegisteredArtists(): Long

    @Query(
        "SELECT COUNT(ap) FROM Appointment ap WHERE ap.verifiedAt IS NOT NULL " +
            "AND LOWER(ap.client.email) <> LOWER(ap.artist.email) " +
            "AND NOT EXISTS (SELECT 1 FROM TestAccount t " +
            "WHERE LOWER(t.email) = LOWER(ap.artist.email) OR LOWER(t.email) = LOWER(ap.client.email))",
    )
    fun countCountedRequests(): Long

    @Query(
        "SELECT COUNT(DISTINCT ap.artist.id) FROM Appointment ap " +
            "WHERE ap.verifiedAt IS NOT NULL AND ap.verifiedAt >= :since " +
            "AND ap.artist.activatedAt IS NOT NULL " +
            "AND LOWER(ap.client.email) <> LOWER(ap.artist.email) " +
            "AND NOT EXISTS (SELECT 1 FROM TestAccount t " +
            "WHERE LOWER(t.email) = LOWER(ap.artist.email) OR LOWER(t.email) = LOWER(ap.client.email))",
    )
    fun countActiveArtists(@Param("since") since: Instant): Long

    @Query(
        "SELECT COUNT(ap) FROM Appointment ap " +
            "WHERE ap.verifiedAt IS NULL AND ap.submittedAt < :threshold " +
            "AND NOT EXISTS (SELECT 1 FROM TestAccount t " +
            "WHERE LOWER(t.email) = LOWER(ap.artist.email) OR LOWER(t.email) = LOWER(ap.client.email))",
    )
    fun countLostUnvalidated(@Param("threshold") threshold: Instant): Long

    // Native: `new` is a JPQL reserved keyword.
    @Query(
        value = "SELECT COUNT(*) FROM appointment ap " +
            "JOIN tattoo_artist a ON a.id = ap.artist_id " +
            "JOIN tattoo_client c ON c.id = ap.client_id " +
            "WHERE ap.verified_at IS NOT NULL AND ap.verified_at < :threshold AND ap.new = TRUE " +
            "AND NOT EXISTS (SELECT 1 FROM test_account t " +
            "WHERE LOWER(t.email) = LOWER(a.email) OR LOWER(t.email) = LOWER(c.email))",
        nativeQuery = true,
    )
    fun countLostUnopened(@Param("threshold") threshold: Instant): Long

    @Query(
        "SELECT COUNT(ap) FROM Appointment ap " +
            "WHERE NOT EXISTS (SELECT 1 FROM TestAccount t " +
            "WHERE LOWER(t.email) = LOWER(ap.artist.email) OR LOWER(t.email) = LOWER(ap.client.email))",
    )
    fun countSubmittedRequests(): Long

    @Query(
        "SELECT COUNT(ap) FROM Appointment ap WHERE ap.verifiedAt IS NOT NULL " +
            "AND NOT EXISTS (SELECT 1 FROM TestAccount t " +
            "WHERE LOWER(t.email) = LOWER(ap.artist.email) OR LOWER(t.email) = LOWER(ap.client.email))",
    )
    fun countValidatedRequests(): Long
}
