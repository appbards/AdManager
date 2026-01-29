package com.appbards.admanager.core.model

data class AdError(
    val code : ErrorCode,
    val message: String,
    val providerError:Any? = null
)

enum class ErrorCode{
    NOT_INITIALIZED,
    NO_INTERNET,
    NO_FILL,
    AD_ALREADY_LOADED,
    AD_NOT_READY,
    SHOW_FAILED,
    PROVIDER_ERROR,
    TIMEOUT
}
