# AdManager

A flexible, provider-agnostic ad management library for Android that allows easy switching between different ad
networks (IronSource/LevelPlay, AdMob, AppLovin, etc.) without changing your application code.

[![](https://jitpack.io/v/AppBards/AdManager.svg)](https://jitpack.io/#AppBards/AdManager)

## Features

✅ **Provider Pattern** - Easy to switch ad networks
✅ **Clean API** - Simple one-line ad calls
✅ **Auto-preloading** - Ads ready when you need them
✅ **Frequency Control** - Show interstitial every X calls
✅ **Coroutines** - Modern async/await support
✅ **Multiple Ad Types** - Interstitial, Rewarded, Banner
✅ **Loading Screen Integration** - Min/max timing control

## Supported Ad Networks

- ✅ **IronSource/LevelPlay 9.2.0** (Ready)
- 🚧 **AdMob** (Coming soon)
- 🚧 **AppLovin** (Coming soon)

## Installation

### Step 1: Add JitPack repository

Add this to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add dependencies

Add to your app's `build.gradle.kts`:

```kotlin
dependencies {
    // Core library (required)
    implementation("com.github.AppBards.AdManager:admanager-core:1.0.0")

    // IronSource provider
    implementation("com.github.AppBards.AdManager:admanager-ironsource:1.0.0")
}
```

## Quick Start

### 1. Initialize AdManager

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = AdConfig(
            appId = "your_ironsource_app_key",
            testMode = true,
            interstitialFrequency = 2,
            showInterstitialOnFirstCall = true
        )

        val provider = IronSourceProvider(this)

        lifecycleScope.launch {
            AdManager.initialize(provider, config)
        }
    }
}
```

### 2. Show Interstitial Ad

```kotlin
// Simple one-line call
AdManager.showInterstitial(activity) {
    // Ad closed, continue your flow
    navigateToNextScreen()
}
```

### 3. Show Rewarded Ad

```kotlin
AdManager.showRewarded(
    activity = this,
    onRewarded = { reward ->
        // User earned reward
        giveCoins(reward.amount)
    },
    onClose = {
        // Ad closed
    }
)
```

### 4. Show Banner Ad

**XML Layout:**

```xml

<FrameLayout android:id="@+id/bannerContainer" android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

**Activity:**

```kotlin
// Load banner
AdManager.loadBanner(binding.bannerContainer, BannerSize.ADAPTIVE)

override fun onPause() {
    super.onPause()
    AdManager.pauseBanner()
}

override fun onResume() {
    super.onResume()
    AdManager.resumeBanner()
}

override fun onDestroy() {
    super.onDestroy()
    AdManager.destroy()
}
```

## Advanced Features

### Loading Screen Integration

```kotlin
AdManager.initializeWithLoading(provider, config) { result ->
    when (result) {
        is InitializationResult.Success -> {
            // Ads initialized successfully
            navigateToMainScreen()
        }
        is InitializationResult.Timeout -> {
            // Timed out, continue anyway
            navigateToMainScreen()
        }
        is InitializationResult.Failed -> {
            // Failed, continue anyway
            navigateToMainScreen()
        }
    }
}
```

### Frequency Control

Control how often interstitial ads are shown:

```kotlin
AdConfig(
    interstitialFrequency = 3,           // Show every 3rd call
    showInterstitialOnFirstCall = true   // Show on first call
)

// Example calls:
AdManager.showInterstitial(activity) { } // Call 1: Shows ✅
AdManager.showInterstitial(activity) { } // Call 2: Skips ⏭️
AdManager.showInterstitial(activity) { } // Call 3: Skips ⏭️
AdManager.showInterstitial(activity) { } // Call 4: Shows ✅
```

### Check Ad Ready Status

```kotlin
if (AdManager.isInterstitialReady()) {
    // Interstitial is loaded and ready
}

if (AdManager.isRewardedReady()) {
    // Rewarded ad is loaded and ready
}
```

## Architecture

The library uses the Provider Pattern to abstract different ad networks:

```
Your App
    ↓
AdManager (Core)
    ↓
IAdProvider Interface
    ↓
┌──────────────┬──────────┬─────────────┐
│  IronSource  │  AdMob   │  AppLovin   │
│   Provider   │ Provider │  Provider   │
└──────────────┴──────────┴─────────────┘
```

This allows you to switch ad networks by changing just one line of code:

```kotlin
// Use IronSource
val provider = IronSourceProvider(activity)

// Switch to AdMob (when available)
val provider = AdMobProvider(activity)
```

## Configuration Options

```kotlin
AdConfig(
    appId: String,                              // Your ad network app ID
    testMode: Boolean = false,                  // Enable test mode
enableLogging: Boolean = false,             // Enable debug logging
timeout: Long = 30000L,                     // Ad load timeout (ms)
loadingMinTime: Long = 3000L,               // Min loading screen time
loadingMaxTime: Long = 10000L,              // Max loading screen time
interstitialPlacementId: String = "...",    // Interstitial placement
rewardedPlacementId: String = "...",        // Rewarded placement
bannerPlacementId: String = "...",          // Banner placement
autoPreloadInterstitial: Boolean = true,    // Auto-preload interstitial
autoPreloadRewarded: Boolean = true,        // Auto-preload rewarded
interstitialFrequency: Int = 1,             // Show frequency
showInterstitialOnFirstCall: Boolean = true // Show on first call
)
```

## Sample App

Check out the [app module](app/) for a complete working example with all ad types.

## AdMob Mediation: Activity configuration (Chartboost)

The `admanager-admob` module bundles the Chartboost mediation adapter. Chartboost
requires that **any activity from which you show a fullscreen ad** (interstitial,
rewarded, or app-open) **and that supports rotation** declare the following in
**your app's** `AndroidManifest.xml`:

```xml
<activity
    android:name=".YourAdShowingActivity"
    android:configChanges="keyboardHidden|orientation|screenSize" />
```

Without it, rotating the device while a Chartboost fullscreen ad is on screen
recreates the activity and can break the ad (dropped callbacks / dismissed ad).

Notes:

- This **cannot** be provided by the library — manifest merging can't add attributes
  to activities your app declares, so each consumer must apply it to their own
  ad-showing activity. See the sample app's [`MainActivity`](app/src/main/AndroidManifest.xml)
  for a reference.
- It's only needed on activities that present **fullscreen** Chartboost ads and can
  rotate. Banner-only activities and orientation-locked activities don't need it.
- If you don't receive Chartboost demand, it has no effect.

## Proguard Rules

If you use ProGuard/R8, the library handles obfuscation rules automatically. No additional configuration needed.

## Requirements

- Android API 24+
- Kotlin 1.9+
- Coroutines support

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues, questions, or feature requests, please [open an issue](https://github.com/AppBards/AdManager/issues).

## Author

AppBards - [@AppBards](https://github.com/AppBards)
