package com.opentok.accelerator.sample.ui

import android.util.Size
import com.opentok.accelerator.core.utils.StreamStatus

class Participant {
    enum class Type {
        LOCAL, REMOTE
    }

    var type: Type
    var id: String? = null
    var status: StreamStatus
    var containerSize: Size

    constructor(type: Type, status: StreamStatus, containerSize: Size, id: String? = null) {
        this.type = type
        this.status = status
        this.containerSize = containerSize
        this.id = id
    }
}