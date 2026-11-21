package com.stencilia.stenciliaback.common

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered

/**
 * Base for every domain-specific advice. Implements [Ordered] at highest precedence so these
 * specific handlers always outrank the lowest-precedence catch-all [UnhandledExceptionAdvice]:
 * without this they default to LOWEST_PRECEDENCE and tie with the catch-all, which could then
 * preempt a specific 4xx handler (turning a user error into an alerted 500).
 */
abstract class AbstractControllerAdvice : Ordered {

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    private val logger = LoggerFactory.getLogger(javaClass)

    protected fun handleException(e: Exception, is5xx: Boolean = false) : Map<String, String?>{
        if (is5xx) logger.warn("⚠ Server-side exception ⚠", e)
        return mapOf("error" to e.message)
    }
}