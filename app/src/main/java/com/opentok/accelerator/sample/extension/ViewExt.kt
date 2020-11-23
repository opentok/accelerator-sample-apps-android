package com.opentok.accelerator.sample.extension

import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE

inline fun View.hide(gone: Boolean = true) {
    visibility = if (gone) GONE else INVISIBLE
}

inline fun View.show() {
    visibility = VISIBLE
}