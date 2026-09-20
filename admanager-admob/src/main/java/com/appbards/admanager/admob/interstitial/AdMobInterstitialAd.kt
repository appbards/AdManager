package com.appbards.admanager.admob.interstitial

import android.app.Activity
import com.appbards.admanager.admob.internal.onMainThread
import com.appbards.admanager.core.callback.InterstitialAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.IInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AdMobInterstitialAd(
    // Activity context is required by ironSource mediation — passing an
    // application Context here causes ironSource ad loads to fail.
    private val activity: Activity,
    private val adUnitId: String
) : IInterstitialAd {

    private var interstitialAd: InterstitialAd? = null

    override suspend fun load(): AdResult = suspendCancellableCoroutine { continuation ->
        // The ad unit ID now lives on the request, and load() no longer takes a Context.
        val adRequest = AdRequest.Builder(adUnitId).build()

        InterstitialAd.load(
            adRequest,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd?.destroy()
                    interstitialAd = ad
                    continuation.resume(AdResult.Success("Interstitial loaded"))
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    discardAd()
                    continuation.resume(
                        AdResult.Failure(
                            AdError(ErrorCode.NO_FILL, adError.message, adError)
                        )
                    )
                }
            }
        )
    }

    override fun isReady(): Boolean = interstitialAd != null

    override fun show(
        activity: Activity,
        callback: InterstitialAdCallback
    ) {
        val ad = interstitialAd
        if (ad == null) {
            callback.onAdFailedToShow(
                AdError(ErrorCode.AD_NOT_READY, "Interstitial ad not loaded")
            )
            return
        }

        // Next-Gen callbacks arrive on a background thread — everything that reaches
        // app code has to be handed back to the main thread.
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                interstitialAd = null  // consumed — core will trigger reload via autoPreload
                onMainThread { callback.onAdShown() }
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                interstitialAd = null
                onMainThread {
                    callback.onAdFailedToShow(
                        AdError(
                            ErrorCode.SHOW_FAILED,
                            fullScreenContentError.message,
                            fullScreenContentError
                        )
                    )
                }
            }

            override fun onAdDismissedFullScreenContent() {
                onMainThread { callback.onAdClosed() }
            }

            override fun onAdClicked() {
                onMainThread { callback.onAdClicked() }
            }
        }
        ad.show(activity)
    }

    override fun destroy() {
        discardAd()
    }

    private fun discardAd() {
        interstitialAd?.destroy()
        interstitialAd = null
    }
}
