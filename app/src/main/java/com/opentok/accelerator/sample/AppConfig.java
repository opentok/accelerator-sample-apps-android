package com.opentok.accelerator.sample;

import com.opentok.accelerator.core.utils.OTConfig;

class AppConfig {
    // Fill the following arguments for your OpenTok Project. All the required information can be found in the OpenTok
    // dashboard
    // https://dashboard.tokbox.com/projects

    static final OTConfig otConfig = new OTConfig(
            "", // Replace with session Id from the dashboard
            "", // Replace with a generated token from the dashboard or using an OpenTok server SDK
            "OpenTokConfig.API_KEY", // Replace with your OpenTok API key from the dashboard
            "accelerator-sample-app",
            false,
            true
    );
}

