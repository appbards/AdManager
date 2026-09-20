package com.appbards.admanager.admob.appOpen

import android.app.Activity
import android.content.Context
import com.appbards.admanager.admob.internal.onMainThread
import com.appbards.admanager.core.callback.AppOpenAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.IAppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
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
    private val adUnitId: String
) : IAppOpenAd {

    @Deprecated(
        message = "The context parameter is unused since the GMA Next-Gen migration " +
                "(AppOpenAd.load() no longer takes a Context) and will be removed in 2.0.0.",
        replaceWith = ReplaceWith(
            "AdMobAppOpenAd(adUnitId)",
            "com.appbards.admanager.admob.appOpen.AdMobAppOpenAd"
        ),
        level = DeprecationLevel.WARNING
    )
    constructor(context: Context, adUnitId: String) : this(adUnitId)

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

        // The ad unit ID now lives on the request, and load() no longer takes a Context.
        AppOpenAd.load(
            AdRequest.Builder(adUnitId).build(),
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd?.destroy()
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    continuation.resume(AdResult.Success("App open ad loaded"))
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    discardAd()
                    isLoadingAd = false
                    continuation.resume(
                        AdResult.Failure(
                            AdError(ErrorCode.NO_FILL, adError.message, adError)
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
            discardAd()
            callback.onAdFailedToShow(
                AdError(ErrorCode.AD_NOT_READY, "App open ad not loaded or has expired")
            )
            return
        }

        // Next-Gen callbacks arrive on a background thread — everything that reaches
        // app code has to be handed back to the main thread.
        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                onMainThread { callback.onAdShown() }
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                appOpenAd = null
                isShowingAd = false
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
                appOpenAd = null  // consumed — must reload before next show
                isShowingAd = false
                onMainThread { callback.onAdClosed() }
            }

            override fun onAdClicked() {
                onMainThread { callback.onAdClicked() }
            }
        }

        // Set isShowingAd before calling show() to prevent a race condition
        // where show() could be called again before onAdShowedFullScreenContent fires
        isShowingAd = true
        ad.show(activity)
    }

    override fun destroy() {
        discardAd()
        isShowingAd = false
        isLoadingAd = false
    }

    private fun discardAd() {
        appOpenAd?.destroy()
        appOpenAd = null
    }

    private fun isExpired(): Boolean {
        val elapsedMs = Date().time - loadTime
        return elapsedMs >= AD_EXPIRY_HOURS * 3_600_000L
    }
}
