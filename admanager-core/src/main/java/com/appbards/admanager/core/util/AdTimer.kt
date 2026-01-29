package com.appbards.admanager.core.util

class AdTimer(private val cooldownMs: Long) {
    private var lastShowTime: Long = 0

    fun canShow(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastShowTime) >= cooldownMs
    }

    fun markShown() {
        lastShowTime = System.currentTimeMillis()
    }

    fun getRemainingTime(): Long {
        val elapsed = System.currentTimeMillis() - lastShowTime
        return maxOf(0, cooldownMs - elapsed)
    }

    fun reset() {
        lastShowTime = 0
    }
}