package com.appbards.admanager.ironsource

import android.app.Activity
import com.appbards.admanager.core.config.AdConfig
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.IAdProvider
import com.appbards.admanager.core.provider.IAppOpenAd
import com.appbards.admanager.core.provider.IBannerAd
import com.appbards.admanager.core.provider.IInterstitialAd
import com.appbards.admanager.core.provider.INativeAd
import com.appbards.admanager.core.provider.IRewardedAd
import com.appbards.admanager.ironsource.banner.IronSourceBannerAd
import com.appbards.admanager.ironsource.interstitial.IronSourceInterstitialAd
import com.appbards.admanager.ironsource.rewarded.IronSourceRewardedAd
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayConfiguration
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * IronSource SDK provider implementation
 * Adapts IronSource SDK to AdManager interfaces
 */
class IronSourceProvider(
    private val activity: Activity
) : IAdProvider {

    private var initialized = false

    override suspend fun initialize(config: AdConfig): AdResult = suspendCancellableCoroutine { continuation ->
        try {
            // Enable test mode if configured
            if (config.testMode) {
                LevelPlay.setMetaData("is_test_suite", "enable")
            }
            val initRequest = LevelPlayInitRequest
                .Builder(config.appId)
                .build()
            LevelPlay.init(
                activity.applicationContext,
                initRequest,
                object : LevelPlayInitListener {
                    override fun onInitSuccess(p0: LevelPlayConfiguration) {
                        initialized = true
                        if (config.testMode) {
                            LevelPlay.launchTestSuite(activity.applicationContext)
                            LevelPlay.validateIntegration(activity.applicationContext)
                        }
                        continuation.resume(
                            AdResult.Success("LevelPlay initialized successfully")
                        )
                    }

                    override fun onInitFailed(p0: LevelPlayInitError) {
                        continuation.resume(
                            AdResult.Failure(
                                AdError(
                                    ErrorCode.PROVIDER_ERROR,
                                    "LevelPlay initialization failed: ${p0.errorMessage}",
                                    p0
                                )
                            )
                        )
                    }
                }
            )
        } catch (e: Exception) {
            continuation.resume(
                AdResult.Failure(
                    AdError(
                        ErrorCode.PROVIDER_ERROR,
                        "LevelPlay initialization exception: ${e.message}",
                        e
                    )
                )
            )
        }
    }

    override fun isInitialized(): Boolean = initialized

    override fun getRewardedAd(placementId: String): IRewardedAd {
        return IronSourceRewardedAd(activity, placementId)
    }

    override fun getInterstitialAd(placementId: String): IInterstitialAd {
        return IronSourceInterstitialAd(activity, placementId)
    }

    override fun getBannerAd(placementId: String): IBannerAd {
        return IronSourceBannerAd(activity, placementId)
    }

    override fun getNativeAd(placementId: String): INativeAd? {
        throw UnsupportedOperationException("LevelPlay does not support native ads in this implementation")
    }

    override fun getAppOpenAd(placementId: String): IAppOpenAd? {
        throw UnsupportedOperationException("LevelPlay does not support app open ads")
    }

    override fun destroy() {
        initialized = false
    }
}