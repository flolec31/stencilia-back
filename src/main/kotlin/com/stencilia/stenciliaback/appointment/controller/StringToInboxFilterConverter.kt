package com.stencilia.stenciliaback.appointment.controller

import com.stencilia.stenciliaback.appointment.dto.InboxFilter
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

/* Binds the inbox `filter` query parameter case-insensitively, so the front-end can
   send `new`/`unread`. An unrecognized value throws, which Spring surfaces as a 400. */
@Component
class StringToInboxFilterConverter : Converter<String, InboxFilter> {
    override fun convert(source: String): InboxFilter = InboxFilter.valueOf(source.trim().uppercase())
}
