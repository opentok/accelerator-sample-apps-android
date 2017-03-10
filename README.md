![logo](tokbox-logo.png)

# OpenTok Accelerator Sample App for Android

## Quick start

This section shows you how to prepare, build, and run the sample application.

### Requirements

To develop your multiparty accelerator sample app:

1. Install [Android Studio](http://developer.android.com/intl/es/sdk/index.html)
1. Review the [OpenTok Android SDK Requirements](https://tokbox.com/developer/sdks/android/#developerandclientrequirements)

### Install the project files

1. Clone the [OpenTok Accelerator Sample App for Android repository](https://github.com/opentok/accelerator-sample-apps-android) from GitHub.
1. Start Android Studio.
1. In the **Quick Start** panel, click **Open an existing Android Studio Project**.
1. Navigate to the **android** folder, select the **AcceleratorSampleApp** folder, and click **Choose**.


### Add dependencies

The multiparty sample app uses the [OpenTok Accelerator Core](https://github.com/opentok/accelerator-core-android), the [Opentok Accelerator Annotation](https://github.com/opentok/accelerator-annotation-android) and [OpenTok Accelerator TextChat](https://github.com/opentok/accelerator-textchat-android). The Accelerator Core is dependency of the Accelerator Annotations and Accelerator TextChat too, so following the transitive dependencies rules in maven, we don't need to add the Accelerator Core in the sample app.

There are two options to add the dependencies:

#### Using Maven

1. Modify the `build.gradle` for your solution and add the following code snippet to the section labeled `repositories`:

  ```gradle
    maven { url  "http://tokbox.bintray.com/maven" }
  ```

2. Modify the `build.gradle` for your activity and add the following code snippet to the section labeled `dependencies`:
  
  ```gradle
    compile 'com.opentok.android:opentok-accelerator-annotation:1.+'
    compile 'com.opentok.android:opentok-accelerator-textchat:1.0.+'
  ```


#### Using the repository

1. Clone the [OpenTok Accelerator TextChat repo](https://github.com/opentok/accelerator-textchat-android).
2. From your app project, right-click the app name and select **New > Module > Import Gradle Project**.
3. Navigate to the directory in which you cloned **OpenTok Accelerator Pack**, select **accelerator-textchat-android**, and click **Finish**.
4. Open the **build.gradle** file for the app and ensure the following lines have been added to the `dependencies` section:

```
compile project(':accelerator-textchat-android')

```

1. Clone the [OpenTok Accelerator Annotation repo](https://github.com/opentok/accelerator-annotation-android).
2. From your app project, right-click the app name and select **New > Module > Import Gradle Project**.
3. Navigate to the directory in which you cloned **OpenTok Accelerator Pack**, select **accelerator-annotation-android**, and click **Finish**.
4. Open the **build.gradle** file for the app and ensure the following lines have been added to the `dependencies` section:

```
compile project(':accelerator-annotation-android')

```

### Configure and build the app

Configure the sample app code. Then, build and run the app.

1. Get values for **API Key**, **Session ID**, and **Token**. See [OpenTok Developer Dashboard](https://dashboard.tokbox.com/).

In Android Studio, open **OpenTokConfig.java** and replace the following empty strings with the corresponding **API Key**, **Session ID**, and **Token** values:

  ```java
    // Replace with a generated Session ID
    public static final String SESSION_ID = "";

    // Replace with a generated token
    public static final String TOKEN = "";

    // Replace with your OpenTok API key
    public static final String API_KEY = "";
  ```

Then, the credentials values will be used in the Core Wrapper configuration.

  ```java
    //init the wrapper
    OTConfig config =
          new OTConfig.OTConfigBuilder(OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN,
            OpenTokConfig.API_KEY).name("one-to-one-sample-app").subscribeAutomatically(true).subscribeToSelf(false).build();
    
    if ( config != null ) {
      mWrapper = new OTWrapper(MainActivity.this, config);
      mWrapper.addBasicListener(mBasicListener);
      mWrapper.addAdvancedListener(mAdvancedListener);

      //...
    }
  ```

## Exploring the code

This section describes best practices the sample app code uses to implement the multiparty communication features.

For detail about the APIs used to develop this sample, see the [OpenTok Android SDK Reference](https://tokbox.com/developer/sdks/android/reference/) and [Android API Reference](http://developer.android.com/reference/packages.html).


### Class design

| Class        | Description  |
| ------------- | ------------- |
| `MainActivity`    | Implements the UI and media control callbacks. |
| `OpenTokConfig`   | Stores the information required to configure the session and authorize the app to make requests to the backend server.   |
| `ActionBarFragment`   | Manages the toolbar for the local audio and video controls, the start/end call, text-chat and screensharing buttons.  |
| `ScreensharingBar`   | Defines a view to represent the ScreenSharingBar. |
| `Participant `   | Represents an item in the data set |
| `ParticipantAdapter `   |  Custom RecyclerView.Adapter responsible for providing views that represent items in the data set.|


### Session and stream management

The `OTWrapper` class, included in the Accelerator Core for Android, is the backbone of the one-to-one communication features for the app.

This class uses the OpenTok API to initiate the client connection to the OpenTok session and manage the audio and video streams.
```java
  
  mWrapper.connect();
  
  mWrapper.startPublishingMedia(new PreviewConfig.PreviewConfigBuilder().
                        name("Tokboxer").build(), false);

  mWrapper.enableLocalMedia(MediaType.AUDIO, audio);
  
  mWrapper.disconnect();

```

The BasicListener and AdvancedListener interface monitor state changes in the communication, and defines the following methods:

```java
  //Basic Listener from OTWrapper
  private BasicListener mBasicListener =
    new PausableBasicListener(new BasicListener<OTWrapper>() {
    @Override
    public void onConnected(OTWrapper otWrapper, int participantsCount, String connId, String data) throws ListenerException { //...}
    @Override
    public void onDisconnected(OTWrapper otWrapper, int participantsCount, String connId, String data) throws ListenerException { //...}
    @Override
    public void onPreviewViewReady(OTWrapper otWrapper, View localView) throws ListenerException { //...}
    @Override
    public void onRemoteViewReady(OTWrapper otWrapper, View remoteView, String remoteId, String data) throws ListenerException { //...}
    @Override
    public void onStartedPublishingMedia(OTWrapper otWrapper, boolean screensharing) throws ListenerException { //...}
    //...
  });

```
```java
  //Advanced Listener from OTWrapper
  private AdvancedListener mAdvancedListener =
    new PausableAdvancedListener(new AdvancedListener<OTWrapper>() {
    @Override
    public void onCameraChanged(OTWrapper otWrapper) throws ListenerException { //... }
    @Override
    public void onReconnecting(OTWrapper otWrapper) throws ListenerException { //... }
    @Override
    public void onReconnected(OTWrapper otWrapper) throws ListenerException { //... }
    @Override
    public void onVideoQualityWarning(OTWrapper otWrapper, String remoteId) throws ListenerException { //... }
    //...
  });
```

### Multiparty UI

The UI for the multiparty audio/video communication is represented by an Android GridLayoutManager with RecycleViews. The different layout distributions depend on the number of participants. To adjust the number of columns for the GridLayout, the `getSpanSize` method has been override. 

```java
 private void setupMultipartyLayout() {
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mParticipantsList.size() == 1) {
                    return 2;
                } else {
                    if (mParticipantsList.size() == 2) {
                        if (position == 0) {
                            return 2;
                        }
                        return 1;
                    } else {
                        if (mParticipantsList.size() == 3) {
                            if (position == 0 || position == 1) {
                                return 1;
                            } else {
                                return 2;
                            }
                        } else {
                            if (mParticipantsList.size() == 4) {
                                return 1;
                            } else {
                                if (mParticipantsList.size() > 4) {
                                    if (mParticipantsList.size() % 2 != 0) {
                                        if (position == mParticipantsList.size() - 1) {
                                            return 2;
                                        } else {
                                            return 1;
                                        }
                                    } else {
                                        return 1;
                                    }
                                }
                            }
                        }
                    }
                }
                return 1;
            }
        });
    }
```



