package com.stencilia.stenciliaback.common

import org.springframework.core.Ordered

/**
 * Base for every domain-specific advice. Implements [Ordered] at highest precedence so these
 * specific handlers always outrank the lowest-precedence catch-all [UnhandledExceptionAdvice]:
 * without this they default to LOWEST_PRECEDENCE and tie with the catch-all, which could then
 * preempt a specific 4xx handler (turning a user error into an alerted 500).
 */
abstract class AbstractControllerAdvice : Ordered {

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    protected fun handleException(e: Exception): Map<String, String?> = mapOf("error" to e.message)
}