package com.appbards.admanager.core

import android.app.Activity
import android.view.ViewGroup
import com.appbards.admanager.core.callback.InterstitialAdCallback
import com.appbards.admanager.core.callback.RewardedAdCallback
import com.appbards.admanager.core.config.AdConfig
import com.appbards.admanager.core.model.AdError
import com.appbards.admanager.core.model.AdResult
import com.appbards.admanager.core.model.AdReward
import com.appbards.admanager.core.model.InitializationResult
import com.appbards.admanager.core.provider.BannerSize
import com.appbards.admanager.core.provider.IAdProvider
import com.appbards.admanager.core.provider.IAppOpenAd
import com.appbards.admanager.core.provider.IBannerAd
import com.appbards.admanager.core.provider.IInterstitialAd
import com.appbards.admanager.core.provider.INativeAd
import com.appbards.admanager.core.provider.IRewardedAd
import com.appbards.admanager.core.util.AdLogger
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
    private var interstitialAd: IInterstitialAd? = null
    private var rewardedAd: IRewardedAd? = null
    private var bannerAd: IBannerAd? = null

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
                            onComplete(InitializationResult.Success(
                                actualInitTime = elapsed,
                                wasDelayed = true
                            ))
                        }
                        is AdResult.Failure -> {
                            AdLogger.e("Initialization failed: ${result.error.message} (delayed to minTime)")
                            onComplete(InitializationResult.Failed(
                                error = result.error,
                                wasDelayed = true
                            ))
                        }
                        null -> { /* shouldn't happen */ }
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
                                onComplete(InitializationResult.Success(
                                    actualInitTime = elapsed,
                                    wasDelayed = false
                                ))
                            }
                            is AdResult.Failure -> {
                                AdLogger.e("Initialization failed: ${result.error.message}")
                                onComplete(InitializationResult.Failed(
                                    error = result.error,
                                    wasDelayed = false
                                ))
                            }
                            null -> { /* shouldn't happen */ }
                        }
                    } else {
                        // Timed out at maxTime
                        initJob.cancel()
                        AdLogger.w("Initialization timed out at maxTime (${config.loadingMaxTime}ms)")
                        onComplete(InitializationResult.Timeout(
                            partialError = (initResult as? AdResult.Failure)?.error
                        ))
                    }
                }
            }
        }
    }

    fun isInitialized(): Boolean = provider?.isInitialized() ?: false

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
     * Load and display banner in container
     * @param container The ViewGroup to add banner to
     * @param size Banner size (default: ADAPTIVE)
     */
    fun loadBanner(container: ViewGroup, size: BannerSize = BannerSize.ADAPTIVE) {
        val placementId = config?.bannerPlacementId ?: "default_banner"

        AdLogger.d("Loading banner ad for placement: $placementId")

        bannerAd?.destroy()
        bannerAd = provider?.getBannerAd(placementId)

        bannerAd?.load(container, size, object : com.appbards.admanager.core.callback.BannerAdCallback {
            override fun onAdLoaded() {
                AdLogger.d("Banner ad loaded and added to container")
            }

            override fun onAdFailedToLoad(error: AdError) {
                AdLogger.e("Banner failed to load: ${error.message}")
            }

            override fun onAdShown() {
                AdLogger.d("Banner ad displayed")
            }

            override fun onAdFailedToShow(error: AdError) {
                AdLogger.e("Banner failed to display: ${error.message}")
            }
        })
    }


    /**
     * Remove banner from view
     */
    fun removeBanner(container: ViewGroup) {
        AdLogger.d("Removing banner ad")
        container.removeAllViews()
        bannerAd?.destroy()
        bannerAd = null
    }

    /**
     * Pause banner (call in onPause)
     */
    fun pauseBanner() {
        bannerAd?.pause()
    }

    /**
     * Resume banner (call in onResume)
     */
    fun resumeBanner() {
        bannerAd?.resume()
    }

    // ========== LEGACY METHODS (for Native & App Open - to be improved later) ==========

    fun getNativeAd(placementId: String): INativeAd? {
        return provider?.getNativeAd(placementId)
    }

    fun getAppOpenAd(placementId: String): IAppOpenAd? {
        return provider?.getAppOpenAd(placementId)
    }

    // ========== CLEANUP ==========

    fun destroy() {
        AdLogger.i("Destroying AdManager")
        interstitialAd?.destroy()
        rewardedAd?.destroy()
        bannerAd?.destroy()
        provider?.destroy()
        interstitialAd = null
        rewardedAd = null
        bannerAd = null
        provider = null
        config = null
        interstitialCallCount = 0
    }
}
