package com.appbards.admanager.admob.nativeAd

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import com.appbards.admanager.core.callback.NativeAdCallback
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.nativeAd.NativeAdViewBinder
import com.appbards.admanager.core.provider.INativeAd
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/*
    // In the app — completely custom layout, no library dependency on the design
    val adView = layoutInflater.inflate(R.layout.my_custom_native_ad, null) as NativeAdView

    nativeAd.show(
     container = binding.nativeAdContainer,
     binder = NativeAdViewBinder(
            rootView        = adView,
         headlineView    = adView.findViewById(R.id.my_headline),
         bodyView        = adView.findViewById(R.id.my_body),
         mediaView       = adView.findViewById(R.id.my_media),
         callToActionView = adView.findViewById(R.id.my_cta_button)
        ),
        callback = myCallback
    )
 */

class AdMobNativeAd(
    private val context: Context,
    private val adUnitId: String
) : INativeAd {

    private var nativeAd: NativeAd? = null


    override suspend fun load(): AdResult = suspendCancellableCoroutine { continuation ->
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    continuation.resume(AdResult.Success("Native ad loaded"))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAd = null
                    continuation.resume(
                        AdResult.Failure(AdError(ErrorCode.NO_FILL, error.message, error))
                    )
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        // loadAd() must be used instead of loadAds() when mediation is enabled
        adLoader.loadAd(AdRequest.Builder().build())
    }

    override fun isReady(): Boolean = nativeAd != null


    override fun show(container: ViewGroup, binder: NativeAdViewBinder, callback: NativeAdCallback) {
        val ad = nativeAd
        if (ad == null) {
            callback.onAdFailedToShow(AdError(ErrorCode.AD_NOT_READY, "Native ad not loaded"))
            return
        }

        // The app must provide a NativeAdView as the rootView
        val adView = binder.rootView as? NativeAdView
        if (adView == null) {
            callback.onAdFailedToShow(
                AdError(ErrorCode.SHOW_FAILED, "NativeAdViewBinder.rootView must be a NativeAdView")
            )
            return
        }

        // Register asset views — only non-null views are registered.
        // The SDK uses these registrations for click and impression tracking.
        binder.headlineView?.let     { adView.headlineView = it }
        binder.bodyView?.let         { adView.bodyView = it }
        binder.callToActionView?.let { adView.callToActionView = it }
        binder.advertiserView?.let   { adView.advertiserView = it }
        binder.iconView?.let         { adView.iconView = it }
        binder.storeView?.let        { adView.storeView = it }
        binder.priceView?.let        { adView.priceView = it }
        binder.starRatingView?.let   { adView.starRatingView = it }
        (binder.mediaView as? MediaView)?.let { adView.mediaView = it }

        // Populate views with ad content — assets are nullable
        binder.headlineView?.text = ad.headline
        ad.body?.let         { binder.bodyView?.text = it }
        ad.callToAction?.let { (binder.callToActionView as? Button)?.text = it }
        ad.advertiser?.let   { binder.advertiserView?.text = it }
        ad.store?.let        { binder.storeView?.text = it }
        ad.price?.let        { binder.priceView?.text = it }
        ad.mediaContent?.let { (binder.mediaView as? MediaView)?.mediaContent = it }

        // Register the NativeAd — wires up clicks, impressions and AdChoices overlay
        adView.setNativeAd(ad)

        callback.onAdShown()
        callback.onNativeAdImpression()

        container.removeAllViews()
        container.addView(adView)
    }

    override fun destroy() {
        nativeAd?.destroy()
        nativeAd = null
    }
}