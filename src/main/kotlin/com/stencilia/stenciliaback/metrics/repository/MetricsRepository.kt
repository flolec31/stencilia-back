package com.stencilia.stenciliaback.metrics.repository

import com.stencilia.stenciliaback.artist.entity.TattooArtist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Hosts the on-demand metric count queries. Bound to [TattooArtist] only to satisfy Spring Data;
 * each JPQL query names its own target entity. Every count excludes `test_account` emails (BR-8).
 */
@Repository
interface MetricsRepository : JpaRepository<TattooArtist, UUID> {

    // BR-3a
    @Query(
        "SELECT COUNT(a) FROM TattooArtist a WHERE a.activatedAt IS NOT NULL " +
            "AND NOT EXISTS (SELECT 1 FROM TestAccount t WHERE LOWER(t.email) = LOWER(a.email))",
    )
    fun countRegisteredArtists(): Long

    // BR-3b: validated AND client email differs from artist email (self-requests excluded)
    @Query(
        "SELECT COUNT(ap) FROM Appointment ap WHERE ap.verifiedAt IS NOT NULL " +
            "AND LOWER(ap.client.email) <> LOWER(ap.artist.email) " +
            "AND NOT EXISTS (SELECT 1 FROM TestAccount t " +
            "WHERE LOWER(t.email) = LOWER(ap.artist.email) OR LOWER(t.email) = LOWER(ap.client.email))",
    )
    fun countCountedRequests(): Long

    // BR-4: registered artists with >=1 counted request validated within the window
    @Query(
        "SELECT COUNT(DISTINCT ap.artist.id) FROM Appointment ap " +
            "WHERE ap.verifiedAt IS NOT NULL AND ap.verifiedAt >= :since " +
            "AND ap.artist.activatedAt IS NOT NULL " +
            "AND LOWER(ap.client.email) <> LOWER(ap.artist.email) " +
            "AND NOT EXISTS (SELECT 1 FROM TestAccount t " +
            "WHERE LOWER(t.email) = LOWER(ap.artist.email) OR LOWER(t.email) = LOWER(ap.client.email))",
    )
    fun countActiveArtists(@Param("since") since: Instant): Long

    // BR-5a
    @Query(
        "SELECT COUNT(ap) FROM Appointment ap " +
            "WHERE ap.verifiedAt IS NULL AND ap.submittedAt < :threshold " +
            "AND NOT EXISTS (SELECT 1 FROM TestAccount t " +
            "WHERE LOWER(t.email) = LOWER(ap.artist.email) OR LOWER(t.email) = LOWER(ap.client.email))",
    )
    fun countLostUnvalidated(@Param("threshold") threshold: Instant): Long

    // BR-5b: validated long ago and never opened by the artist. Native because `new` is a JPQL keyword.
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

    // BR-6b: every submission
    @Query(
        "SELECT COUNT(ap) FROM Appointment ap " +
            "WHERE NOT EXISTS (SELECT 1 FROM TestAccount t " +
            "WHERE LOWER(t.email) = LOWER(ap.artist.email) OR LOWER(t.email) = LOWER(ap.client.email))",
    )
    fun countSubmittedRequests(): Long

    // BR-6c: validated (self-requests included)
    @Query(
        "SELECT COUNT(ap) FROM Appointment ap WHERE ap.verifiedAt IS NOT NULL " +
            "AND NOT EXISTS (SELECT 1 FROM TestAccount t " +
            "WHERE LOWER(t.email) = LOWER(ap.artist.email) OR LOWER(t.email) = LOWER(ap.client.email))",
    )
    fun countValidatedRequests(): Long
}
