package com.opentok.accelerator.sample.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.opentok.accelerator.sample.R

class ScreenSharingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {


    private val textView: TextView
    private val closeButton: ImageButton

    private var onCloseListener: (() -> Unit)? = null
    
    init {
        //ToDo: Change to layout
        setBackgroundColor(ContextCompat.getColor(context, R.color.screen_sharing_bar))

        closeButton = ImageButton(context)
        closeButton.setImageDrawable(VectorDrawableCompat.create(resources, R.drawable.close, null))
        closeButton.setOnClickListener {
            onCloseListener?.invoke()
        }
        closeButton.background = null
        closeButton.isClickable = true

        var params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )

        params.addRule(ALIGN_PARENT_RIGHT)
        this.addView(closeButton, params)
        textView = TextView(context)
        textView.setText(R.string.screen_sharing_text)
        params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        params.addRule(CENTER_HORIZONTAL)
        params.addRule(CENTER_VERTICAL)
        this.addView(textView, params)
    }

    fun setOnCloseListener(listener: () -> Unit) {
        onCloseListener = listener
    }
}