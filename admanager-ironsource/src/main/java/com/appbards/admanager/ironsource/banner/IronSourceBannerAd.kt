package com.appbards.admanager.ironsource.banner

import android.app.Activity
import android.view.View
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
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

    override fun load(size: BannerSize): AdResult {
        try {
            // Destroy old banner if exists
            bannerAdView?.destroy()
            bannerAdView = null
            isLoaded = false

            // Convert our BannerSize to LevelPlay size
            val levelPlaySize = getLevelPlayBannerSize(size)

            // Build banner config
            val adConfig = LevelPlayBannerAdView.Config.Builder()
                .setAdSize(levelPlaySize)
                .build()

            // Create banner ad view
            bannerAdView = LevelPlayBannerAdView(activity, adUnitId, adConfig)

            // Set listener
            bannerAdView?.bannerListener = object : LevelPlayBannerAdViewListener {
                override fun onAdLoaded(p0: LevelPlayAdInfo) {
                    isLoaded = true
                }

                override fun onAdLoadFailed(p0: LevelPlayAdError) {
                    isLoaded = false
                    bannerAdView = null
                }

                override fun onAdDisplayed(p0: LevelPlayAdInfo) {
                    super.onAdDisplayed(p0)
                }

                override fun onAdDisplayFailed(
                    p0: LevelPlayAdInfo,
                    p1: LevelPlayAdError
                ) {
                    super.onAdDisplayFailed(p0, p1)
                }

                override fun onAdClicked(p0: LevelPlayAdInfo) {
                    super.onAdClicked(p0)
                }

                override fun onAdExpanded(p0: LevelPlayAdInfo) {
                    super.onAdExpanded(p0)
                }

                override fun onAdCollapsed(p0: LevelPlayAdInfo) {
                    super.onAdCollapsed(p0)
                }

                override fun onAdLeftApplication(p0: LevelPlayAdInfo) {
                    super.onAdLeftApplication(p0)
                }
            }

            // Load the banner
            bannerAdView?.loadAd()

            return AdResult.Success("Banner ad loading")
        } catch (e: Exception) {
            return AdResult.Failure(
                AdError(
                    ErrorCode.PROVIDER_ERROR,
                    "Failed to load banner: \${e.message}",
                    e
                )
            )
        }
    }

    override fun getView(): View? {
        return if (isLoaded) bannerAdView else null
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