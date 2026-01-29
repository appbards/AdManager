package com.appbards.admanager.core.callback

import com.appbards.admanager.core.model.AdReward

interface RewardedAdCallback : AdCallback {
    fun onUserRewarded(reward: AdReward)
}