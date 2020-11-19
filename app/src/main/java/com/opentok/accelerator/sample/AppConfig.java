package com.opentok.accelerator.sample;

import com.opentok.accelerator.core.utils.OTConfig;

class AppConfig {
    // Fill the following arguments for your OpenTok Project. All the required information can be found in the OpenTok
    // dashboard
    // https://tokbox.com/account

    static final OTConfig otConfig = new OTConfig(
            "", // Replace with API key from the dashboard
            "", // Replace with a session id from the dashboard (or an OpenTok server SDK)
            "", // Replace with token from the dashboard (or an OpenTok server SDK)
            "accelerator-sample-app",
            false,
            true
    );
}
