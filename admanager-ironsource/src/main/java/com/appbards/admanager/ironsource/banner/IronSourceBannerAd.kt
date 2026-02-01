package com.appbards.admanager.ironsource.banner

import android.app.Activity
import android.view.ViewGroup
import com.appbards.admanager.core.callback.BannerAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.BannerSize
import com.appbards.admanager.core.provider.IBannerAd
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.LevelPlayAdSize
import com.unity3d.mediation.banner.LevelPlayBannerAdView
import com.unity3d.mediation.banner.LevelPlayBannerAdViewListener

class IronSourceBannerAd(
    private val activity: Activity,
    private val adUnitId: String
) : IBannerAd {

    private var bannerAdView: LevelPlayBannerAdView? = null
    private var isLoaded = false

    override fun load(container: ViewGroup, size: BannerSize, callback: BannerAdCallback) {
        try {
            // Destroy old banner if exists
            bannerAdView?.destroy()
            bannerAdView = null

            // Convert BannerSize to LevelPlay size
            val levelPlaySize = when (size) {
                BannerSize.BANNER -> LevelPlayAdSize.BANNER
                BannerSize.LARGE_BANNER -> LevelPlayAdSize.LARGE
                BannerSize.MEDIUM_RECTANGLE -> LevelPlayAdSize.MEDIUM_RECTANGLE
                BannerSize.LEADERBOARD -> LevelPlayAdSize.BANNER
                BannerSize.ADAPTIVE -> LevelPlayAdSize.createAdaptiveAdSize(activity) ?: LevelPlayAdSize.BANNER
                BannerSize.FULL_BANNER -> LevelPlayAdSize.BANNER
            }

            // Build banner config
            val adConfig = LevelPlayBannerAdView.Config.Builder()
                .setAdSize(levelPlaySize)
                .build()

            // Create banner ad view
            bannerAdView = LevelPlayBannerAdView(activity, adUnitId, adConfig)

            // Set listener
            bannerAdView?.bannerListener = object : LevelPlayBannerAdViewListener {
                override fun onAdLoaded(adInfo: LevelPlayAdInfo) {
                    // Add banner to container when loaded
                    bannerAdView?.let { view ->
                        container.removeAllViews()
                        container.addView(view)
                    }
                    callback.onAdLoaded()
                }

                override fun onAdLoadFailed(error: LevelPlayAdError) {
                    callback.onAdFailedToLoad(
                        AdError(
                            ErrorCode.NO_FILL,
                            "Banner failed to load: ${error.errorMessage}",
                            error
                        )
                    )
                }

                override fun onAdDisplayed(adInfo: LevelPlayAdInfo) {
                    callback.onAdShown()
                }

                override fun onAdDisplayFailed(adInfo: LevelPlayAdInfo, error: LevelPlayAdError) {
                    callback.onAdFailedToShow(
                        AdError(
                            ErrorCode.SHOW_FAILED,
                            "Banner failed to display: ${error.errorMessage}",
                            error
                        )
                    )
                }

                override fun onAdClicked(adInfo: LevelPlayAdInfo) {
                    callback.onAdClicked()
                }

                override fun onAdExpanded(adInfo: LevelPlayAdInfo) {
                    callback.onBannerExpanded()
                }

                override fun onAdCollapsed(adInfo: LevelPlayAdInfo) {
                    callback.onBannerCollapsed()
                }

                override fun onAdLeftApplication(adInfo: LevelPlayAdInfo) {
                    // User left app
                }
            }

            // Load the banner
            bannerAdView?.loadAd()

        } catch (e: Exception) {
            callback.onAdFailedToLoad(
                AdError(
                    ErrorCode.PROVIDER_ERROR,
                    "Failed to load banner: ${e.message}",
                    e
                )
            )
        }
    }

    override fun pause() {
        bannerAdView?.pauseAutoRefresh()
    }

    override fun resume() {
        bannerAdView?.resumeAutoRefresh()
    }

    override fun destroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        isLoaded = false
    }

    private fun getLevelPlayBannerSize(size: BannerSize) =
        when (size) {
            BannerSize.BANNER -> LevelPlayAdSize.BANNER
            BannerSize.LARGE_BANNER -> LevelPlayAdSize.LARGE
            BannerSize.MEDIUM_RECTANGLE -> LevelPlayAdSize.MEDIUM_RECTANGLE
            BannerSize.LEADERBOARD -> LevelPlayAdSize.BANNER // LevelPlay doesn't have leaderboard
            BannerSize.ADAPTIVE -> LevelPlayAdSize.createAdaptiveAdSize(activity) ?: LevelPlayAdSize.BANNER
            BannerSize.FULL_BANNER -> LevelPlayAdSize.BANNER
        }
}