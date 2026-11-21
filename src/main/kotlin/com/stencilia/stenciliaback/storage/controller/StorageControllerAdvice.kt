package com.stencilia.stenciliaback.storage.controller

import com.stencilia.stenciliaback.common.AbstractControllerAdvice
import com.stencilia.stenciliaback.storage.exception.ImageTooLargeException
import com.stencilia.stenciliaback.storage.exception.InvalidImageContentTypeException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class StorageControllerAdvice : AbstractControllerAdvice() {

    @ExceptionHandler(InvalidImageContentTypeException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidImageContentType(e: InvalidImageContentTypeException) = handleException(e)

    @ExceptionHandler(ImageTooLargeException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleImageTooLarge(e: ImageTooLargeException) = handleException(e)

}
