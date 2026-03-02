package com.appbards.admanager

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.appbards.admanager.core.AdManager
import com.appbards.admanager.core.config.AdConfig
import com.appbards.admanager.core.model.InitializationResult
import com.appbards.admanager.ironsource.IronSourceProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var bannerContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupAds()

    }

    private fun setupAds() {
        lifecycleScope.launch {
            val config = AdConfig(
                appId = "",
                interstitialPlacementId = "",
                rewardedPlacementId = "",
                bannerPlacementId = "",
                testMode = true,
                enableLogging = true,
                loadingMinTime = 2000,
                loadingMaxTime = 10000,
                autoPreloadRewarded = true,
                autoPreloadInterstitial = true,
                interstitialFrequency = 2,
                showInterstitialOnFirstCall = true
            )

            val provider = IronSourceProvider(this@MainActivity)

            AdManager.initializeWithLoading(
                adProvider = provider,
                config = config
            ) { result ->
                when (result) {
                    is InitializationResult.Failed -> startMainActivity()
                    is InitializationResult.Success -> startMainActivity()
                    is InitializationResult.Timeout -> startMainActivity()
                }
            }
        }
    }

    private fun startMainActivity(){}
}
