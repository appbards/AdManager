package com.appbards.admanager.admob.nativeAd

import android.content.Context
import android.view.View
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
        val adView = bind(binder, callback) ?: return

        container.removeAllViews()
        container.addView(adView)
    }

    /**
     * Compose-friendly show. The [binder].rootView (a NativeAdView) is expected to be
     * already attached to the composition by the caller (e.g. via `AndroidView`), so we
     * only bind and register — no container swap.
     *
     * Note: this fires impression/registration on every call, so callers must guard
     * against re-invoking it on each recomposition.
     */
    override fun show(binder: NativeAdViewBinder, callback: NativeAdCallback) {
        bind(binder, callback)
    }

    /**
     * Registers asset views, populates them with ad content and wires up the NativeAd.
     * Returns the [NativeAdView] on success, or null (after invoking the failure
     * callback) when the ad isn't loaded or the rootView isn't a NativeAdView.
     */
    private fun bind(binder: NativeAdViewBinder, callback: NativeAdCallback): NativeAdView? {
        val ad = nativeAd
        if (ad == null) {
            callback.onAdFailedToShow(AdError(ErrorCode.AD_NOT_READY, "Native ad not loaded"))
            return null
        }

        // The app must provide a NativeAdView as the rootView
        val adView = binder.rootView as? NativeAdView
        if (adView == null) {
            callback.onAdFailedToShow(
                AdError(ErrorCode.SHOW_FAILED, "NativeAdViewBinder.rootView must be a NativeAdView")
            )
            return null
        }

        // Register asset views — only non-null views are registered.
        // The SDK uses these registrations for click and impression tracking.
        binder.headlineView?.let     { adView.headlineView = it }
        binder.bodyView?.let         { adView.bodyView = it }
        binder.callToActionView?.let { adView.callToActionView = it }
        binder.advertiserView?.let   { adView.advertiserView = it }
        binder.storeView?.let        { adView.storeView = it }
        binder.priceView?.let        { adView.priceView = it }
        binder.starRatingView?.let   { adView.starRatingView = it }

        // Populate views with ad content — assets are nullable
        binder.headlineView?.text = ad.headline
        ad.body?.let         { binder.bodyView?.text = it }
        ad.callToAction?.let { (binder.callToActionView as? Button)?.text = it }
        ad.advertiser?.let   { binder.advertiserView?.text = it }
        ad.store?.let        { binder.storeView?.text = it }
        ad.price?.let        { binder.priceView?.text = it }

        // Icon — register and populate. Hide the view when the ad has no icon
        // asset so it doesn't leave an empty box.
        binder.iconView?.let { iconView ->
            val iconDrawable = ad.icon?.drawable
            if (iconDrawable != null) {
                iconView.setImageDrawable(iconDrawable)
                iconView.visibility = View.VISIBLE
                adView.iconView = iconView
            } else {
                iconView.visibility = View.GONE
            }
        }

        // Media vs icon: in preferIcon mode the media view would be too big for
        // the slot, so hide it and rely on the icon above. Otherwise register and
        // populate the media view as usual.
        val mediaView = binder.mediaView as? MediaView
        if (binder.preferIcon) {
            mediaView?.visibility = View.GONE
        } else {
            mediaView?.let {
                it.visibility = View.VISIBLE
                adView.mediaView = it
                ad.mediaContent?.let { content -> it.mediaContent = content }
            }
        }

        // Register the NativeAd — wires up clicks, impressions and AdChoices overlay
        adView.setNativeAd(ad)

        callback.onAdShown()
        callback.onNativeAdImpression()

        return adView
    }

    override fun destroy() {
        nativeAd?.destroy()
        nativeAd = null
    }
}