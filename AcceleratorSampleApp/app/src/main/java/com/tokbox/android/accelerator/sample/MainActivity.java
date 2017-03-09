package com.tokbox.android.accelerator.sample;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.opentok.android.OpentokError;
import com.tokbox.android.accelerator.sample.ui.ActionBarFragment;
import com.tokbox.android.accpack.textchat.ChatMessage;
import com.tokbox.android.accpack.textchat.TextChatFragment;
import com.tokbox.android.annotations.AnnotationsToolbar;
import com.tokbox.android.annotations.AnnotationsView;
import com.tokbox.android.annotations.utils.AnnotationsVideoRenderer;
import com.tokbox.android.otsdkwrapper.listeners.AdvancedListener;
import com.tokbox.android.otsdkwrapper.listeners.BasicListener;
import com.tokbox.android.otsdkwrapper.listeners.ListenerException;
import com.tokbox.android.otsdkwrapper.listeners.PausableAdvancedListener;
import com.tokbox.android.otsdkwrapper.listeners.PausableBasicListener;
import com.tokbox.android.otsdkwrapper.utils.MediaType;
import com.tokbox.android.otsdkwrapper.utils.OTConfig;
import com.tokbox.android.otsdkwrapper.utils.PreviewConfig;
import com.tokbox.android.otsdkwrapper.utils.StreamStatus;
import com.tokbox.android.otsdkwrapper.wrapper.OTWrapper;
import com.tokbox.android.accelerator.sample.config.OpenTokConfig;
import com.tokbox.android.accelerator.sample.ui.PreviewCameraFragment;
import com.tokbox.android.accelerator.sample.ui.RemoteControlFragment;
import com.tokbox.android.accelerator.sample.ui.ScreenSharingBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements ActionBarFragment.PreviewControlCallbacks,
        RemoteControlFragment.RemoteControlCallbacks, PreviewCameraFragment.PreviewCameraCallbacks, AnnotationsView.AnnotationsListener,
        ScreenSharingBar.ScreenSharingBarListener, TextChatFragment.TextChatListener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final int permsRequestCode = 200;

    //OpenTok calls
    private OTWrapper mWrapper;

    private RelativeLayout mPreviewViewContainer;
    private RelativeLayout mRemoteViewContainer;
    private RelativeLayout mRemoteViewContainer2;
    private RelativeLayout mRemoteViewContainer3;
    private RelativeLayout.LayoutParams mLayoutParamsPreview;
    private RelativeLayout mCameraFragmentContainer;
    private RelativeLayout mActionBarContainer;
    private GridLayout mRemotesViews;

    private TextView mAlert;
    //audio only views
    private RelativeLayout mRemoteAudioOnlyView;
    private RelativeLayout mLocalAudioOnlyView;
    private WebView mWebViewContainer;

    private ImageView mAudioOnlyImage;
    private View mRemoteView;

    //UI control bars fragments
    private ActionBarFragment mActionBarFragment;
    private RemoteControlFragment mRemoteFragment;
    private PreviewCameraFragment mCameraFragment;
    private FragmentTransaction mFragmentTransaction;
    private FrameLayout mTextChatContainer;

    //TextChat fragment
    private TextChatFragment mTextChatFragment;

    ProgressDialog mProgressDialog;

    //annotations
    private AnnotationsToolbar mAnnotationsToolbar;
    private AnnotationsVideoRenderer mRemoteRenderer;
    private AnnotationsVideoRenderer mScreensharingRenderer;
    private AnnotationsView mRemoteAnnotationsView;

    //screensharing
    private AnnotationsView mScreenAnnotationsView;
    private View mScreenSharingView;
    private ScreenSharingBar mScreensharingBar;

    private TextView mCallToolbar;

    private CountDownTimer mCountDownTimer;
    private String mRemoteConnId;
    private int mOrientation;

    //permissions
    private boolean mAudioPermission = false;
    private boolean mVideoPermission = false;
    private boolean mWriteExternalStoragePermission = false;
    private boolean mReadExternalStoragePermission = false;

    //status
    private boolean isConnected = false;
    private boolean isCallInProgress = false;
    private boolean isRemoteAnnotations = false;
    private boolean isScreensharing = false;
    private boolean isAnnotations = false;

    //Remote
    private String mRemoteId;
    private String mScreenRemoteId;
    private int mRemotesCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate");

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreviewViewContainer = (RelativeLayout) findViewById(R.id.publisherview);
        mRemoteViewContainer = (RelativeLayout) findViewById(R.id.subscriberview);
        mRemoteViewContainer2 = (RelativeLayout) findViewById(R.id.subscriberview2);
        mRemoteViewContainer3 =(RelativeLayout) findViewById(R.id.subscriberview3);
        mWebViewContainer = (WebView) findViewById(R.id.webview);
        mAlert = (TextView) findViewById(R.id.quality_warning);
        mRemoteAudioOnlyView = (RelativeLayout) findViewById(R.id.audioOnlyView);
        mLocalAudioOnlyView = (RelativeLayout) findViewById(R.id.localAudioOnlyView);
        mCameraFragmentContainer = (RelativeLayout) findViewById(R.id.camera_preview_fragment_container);
        mActionBarContainer = (RelativeLayout) findViewById(R.id.actionbar_preview_fragment_container);
        mTextChatContainer = (FrameLayout) findViewById(R.id.textchat_fragment_container);

        mAnnotationsToolbar = (AnnotationsToolbar) findViewById(R.id.annotations_bar);

        mCallToolbar = (TextView) findViewById(R.id.call_toolbar);

        //request Marshmallow camera permission
        if (ContextCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, permsRequestCode);
            }
        } else {
            mVideoPermission = true;
            mAudioPermission = true;
            mWriteExternalStoragePermission = true;
            mReadExternalStoragePermission = true;
        }

        //init the sdk wrapper
        OTConfig config =
                new OTConfig.OTConfigBuilder(OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN, OpenTokConfig.API_KEY).name("one-to-one-screensharing-sample-app").subscribeAutomatically(true).subscribeToSelf(false).build();

        mWrapper = new OTWrapper(MainActivity.this, config);

        //set listener to receive the communication events, and add UI to these events
        mWrapper.addBasicListener(mBasicListener);
        mWrapper.addAdvancedListener(mAdvancedListener);
        //use a custom video renderer for the annotations. It will be applied to the remote. It will be applied before to start subscribing
        mRemoteRenderer = new AnnotationsVideoRenderer(this);
        mScreensharingRenderer = new AnnotationsVideoRenderer(this);
        mWrapper.setRemoteVideoRenderer(mRemoteRenderer, true);

        //connect
        if (mWrapper != null) {
            mWrapper.connect();
        }

        //show connections dialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Please wait");
        mProgressDialog.setMessage("Connecting...");
        mProgressDialog.show();

        //init controls fragments
        if (savedInstanceState == null) {
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            initCameraFragment(); //to swap camera
            initActionBarFragment(); //to enable/disable local media
            initTextChatFragment(); //to send/receive text-messages
            mFragmentTransaction.commitAllowingStateLoss();
        }

        //get orientation
        mOrientation = getResources().getConfiguration().orientation;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reloadViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWrapper != null) {
            mWrapper.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWrapper != null) {
            mWrapper.resume(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_menu, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(final int permsRequestCode, final String[] permissions,
                                           int[] grantResults) {
        switch (permsRequestCode) {
            case 200:
                mVideoPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                mAudioPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                mReadExternalStoragePermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                mWriteExternalStoragePermission = grantResults[3] == PackageManager.PERMISSION_GRANTED;

                if (!mVideoPermission || !mAudioPermission || !mReadExternalStoragePermission || !mWriteExternalStoragePermission) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(getResources().getString(R.string.permissions_denied_title));
                    builder.setMessage(getResources().getString(R.string.alert_permissions_denied));
                    builder.setPositiveButton("I'M SURE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("RE-TRY", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(permissions, permsRequestCode);
                            }
                        }
                    });
                    builder.show();
                }

                break;
        }
    }

    public OTWrapper getWrapper() {
        return mWrapper;
    }

    public boolean isCallInProgress() {
        return isCallInProgress;
    }

    public void showRemoteControlBar(View v) {
        if (mRemoteFragment != null && (mRemoteId != null)) {
            mRemoteFragment.show();
        }
    }

    public boolean isScreensharing() {
        return isScreensharing;
    }

    @Override
    public void onScreenSharing() {
        if (isScreensharing) {
            stopScreensharing();
            //start avcall
            showAVCall(true);
            mWrapper.startPublishingMedia(new PreviewConfig.PreviewConfigBuilder().
                    name("Tokboxer").build(), false); //restar av call
            isCallInProgress = true;
            mWebViewContainer.setVisibility(View.GONE);
            mActionBarFragment.showAnnotations(false);
        }
        else{
            isScreensharing = true;
            showAVCall(false);
            mWrapper.stopPublishingMedia(false); //stop call
            isCallInProgress = false;
            PreviewConfig.PreviewConfigBuilder builder = new PreviewConfig.PreviewConfigBuilder().name("TokboxerScreen").renderer(mScreensharingRenderer);
            mWrapper.startPublishingMedia(builder.build(), true); //start screensharing
            mWebViewContainer.setVisibility(View.VISIBLE);
            mActionBarFragment.showAnnotations(true);
        }
    }

    @Override
    public void onAnnotations() {
        if (!isAnnotations) {
            showAnnotationsToolbar(true);
        } else {
            showAnnotationsToolbar(false);
        }
    }

    @Override
    public void onTextChat() {
        if (mTextChatContainer.getVisibility() == View.VISIBLE) {
            mTextChatContainer.setVisibility(View.GONE);
            showAVCall(true);
        } else {
            showAVCall(false);
            mTextChatContainer.setVisibility(View.VISIBLE);
        }
    }

    //TextChat Fragment listener events
    @Override
    public void onNewSentMessage(ChatMessage message) {
        Log.i(LOG_TAG, "New sent message");
    }

    @Override
    public void onNewReceivedMessage(ChatMessage message) {
        Log.i(LOG_TAG, "New received message");
        if (mTextChatContainer.getVisibility() != View.VISIBLE ) {
            mActionBarFragment.unreadMessages(true);
        }
        else
            mActionBarFragment.unreadMessages(false);
    }

    @Override
    public void onTextChatError(String error) {
        Log.i(LOG_TAG, "Error on text chat " + error);
    }

    @Override
    public void onClosed() {
        Log.i(LOG_TAG, "OnClosed text-chat");
        mTextChatContainer.setVisibility(View.GONE);
        showAVCall(true);
        mActionBarFragment.unreadMessages(false);
        restartTextChatLayout(true);
    }

    @Override
    public void onRestarted() {
        Log.i(LOG_TAG, "Restarted textchat");
    }

    //Audio remote button event
    @Override
    public void onDisableRemoteAudio(boolean audio) {
        if (mWrapper != null) {
            mWrapper.enableReceivedMedia(mRemoteId, MediaType.AUDIO, audio);
        }
    }

    //Video remote button event
    @Override
    public void onDisableRemoteVideo(boolean video) {
        if (mWrapper != null) {
            mWrapper.enableReceivedMedia(mRemoteId, MediaType.VIDEO, video);
        }
    }

    //Camera control button event
    @Override
    public void onCameraSwap() {
        if (mWrapper != null) {
            mWrapper.cycleCamera();
        }
    }

    //ScreensharingBar event
    @Override
    public void onClose() {
        onScreenSharing();
    }

    public void onCallToolbar(View view) {
        showAll();
    }

    //Video local button event
    @Override
    public void onDisableLocalVideo(boolean video) {
        if (mWrapper != null) {
            mWrapper.enableLocalMedia(MediaType.VIDEO, video);

            if ( mRemoteId != null || mScreenRemoteId != null ) {
                if (!video) {
                    mAudioOnlyImage = new ImageView(this);
                    mAudioOnlyImage.setImageResource(R.drawable.avatar);
                    mAudioOnlyImage.setBackgroundResource(R.drawable.bckg_audio_only);
                    mPreviewViewContainer.addView(mAudioOnlyImage, mLayoutParamsPreview);
                } else {
                    mPreviewViewContainer.removeView(mAudioOnlyImage);
                }
            } else {
                if (!video) {
                    mLocalAudioOnlyView.setVisibility(View.VISIBLE);
                    mPreviewViewContainer.addView(mLocalAudioOnlyView);
                } else {
                    mLocalAudioOnlyView.setVisibility(View.GONE);
                    mPreviewViewContainer.removeView(mLocalAudioOnlyView);
                }
            }
        }
    }

    @Override
    public void onDisableLocalAudio(boolean audio) {
        if (mWrapper != null) {
            mWrapper.enableLocalMedia(MediaType.AUDIO, audio);
        }
    }

    //Call button event
    @Override
    public void onCall() {
        Log.i(LOG_TAG, "OnCall");
        if ( mWrapper != null && isConnected ) {
            if (!isCallInProgress && !isScreensharing) {
                mWrapper.startPublishingMedia(new PreviewConfig.PreviewConfigBuilder().
                        name("Tokboxer").build(), false);

                if (mActionBarFragment != null) {
                    mActionBarFragment.setEnabled(true);
                }
                isCallInProgress = true;
            }
            else {
                if (isScreensharing) {
                    stopScreensharing();
                    showAVCall(true);
                }
                else {
                    mWrapper.stopPublishingMedia(false);
                    isCallInProgress = false;
                }
                cleanViewsAndControls();
            }
        }
    }

    //Annotations events
    @Override
    public void onScreencaptureReady(Bitmap bmp) {
        saveScreencapture(bmp);
    }

    @Override
    public void onAnnotationsSelected(AnnotationsView.Mode mode) {
        if (mode.equals(AnnotationsView.Mode.Pen) || mode.equals(AnnotationsView.Mode.Text)) {
            showAll();
            //show minimized calltoolbar
            mCallToolbar.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mAnnotationsToolbar.getLayoutParams();
            params.addRule(RelativeLayout.ABOVE, mCallToolbar.getId());
            mAnnotationsToolbar.setLayoutParams(params);
            mAnnotationsToolbar.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onAnnotationsDone() {
    }

    @Override
    public void onError(String error) {
    }

    //Basic Listener from OTWrapper
    private BasicListener mBasicListener =
            new PausableBasicListener(new BasicListener<OTWrapper>() {

                @Override
                public void onConnected(OTWrapper otWrapper, int participantsCount, String connId, String data) throws ListenerException {
                    Log.i(LOG_TAG, "Connected to the session. Number of participants: " + participantsCount);
                    if (mWrapper.getOwnConnId() == connId) {
                        isConnected = true;
                        mProgressDialog.dismiss();
                        //TextchatFragment requires a session. In the current accelerator, the session is connected in the app and then,
                        // the accelerator is initialized.
                        if (mTextChatFragment != null) {
                            mTextChatFragment.init();
                        }
                    }
                    else {
                        mRemoteConnId = connId;
                    }
                }

                @Override
                public void onDisconnected(OTWrapper otWrapper, int participantsCount, String connId, String data) throws ListenerException {
                    Log.i(LOG_TAG, "Connection dropped: " + connId);
                    if (connId == mWrapper.getOwnConnId()) {
                        Log.i(LOG_TAG, "Disconnected to the session");
                        cleanViewsAndControls();
                    }
                }

                @Override
                public void onPreviewViewReady(OTWrapper otWrapper, View localView) throws ListenerException {
                    if (isScreensharing) {
                        mScreenSharingView = localView;
                        mWebViewContainer.setWebViewClient(new WebViewClient());
                        WebSettings webSettings = mWebViewContainer.getSettings();
                        webSettings.setJavaScriptEnabled(true);
                        mWebViewContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                        mWebViewContainer.loadUrl("http://www.tokbox.com");
                    }
                    else {
                        setLocalView(localView);
                    }
                }

                @Override
                public void onPreviewViewDestroyed(OTWrapper otWrapper, View localView) throws ListenerException {
                    Log.i(LOG_TAG, "Local preview view is destroyed");
                    setLocalView(null);
                }

                @Override
                public void onRemoteViewReady(OTWrapper otWrapper, View remoteView, String remoteId, String data) throws ListenerException {
                    Log.i(LOG_TAG, "Remove view is ready");
                    //if (remoteId == mRemoteId) {
                        if (isCallInProgress()) {
                            mRemotesCount++;
                            setRemoteView(remoteView, remoteId);
                        }
                    //}
                    //else {
                        if ( mWrapper.getRemoteStreamStatus(remoteId).getType() == StreamStatus.StreamType.SCREEN ) {
                            setRemoteView(remoteView, remoteId);
                            mScreenRemoteId = remoteId;
                        }
                    //}

                    mRemoteView = remoteView;
                }

                @Override
                public void onRemoteViewDestroyed(OTWrapper otWrapper, View remoteView, String remoteId) throws ListenerException {
                    Log.i(LOG_TAG, "Remote view is destroyed");
                    mRemotesCount--;
                    setRemoteView(null, remoteId);
                    if ( remoteId == mRemoteId ){
                        mRemoteId = null;
                    }
                    else {
                        if ( remoteId == mScreenRemoteId ){
                            mScreenRemoteId = null;
                        }
                    }
                    reloadViews();
                }

                @Override
                public void onStartedPublishingMedia(OTWrapper otWrapper, boolean screensharing) throws ListenerException {
                    Log.i(LOG_TAG, "Local started streaming video.");

                    //Check if there are some connected remotes
                    checkRemotes();

                    if (screensharing) {
                        screenAnnotations();
                    }
                }

                @Override
                public void onStoppedPublishingMedia(OTWrapper otWrapper, boolean isScreensharing) throws ListenerException {
                    Log.i(LOG_TAG, "Local stopped streaming video.");
                }

                @Override
                public void onRemoteJoined(OTWrapper otWrapper, String remoteId) throws ListenerException {
                    Log.i(LOG_TAG, "A new remote joined.");
                    if (mRemoteId == null) { //one-to-one, the first to arrive, will be the used
                        mRemoteId = remoteId;
                        initRemoteFragment(remoteId);
                    }
                    else {
                        //check remote screen
                        if ( mWrapper.getRemoteStreamStatus(remoteId).getType() == StreamStatus.StreamType.SCREEN ) {
                            mScreenRemoteId = remoteId;
                        }
                    }
                }

                @Override
                public void onRemoteLeft(OTWrapper otWrapper, String remoteId) throws ListenerException {
                    Log.i(LOG_TAG, "A new remote left.");
                    if (mRemoteId != null && remoteId == mRemoteId) { //one-to-one
                        mRemoteId = null;
                    }
                    else {
                        if ( mScreenRemoteId != null && remoteId == mScreenRemoteId ){
                            mScreenRemoteId = null;
                        }
                    }
                }

                @Override
                public void onRemoteVideoChanged(OTWrapper otWrapper, String remoteId, String reason, boolean videoActive, boolean subscribed) throws ListenerException {
                    Log.i(LOG_TAG, "Remote video changed");
                    if (isCallInProgress) {
                        if (reason.equals("quality")) {
                            //network quality alert
                            mAlert.setBackgroundResource(R.color.quality_alert);
                            mAlert.setTextColor(MainActivity.this.getResources().getColor(R.color.white));
                            mAlert.bringToFront();
                            mAlert.setVisibility(View.VISIBLE);
                            mAlert.postDelayed(new Runnable() {
                                public void run() {
                                    mAlert.setVisibility(View.GONE);
                                }
                            }, 7000);
                        }

                        if (!videoActive) {
                            onRemoteAudioOnly(true); //video is not active
                        } else {
                            onRemoteAudioOnly(false);
                        }
                    }
                }

                @Override
                public void onError(OTWrapper otWrapper, OpentokError error) throws ListenerException {
                    Log.i(LOG_TAG, "Error " + error.getErrorCode() + "-" + error.getMessage());
                    Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    mWrapper.disconnect(); //end communication
                    mProgressDialog.dismiss();
                    cleanViewsAndControls(); //restart views
                }
            });

    //Advanced Listener from OTWrapper
    private AdvancedListener mAdvancedListener =
            new PausableAdvancedListener(new AdvancedListener<OTWrapper>() {

                @Override
                public void onCameraChanged(OTWrapper otWrapper) throws ListenerException {
                    Log.i(LOG_TAG, "The camera changed");
                }

                @Override
                public void onReconnecting(OTWrapper otWrapper) throws ListenerException {
                    Log.i(LOG_TAG, "The session is reconnecting.");
                    Toast.makeText(MainActivity.this, R.string.reconnecting, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onReconnected(OTWrapper otWrapper) throws ListenerException {
                    Log.i(LOG_TAG, "The session reconnected.");
                    Toast.makeText(MainActivity.this, R.string.reconnected, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onVideoQualityWarning(OTWrapper otWrapper, String remoteId) throws ListenerException {
                    Log.i(LOG_TAG, "The quality has degraded");

                    mAlert.setBackgroundResource(R.color.quality_warning);
                    mAlert.setTextColor(MainActivity.this.getResources().getColor(R.color.warning_text));

                    mAlert.bringToFront();
                    mAlert.setVisibility(View.VISIBLE);
                    mAlert.postDelayed(new Runnable() {
                        public void run() {
                            mAlert.setVisibility(View.GONE);
                        }
                    }, 7000);
                }

                @Override
                public void onVideoQualityWarningLifted(OTWrapper otWrapper, String remoteId) throws ListenerException {
                    Log.i(LOG_TAG, "The quality has improved");
                }

                @Override
                public void onError(OTWrapper otWrapper, OpentokError error) throws ListenerException {
                    Log.i(LOG_TAG, "Error " + error.getErrorCode() + "-" + error.getMessage());
                    Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    mWrapper.disconnect(); //end communication
                    mProgressDialog.dismiss();
                    cleanViewsAndControls(); //restart views
                }
            });

    private void checkRemotes(){
        if (mRemoteId != null){
            if (!mWrapper.isReceivedMediaEnabled(mRemoteId, MediaType.VIDEO)){
                onRemoteAudioOnly(true);
            }
            else {
                setRemoteView(mWrapper.getRemoteStreamStatus(mRemoteId).getView(), mRemoteId);
            }
        }
        if (mScreenRemoteId != null){
            setRemoteView(mWrapper.getRemoteStreamStatus(mScreenRemoteId).getView(), mScreenRemoteId);
        }
    }

    private void stopScreensharing() {
        //hide screensharing bar and view
        isScreensharing = false;
        ((ViewGroup)mScreenSharingView).removeView(mScreenAnnotationsView);
        showScreensharingBar(false);
        mActionBarFragment.restartScreensharing(); //restart screensharing UI

        //hide annotations
        showAnnotationsToolbar(false);

        mWrapper.stopPublishingMedia(true);
    }

    private void saveScreencapture(Bitmap bmp) {
        if (bmp != null) {
            Bitmap annotationsBmp = null;
            Bitmap overlayBmp = null;
            if ( mRemoteAnnotationsView != null ){
                annotationsBmp= getBitmapFromView(mRemoteAnnotationsView);
                overlayBmp = mergeBitmaps(bmp, annotationsBmp);
            }
            else {
                overlayBmp = bmp;
            }

            String filename;
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            filename = sdf.format(date);
            try {
                String path = Environment.getExternalStorageDirectory().toString();
                OutputStream fOut = null;
                File file = new File(path, filename + ".jpg");
                fOut = new FileOutputStream(file);

                overlayBmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                fOut.flush();
                fOut.close();

                MediaStore.Images.Media.insertImage(getContentResolver()
                        , file.getAbsolutePath(), file.getName(), file.getName());

                openScreenshot(file);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable =view.getBackground();
        if (bgDrawable!=null)
            bgDrawable.draw(canvas);
        view.draw(canvas);
        return returnedBitmap;
    }

    private Bitmap mergeBitmaps(Bitmap bmp1, Bitmap bmp2){
        Bitmap bmpOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        bmp2 = Bitmap.createScaledBitmap(bmp2, bmp1.getWidth(), bmp1.getHeight(),
                true);
        Canvas canvas = new Canvas(bmpOverlay);
        canvas.drawBitmap(bmp1, 0,0, null);
        canvas.drawBitmap(bmp2, 0,0, null);

        return bmpOverlay;
    }

    private void openScreenshot(File imageFile) {
        Uri uri = Uri.fromFile(imageFile);
        Intent intentSend = new Intent();
        intentSend.setAction(Intent.ACTION_SEND);
        intentSend.setType("image/*");

        intentSend.putExtra(Intent.EXTRA_SUBJECT, "");
        intentSend.putExtra(Intent.EXTRA_TEXT, "");
        intentSend.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intentSend, "Share Screenshot"));
    }

    private void showAnnotationsToolbar(boolean show) {
        isAnnotations = show;
        if (show) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mAnnotationsToolbar.getLayoutParams();
            params.addRule(RelativeLayout.ABOVE, mActionBarContainer.getId());
            mAnnotationsToolbar.setLayoutParams(params);
            mAnnotationsToolbar.setVisibility(View.VISIBLE);
            mActionBarContainer.setVisibility(View.VISIBLE);
        } else {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
                mCountDownTimer = null;
            }
            mCallToolbar.setVisibility(View.GONE);
            mAnnotationsToolbar.setVisibility(View.GONE);
            mActionBarContainer.setVisibility(View.VISIBLE);
            mAnnotationsToolbar.restart();
        }
    }

    private void showAll() {
        mCallToolbar.setVisibility(View.GONE);
        showAnnotationsToolbar(true);
        mCountDownTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (isAnnotations) {
                    mCallToolbar.setVisibility(View.VISIBLE);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mAnnotationsToolbar.getLayoutParams();
                    params.addRule(RelativeLayout.ABOVE, mCallToolbar.getId());
                    mAnnotationsToolbar.setLayoutParams(params);
                    mAnnotationsToolbar.setVisibility(View.VISIBLE);
                    mActionBarContainer.setVisibility(View.GONE);
                }
            }
        }.start();
    }

    private void showScreensharingBar(boolean show){
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        if ( show ) {
            mScreensharingBar = new ScreenSharingBar(MainActivity.this, this);

            //add screensharing bar on top of the screen
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                    0 | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.LEFT | Gravity.TOP;
            params.x = 0;
            params.y = 0;


            wm.addView(mScreensharingBar, params);
        }
        else {
            wm.removeView(mScreensharingBar);
            mScreensharingBar = null;
        }
    }
    private void initActionBarFragment() {
        mActionBarFragment = new ActionBarFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.actionbar_preview_fragment_container, mActionBarFragment).commit();

        if (isRemoteAnnotations || isAnnotations) {
            mActionBarFragment.enableAnnotations(true);
        }
    }

    private void initRemoteFragment(String remoteId) {
        mRemoteFragment = new RemoteControlFragment();

        Bundle args = new Bundle();
        args.putString("remoteId", remoteId);
        mRemoteFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.actionbar_remote_fragment_container, mRemoteFragment).commit();
    }

    private void initCameraFragment() {
        mCameraFragment = new PreviewCameraFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.camera_preview_fragment_container, mCameraFragment).commit();
    }

    private void initTextChatFragment() {
        mTextChatFragment = TextChatFragment.newInstance(mWrapper.getSession(), OpenTokConfig.API_KEY);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.textchat_fragment_container, mTextChatFragment).commit();
        getSupportFragmentManager().executePendingTransactions();
        try {
            mTextChatFragment.setSenderAlias("Tokboxer");
            mTextChatFragment.setMaxTextLength(140);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mTextChatFragment.setListener(this);
    }

    private void cleanViewsAndControls() {
        if (mRemoteId != null){
            setRemoteView(null, mRemoteId);
        }
        if (mScreenRemoteId != null){
            mWrapper.removeRemote(mScreenRemoteId);
            setRemoteView(null, mScreenRemoteId);
        }
        if (mPreviewViewContainer.getChildCount() > 0) {
            setLocalView(null);
        }
        if (mActionBarFragment != null)
            mActionBarFragment.restart();
        if (mRemoteFragment != null)
            mRemoteFragment.restart();
        if (mTextChatFragment != null) {
            restartTextChatLayout(true);
            mTextChatFragment.restart();
            mTextChatContainer.setVisibility(View.GONE);
        }
        if (mActionBarContainer != null)
            mActionBarContainer.setBackground(null);
        if (mRemoteAnnotationsView != null) {
            mRemoteViewContainer.removeView(mRemoteAnnotationsView);
        }

        mWebViewContainer.setVisibility(View.GONE);

        mRemoteAnnotationsView = null;
        mScreenAnnotationsView = null;
    }

    private void reloadViews(){
        if (mRemoteView != null) {
            mRemoteViewContainer.removeView(mRemoteView);
        }
        if ( mRemoteId != null ) {
            setRemoteView(mWrapper.getRemoteStreamStatus(mRemoteId).getView(), mRemoteId);
        }
        if ( mScreenRemoteId != null ) {
            setRemoteView(mWrapper.getRemoteStreamStatus(mScreenRemoteId).getView(), mScreenRemoteId);
        }
    }

    private void showAVCall(boolean show) {
        if (show) {
            mPreviewViewContainer.setVisibility(View.VISIBLE);
            mRemoteViewContainer.setVisibility(View.VISIBLE);
            mCameraFragmentContainer.setVisibility(View.VISIBLE);
        } else {
            mPreviewViewContainer.setVisibility(View.GONE);
            mRemoteViewContainer.setVisibility(View.GONE);
            mCameraFragmentContainer.setVisibility(View.GONE);
        }
    }

    private void setLocalView(View localView){
        if (localView != null) {
            mPreviewViewContainer.removeAllViews();
            mLayoutParamsPreview = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            if ( mRemoteId != null || mScreenRemoteId != null ) {
                mLayoutParamsPreview.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,
                        RelativeLayout.TRUE);
                mLayoutParamsPreview.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                        RelativeLayout.TRUE);
                mLayoutParamsPreview.width = (int) getResources().getDimension(R.dimen.preview_width);
                mLayoutParamsPreview.height = (int) getResources().getDimension(R.dimen.preview_height);
                mLayoutParamsPreview.rightMargin = (int) getResources().getDimension(R.dimen.preview_rightMargin);
                mLayoutParamsPreview.bottomMargin = (int) getResources().getDimension(R.dimen.preview_bottomMargin);
            }
            mPreviewViewContainer.addView(localView, mLayoutParamsPreview);

        }
        else {
            mPreviewViewContainer.removeAllViews();
        }
    }
    private void screenAnnotations() {
        try {
            if ( isScreensharing ) {
                mScreenAnnotationsView = new AnnotationsView(this, mWrapper.getSession(), OpenTokConfig.API_KEY, true);
                //size of annotations screen, by default will be all the screen
                //take into account the calltoolbar as well
                mCallToolbar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                mAnnotationsToolbar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                mAnnotationsToolbar.getChildAt(0).measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); //color toolbar

                Point display = new Point();
                getWindowManager().getDefaultDisplay().getSize(display);
                int height = display.y - mAnnotationsToolbar.getMeasuredHeight() - mCallToolbar.getMeasuredHeight() - mAnnotationsToolbar.getChildAt(0).getMeasuredHeight();
                int width =  getWindow().getDecorView().getRootView().getWidth();

                mScreenAnnotationsView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
                mScreenAnnotationsView.attachToolbar(mAnnotationsToolbar);
                mScreenAnnotationsView.setVideoRenderer(mScreensharingRenderer);
                mScreenAnnotationsView.setAnnotationsListener(this);

                ((ViewGroup) mScreenSharingView).addView(mScreenAnnotationsView);
                mActionBarFragment.enableAnnotations(true);
                showScreensharingBar(true);
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, "Exception - enableRemoteAnnotations: " + e);
        }
    }

    private void remoteAnnotations() {
        try {
            mRemoteAnnotationsView = new AnnotationsView(this, mWrapper.getSession(), OpenTokConfig.API_KEY, mRemoteConnId);
            mRemoteAnnotationsView.setVideoRenderer(mRemoteRenderer);
            mRemoteAnnotationsView.attachToolbar(mAnnotationsToolbar);
            mRemoteAnnotationsView.setAnnotationsListener(this);
            ((ViewGroup) mRemoteViewContainer).addView(mRemoteAnnotationsView);
            mActionBarFragment.enableAnnotations(true);
        } catch (Exception e) {
            Log.i(LOG_TAG, "Exception - enableRemoteAnnotations: " + e);
        }
    }

    private RelativeLayout.LayoutParams getMultipartyLayoutParams() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if ( mRemoteId != null || mScreenRemoteId != null ) {
            mLayoutParamsPreview.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                    RelativeLayout.TRUE);
            mLayoutParamsPreview.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                    RelativeLayout.TRUE);
           // mLayoutParamsPreview.addRule(RelativeLayout.ABOVE, mPreviewViewContainer.getId());

            mLayoutParamsPreview.width = (int) getResources().getDimension(R.dimen.preview_width);
            mLayoutParamsPreview.height = (int) getResources().getDimension(R.dimen.preview_height);
            mLayoutParamsPreview.rightMargin = (int) getResources().getDimension(R.dimen.preview_rightMargin);
       //     mLayoutParamsPreview.bottomMargin = (int) getResources().getDimension(R.dimen.preview_bottomMargin);
        }

        return params;
    }

    private void setRemoteView(View remoteView, String remoteId){
        if (mPreviewViewContainer.getChildCount() > 0) {
            setLocalView(mPreviewViewContainer.getChildAt(0)); //main preview view
        }

        if (remoteView != null) {
            if (mRemotesCount == 1) {
                if (mWrapper.getRemoteStreamStatus(remoteId).getType() == StreamStatus.StreamType.SCREEN) {
                    Log.i(LOG_TAG, "remote view screen");
                    if (mRemoteViewContainer.getChildCount() > 2) {
                        mRemoteViewContainer.removeViewAt(mRemoteViewContainer.getChildCount() - 1);
                    }
                    //force landscape
                    if (mWrapper.getRemoteStreamStatus(remoteId).getWidth() > mWrapper.getRemoteStreamStatus(remoteId).getHeight()) {
                        forceLandscape();
                    }
                    //show remote view
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                            this.getResources().getDisplayMetrics().widthPixels, this.getResources()
                            .getDisplayMetrics().heightPixels);
                    mRemoteViewContainer.addView(remoteView, layoutParams);
                    remoteAnnotations();
                    isRemoteAnnotations = true;
                } else {
                    //show remote view
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                            this.getResources().getDisplayMetrics().widthPixels, this.getResources()
                            .getDisplayMetrics().heightPixels);
                    mRemoteViewContainer.removeView(remoteView);
                    mRemoteViewContainer.addView(remoteView, layoutParams);
                    mRemoteViewContainer.setClickable(true);
                    if (mRemoteFragment != null)
                        mRemoteFragment.show();
                }
            }
            else {
                if (mRemotesCount == 2) {
                    mRemoteViewContainer2.addView(remoteView, getMultipartyLayoutParams());
                }
            }
        } else { //view null --> remove view
            if (mRemoteViewContainer.getChildCount() > 0 ) {
                mRemoteViewContainer.removeAllViews();
                mRemoteViewContainer.addView(mRemoteAudioOnlyView); //restart audioOnly view
            }
            mRemoteViewContainer.setClickable(false);
            mRemoteAudioOnlyView.setVisibility(View.GONE);
            showAnnotationsToolbar(false);
            restartOrientation();
        }
    }

    private void onRemoteAudioOnly(boolean enabled) {
        if (enabled) {
            mRemoteView.setVisibility(View.GONE);
            mRemoteAudioOnlyView.setVisibility(View.VISIBLE);
        }
        else {
            mRemoteView.setVisibility(View.VISIBLE);
            mRemoteAudioOnlyView.setVisibility(View.GONE);
        }
    }

    private int dpToPx(int dp) {
        double screenDensity = this.getResources().getDisplayMetrics().density;
        return (int) (screenDensity * (double) dp);
    }

    private void forceLandscape() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private void restartOrientation() {
        setRequestedOrientation(mOrientation);
    }

    private void restartTextChatLayout(boolean restart) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mTextChatContainer.getLayoutParams();

        if (restart) {
            //restart to the original size
            params.width = RelativeLayout.LayoutParams.MATCH_PARENT;
            params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        } else {
            //go to the minimized size
            params.height = dpToPx(40);
            params.addRule(RelativeLayout.ABOVE, R.id.actionbar_preview_fragment_container);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        }
        mTextChatContainer.setLayoutParams(params);
    }
}