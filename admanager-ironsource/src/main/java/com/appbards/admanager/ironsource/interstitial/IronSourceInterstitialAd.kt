package com.appbards.admanager.ironsource.interstitial

import android.app.Activity
import com.appbards.admanager.core.callback.InterstitialAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.IInterstitialAd
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAd
import com.unity3d.mediation.interstitial.LevelPlayInterstitialAdListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * IronSource/LevelPlay Interstitial Ad implementation
 */
class IronSourceInterstitialAd(
    private val activity: Activity,
    private val adUnitId: String
) : IInterstitialAd {

    private var interstitialAd: LevelPlayInterstitialAd? = null
    private var currentCallback: InterstitialAdCallback? = null
    private var loadContinuation: Continuation<AdResult>? = null

    init {
        // Create and configure the interstitial ad instance
        interstitialAd = LevelPlayInterstitialAd(adUnitId)

        // Set listener ONCE during initialization
        interstitialAd?.setListener(object : LevelPlayInterstitialAdListener {
            override fun onAdLoaded(adInfo: LevelPlayAdInfo) {
                // Resume load() coroutine if waiting
                loadContinuation?.let { continuation ->
                    continuation.resume(AdResult.Success("Interstitial ad loaded"))
                    loadContinuation = null
                }
            }

            override fun onAdLoadFailed(error: LevelPlayAdError) {
                // Resume load() coroutine with failure
                loadContinuation?.let { continuation ->
                    continuation.resume(
                        AdResult.Failure(
                            AdError(
                                ErrorCode.NO_FILL,
                                "Failed to load interstitial: ${error.errorMessage}",
                                error
                            )
                        )
                    )
                    loadContinuation = null
                }

                // Also notify show callback if ad failed during show
                currentCallback?.onAdFailedToShow(
                    AdError(
                        ErrorCode.AD_NOT_READY,
                        "Ad failed to load: ${error.errorMessage}",
                        error
                    )
                )
                currentCallback = null
            }

            override fun onAdDisplayed(adInfo: LevelPlayAdInfo) {
                currentCallback?.onAdShown()
            }

            override fun onAdClosed(adInfo: LevelPlayAdInfo) {
                currentCallback?.onAdClosed()
                currentCallback = null

                // Auto-reload for next time
                interstitialAd?.loadAd()
            }

            override fun onAdDisplayFailed(error: LevelPlayAdError, adInfo: LevelPlayAdInfo) {
                currentCallback?.onAdFailedToShow(
                    AdError(
                        ErrorCode.SHOW_FAILED,
                        "Failed to show interstitial: ${error.errorMessage}",
                        error
                    )
                )
                currentCallback = null
            }
        })
    }

    override suspend fun load(): AdResult = suspendCancellableCoroutine { continuation ->
        // Check if ad is already loaded
        if (interstitialAd?.isAdReady == true) {
            continuation.resume(AdResult.Success("Interstitial ad already loaded"))
            return@suspendCancellableCoroutine
        }

        loadContinuation = continuation

        // Load the ad
        interstitialAd?.loadAd()

        continuation.invokeOnCancellation {
            loadContinuation = null
        }
    }


    override fun isReady(): Boolean {
        return interstitialAd?.isAdReady ?: false
    }

    override fun show(activity: Activity, callback: InterstitialAdCallback) {
        currentCallback = callback

        // Show the ad (listener already set in init)
        interstitialAd?.showAd(activity)
    }

    override fun destroy() {
        currentCallback = null
        loadContinuation = null
        interstitialAd = null
    }
}
