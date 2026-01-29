package com.appbards.admanager.core.provider

import android.view.View
import com.appbards.admanager.core.callback.BannerAdCallback
import com.appbards.admanager.core.model.AdResult

enum class BannerSize {
    BANNER,              // 320x50
    LARGE_BANNER,        // 320x100
    MEDIUM_RECTANGLE,    // 300x250
    FULL_BANNER,         // 468x60
    LEADERBOARD,         // 728x90
    ADAPTIVE             // Adaptive banner (width-based)
}

enum class BannerPosition {
    TOP,
    BOTTOM
}

interface IBannerAd {
    fun load(size: BannerSize): AdResult
    fun getView(): View?  // Returns the banner view to add to your layout
    fun pause()
    fun resume()
    fun destroy()
}