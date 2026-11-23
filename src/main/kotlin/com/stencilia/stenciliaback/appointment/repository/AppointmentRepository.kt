package com.stencilia.stenciliaback.appointment.repository

import com.stencilia.stenciliaback.appointment.entity.Appointment
import com.stencilia.stenciliaback.discussion.entity.MessageSender
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface AppointmentRepository : JpaRepository<Appointment, UUID> {
    fun findByArtistIdAndVerifiedAtNotNull(artistId: UUID, pageable: Pageable): Page<Appointment>
    fun findByArtistIdAndVerifiedAtNotNullAndArchivedTrueOrderByVerifiedAtDescIdAsc(
        artistId: UUID,
        pageable: Pageable
    ): Page<Appointment>

    /* Inbox feed: non-archived verified requests of an artist.
       When neither filter flag is set every request passes; otherwise the selected
       predicates are OR-combined (new, or has an unread client message).
       Ordered: new first, then requests with an unread client message, then most
       recently received, with id as a stable tiebreaker. */
    @Query(
        """
        SELECT a FROM Appointment a
        WHERE a.artist.id = :artistId AND a.verifiedAt IS NOT NULL AND a.archived = false
          AND (
            (:filterNew = false AND :filterUnread = false)
            OR (:filterNew = true AND a.new = true)
            OR (:filterUnread = true AND EXISTS (
                  SELECT 1 FROM Message m
                  WHERE m.appointment = a AND m.sender = :clientSender AND m.readAt IS NULL))
          )
        ORDER BY a.new DESC,
          (CASE WHEN EXISTS (
              SELECT 1 FROM Message m2
              WHERE m2.appointment = a AND m2.sender = :clientSender AND m2.readAt IS NULL)
            THEN 1 ELSE 0 END) DESC,
          a.verifiedAt DESC,
          a.id ASC
        """,
        countQuery = """
        SELECT COUNT(a) FROM Appointment a
        WHERE a.artist.id = :artistId AND a.verifiedAt IS NOT NULL AND a.archived = false
          AND (
            (:filterNew = false AND :filterUnread = false)
            OR (:filterNew = true AND a.new = true)
            OR (:filterUnread = true AND EXISTS (
                  SELECT 1 FROM Message m
                  WHERE m.appointment = a AND m.sender = :clientSender AND m.readAt IS NULL))
          )
        """
    )
    fun findInbox(
        @Param("artistId") artistId: UUID,
        @Param("filterNew") filterNew: Boolean,
        @Param("filterUnread") filterUnread: Boolean,
        @Param("clientSender") clientSender: MessageSender,
        pageable: Pageable
    ): Page<Appointment>
    fun findByIdAndVerifiedAtNotNull(appointmentId: UUID): Optional<Appointment>
    fun findByArtistIdAndClientEmailIgnoreCaseAndVerifiedAtNotNullOrderBySubmittedAtDesc(
        artistId: UUID,
        email: String
    ): List<Appointment>
}
