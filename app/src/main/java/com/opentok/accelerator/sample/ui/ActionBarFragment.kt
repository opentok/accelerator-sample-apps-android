package com.opentok.accelerator.sample.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.opentok.accelerator.core.utils.MediaType
import com.opentok.accelerator.sample.MainActivity
import com.opentok.accelerator.sample.R
import com.opentok.accelerator.sample.extension.hide
import com.opentok.accelerator.sample.extension.show

//
class ActionBarFragment : Fragment() {
    private lateinit var mainActivity: MainActivity
    private lateinit var audioButton: ImageButton
    private lateinit var cameraButton: ImageButton
    private lateinit var callButton: ImageButton
    private lateinit var screenSharingButton: ImageButton
    private lateinit var annotationsButton: ImageButton
    private lateinit var textChatButton: ImageButton
    private lateinit var unreadMessagesButton: ImageButton

    private var drawableStartCall: VectorDrawableCompat? = null
    private var drawableEndCall: VectorDrawableCompat? = null
    private var drawableBckButton: VectorDrawableCompat? = null

    private var previewControlCallbacks: PreviewControlCallbacks? = null


    interface PreviewControlCallbacks {
        fun onDisableLocalAudio(audio: Boolean)
        fun onDisableLocalVideo(video: Boolean)
        fun onCall()
        fun onScreenSharing()
        fun onAnnotations()
        fun onTextChat()
    }

    private val mButtonClickListener = View.OnClickListener {
        when (it.id) {
            R.id.localAudio -> localAudioOnClickListener()
            R.id.localVideo -> localVideoOnClickListener()
            R.id.call -> callOnClickListener()
            R.id.screenSharing -> screenSharingOnClickListener()
            R.id.annotations -> annotationsOnClickListener()
            R.id.textChat -> textChatOnClickListener()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
        previewControlCallbacks = context
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mainActivity = activity as MainActivity
            previewControlCallbacks = activity
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.actionbar_fragment, container, false)

        audioButton = rootView.findViewById(R.id.localAudio) as ImageButton
        cameraButton = rootView.findViewById(R.id.localVideo) as ImageButton
        callButton = rootView.findViewById(R.id.call) as ImageButton
        screenSharingButton = rootView.findViewById(R.id.screenSharing) as ImageButton
        annotationsButton = rootView.findViewById(R.id.annotations) as ImageButton
        textChatButton = rootView.findViewById(R.id.textChat) as ImageButton
        unreadMessagesButton = rootView.findViewById(R.id.unread_messages) as ImageButton
        drawableStartCall = VectorDrawableCompat.create(resources, R.drawable.initiate_call_button, null)
        drawableEndCall = VectorDrawableCompat.create(resources, R.drawable.end_call_button, null)
        drawableBckButton = VectorDrawableCompat.create(resources, R.drawable.background_icon, null)

        updateMicrophoneButton()
        updateCameraButton()

        audioButton.background = drawableBckButton

        cameraButton.background = drawableBckButton
        callButton.setImageResource(if (mainActivity.isCallInProgress) R.drawable.hang_up else R.drawable.start_call)
        callButton.background = if (mainActivity.isCallInProgress) drawableEndCall else drawableStartCall
        callButton.setOnClickListener(mButtonClickListener)
        setEnabled(mainActivity.isCallInProgress)
        return rootView
    }

    private fun updateCameraButton() {
        val icon = if (mainActivity.otWrapper.isLocalMediaEnabled(MediaType.VIDEO)) R.drawable.video_icon else R
            .drawable
            .no_video_icon
        cameraButton.setImageResource(icon)
    }

    private fun updateMicrophoneButton() {
        val icon = if (mainActivity.otWrapper.isLocalMediaEnabled(MediaType.AUDIO)) R.drawable.mic_icon else R.drawable
            .muted_mic_icon
        audioButton.setImageResource(icon)
    }


