package com.appbards.admanager.core.model

sealed class AdResult {
    data class Success(val message: String = "") : AdResult()
    data class Failure(val error: AdError) : AdResult()
}