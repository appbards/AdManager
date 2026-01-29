package com.appbards.admanager.core.provider

import android.view.ViewGroup
import com.appbards.admanager.core.callback.NativeAdCallback
import com.appbards.admanager.core.model.AdResult

interface INativeAd {
    suspend fun load(): AdResult
    fun isReady(): Boolean
    fun show(container: ViewGroup, callback: NativeAdCallback)
    fun destroy()
}