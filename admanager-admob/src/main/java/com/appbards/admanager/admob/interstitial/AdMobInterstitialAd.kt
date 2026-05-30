package com.appbards.admanager.admob.interstitial

import android.app.Activity
import android.content.Context
import com.appbards.admanager.core.callback.InterstitialAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.IInterstitialAd
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AdMobInterstitialAd(
    private val context: Context,
    private val adUnitId: String
) : IInterstitialAd {

    private var interstitialAd: InterstitialAd? = null

    override suspend fun load(): AdResult = suspendCancellableCoroutine { continuation ->
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    continuation.resume(AdResult.Success("Interstitial loaded"))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    continuation.resume(
                        AdResult.Failure(
                            AdError(ErrorCode.NO_FILL, error.message, error)
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

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                interstitialAd = null  // consumed — core will trigger reload via autoPreload
                callback.onAdShown()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                callback.onAdFailedToShow(
                    AdError(ErrorCode.SHOW_FAILED, adError.message, adError)
                )
            }

            override fun onAdDismissedFullScreenContent() {
                callback.onAdClosed()
            }

            override fun onAdClicked() {
                callback.onAdClicked()
            }
        }
        ad.show(activity)
    }

    override fun destroy() {
        interstitialAd = null
    }
}