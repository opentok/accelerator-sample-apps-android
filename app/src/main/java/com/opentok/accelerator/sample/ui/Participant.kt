package com.opentok.accelerator.sample.ui

import android.util.Size
import com.opentok.accelerator.core.utils.StreamStatus

class Participant(var type: Type, var status: StreamStatus, var containerSize: Size, id: String? = null) {
    enum class Type {
        LOCAL, REMOTE
    }

    var remoteId: String? = id
}