package com.appbards.admanager.admob.banner

import android.content.Context
import android.view.ViewGroup
import com.appbards.admanager.core.callback.BannerAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.BannerSize
import com.appbards.admanager.core.provider.IBannerAd
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class AdMobBannerAd(
    private val context: Context,
    private val adUnitId: String
) : IBannerAd {

    private var adView: AdView? = null


    override fun load(
        container: ViewGroup,
        size: BannerSize,
        callback: BannerAdCallback
    ) {
        // Clean up any existing banner before loading a new one
        adView?.destroy()

        val adView = AdView(context).also { this.adView = it }
        adView.adUnitId = adUnitId
        adView.setAdSize(size.toAdMobSize(container))

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                callback.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                callback.onAdFailedToLoad(
                    AdError(ErrorCode.NO_FILL, error.message, error)
                )
            }

            override fun onAdImpression() {
                callback.onAdShown()
            }

            override fun onAdClicked() {
                callback.onAdClicked()
            }

            override fun onAdOpened() {
                callback.onBannerExpanded()
            }

            override fun onAdClosed() {
                callback.onBannerCollapsed()
                callback.onAdClosed()
            }
        }

        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    override fun pause() {
        adView?.pause()
    }

    override fun resume() {
        adView?.resume()
    }

    override fun destroy() {
        val parentView = adView?.parent
        if (parentView is ViewGroup) {
            parentView.removeView(adView)
        }
        adView?.destroy()
        adView = null
    }

    private fun BannerSize.toAdMobSize(container: ViewGroup): AdSize = when (this) {
        BannerSize.BANNER -> AdSize.BANNER
        BannerSize.LARGE_BANNER -> AdSize.LARGE_BANNER
        BannerSize.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
        BannerSize.FULL_BANNER -> AdSize.FULL_BANNER
        BannerSize.LEADERBOARD -> AdSize.LEADERBOARD
        BannerSize.ADAPTIVE -> {
            val displayMetrics = context.resources.displayMetrics
            val adWidth = (container.width / displayMetrics.density).toInt()
                .takeIf { it > 0 }
                ?: (displayMetrics.widthPixels / displayMetrics.density).toInt()
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidth)
        }
    }
}