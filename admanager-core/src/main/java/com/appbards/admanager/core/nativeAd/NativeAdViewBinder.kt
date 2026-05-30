package com.appbards.admanager.core.nativeAd

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Holds references to the views in your custom native ad layout.
 * Inflate your own layout in the app, find views by ID, then pass
 * this binder to INativeAd.show() — the library populates the data.
 *
 * All fields except [rootView] are optional. Only non-null views
 * will be populated and registered with the ad SDK.
 */
data class NativeAdViewBinder(
    val rootView: ViewGroup,
    val headlineView: TextView? = null,
    val bodyView: TextView? = null,
    val mediaView: View? = null,
    val callToActionView: View? = null,
    val advertiserView: TextView? = null,
    val iconView: ImageView? = null,
    val storeView: TextView? = null,
    val priceView: TextView? = null,
    val starRatingView: View? = null
)