package com.appbards.admanager.core.provider

import android.app.Activity
import com.appbards.admanager.core.callback.RewardedAdCallback
import com.appbards.admanager.core.model.AdResult

interface IRewardedAd {
    suspend fun load(): AdResult
    fun isReady(): Boolean
    fun show(activity: Activity, callback: RewardedAdCallback)
    fun destroy()
}