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
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/*
    // In the app — completely custom layout, no library dependency on the design.
    // The layout's root must be a
    // com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView and its media
    // slot a com.google.android.libraries.ads.mobile.sdk.nativead.MediaView.
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
    private val adUnitId: String
) : INativeAd {

    @Deprecated(
        message = "The context parameter is unused since the GMA Next-Gen migration " +
                "(NativeAdLoader.load() no longer takes a Context) and will be removed in 2.0.0.",
        replaceWith = ReplaceWith(
            "AdMobNativeAd(adUnitId)",
            "com.appbards.admanager.admob.nativeAd.AdMobNativeAd"
        ),
        level = DeprecationLevel.WARNING
    )
    constructor(context: Context, adUnitId: String) : this(adUnitId)

    private var nativeAd: NativeAd? = null


    override suspend fun load(): AdResult = suspendCancellableCoroutine { continuation ->
        // AdLoader is gone: the ad unit ID, requested native types and the options that
        // used to live in NativeAdOptions are all declared on NativeAdRequest now.
        val request = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
            .setAdChoicesPlacement(AdChoicesPlacement.TOP_RIGHT)
            .build()

        // NativeAdLoader is used statically, and loads a single ad per call — which is
        // what mediation requires anyway.
        NativeAdLoader.load(
            request,
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    this@AdMobNativeAd.nativeAd?.destroy()
                    this@AdMobNativeAd.nativeAd = nativeAd
                    if (continuation.isActive) {
                        continuation.resume(AdResult.Success("Native ad loaded"))
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    nativeAd?.destroy()
                    nativeAd = null
                    if (continuation.isActive) {
                        continuation.resume(
                            AdResult.Failure(AdError(ErrorCode.NO_FILL, adError.message, adError))
                        )
                    }
                }
            }
        )
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
        // the slot, so hide it and rely on the icon above. Otherwise show it and let
        // registerNativeAd() below populate it.
        val mediaView = binder.mediaView as? MediaView
        if (binder.preferIcon) {
            mediaView?.visibility = View.GONE
        } else {
            mediaView?.visibility = View.VISIBLE
        }

        // Register the NativeAd — wires up clicks, impressions, the media content and
        // the AdChoices overlay. This replaces setNativeAd() plus the manual
        // mediaView/mediaContent assignment; the media view is passed in here instead.
        adView.registerNativeAd(ad, mediaView.takeUnless { binder.preferIcon })

        callback.onAdShown()
        callback.onNativeAdImpression()

        return adView
    }

    override fun destroy() {
        nativeAd?.destroy()
        nativeAd = null
    }
}
