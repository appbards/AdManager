package com.appbards.admanager.admob

import android.app.Activity
import com.appbards.admanager.core.config.AdConfig
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.provider.IAdProvider
import com.appbards.admanager.core.provider.IAppOpenAd
import com.appbards.admanager.core.provider.IBannerAd
import com.appbards.admanager.core.provider.IInterstitialAd
import com.appbards.admanager.core.provider.INativeAd
import com.appbards.admanager.core.provider.IRewardedAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AdMobProvider(
    private val activity: Activity
) : IAdProvider {

    private var initialized = false
    private lateinit var consentInformation: ConsentInformation

    override suspend fun initialize(config: AdConfig): AdResult =
        suspendCancellableCoroutine { continuation ->
            try {
                consentInformation = UserMessagingPlatform.getConsentInformation(activity)

                val consentParamsBuilder = ConsentRequestParameters.Builder()
                    .setTagForUnderAgeOfConsent(false)

                // In test mode, force EEA geography so the consent form always appears
                // and register any test device hashes provided
                if (config.testMode) {
                    val debugSettings = ConsentDebugSettings.Builder(activity)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .apply {
                            config.testDeviceIds.forEach { addTestDeviceHashedId(it) }
                        }
                        .build()
                    consentParamsBuilder.setConsentDebugSettings(debugSettings)
                }

                val consentParams = consentParamsBuilder.build()

                // Step 1: Check if consent info needs updating
                consentInformation.requestConsentInfoUpdate(
                    activity,
                    consentParams,
                    {
                        // Step 2: show consent form if required
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                            // formError is non-null if the form failed to load/show.
                            // Either way we proceed — consent failure must never block ads.
                            initializeMobileAds(config, continuation)
                        }
                    },
                    { _ ->
                        // Consent info request failed — proceed to MobileAds init anyway
                        initializeMobileAds(config, continuation)
                    }
                )
            } catch (e: Exception) {
                continuation.resume(
                    AdResult.Failure(
                        AdError(ErrorCode.PROVIDER_ERROR, "AdMob init exception: ${e.message}", e)
                    )
                )
            }
        }

    private fun initializeMobileAds(
        config: AdConfig,
        continuation: kotlin.coroutines.Continuation<AdResult>
    ) {
        // Register physical test devices (emulators are handled automatically by the SDK)
        if (config.testDeviceIds.isNotEmpty()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(config.testDeviceIds)
                    .build()
            )
        }

        // OnInitializationCompleteListener fires once all mediation adapters have reported
        MobileAds.initialize(activity.applicationContext) {
            initialized = true
            continuation.resume(AdResult.Success("AdMob initialized successfully"))
        }
    }

    override fun isInitialized(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRewardedAd(placementId: String): IRewardedAd? {
        TODO("Not yet implemented")
    }

    override fun getInterstitialAd(placementId: String): IInterstitialAd? {
        TODO("Not yet implemented")
    }

    override fun getBannerAd(placementId: String): IBannerAd? {
        TODO("Not yet implemented")
    }

    override fun getNativeAd(placementId: String): INativeAd? {
        TODO("Not yet implemented")
    }

    override fun getAppOpenAd(placementId: String): IAppOpenAd? {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }
}