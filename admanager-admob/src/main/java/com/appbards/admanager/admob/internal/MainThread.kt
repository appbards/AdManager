package com.appbards.admanager.admob.internal

import android.os.Handler
import android.os.Looper

/**
 * The GMA Next-Gen SDK invokes its callbacks on a background thread, whereas this
 * library's public callbacks are consumed by app code that updates views. Every hop
 * from an SDK callback into an [com.appbards.admanager.core.callback.AdCallback]
 * therefore goes through here.
 */
internal inline fun onMainThread(crossinline block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block()
    } else {
        mainHandler.post { block() }
    }
}

private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }
