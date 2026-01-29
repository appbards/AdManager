package com.appbards.admanager.core.provider

import android.app.Activity
import com.appbards.admanager.core.callback.InterstitialAdCallback
import com.appbards.admanager.core.model.AdResult

interface IInterstitialAd {
    suspend fun load(): AdResult
    fun isReady(): Boolean
    fun show(activity: Activity, callback: InterstitialAdCallback)
    fun destroy()
}