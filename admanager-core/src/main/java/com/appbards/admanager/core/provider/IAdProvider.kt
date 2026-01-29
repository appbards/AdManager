package com.appbards.admanager.core.provider

import com.appbards.admanager.core.config.AdConfig
import com.appbards.admanager.core.model.AdResult

interface IAdProvider {
    suspend fun initialize(config: AdConfig): AdResult
    fun isInitialized(): Boolean
    fun getRewardedAd(placementId: String): IRewardedAd?
    fun getInterstitialAd(placementId: String): IInterstitialAd?
    fun getBannerAd(placementId: String): IBannerAd?
    fun getNativeAd(placementId: String): INativeAd?
    fun getAppOpenAd(placementId: String): IAppOpenAd?
    fun destroy()
}