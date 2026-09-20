package com.appbards.admanager.admob.banner

import android.app.Activity
import android.view.ViewGroup
import com.appbards.admanager.admob.internal.onMainThread
import com.appbards.admanager.core.callback.BannerAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.BannerSize
import com.appbards.admanager.core.provider.IBannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

class AdMobBannerAd(
    // Activity context is required by ironSource mediation — passing an
    // application Context here causes ironSource ad loads to fail.
    private val activity: Activity,
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

        // Ad unit ID and ad size are declared on the request now, not on the AdView.
        val adView = AdView(activity).also { this.adView = it }
        val adRequest = BannerAdRequest.Builder(adUnitId, size.toAdMobSize(container)).build()

        container.removeAllViews()
        container.addView(adView)

        // loadAd() registers the loaded BannerAd with this AdView itself; the
        // AdLoadCallback only has to wire up event reporting. Next-Gen callbacks arrive
        // on a background thread, so everything reaching app code is posted to main.
        adView.loadAd(
            adRequest,
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    ad.adEventCallback = object : BannerAdEventCallback {
                        override fun onAdImpression() {
                            onMainThread { callback.onAdShown() }
                        }

                        override fun onAdClicked() {
                            onMainThread { callback.onAdClicked() }
                        }

                        // A banner that opens an overlay reports it as full screen
                        // content, which is the old onAdOpened/onAdClosed pair.
                        override fun onAdShowedFullScreenContent() {
                            onMainThread { callback.onBannerExpanded() }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            onMainThread {
                                callback.onBannerCollapsed()
                                callback.onAdClosed()
                            }
                        }
                    }
                    onMainThread { callback.onAdLoaded() }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    onMainThread {
                        callback.onAdFailedToLoad(
                            AdError(ErrorCode.NO_FILL, adError.message, adError)
                        )
                    }
                }
            }
        )
    }

    // The Next-Gen AdView has no pause()/resume() — the SDK manages the banner's
    // lifecycle itself. Kept as no-ops so the IBannerAd contract is unchanged.
    @Deprecated(message = "Not used any more in Next-Gen")
    override fun pause() = Unit

    @Deprecated(message = "Not used any more in Next-Gen")
    override fun resume() = Unit

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
            val displayMetrics = activity.resources.displayMetrics
            val adWidth = (container.width / displayMetrics.density).toInt()
                .takeIf { it > 0 }
                ?: (displayMetrics.widthPixels / displayMetrics.density).toInt()
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, adWidth)
        }
    }
}
