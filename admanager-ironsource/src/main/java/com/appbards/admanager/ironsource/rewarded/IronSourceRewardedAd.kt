package com.appbards.admanager.ironsource.rewarded

import android.app.Activity
import com.appbards.admanager.core.callback.RewardedAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.AdReward
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.IRewardedAd
import com.unity3d.mediation.LevelPlayAdError
import com.unity3d.mediation.LevelPlayAdInfo
import com.unity3d.mediation.rewarded.LevelPlayReward
import com.unity3d.mediation.rewarded.LevelPlayRewardedAd
import com.unity3d.mediation.rewarded.LevelPlayRewardedAdListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class IronSourceRewardedAd(
    private val activity: Activity,
    private val adUnitId: String
) : IRewardedAd {

    private var rewardedAd: LevelPlayRewardedAd? = null
    private var currentCallback: RewardedAdCallback? = null
    private var loadContinuation: Continuation<AdResult>? = null

    init {
        // Create the rewarded ad object
        rewardedAd = LevelPlayRewardedAd(adUnitId)

        // Set listener ONCE during initialization (recommended before loading)
        rewardedAd?.setListener(object : LevelPlayRewardedAdListener {
            override fun onAdLoaded(p0: LevelPlayAdInfo) {
                // Resume load() coroutine if waiting
                loadContinuation?.let { continuation ->
                    continuation.resume(AdResult.Success("Rewarded ad loaded"))
                    loadContinuation = null
                }
            }

            override fun onAdLoadFailed(p0: LevelPlayAdError) {
                // Resume load() coroutine with failure
                loadContinuation?.let { continuation ->
                    continuation.resume(
                        AdResult.Failure(
                            AdError(
                                ErrorCode.NO_FILL,
                                "Failed to load rewarded ad: ${p0.errorMessage}",
                                p0
                            )
                        )
                    )
                    loadContinuation = null
                }
            }

            override fun onAdDisplayed(p0: LevelPlayAdInfo) {
                currentCallback?.onAdShown()
            }

            override fun onAdRewarded(
                p0: LevelPlayReward,
                p1: LevelPlayAdInfo
            ) {
                val adReward = AdReward(
                    type = p0.name,
                    amount = p0.amount
                )
                currentCallback?.onUserRewarded(adReward)
            }

            override fun onAdDisplayFailed(
                p0: LevelPlayAdError,
                p1: LevelPlayAdInfo
            ) {
                currentCallback?.onAdFailedToShow(
                    AdError(
                        ErrorCode.SHOW_FAILED,
                        "Failed to show rewarded ad: ${p0.errorMessage}",
                        p0
                    )
                )
                currentCallback = null
            }

            override fun onAdClicked(p0: LevelPlayAdInfo) {
                currentCallback?.onAdClicked()
            }

            override fun onAdClosed(p0: LevelPlayAdInfo) {
                currentCallback?.onAdClosed()
                currentCallback = null

                // Auto-reload for next time
                rewardedAd?.loadAd()
            }

            override fun onAdInfoChanged(p0: LevelPlayAdInfo) {
                // Called after the ad info is updated
                // Available when another Rewarded ad has loaded with a higher CPM/Rate
            }
        })
    }

    override suspend fun load(): AdResult = suspendCancellableCoroutine { continuation ->
        // Check if ad is already loaded
        if (rewardedAd?.isAdReady == true) {
            continuation.resume(AdResult.Success("Rewarded ad already loaded"))
            return@suspendCancellableCoroutine
        }

        loadContinuation = continuation

        // Load the ad
        rewardedAd?.loadAd()

        continuation.invokeOnCancellation {
            loadContinuation = null
        }
    }


    override fun isReady(): Boolean {
        return rewardedAd?.isAdReady ?: false
    }

    override fun show(
        activity: Activity,
        callback: RewardedAdCallback
    ) {
        currentCallback = callback

        // Check that ad is ready before showing
        if (rewardedAd?.isAdReady == true) {
            // Show the ad (listener already set in init)
            rewardedAd?.showAd(activity)
        } else {
            // Ad not ready
            callback.onAdFailedToShow(
                AdError(
                    ErrorCode.AD_NOT_READY,
                    "Rewarded ad is not ready to show"
                )
            )
            currentCallback = null
        }
    }

    override fun destroy() {
        currentCallback = null
        loadContinuation = null
        rewardedAd = null
    }
}