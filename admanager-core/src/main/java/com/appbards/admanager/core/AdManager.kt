package com.appbards.admanager.core

import android.app.Activity
import android.view.ViewGroup
import com.appbards.admanager.core.callback.AppOpenAdCallback
import com.appbards.admanager.core.callback.BannerAdCallback
import com.appbards.admanager.core.callback.InterstitialAdCallback
import com.appbards.admanager.core.callback.NativeAdCallback
import com.appbards.admanager.core.callback.RewardedAdCallback
import com.appbards.admanager.core.config.AdConfig
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.AdReward
import com.appbards.admanager.core.model.ErrorCode
import com.appbards.admanager.core.model.InitializationResult
import com.appbards.admanager.core.nativeAd.NativeAdViewBinder
import com.appbards.admanager.core.provider.BannerSize
import com.appbards.admanager.core.provider.IAdProvider
import com.appbards.admanager.core.provider.IAppOpenAd
import com.appbards.admanager.core.provider.IBannerAd
import com.appbards.admanager.core.provider.IInterstitialAd
import com.appbards.admanager.core.provider.INativeAd
import com.appbards.admanager.core.provider.IRewardedAd
import com.appbards.admanager.core.util.AdLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object AdManager {
    private var provider: IAdProvider? = null
    private var config: AdConfig? = null

    // Internal ad instances
    private var appOpenAd: IAppOpenAd? = null
    private var interstitialAd: IInterstitialAd? = null
    private var rewardedAd: IRewardedAd? = null

    // Coroutine scope for background operations
    private val adScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Frequency control
    private var interstitialCallCount = 0

    /**
     * Standard initialization without loading screen timing
     */
    suspend fun initialize(adProvider: IAdProvider, config: AdConfig): AdResult {
        this.config = config
        AdLogger.enable(config.enableLogging)
        AdLogger.i("Initializing AdManager with ${adProvider::class.simpleName}")

        return adProvider.initialize(config).also { result ->
            when (result) {
                is AdResult.Success -> {
                    provider = adProvider
                    AdLogger.i("AdManager initialized successfully")

                    // Auto-preload ads if enabled
                    if (config.autoPreloadInterstitial) {
                        preloadInterstitial()
                    }
                    if (config.autoPreloadRewarded) {
                        preloadRewarded()
                    }
                }

                is AdResult.Failure -> {
                    AdLogger.e("AdManager initialization failed: ${result.error.message}")
                }
            }
        }
    }

    /**
     * Initialize with loading screen timing constraints
     * - Callback won't be called before minTime
     * - Callback will be called at maxTime regardless of initialization status
     * - If initialization fails, callback is called after minTime
     */
    suspend fun initializeWithLoading(
        adProvider: IAdProvider,
        config: AdConfig,
        onComplete: (InitializationResult) -> Unit
    ) {
        this.config = config
        AdLogger.enable(config.enableLogging)
        AdLogger.i("Initializing AdManager with loading constraints: min=${config.loadingMinTime}ms, max=${config.loadingMaxTime}ms")

        val startTime = System.currentTimeMillis()

        coroutineScope {
            var initResult: AdResult? = null
            var initCompleted = false

            // Start initialization
            val initJob = launch {
                initResult = adProvider.initialize(config)
                initCompleted = true
                if (initResult is AdResult.Success) {
                    provider = adProvider

                    // Auto-preload ads if enabled
                    if (config.autoPreloadInterstitial) {
                        preloadInterstitial()
                    }
                    if (config.autoPreloadRewarded) {
                        preloadRewarded()
                    }
                }
            }

            // Wait for minTime or maxTime
            launch {
                delay(config.loadingMinTime)

                if (initCompleted) {
                    // Init finished before minTime - we've already waited minTime
                    val elapsed = System.currentTimeMillis() - startTime
                    when (val result = initResult) {
                        is AdResult.Success -> {
                            AdLogger.i("Initialization completed in ${elapsed}ms (delayed to minTime)")
                            onComplete(
                                InitializationResult.Success(
                                    actualInitTime = elapsed,
                                    wasDelayed = true
                                )
                            )
                        }

                        is AdResult.Failure -> {
                            AdLogger.e("Initialization failed: ${result.error.message} (delayed to minTime)")
                            onComplete(
                                InitializationResult.Failed(
                                    error = result.error,
                                    wasDelayed = true
                                )
                            )
                        }

                        null -> { /* shouldn't happen */
                        }
                    }
                } else {
                    // Init still running after minTime - wait up to maxTime
                    val remainingTime = config.loadingMaxTime - config.loadingMinTime

                    withTimeoutOrNull(remainingTime) {
                        initJob.join()
                    }

                    val elapsed = System.currentTimeMillis() - startTime

                    if (initCompleted) {
                        // Completed between minTime and maxTime
                        when (val result = initResult) {
                            is AdResult.Success -> {
                                AdLogger.i("Initialization completed in ${elapsed}ms")
                                onComplete(
                                    InitializationResult.Success(
                                        actualInitTime = elapsed,
                                        wasDelayed = false
                                    )
                                )
                            }

                            is AdResult.Failure -> {
                                AdLogger.e("Initialization failed: ${result.error.message}")
                                onComplete(
                                    InitializationResult.Failed(
                                        error = result.error,
                                        wasDelayed = false
                                    )
                                )
                            }

                            null -> { /* shouldn't happen */
                            }
                        }
                    } else {
                        // Timed out at maxTime
                        initJob.cancel()
                        AdLogger.w("Initialization timed out at maxTime (${config.loadingMaxTime}ms)")
                        onComplete(
                            InitializationResult.Timeout(
                                partialError = (initResult as? AdResult.Failure)?.error
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun initializeWithLoadingAndAppOpen(
        adProvider: IAdProvider,
        config: AdConfig,
        activity: Activity,
        onComplete: (InitializationResult) -> Unit
    ) {
        this.config = config
        AdLogger.enable(config.enableLogging)
        val startTime = System.currentTimeMillis()

        coroutineScope {
            // Signal: true = app open is ready, false = not ready
            val appOpenSignal = CompletableDeferred<Boolean>()
            var sdkResult: AdResult? = null

            // Job 1: SDK init → then App Open load sequentially
            launch {
                sdkResult = adProvider.initialize(config)
                if (sdkResult is AdResult.Success) {
                    provider = adProvider

                    // App Open gets full network priority
                    preloadAppOpenSuspend()
                    appOpenSignal.complete(appOpenAd?.isReady() == true)

                    // Now kick off the rest — they load in the background
                    // while the app open ad is showing (or after it's skipped)
                    if (config.autoPreloadInterstitial) preloadInterstitial()
                    if (config.autoPreloadRewarded) preloadRewarded()
                } else {
                    appOpenSignal.complete(false)
                }
            }

            // Job 2: Timing control
            launch {
                // Always wait at least minTime
                delay(config.loadingMinTime)

                val remaining = config.loadingMaxTime - (System.currentTimeMillis() - startTime)

                // Wait for the app open signal within the remaining window
                val appOpenReady = when {
                    appOpenSignal.isCompleted -> appOpenSignal.getCompleted()
                    remaining > 0 -> withTimeoutOrNull(remaining) { appOpenSignal.await() } ?: false
                    else -> false
                }

                val elapsed = System.currentTimeMillis() - startTime

                val result = when {
                    provider == null -> InitializationResult.Timeout(
                        partialError = (sdkResult as? AdResult.Failure)?.error
                    )

                    sdkResult is AdResult.Failure -> InitializationResult.Failed(
                        error = (sdkResult as AdResult.Failure).error,
                        wasDelayed = true
                    )

                    else -> InitializationResult.Success(
                        actualInitTime = elapsed,
                        wasDelayed = true
                    )
                }

                if (appOpenReady) {
                    showAppOpen(activity) { onComplete(result) }
                } else {
                    onComplete(result)
                }
            }
        }
    }

    fun isInitialized(): Boolean = provider?.isInitialized() ?: false

    // ========== APP OPEN AD ==========

    fun isAppOpenReady(): Boolean = appOpenAd?.isReady() ?: false

    private suspend fun preloadAppOpenSuspend(): AdResult {
        val placementId = config?.appOpenPlacementId ?: return AdResult.Failure(
            AdError(ErrorCode.PROVIDER_ERROR, "No app open placement ID configured")
        )
        appOpenAd = provider?.getAppOpenAd(placementId) ?: return AdResult.Failure(
            AdError(ErrorCode.PROVIDER_ERROR, "Provider not initialized")
        )
        return appOpenAd!!.load()
    }

    fun showAppOpen(activity: Activity, onComplete: () -> Unit) {
        if (appOpenAd?.isReady() == true) {
            appOpenAd?.show(activity, object : AppOpenAdCallback {
                override fun onAdClosed() {
                    onComplete()
                }

                override fun onAdFailedToShow(error: AdError) {
                    onComplete()
                }
            })
        } else {
            onComplete()
        }
    }

    // ========== INTERSTITIAL AD ==========

    /**
     * Show interstitial ad with frequency control
     * @param activity The activity context
     * @param onClose Callback when ad closes (or fails to show)
     */
    fun showInterstitial(activity: Activity, onClose: () -> Unit) {
        interstitialCallCount++

        val shouldShow = shouldShowInterstitial()

        if (!shouldShow) {
            AdLogger.d("Interstitial skipped due to frequency control (call #$interstitialCallCount)")
            onClose()
            return
        }

        if (interstitialAd?.isReady() == true) {
            AdLogger.d("Showing interstitial ad (call #$interstitialCallCount)")
            interstitialAd?.show(activity, object : InterstitialAdCallback {
                override fun onAdShown() {
                    AdLogger.d("Interstitial ad shown")
                }

                override fun onAdClosed() {
                    AdLogger.d("Interstitial ad closed")
                    onClose()
                    // Auto-reload for next time
                    preloadInterstitial()
                }

                override fun onAdFailedToShow(error: AdError) {
                    AdLogger.e("Interstitial failed to show: ${error.message}")
                    onClose()
                    // Try to reload
                    preloadInterstitial()
                }
            })
        } else {
            AdLogger.w("Interstitial not ready, calling onClose immediately")
            onClose()
            // Try to load if not already loading
            preloadInterstitial()
        }
    }

    /**
     * Determine if interstitial should show based on frequency settings
     */
    private fun shouldShowInterstitial(): Boolean {
        val frequency = config?.interstitialFrequency ?: 1
        val showOnFirst = config?.showInterstitialOnFirstCall ?: true

        // If frequency is 1 or less, always show
        if (frequency <= 1) return true

        // First call behavior
        if (interstitialCallCount == 1) {
            return showOnFirst
        }

        // After first call, show every X calls
        // If showOnFirst = true, we adjust the count
        return if (showOnFirst) {
            // First call already shown, so check (callCount - 1) % frequency
            (interstitialCallCount - 1) % frequency == 0
        } else {
            // First call skipped, so check callCount % frequency
            interstitialCallCount % frequency == 0
        }
    }

    /**
     * Reset interstitial call counter (useful for testing or session resets)
     */
    fun resetInterstitialCounter() {
        interstitialCallCount = 0
        AdLogger.d("Interstitial counter reset")
    }

    /**
     * Check if interstitial is ready
     */
    fun isInterstitialReady(): Boolean = interstitialAd?.isReady() ?: false

    private fun preloadInterstitial() {
        val placementId = config?.interstitialPlacementId ?: return

        AdLogger.d("Preloading interstitial ad for placement: $placementId")
        interstitialAd = provider?.getInterstitialAd(placementId)

        adScope.launch {
            val result = interstitialAd?.load()
            AdLogger.d("Interstitial preload result: $result")
        }
    }

    // ========== REWARDED AD ==========

    /**
     * Show rewarded ad
     * @param activity The activity context
     * @param onRewarded Callback when user earns reward
     * @param onClose Callback when ad closes (or fails)
     */
    fun showRewarded(
        activity: Activity,
        onRewarded: (AdReward) -> Unit,
        onClose: () -> Unit
    ) {
        if (rewardedAd?.isReady() == true) {
            AdLogger.d("Showing rewarded ad")
            rewardedAd?.show(activity, object : RewardedAdCallback {
                override fun onAdShown() {
                    AdLogger.d("Rewarded ad shown")
                }

                override fun onUserRewarded(reward: AdReward) {
                    AdLogger.d("User rewarded: ${reward.amount} ${reward.type}")
                    onRewarded(reward)
                }

                override fun onAdClosed() {
                    AdLogger.d("Rewarded ad closed")
                    onClose()
                    // Auto-reload for next time
                    preloadRewarded()
                }

                override fun onAdFailedToShow(error: AdError) {
                    AdLogger.e("Rewarded ad failed to show: ${error.message}")
                    onClose()
                    preloadRewarded()
                }
            })
        } else {
            AdLogger.w("Rewarded ad not ready, calling onClose immediately")
            onClose()
            preloadRewarded()
        }
    }

    /**
     * Check if rewarded ad is ready
     */
    fun isRewardedReady(): Boolean = rewardedAd?.isReady() ?: false

    private fun preloadRewarded() {
        val placementId = config?.rewardedPlacementId ?: return

        AdLogger.d("Preloading rewarded ad for placement: $placementId")
        rewardedAd = provider?.getRewardedAd(placementId)

        adScope.launch {
            val result = rewardedAd?.load()
            AdLogger.d("Rewarded ad preload result: $result")
        }
    }

    // ========== BANNER AD ==========

    /**
     * Load and display banner in container.
     * Returns an [IBannerAd] instance that the caller must manage per-screen:
     *   - call [IBannerAd.pause] in onPause
     *   - call [IBannerAd.resume] in onResume
     *   - call [IBannerAd.destroy] in onDestroy
     *
     * Each Activity/Fragment should hold its own reference so that multiple
     * screens can show banners simultaneously without interfering with each other.
     *
     * @param container The ViewGroup to add the banner to
     * @param size Banner size (default: ADAPTIVE)
     * @return The [IBannerAd] instance, or null if the provider is not initialized
     */
    @JvmOverloads
    fun loadBanner(
        container: ViewGroup,
        size: BannerSize = BannerSize.ADAPTIVE,
        callback: BannerAdCallback? = null
    ): IBannerAd? {
        val placementId = config?.bannerPlacementId ?: "default_banner"

        AdLogger.d("Loading banner ad for placement: $placementId")

        val banner = provider?.getBannerAd(placementId) ?: return null

        banner.load(container, size, object : com.appbards.admanager.core.callback.BannerAdCallback {
            override fun onAdLoaded() {
                AdLogger.d("Banner ad loaded and added to container")
                callback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: AdError) {
                AdLogger.e("Banner failed to load: ${error.message}")
                callback?.onAdFailedToLoad(error)
            }

            override fun onAdShown() {
                AdLogger.d("Banner ad displayed")
                callback?.onAdShown()
            }

            override fun onAdFailedToShow(error: AdError) {
                AdLogger.e("Banner failed to display: ${error.message}")
                callback?.onAdFailedToShow(error)
            }
        })

        return banner
    }

    /**
     * Remove banner from its container and release resources.
     * Pass the [IBannerAd] instance returned by [loadBanner].
     */
    fun removeBanner(container: ViewGroup, banner: IBannerAd? = null) {
        AdLogger.d("Removing banner ad")
        container.removeAllViews()
        banner?.destroy()
    }

    // ========== NATIVE AD ==========

    /*
        private var nativeAd: INativeAd? = null

        override fun onCreate(...) {
          val adView = layoutInflater.inflate(R.layout.my_native_ad, null) as NativeAdView
         val binder = NativeAdViewBinder(
                rootView        = adView,
                headlineView    = adView.findViewById(R.id.headline),
                bodyView        = adView.findViewById(R.id.body),
                mediaView       = adView.findViewById(R.id.media),
                callToActionView = adView.findViewById(R.id.cta)
         )

         nativeAd = AdManager.loadNativeAd(nativeContainer, binder, object : NativeAdCallback {
            override fun onAdShown() { placeholder.visibility = View.GONE }
                override fun onAdFailedToShow(error: AdError) { placeholder.visibility = View.GONE }
            })
        }

        override fun onDestroy() {
           super.onDestroy()
          nativeAd?.destroy()
          nativeAd = null
        }
     */

    fun loadNativeAd(
        container: ViewGroup,
        binder: NativeAdViewBinder,
        callback: NativeAdCallback? = null
    ): INativeAd? {
        val placementId = config?.nativePlacementId ?: return null
        val native = provider?.getNativeAd(placementId) ?: return null

        adScope.launch {
            val result = native.load()
            if (result is AdResult.Success) {
                native.show(container, binder, object : NativeAdCallback {
                    override fun onAdShown() {
                        AdLogger.d("Native ad shown")
                        callback?.onAdShown()
                    }

                    override fun onAdFailedToShow(error: AdError) {
                        AdLogger.e("Native ad failed to show: ${error.message}")
                        callback?.onAdFailedToShow(error)
                    }

                    override fun onNativeAdImpression() {
                        callback?.onNativeAdImpression()
                    }

                    override fun onAdClicked() {
                        callback?.onAdClicked()
                    }
                })
            } else if (result is AdResult.Failure) {
                AdLogger.e("Native ad failed to load: ${result.error.message}")
                callback?.onAdFailedToLoad(result.error)
            }
        }

        return native
    }


    // ========== CLEANUP ==========

    fun destroy() {
        AdLogger.i("Destroying AdManager")
        provider?.destroy()
        interstitialAd?.destroy()
        rewardedAd?.destroy()
        appOpenAd?.destroy()
        appOpenAd = null
        interstitialAd = null
        rewardedAd = null
        provider = null
        config = null
        interstitialCallCount = 0
    }
}
