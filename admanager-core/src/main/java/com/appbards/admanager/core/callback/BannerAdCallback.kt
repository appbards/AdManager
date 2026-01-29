package com.appbards.admanager.core.callback

interface BannerAdCallback : AdCallback {
    fun onBannerExpanded() {}
    fun onBannerCollapsed() {}
}