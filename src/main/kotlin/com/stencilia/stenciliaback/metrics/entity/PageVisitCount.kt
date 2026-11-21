package com.stencilia.stenciliaback.metrics.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "page_visit_count")
class PageVisitCount(
    @Id var artistId: UUID,
    var count: Long = 0,
)
