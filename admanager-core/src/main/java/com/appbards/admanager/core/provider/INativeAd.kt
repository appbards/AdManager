package com.appbards.admanager.core.provider

import android.view.ViewGroup
import com.appbards.admanager.core.callback.NativeAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.nativeAd.NativeAdViewBinder

interface INativeAd {
    suspend fun load(): AdResult
    fun isReady(): Boolean

    /**
     * View-based show. Binds the ad into [binder] and swaps it into [container]
     * (the container is cleared first). Use from Activities/Fragments/RecyclerView.
     */
    fun show(container: ViewGroup, binder: NativeAdViewBinder, callback: NativeAdCallback)

    /**
     * Compose-friendly show. Binds the ad into the caller-hosted ad view
     * ([binder].rootView) without touching any container — intended for use inside
     * an `AndroidView` where the ad view is already attached to the composition.
     *
     * Providers that don't support native ads report failure via [callback].
     */
    fun show(binder: NativeAdViewBinder, callback: NativeAdCallback) {
        callback.onAdFailedToShow(
            AdError(ErrorCode.SHOW_FAILED, "Compose show() is not supported by this provider")
        )
    }

    fun destroy()
}