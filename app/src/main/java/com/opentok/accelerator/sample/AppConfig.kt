package com.opentok.accelerator.sample

import com.opentok.accelerator.core.utils.OTConfig

object AppConfig {
    // Fill the following arguments for your OpenTok Project. All the required information can be found in the OpenTok
    // dashboard
    // https://tokbox.com/account
    val otConfig = OTConfig(
        apiKey = "",  // Replace with API key from the dashboard
        sessionId = "",  // Replace with a session id
        token = "" , // from the dashboard (or an OpenTok server SDK)
        sessionName = "accelerator-sample-app",
        subscribeToSelf = false,
        subscribeAutomatically = true
    )
}
