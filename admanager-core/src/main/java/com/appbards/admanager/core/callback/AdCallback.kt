package com.appbards.admanager.core.callback

import com.appbards.admanager.core.model.AdError

interface AdCallback {
    fun onAdLoaded() {}
    fun onAdFailedToLoad(error: AdError) {}
    fun onAdShown() {}
    fun onAdFailedToShow(error: AdError) {}
    fun onAdClicked() {}
    fun onAdClosed() {}
}