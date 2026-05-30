package com.appbards.admanager.core.provider

import android.view.ViewGroup
import com.appbards.admanager.core.callback.NativeAdCallback
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.nativeAd.NativeAdViewBinder

interface INativeAd {
    suspend fun load(): AdResult
    fun isReady(): Boolean
    fun show(container: ViewGroup, binder: NativeAdViewBinder, callback: NativeAdCallback)
    fun destroy()
}