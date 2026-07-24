package com.appbards.admanager.core.provider

import com.appbards.admanager.core.config.AdConfig
import com.appbards.admanager.core.model.AdResult

interface IAdProvider {
    /**
     * Gather user consent (e.g. GDPR/UMP) if required.
     *
     * This runs interactively (it may display a consent form) and must complete
     * BEFORE any timed initialization begins, so a loading timeout can never
     * dismiss the consent form out from under the user.
     *
     * Default is a no-op for providers that don't manage consent.
     */
    suspend fun gatherConsent(config: AdConfig) {}

    suspend fun initialize(config: AdConfig): AdResult
    fun isInitialized(): Boolean
    fun getRewardedAd(placementId: String): IRewardedAd?
    fun getInterstitialAd(placementId: String): IInterstitialAd?
    fun getBannerAd(placementId: String): IBannerAd?
    fun getNativeAd(placementId: String): INativeAd?
    fun getAppOpenAd(placementId: String): IAppOpenAd?
    fun destroy()
}