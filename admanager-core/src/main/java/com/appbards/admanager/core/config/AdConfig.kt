package com.appbards.admanager.core.config

data class AdConfig(
    val appId: String,
    val testMode: Boolean = false,
    val enableLogging: Boolean = false,
    val timeout: Long = 30000L,
    val loadingMinTime: Long = 3000L,
    val loadingMaxTime: Long = 10000L,

    // Auto-preload settings
    val autoPreloadInterstitial: Boolean = true,
    val autoPreloadRewarded: Boolean = true,

    // Placement IDs
    val interstitialPlacementId: String = "default_interstitial",
    val rewardedPlacementId: String = "default_rewarded",
    val bannerPlacementId: String = "default_banner",

    // Frequency control for interstitial
    val interstitialFrequency: Int = 1,  // Show every X calls (1 = show every time)
    val showInterstitialOnFirstCall: Boolean = true  // Show on first call regardless of frequency
)
