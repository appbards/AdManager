package com.appbards.admanager.core.model

sealed class InitializationResult {
    data class Success(
        val actualInitTime: Long,
        val wasDelayed: Boolean
    ) : InitializationResult()

    data class Timeout(
        val partialError: AdError?
    ) : InitializationResult()

    data class Failed(
        val error: AdError,
        val wasDelayed: Boolean
    ) : InitializationResult()
}