    private fun localAudioOnClickListener() {
        if (!mainActivity.otWrapper.isLocalMediaEnabled(MediaType.AUDIO)) {
            previewControlCallbacks?.onDisableLocalAudio(true)
            audioButton.setImageResource(R.drawable.mic_icon)
        } else {
            previewControlCallbacks?.onDisableLocalAudio(false)
            audioButton.setImageResource(R.drawable.muted_mic_icon)
        }
    }

    private fun localVideoOnClickListener() {
        if (!mainActivity.otWrapper.isLocalMediaEnabled(MediaType.VIDEO)) {
            previewControlCallbacks?.onDisableLocalVideo(true)
            cameraButton.setImageResource(R.drawable.video_icon)
        } else {
            previewControlCallbacks?.onDisableLocalVideo(false)
            cameraButton.setImageResource(R.drawable.no_video_icon)
        }
    }

    private fun callOnClickListener() {
        callButton.setImageResource(if (!mainActivity.isCallInProgress) R.drawable.hang_up else R.drawable.start_call)
        callButton.background = if (!mainActivity.isCallInProgress) drawableEndCall else drawableStartCall
        previewControlCallbacks?.onCall()
    }

    private fun screenSharingOnClickListener() {
        cameraButton.setOnClickListener(if (!mainActivity.isScreenSharing) null else mButtonClickListener)
        annotationsButton.setOnClickListener(if (!mainActivity.isScreenSharing) mButtonClickListener else null)
        screenSharingButton.setBackgroundResource(if (!mainActivity.isScreenSharing) R.drawable.bckg_icon_selected
        else R
            .drawable.background_icon)
        previewControlCallbacks?.onScreenSharing()
    }

    private fun annotationsOnClickListener() {
        restartAnnotations()
        previewControlCallbacks?.onAnnotations()
    }

    private fun textChatOnClickListener() {
        previewControlCallbacks?.onTextChat()
    }

    fun unreadMessages(unread: Boolean) {
        activity?.runOnUiThread {
            if (unread) {
                unreadMessagesButton.show()
            } else {
                unreadMessagesButton.hide()
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            audioButton.setOnClickListener(mButtonClickListener)
            cameraButton.setOnClickListener(mButtonClickListener)
            screenSharingButton.setOnClickListener(mButtonClickListener)
            textChatButton.setOnClickListener(mButtonClickListener)
        } else {
            audioButton.setOnClickListener(null)
            cameraButton.setOnClickListener(null)
            audioButton.setImageResource(R.drawable.mic_icon)
            cameraButton.setImageResource(R.drawable.video_icon)
            screenSharingButton.setOnClickListener(null)
            annotationsButton.setOnClickListener(null)
            textChatButton.setOnClickListener(null)
        }
    }

    fun setCallButtonEnabled(enabled: Boolean) {
        callButton.isEnabled = enabled
    }

    fun restart() {
        setEnabled(false)
        callButton.setBackgroundResource(R.drawable.initiate_call_button)
        callButton.setImageResource(R.drawable.start_call)
        screenSharingButton.setBackgroundResource(R.drawable.background_icon)
        annotationsButton.setBackgroundResource(R.drawable.background_icon)
    }

    private fun restartAnnotations() {
        annotationsButton.setBackgroundResource(R.drawable.background_icon)
        enableAnnotations(false)
    }

    fun enableAnnotations(enable: Boolean) {
        annotationsButton.setOnClickListener(if (enable) mButtonClickListener else null)
    }

    fun restartScreenSharing() {
        screenSharingButton.setBackgroundResource(R.drawable.background_icon)
    }

    fun showAnnotations(show: Boolean) {
        val params = screenSharingButton.layoutParams as RelativeLayout.LayoutParams

        if (show) {
            textChatButton.hide()
            annotationsButton.show()
            params.addRule(RelativeLayout.RIGHT_OF, annotationsButton.id)
        } else {
            annotationsButton.hide()
            textChatButton.show()
            params.addRule(RelativeLayout.RIGHT_OF, textChatButton.id)
        }
        screenSharingButton.layoutParams = params
    }
}