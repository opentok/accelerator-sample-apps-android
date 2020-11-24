# OpenTok Accelerator Sample App for Android

<img src="https://assets.tokbox.com/img/vonage/Vonage_VideoAPI_black.svg" height="48px" alt="Tokbox is now known as Vonage" />

This project demonstrates Accelerator Sample App - multiparty, web-based audio, video, and messaging solution for Android Platform.
This project utilizes [accelerator-core-android](https://github.com/opentok/accelerator-core-android).

> Under the hood [accelerator-core-android](https://github.com/opentok/accelerator-core-android) utilizes [Vonage Video API Android SDK](https://tokbox.com/developer/sdks/android/) and opentokRTC.

## Getting started

### Open this project

There are a few ways to open this project.

#### Android Studio

1. Go to `Android Studio` and select `File > New > From Version control > Git`
2. Enter `git@github.com:opentok/accelerator-sample-apps-android.git` into URL field

#### Command-line + Android Studio

1. Run `git clone git@github.com:opentok/accelerator-sample-apps-android.git` to clone project
2. Go to `Android Studio` and select `File > Open` and navigate to the newly cloned directory

### Set app config

Before running the application, we need to configure project specific settings. 
Open the [dashboard](https://dashboard.tokbox.com/), select specific application and to get the `apiKey`, `sessionId` and `token`.

Open `com.opentok.accelerator.sample.AppConfig` file and fill the `apiKey`, `sessionId` and `token` for your Android project.

> For production deployment, you must generate the `SessionId` and `token` using one of the [OpenTok Server SDKs](https://tokbox.com/developer/sdks/server/)
(https://tokbox.com/developer/sdks/server/).

## Replace maven dependency with accelerator-core-android code

This project is using `implementation 'com.opentok.android:opentok-accelerator-core:x.y.z'` dependency to utilize accelerators. This dependency can be 
replaced with [accelerator-core-android](https://github.com/opentok/accelerator-core-android) source code to facilicate developement of your application. 

### Remove dependency

1. Modify the root `build.gradle` file and remove the custom repository:

```groovy
maven { 
  url  "http://tokbox.bintray.com/maven" 
}
  ```

2. Modify the `app/build.gradle` file and remove accelerator dependency:
  
```groovy
implementation 'com.opentok.android:opentok-accelerator-core:x.y.z'
```

### Using the repository

1. Clone the [OpenTok Accelerator Core repo](https://github.com/opentok/accelerator-core-android).
2. Go to `Android Studio` and select `New > Module > Import Gradle Project` and navigate to the newly cloned directory.

## Exploring the code

This section describes the best practices the sample app code uses to implement the communication features.

For detail about the APIs used to develop this sample, see the [OpenTok Android SDK Reference](https://tokbox.com/developer/sdks/android/reference/) and [Android API Reference](http://developer.android.com/reference/packages.html).

### Class design

| Class                 | Description                                                                                                            |
| --------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `MainActivity`        | Implements the UI and media control callbacks.                                                                         |
| `OpenTokConfig`       | Stores the information required to configure the session and authorize the app to make requests to the backend server. |
| `ActionBarFragment`   | Manages the toolbar for the local audio and video controls, the start/end call, text-chat, and screen sharing buttons.   |
| `ScreensharingBar`    | Defines a view to represent the ScreenSharingBar.                                                                      |
| `Participant `        | Represents an item in the data set                                                                                     |
| `ParticipantAdapter ` | Custom RecyclerView.Adapter responsible for providing views that represent items in the data set.                      |

### Session and streams management

The `OTWrapper` class, included in the [accelerator-core-android](https://github.com/opentok/accelerator-core-android) is the backbone of the communication features for this app.

This class uses the OpenTok API to initiate the client connection to the OpenTok session and manage the audio and video streams.

```kotlin
otWrapper.connect()
val previewConfig = new PreviewConfig.PreviewConfigBuilder().name("myConfig").build()
otWrapper.startPublishingMedia(previewConfig, false)
otWrapper.enableLocalMedia(MediaType.AUDIO, audio)
otWrapper.disconnect()
```

The `BasicListener` and `AdvancedListener` interface monitor `OTWraper` state changes.

## Development and Contributing

Interested in contributing? We :heart: pull requests! See the [Contribution](CONTRIBUTING.md) guidelines.

## Getting Help

We love to hear from you so if you have questions, comments or find a bug in the project, let us know! You can either:

- Open an issue on this repository
- See <https://support.tokbox.com/> for support options
- Tweet at us! We're [@VonageDev](https://twitter.com/VonageDev) on Twitter
- Or [join the Vonage Developer Community Slack](https://developer.nexmo.com/community/slack)

## Further Reading

- Check out the Developer Documentation at <https://tokbox.com/developer/>