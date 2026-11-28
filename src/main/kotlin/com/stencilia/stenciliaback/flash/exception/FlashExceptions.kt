package com.stencilia.stenciliaback.flash.exception

import com.stencilia.stenciliaback.flash.service.FlashService

class FlashNotFoundException : RuntimeException("Flash not found")

class FlashLimitReachedException :
    RuntimeException("An artist can have at most ${FlashService.MAX_FLASHES} flashes")

class InvalidFlashImageKeyException : RuntimeException("The image key does not belong to you")

class FlashImageAlreadyUsedException : RuntimeException("This image is already used by another flash")

class InvalidFlashOrderException : RuntimeException("The order must list each of your flashes exactly once")
