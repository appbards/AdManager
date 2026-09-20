package com.appbards.admanager.admob.rewarded

import android.app.Activity
import com.appbards.admanager.admob.internal.onMainThread
import com.appbards.admanager.core.callback.RewardedAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.AdReward
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.IRewardedAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AdMobRewardedAd (
    // Activity context is required by ironSource mediation — passing an
    // application Context here causes ironSource ad loads to fail.
    private val activity: Activity,
    private val adUnitId: String
) : IRewardedAd {

    private var rewardedAd: RewardedAd? = null

    override suspend fun load(): AdResult = suspendCancellableCoroutine { continuation ->
        // The ad unit ID now lives on the request, and load() no longer takes a Context.
        val adRequest = AdRequest.Builder(adUnitId).build()

        RewardedAd.load(
            adRequest,
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd?.destroy()
                    rewardedAd = ad
                    continuation.resume(AdResult.Success("Rewarded ad loaded"))
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

    override fun isReady(): Boolean = rewardedAd != null


    override fun show(activity: Activity, callback: RewardedAdCallback) {
        val ad = rewardedAd
        if (ad == null) {
            callback.onAdFailedToShow(
                AdError(ErrorCode.AD_NOT_READY, "Rewarded ad not loaded")
            )
            return
        }

        // Next-Gen callbacks arrive on a background thread — everything that reaches
        // app code has to be handed back to the main thread.
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                rewardedAd = null  // consumed — core will trigger reload via autoPreload
                onMainThread { callback.onAdShown() }
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                rewardedAd = null
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

        ad.show(activity, object : OnUserEarnedRewardListener {
            override fun onUserEarnedReward(reward: RewardItem) {
                // Called by AdMob when the user earns the reward.
                // Note: this fires BEFORE onAdDismissedFullScreenContent.
                onMainThread {
                    callback.onUserRewarded(
                        AdReward(
                            type = reward.type,
                            amount = reward.amount
                        )
                    )
                }
            }
        })
    }

    override fun destroy() {
        discardAd()
    }

    private fun discardAd() {
        rewardedAd?.destroy()
        rewardedAd = null
    }
}
