package com.appbards.admanager.admob.appOpen

import android.app.Activity
import android.content.Context
import com.appbards.admanager.core.callback.AppOpenAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.IAppOpenAd
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import kotlin.coroutines.resume

/*
    // Example splash screen flow
    adManager.getAppOpenAd()?.let { ad ->
        if (ad.isReady()) {
         ad.show(activity, object : AppOpenAdCallback {
             override fun onAdClosed() {
                 navigateToMain()
             }
             override fun onAdFailedToShow(error: AdError) {
                 navigateToMain()
             }
         })
        } else {
         navigateToMain()
        }
    }
 */


class AdMobAppOpenAd(
    private val context: Context,
    private val adUnitId: String
) : IAppOpenAd {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false  // internal — exposed so the app can check it
    private var loadTime: Long = 0

    companion object {
        private const val AD_EXPIRY_HOURS = 4L
    }

    override suspend fun load(): AdResult = suspendCancellableCoroutine { continuation ->
        if (isLoadingAd) {
            continuation.resume(
                AdResult.Failure(
                    AdError(ErrorCode.AD_ALREADY_LOADED, "App open ad is already loading")
                )
            )
            return@suspendCancellableCoroutine
        }

        isLoadingAd = true

        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    continuation.resume(AdResult.Success("App open ad loaded"))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isLoadingAd = false
                    continuation.resume(
                        AdResult.Failure(
                            AdError(ErrorCode.NO_FILL, error.message, error)
                        )
                    )
                }
            }
        )
    }

    // Checks both that the ad is loaded AND that it hasn't expired (4-hour limit)
    override fun isReady(): Boolean = appOpenAd != null && !isExpired()


    override fun show(activity: Activity, callback: AppOpenAdCallback) {
        if (isShowingAd) {
            callback.onAdFailedToShow(
                AdError(ErrorCode.SHOW_FAILED, "App open ad is already showing")
            )
            return
        }

        val ad = appOpenAd
        if (ad == null || isExpired()) {
            appOpenAd = null
            callback.onAdFailedToShow(
                AdError(ErrorCode.AD_NOT_READY, "App open ad not loaded or has expired")
            )
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                callback.onAdShown()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                appOpenAd = null
                isShowingAd = false
                callback.onAdFailedToShow(
                    AdError(ErrorCode.SHOW_FAILED, adError.message, adError)
                )
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null  // consumed — must reload before next show
                isShowingAd = false
                callback.onAdClosed()
            }

            override fun onAdClicked() {
                callback.onAdClicked()
            }
        }

        // Set isShowingAd before calling show() to prevent a race condition
        // where show() could be called again before onAdShowedFullScreenContent fires
        isShowingAd = true
        ad.show(activity)
    }

    override fun destroy() {
        appOpenAd = null
        isShowingAd = false
        isLoadingAd = false
    }

    private fun isExpired(): Boolean {
        val elapsedMs = Date().time - loadTime
        return elapsedMs >= AD_EXPIRY_HOURS * 3_600_000L
    }
}