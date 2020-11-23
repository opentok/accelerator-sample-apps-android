package com.opentok.accelerator.sample.extension

import android.graphics.Bitmap
import android.graphics.Canvas

fun Bitmap.merge(otherBitmap: Bitmap?): Bitmap {
    otherBitmap ?: return this

    val bitmapOverlay = Bitmap.createBitmap(this.width, this.height, this.config)
    val scaledOtherBitmap = Bitmap.createScaledBitmap(otherBitmap, this.width, this.height, true)

    val canvas = Canvas(bitmapOverlay)
    canvas.drawBitmap(this, 0f, 0f, null)
    canvas.drawBitmap(scaledOtherBitmap, 0f, 0f, null)

    return bitmapOverlay
}