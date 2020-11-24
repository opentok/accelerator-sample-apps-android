package com.opentok.accelerator.sample.extension

import android.graphics.Bitmap
import android.graphics.Canvas
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

fun View.getBitmap(): Bitmap {
    val returnedBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(returnedBitmap)
    val bgDrawable = this.background
    bgDrawable?.draw(canvas)
    this.draw(canvas)
    return returnedBitmap
}