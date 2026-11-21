package com.stencilia.stenciliaback.metrics

import com.stencilia.stenciliaback.metrics.repository.MetricsRepository
import com.stencilia.stenciliaback.metrics.repository.PageVisitCountRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Registers the admin on-demand metrics (product #6) as Micrometer gauges. Each gauge's supplier
 * runs its SQL at scrape time, so values are live to the scrape interval. A query failure yields
 * NaN rather than propagating, so a DB blip never 500s the scrape.
 */
@Component
class MetricsRegistrar(
    private val meterRegistry: MeterRegistry,
    private val metricsRepository: MetricsRepository,
    private val pageVisitCountRepository: PageVisitCountRepository,
) {

    @PostConstruct
    fun register() {
        gauge("stencilia.artists.registered") { metricsRepository.countRegisteredArtists() }
        gauge("stencilia.requests.counted") { metricsRepository.countCountedRequests() }
        gauge("stencilia.artists.active") {
            metricsRepository.countActiveArtists(Instant.now().minus(30, ChronoUnit.DAYS))
        }
        gauge("stencilia.requests.lost.unvalidated") {
            metricsRepository.countLostUnvalidated(Instant.now().minus(24, ChronoUnit.HOURS))
        }
        gauge("stencilia.requests.lost.unopened") {
            metricsRepository.countLostUnopened(Instant.now().minus(7, ChronoUnit.DAYS))
        }
        // Named `stencilia.visits` (not `.total`): Micrometer strips the `_total` suffix from gauges,
        // since Prometheus reserves it for counters.
        gauge("stencilia.visits") { pageVisitCountRepository.sumVisitsExcludingTestAccounts() }
        gauge("stencilia.requests.submitted") { metricsRepository.countSubmittedRequests() }
        gauge("stencilia.requests.validated") { metricsRepository.countValidatedRequests() }
    }

    private fun gauge(name: String, supplier: () -> Long) {
        Gauge.builder(name) { runCatching { supplier().toDouble() }.getOrDefault(Double.NaN) }
            .register(meterRegistry)
    }
}
