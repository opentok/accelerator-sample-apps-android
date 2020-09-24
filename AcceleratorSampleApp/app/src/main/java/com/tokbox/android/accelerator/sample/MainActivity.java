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
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;

import com.opentok.android.OpentokError;
import com.tokbox.android.accelerator.sample.ui.ActionBarFragment;
import com.tokbox.android.accelerator.sample.ui.Participant;
import com.tokbox.android.accelerator.sample.ui.ParticipantsAdapter;
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
import com.tokbox.android.accelerator.sample.ui.ScreenSharingBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements ActionBarFragment.PreviewControlCallbacks, AnnotationsView.AnnotationsListener,
        ScreenSharingBar.ScreenSharingBarListener, TextChatFragment.TextChatListener, ParticipantsAdapter.ParticipantAdapterListener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final int permsRequestCode = 200;

    //OpenTok calls
    private OTWrapper mWrapper;

    //Participants Grid management
    private RecyclerView mParticipantsGrid;
    private GridLayoutManager mLayoutManager;
    private ParticipantsAdapter mParticipantsAdapter;
    private List<Participant> mParticipantsList = new ArrayList<>();

    //Fragments and containers
    private ActionBarFragment mActionBarFragment;
    private FragmentTransaction mFragmentTransaction;
    private FrameLayout mTextChatContainer;
    private RelativeLayout mActionBarContainer;
    private TextChatFragment mTextChatFragment;

    //Annotations
    private AnnotationsToolbar mAnnotationsToolbar;
    private AnnotationsVideoRenderer mRemoteRenderer;
    private AnnotationsVideoRenderer mScreensharingRenderer;
    private AnnotationsView mRemoteAnnotationsView;

    //ScreenSharing
    private RelativeLayout mScreenSharingContainer;
    private AnnotationsView mScreenAnnotationsView;
    private View mScreenSharingView;
    private ScreenSharingBar mScreensharingBar;
    private TextView mCallToolbar;
    private String mScreenRemoteId;
    private WebView mWebViewContainer;

    private TextView mAlert;
    private CountDownTimer mCountDownTimer;
    private ProgressDialog mProgressDialog;

    //Permissions
    private boolean mAudioPermission = false;
    private boolean mVideoPermission = false;
    private boolean mWriteExternalStoragePermission = false;
    private boolean mReadExternalStoragePermission = false;

    //status
    private boolean isConnected = false;
    private boolean isCallInProgress = false;
    private boolean isRemoteAnnotations = false;
    private boolean isScreenSharing = false;
    private boolean isAnnotations = false;
    private boolean isReadyToCall = false;

    //Current orientation
    private int mOrientation;

    //Current remote
    private String mCurrentRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mParticipantsGrid = (RecyclerView) findViewById(R.id.grid_container);
        mLayoutManager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        setupMultipartyLayout();
        mParticipantsGrid.setLayoutManager(mLayoutManager);
        try {
            mParticipantsAdapter = new ParticipantsAdapter(MainActivity.this, mParticipantsList, MainActivity.this);
            if (mParticipantsAdapter != null) {
                mParticipantsGrid.setAdapter(mParticipantsAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mWebViewContainer = (WebView) findViewById(R.id.webview);
        mAlert = (TextView) findViewById(R.id.quality_warning);
        mScreenSharingContainer = (RelativeLayout) findViewById(R.id.screen_sharing_container);
        mActionBarContainer = (RelativeLayout) findViewById(R.id.actionbar_fragment_container);
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
                new OTConfig.OTConfigBuilder(OpenTokConfig.SESSION_ID, OpenTokConfig.TOKEN, OpenTokConfig.API_KEY).name("accelerator-sample-app").subscribeAutomatically(true).subscribeToSelf(false).build();

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
        if (permsRequestCode == 200) {
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
        }
    }

    public OTWrapper getWrapper() {
        return mWrapper;
    }

    public boolean isCallInProgress() {
        return isCallInProgress;
    }

    public boolean isScreenSharing() {
        return isScreenSharing;
    }

    public void onCallToolbar(View view) {
        showAll();
    }

    //ParticipantAdapter listener
    @Override
    public void mediaControlChanged(String remoteId) {
        Log.i(LOG_TAG, "Participant control changed: "+remoteId);
        mCurrentRemote = remoteId;
    }

    @Override
    public void onScreenSharing() {
        if (isScreenSharing) {
            stopScreenSharing();
            //start avcall
            isCallInProgress = true;
            showAVCall(true);
            mWrapper.startPublishingMedia(new PreviewConfig.PreviewConfigBuilder().
                    name("Tokboxer").build(), false); //restart av call
            mWebViewContainer.setVisibility(View.GONE);
            mActionBarFragment.showAnnotations(false);
        } else {
            isScreenSharing = true;
            showAVCall(false);
            mWrapper.stopPublishingMedia(false); //stop call
            isCallInProgress = false;
            PreviewConfig.PreviewConfigBuilder builder = new PreviewConfig.PreviewConfigBuilder().name("TokboxerScreen").renderer(mScreensharingRenderer);
            mWrapper.startPublishingMedia(builder.build(), true); //start screen sharing
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
            mActionBarFragment.unreadMessages(false);
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
        if (mTextChatContainer.getVisibility() != View.VISIBLE) {
            mActionBarFragment.unreadMessages(true);
        } else
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
        Log.i(LOG_TAG, "Restarted text chat");
    }


    //Screen sharing bar listener event
    @Override
    public void onClose() {
        Log.i(LOG_TAG, "Close screen sharing");
        onScreenSharing();
    }

    //Video local button event
    @Override
    public void onDisableLocalVideo(boolean video) {
        Log.i(LOG_TAG, "Disable/Enable local video");
        if (mWrapper != null) {
            mWrapper.enableLocalMedia(MediaType.VIDEO, video);
            updateParticipant(Participant.Type.LOCAL, null, video);
        }
    }

    @Override
    public void onDisableLocalAudio(boolean audio) {
        Log.i(LOG_TAG, "Disable/Enable local audio_icon");
        if (mWrapper != null) {
            mWrapper.enableLocalMedia(MediaType.AUDIO, audio);
        }
    }

    //Call button event
    @Override
    public void onCall() {
        Log.i(LOG_TAG, "OnCall");
        mActionBarFragment.setCallButtonEnabled(false);
        if (mWrapper != null && isConnected) {
            if (!isCallInProgress && !isScreenSharing) {
                isCallInProgress = true;
                mWrapper.startPublishingMedia(new PreviewConfig.PreviewConfigBuilder().
                        name("Tokboxer").build(), false);
                if (mActionBarFragment != null) {
                    mActionBarFragment.setEnabled(true);
                }
            } else {
                if (isScreenSharing) {
                    stopScreenSharing();
                    showAVCall(true);
                } else {
                    mWrapper.stopPublishingMedia(false);
                    mWrapper.disconnect();
                    isCallInProgress = false;
                }
                cleanViewsAndControls();
            }
        }
        else {
            if (mWrapper != null) {
                isReadyToCall = true;
                mWrapper.connect();
            }
        }
    }

    //Annotations events
    @Override
    public void onScreencaptureReady(Bitmap bmp) {
        Log.i(LOG_TAG, "Screen capture is ready");
        saveScreenCapture(bmp);
    }

    @Override
    public void onAnnotationsSelected(AnnotationsView.Mode mode) {
        if (mode.equals(AnnotationsView.Mode.Pen) || mode.equals(AnnotationsView.Mode.Text)) {
            showAll();
            //show minimized call toolbar
            mCallToolbar.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mAnnotationsToolbar.getLayoutParams();
            params.addRule(RelativeLayout.ABOVE, mCallToolbar.getId());
            mAnnotationsToolbar.setLayoutParams(params);
            mAnnotationsToolbar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAnnotationsDone() {
        Log.i(LOG_TAG, "Annotations have been done");
    }

    @Override
    public void onError(String error) {
        Log.i(LOG_TAG, "Annotations error: " + error);
    }

    //Basic Listener from OTWrapper
    private BasicListener mBasicListener =
            new PausableBasicListener(new BasicListener<OTWrapper>() {

                @Override
                public void onConnected(OTWrapper otWrapper, int participantsCount, String connId, String data) throws ListenerException {
                    Log.i(LOG_TAG, "Connected to the session. Number of participants: " + participantsCount);
                    if (Objects.equals(mWrapper.getOwnConnId(), connId)) {
                        isConnected = true;
                        mProgressDialog.dismiss();
                        //TextchatFragment requires a session. In the current accelerator, the session is connected in the app and then,
                        // the accelerator is initialized.
                        if (mTextChatFragment != null) {
                            mTextChatFragment.init();
                        }
                    }

                    if (isReadyToCall) {
                        isReadyToCall = false;
                        onCall();
                    }
                }

                @Override
                public void onDisconnected(OTWrapper otWrapper, int participantsCount, String connId, String data) throws ListenerException {
                    Log.i(LOG_TAG, "Connection dropped: " + connId);
                    if (Objects.equals(connId, mWrapper.getOwnConnId())) {
                        Log.i(LOG_TAG, "Disconnected to the session");
                        cleanViewsAndControls();
                        isConnected = false;
                    }
                }

                @Override
                public void onPreviewViewReady(OTWrapper otWrapper, View localView) throws ListenerException {
                    Log.i(LOG_TAG, "Local preview view is ready");
                    if (isScreenSharing) {
                        //Share local web view
                        mScreenSharingView = localView;
                        mWebViewContainer.setWebViewClient(new WebViewClient());
                        WebSettings webSettings = mWebViewContainer.getSettings();
                        webSettings.setJavaScriptEnabled(true);
                        mWebViewContainer.loadUrl("https://www.tokbox.com");
                    } else {
                        //audio/video call view
                        Participant participant = new Participant(Participant.Type.LOCAL, mWrapper.getLocalStreamStatus(), getParticipantSize());
                        addNewParticipant(participant);
                    }
                }

                @Override
                public void onPreviewViewDestroyed(OTWrapper otWrapper, View localView) throws ListenerException {
                    Log.i(LOG_TAG, "Local preview view is destroyed");
                    removeParticipant(Participant.Type.LOCAL, null);
                }

                @Override
                public void onRemoteViewReady(OTWrapper otWrapper, View remoteView, String remoteId, String data) throws ListenerException {
                    Log.i(LOG_TAG, "Participant remote view is ready");
                    if (mWrapper.getRemoteStreamStatus(remoteId).getType() == StreamStatus.StreamType.SCREEN) {
                        Log.i(LOG_TAG, "Participant is sharing the screen");
                        mScreenRemoteId = remoteId;
                        addRemoteScreenSharing(remoteId, remoteView);
                    } else {
                        Participant newParticipant = new Participant(Participant.Type.REMOTE, mWrapper.getRemoteStreamStatus(remoteId), getParticipantSize(), remoteId);
                        addNewParticipant(newParticipant);
                    }
                }

                @Override
                public void onRemoteViewDestroyed(OTWrapper otWrapper, View remoteView, String remoteId) throws ListenerException {
                    Log.i(LOG_TAG, "Remote view is destroyed");
                    if (Objects.equals(remoteId, mScreenRemoteId)) {
                        mScreenRemoteId = null;
                        removeRemoteScreenSharing();
                    } else {
                        removeParticipant(Participant.Type.REMOTE, remoteId);
                    }
                }

                @Override
                public void onStartedPublishingMedia(OTWrapper otWrapper, boolean screenSharing) throws ListenerException {
                    Log.i(LOG_TAG, "Local started streaming video.");

                    if (screenSharing) {
                        screenAnnotations();
                    }

                    mActionBarFragment.setCallButtonEnabled(true);
                }

                @Override
                public void onStoppedPublishingMedia(OTWrapper otWrapper, boolean screenSharing) throws ListenerException {
                    Log.i(LOG_TAG, "Local stopped streaming video.");
                    mActionBarFragment.setCallButtonEnabled(true);
                    isCallInProgress = false;
                }

                @Override
                public void onRemoteJoined(OTWrapper otWrapper, String remoteId) throws ListenerException {
                    Log.i(LOG_TAG, "A new remote joined.");
                }

                @Override
                public void onRemoteLeft(OTWrapper otWrapper, String remoteId) throws ListenerException {
                    Log.i(LOG_TAG, "A new remote left.");
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
                        updateParticipant(Participant.Type.REMOTE, remoteId, videoActive);
                    }
                }

                @Override
                public void onError(OTWrapper otWrapper, OpentokError error) throws ListenerException {
                    Log.i(LOG_TAG, "Error " + error.getErrorCode() + "-" + error.getMessage());
                    Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    mWrapper.disconnect(); //end communication
                    mProgressDialog.dismiss();
                    cleanViewsAndControls(); //restart views
                    mActionBarFragment.setCallButtonEnabled(false);
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
                public void onReconnected(OTWrapper otWrapper, String remoteId) {
                    Log.i(LOG_TAG, "The session reconnected remoteId: " + remoteId);
                    Toast.makeText(MainActivity.this, R.string.reconnected, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onDisconnected(OTWrapper otWrapper, String remoteId) {
                    Log.i(LOG_TAG, "The session disconnected remoteId: " + remoteId);
                    Toast.makeText(MainActivity.this, R.string.disconnected, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onAudioEnabled(OTWrapper otWrapper, String remoteId) {
                    Log.i(LOG_TAG, "Audio enabled remoteId: " + remoteId);
                    Toast.makeText(MainActivity.this, R.string.audio_enabled, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onAudioDisabled(OTWrapper otWrapper, String remoteId) {
                    Log.i(LOG_TAG, "Audio disabled remoteId: " + remoteId);
                    Toast.makeText(MainActivity.this, R.string.audio_disabled, Toast.LENGTH_LONG).show();
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
                public void onAudioLevelUpdated(float audioLevel) throws ListenerException {
                    Log.i(LOG_TAG, "Audio level changed. Level: " + audioLevel);
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

    //Private methods
    private void stopScreenSharing() {
        //hide screen sharing bar and view
        isScreenSharing = false;
        ((ViewGroup) mScreenSharingView).removeView(mScreenAnnotationsView);
        showScreensharingBar(false);
        mActionBarFragment.restartScreenSharing(); //restart screen sharing UI

        //hide annotations
        showAnnotationsToolbar(false);

        mWrapper.stopPublishingMedia(true);
    }

    private void saveScreenCapture(Bitmap bmp) {
        if (bmp != null) {
            Bitmap annotationsBmp = null;
            Bitmap overlayBmp = null;
            if (mRemoteAnnotationsView != null) {
                annotationsBmp = getBitmapFromView(mRemoteAnnotationsView);
                overlayBmp = mergeBitmaps(bmp, annotationsBmp);
            } else {
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
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        view.draw(canvas);
        return returnedBitmap;
    }

    private Bitmap mergeBitmaps(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmpOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        bmp2 = Bitmap.createScaledBitmap(bmp2, bmp1.getWidth(), bmp1.getHeight(),
                true);
        Canvas canvas = new Canvas(bmpOverlay);
        canvas.drawBitmap(bmp1, 0, 0, null);
        canvas.drawBitmap(bmp2, 0, 0, null);

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

    private void showScreensharingBar(boolean show) {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (show) {
            mScreensharingBar = new ScreenSharingBar(MainActivity.this, this);

            //add screen sharing bar on top of the screen
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.LEFT | Gravity.TOP;
            params.x = 0;
            params.y = 0;


            wm.addView(mScreensharingBar, params);
        } else {
            wm.removeView(mScreensharingBar);
            mScreensharingBar = null;
        }
    }

    private void initActionBarFragment() {
        mActionBarFragment = new ActionBarFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.actionbar_fragment_container, mActionBarFragment).commit();

        if (isRemoteAnnotations || isAnnotations) {
            mActionBarFragment.enableAnnotations(true);
        }
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
        mParticipantsGrid.removeAllViews();
        mParticipantsList.clear();

        if (mActionBarFragment != null)
            mActionBarFragment.restart();
        if (mTextChatFragment != null) {
            restartTextChatLayout(true);
            mTextChatFragment.restart();
            mTextChatContainer.setVisibility(View.GONE);
        }
        if (mActionBarContainer != null)
            mActionBarContainer.setBackground(null);
        mWebViewContainer.setVisibility(View.GONE);

        mRemoteAnnotationsView = null;
        mCurrentRemote = null;
        if (mScreenAnnotationsView != null) {
            mScreenSharingContainer.setVisibility(View.GONE);
            mScreenAnnotationsView.removeAllViews();
        }
        isCallInProgress = false;
        isReadyToCall = false;
        isRemoteAnnotations = false;
        isScreenSharing = false;
        isAnnotations = false;
        restartOrientation();
    }

    private void showAVCall(boolean show) {
        if (show && mScreenRemoteId == null ) {
            mParticipantsGrid.setVisibility(View.VISIBLE);
            mScreenSharingContainer.setVisibility(View.GONE);
            mScreenSharingContainer.removeAllViews();
        } else {
            mParticipantsGrid.setVisibility(View.GONE);
            mParticipantsGrid.removeAllViews();
        }
        mParticipantsAdapter.notifyDataSetChanged();
    }

    private void screenAnnotations() {
        try {
            if (isScreenSharing) {
                mScreenAnnotationsView = new AnnotationsView(this, mWrapper.getSession(), OpenTokConfig.API_KEY, true);
                //size of annotations screen, by default will be all the screen
                //take into account the calltoolbar as well
                mCallToolbar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                mAnnotationsToolbar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                mAnnotationsToolbar.getChildAt(0).measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); //color toolbar

                Point display = new Point();
                getWindowManager().getDefaultDisplay().getSize(display);
                int height = display.y - mAnnotationsToolbar.getMeasuredHeight() - mCallToolbar.getMeasuredHeight() - mAnnotationsToolbar.getChildAt(0).getMeasuredHeight();
                int width = getWindow().getDecorView().getRootView().getWidth();

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
            mRemoteAnnotationsView = new AnnotationsView(this, mWrapper.getSession(), OpenTokConfig.API_KEY, mWrapper.getRemoteConnId(mScreenRemoteId));
            mRemoteAnnotationsView.setVideoRenderer(mRemoteRenderer);
            mRemoteAnnotationsView.attachToolbar(mAnnotationsToolbar);
            mRemoteAnnotationsView.setAnnotationsListener(this);
            ((ViewGroup) mScreenSharingContainer).addView(mRemoteAnnotationsView);
            mActionBarFragment.enableAnnotations(true);
            mActionBarFragment.showAnnotations(true);
        } catch (Exception e) {
            Log.i(LOG_TAG, "Exception - enableRemoteAnnotations: " + e);
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
            params.addRule(RelativeLayout.ABOVE, R.id.actionbar_fragment_container);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        }
        mTextChatContainer.setLayoutParams(params);
    }

    private void addRemoteScreenSharing(String remoteId, View screenView) {
        if (mWrapper.getRemoteStreamStatus(remoteId).getWidth() > mWrapper.getRemoteStreamStatus(remoteId).getHeight()) {
            forceLandscape();
        }
        showAVCall(false);
        mScreenSharingContainer.removeAllViews();
        mScreenSharingContainer.setVisibility(View.VISIBLE);
        mScreenSharingContainer.addView(screenView);
        remoteAnnotations();
        isRemoteAnnotations = true;
    }

    private void removeRemoteScreenSharing() {
        showAVCall(true);
        isRemoteAnnotations = false;
        showAnnotationsToolbar(false);
        restartOrientation();
    }

    private void addNewParticipant(Participant newParticipant) {
        mParticipantsList.add(newParticipant);
        if (isCallInProgress) {
            updateParticipantList();
        }
    }

    private void removeParticipant(Participant.Type type, String id) {
        for (int i = 0; i < mParticipantsList.size(); i++) {
            Participant participant = mParticipantsList.get(i);
            if (participant.getType().equals(type)) {
                if (type.equals(Participant.Type.REMOTE)) {
                    //remote participant
                    if (participant.getId().equals(id)) {
                        mParticipantsList.remove(i);
                    }
                } else {
                    //local participant
                    mParticipantsList.remove(i);
                }
            }
        }
        //update list
        updateParticipantList();
        Collections.reverse(mParticipantsList);
        mParticipantsAdapter.notifyDataSetChanged();
    }

    private void updateParticipantList() {
        //update list
        for (int i = 0; i < mParticipantsList.size(); i++) {
            Participant participant = mParticipantsList.get(i);
            if (i == 0) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int width = metrics.widthPixels;

                Size newSize = getParticipantSize();
                participant.setContainer(new Size(width, newSize.getHeight()));
            } else {
                participant.setContainer(getParticipantSize());
            }
            mParticipantsList.set(i, participant);
        }

        Collections.reverse(mParticipantsList);
        mParticipantsAdapter.notifyDataSetChanged();
    }

    private void updateParticipant(Participant.Type type, String id, boolean audioOnly) {
        for (int i = 0; i < mParticipantsList.size(); i++) {
            Participant participant = mParticipantsList.get(i);
            if (participant.getType().equals(type)) {
                if (type.equals(Participant.Type.REMOTE)) {
                    if (participant.getId().equals(id)) {
                        participant.getStatus().setHas(MediaType.VIDEO, audioOnly);
                        mParticipantsList.set(i, participant);
                    }
                } else {
                    participant.getStatus().setHas(MediaType.VIDEO, audioOnly);
                    mParticipantsList.set(i, participant);
                }
            }
        }
        mParticipantsAdapter.notifyDataSetChanged();
    }

    private Size getParticipantSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels; // absolute width in pixels
        int height = metrics.heightPixels; // absolute height in pixels

        if (mParticipantsList.size() == 2) {
            return new Size(width, height / 2);
        } else {
            if (mParticipantsList.size() > 2) {
                return new Size(width / 2, height / 2);
            }
        }
        return new Size(width, height);
    }

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

    public void onRemoteVideoChanged(View v) {
       if (mWrapper.getRemoteStreamStatus(mCurrentRemote).subscribedTo(MediaType.VIDEO)) {
           mWrapper.enableReceivedMedia(mCurrentRemote, MediaType.VIDEO, false);
           ((ImageButton)v).setImageResource(R.drawable.no_video_icon);
           updateParticipant(Participant.Type.REMOTE, mCurrentRemote, true);

       }
       else {
           mWrapper.enableReceivedMedia(mCurrentRemote, MediaType.VIDEO, true);
           ((ImageButton)v).setImageResource(R.drawable.video_icon);
           updateParticipant(Participant.Type.REMOTE, mCurrentRemote, false);
       }
    }

    public void onRemoteAudioChanged(View v) {
        if (mWrapper.getRemoteStreamStatus(mCurrentRemote).subscribedTo(MediaType.AUDIO)) {
            mWrapper.enableReceivedMedia(mCurrentRemote, MediaType.AUDIO, false);
            ((ImageButton)v).setImageResource(R.drawable.no_audio_icon);
        }
        else {
            mWrapper.enableReceivedMedia(mCurrentRemote, MediaType.AUDIO, true);
            ((ImageButton)v).setImageResource(R.drawable.audio_icon);
        }
    }

}
