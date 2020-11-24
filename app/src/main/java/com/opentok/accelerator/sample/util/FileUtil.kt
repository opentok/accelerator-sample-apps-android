package com.opentok.accelerator.sample.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import java.io.File

object FileUtil {
    fun shareImage(context: Context, imageFile: File) {

        if(!hasImageExtension(imageFile)) return

        // target API 24 requires FileProvider that have to be configured in the AndroidManifest.xm
        val uri = FileProvider.getUriForFile(
            context,
            "com.opentok.accelerator.sample.provider",
            imageFile
        )

        val intentSend = Intent()
        intentSend.action = Intent.ACTION_SEND
        intentSend.type = "image/*"
        intentSend.putExtra(Intent.EXTRA_SUBJECT, "")
        intentSend.putExtra(Intent.EXTRA_TEXT, "")
        intentSend.putExtra(Intent.EXTRA_STREAM, uri)

        startActivity(context, Intent.createChooser(intentSend, "Share Screenshot"), null)
    }

    private fun hasImageExtension(file: File): Boolean {
        val allowedExtensions = arrayOf("jpg", "jpeg", "png", "gif", "webp")
        val fileName = file.name.toLowerCase()

        return allowedExtensions.any { fileName.endsWith(".$it") }
    }
}