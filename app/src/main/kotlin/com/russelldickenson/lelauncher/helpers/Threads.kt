package com.russelldickenson.lelauncher.helpers

import android.os.Looper

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()

fun ensureBackgroundThread(callback: () -> Unit) {
    if (isOnMainThread()) {
        Thread { callback() }.start()
    } else {
        callback()
    }
}
