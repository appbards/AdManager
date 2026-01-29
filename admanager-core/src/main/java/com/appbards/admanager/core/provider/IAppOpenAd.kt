package com.appbards.admanager.core.provider

import android.app.Activity
import com.appbards.admanager.core.callback.AppOpenAdCallback
import com.appbards.admanager.core.model.AdResult

interface IAppOpenAd {
    suspend fun load(): AdResult
    fun isReady(): Boolean
    fun show(activity: Activity, callback: AppOpenAdCallback)
    fun destroy()
}