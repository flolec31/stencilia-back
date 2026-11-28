package com.stencilia.stenciliaback.flash.entity

import com.stencilia.stenciliaback.artist.entity.TattooArtist
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "flash")
class Flash(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne @JoinColumn(name = "artist_id", nullable = false)
    var artist: TattooArtist,
    var imageKey: String,
    var caption: String? = null,
    var position: Int,
    var createdAt: Instant = Instant.now(),
)
