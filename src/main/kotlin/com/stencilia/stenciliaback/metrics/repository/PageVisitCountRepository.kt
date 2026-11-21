package com.stencilia.stenciliaback.metrics.repository

import com.stencilia.stenciliaback.metrics.entity.PageVisitCount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PageVisitCountRepository : JpaRepository<PageVisitCount, UUID> {

    // Atomic upsert: no app-side read-modify-write, so concurrent visits can't lose an increment.
    @Modifying
    @Query(
        value = "INSERT INTO page_visit_count(artist_id, count) VALUES (:artistId, 1) " +
            "ON CONFLICT (artist_id) DO UPDATE SET count = page_visit_count.count + 1",
        nativeQuery = true,
    )
    fun increment(@Param("artistId") artistId: UUID)

    // Native with an explicit bigint cast: Postgres SUM(bigint) yields numeric, which won't coerce
    // to the Long return type.
    @Query(
        value = "SELECT CAST(COALESCE(SUM(p.count), 0) AS bigint) FROM page_visit_count p " +
            "JOIN tattoo_artist a ON a.id = p.artist_id " +
            "WHERE NOT EXISTS (SELECT 1 FROM test_account t WHERE LOWER(t.email) = LOWER(a.email))",
        nativeQuery = true,
    )
    fun sumVisitsExcludingTestAccounts(): Long
}
