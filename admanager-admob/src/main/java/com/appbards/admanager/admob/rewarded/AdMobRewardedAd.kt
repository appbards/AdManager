package com.appbards.admanager.admob.rewarded

import android.app.Activity
import android.content.Context
import com.appbards.admanager.core.callback.RewardedAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.AdReward
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.IRewardedAd
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AdMobRewardedAd (
    private val context: Context,
    private val adUnitId: String
) : IRewardedAd {

    private var rewardedAd: RewardedAd? = null

    override suspend fun load(): AdResult = suspendCancellableCoroutine { continuation ->
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    continuation.resume(AdResult.Success("Rewarded ad loaded"))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    continuation.resume(
                        AdResult.Failure(
                            AdError(ErrorCode.NO_FILL, error.message, error)
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

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                rewardedAd = null  // consumed — core will trigger reload via autoPreload
                callback.onAdShown()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                rewardedAd = null
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

        ad.show(activity) { rewardItem ->
            // Called by AdMob when the user earns the reward.
            // Note: this fires BEFORE onAdDismissedFullScreenContent.
            callback.onUserRewarded(
                AdReward(
                    type = rewardItem.type,
                    amount = rewardItem.amount
                )
            )
        }
    }

    override fun destroy() {
        rewardedAd = null
    }
}