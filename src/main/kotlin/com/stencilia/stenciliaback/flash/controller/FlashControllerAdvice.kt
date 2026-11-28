package com.stencilia.stenciliaback.flash.controller

import com.stencilia.stenciliaback.common.AbstractControllerAdvice
import com.stencilia.stenciliaback.flash.exception.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class FlashControllerAdvice : AbstractControllerAdvice() {

    @ExceptionHandler(FlashNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleFlashNotFound(e: FlashNotFoundException) = handleException(e)

    @ExceptionHandler(FlashLimitReachedException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleFlashLimitReached(e: FlashLimitReachedException) = handleException(e)

    @ExceptionHandler(InvalidFlashOrderException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidFlashOrder(e: InvalidFlashOrderException) = handleException(e)
}
