package hb.xvideoplayer;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import mxvideoplayer.app.com.xvideoplayer.R;

import static hb.xvideoplayer.MxUtils.scanForActivity;


public abstract class MxVideoPlayer extends FrameLayout implements MxMediaPlayerListener, View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, View.OnTouchListener, TextureView.SurfaceTextureListener {

    public static final String TAG = "MxVideoPlayer";

    public static final int FULL_SCREEN_NORMAL_DELAY = 500;
    private static final int THRESHOLD = 70;

    public static final int WINDOW_TINY_WIDTH = 430;
    public static final int WINDOW_TINY_HEIGHT = 400;

    public static final int SCREEN_LAYOUT_NORMAL = 0;
    public static final int SCREEN_LAYOUT_LIST = 1;
    public static final int SCREEN_WINDOW_FULLSCREEN = 2;
    public static final int SCREEN_WINDOW_TINY = 3;

    public static final int CURRENT_STATE_NORMAL = 0;
    public static final int CURRENT_STATE_PREPARING = 1;
    public static final int CURRENT_STATE_PLAYING = 2;
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;
    public static final int CURRENT_STATE_PAUSE = 5;
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    public static final int CURRENT_STATE_ERROR = 7;

    protected static boolean WIFI_TIP_DIALOG_SHOWED = false;

    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE; //横屏
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;   //竖屏

    public int mCurrentState = -1;
    public int mCurrentScreen = -1;

    public String mPlayUrl = "";
    public Object[] mObjects = null;
    public boolean mLooping = false;
    public Map<String, String> mDataMap = new HashMap<>();

    protected static WeakReference<MxUserAction> mUserAction;
    protected static Timer mUpdateProgressTimer;
    public static long CLICK_QUIT_FULLSCREEN_TIME = 0;

    protected ProgressTimerTask mProgressTimerTask;

    public ImageView mPlayControllerButton;
    public ImageView mFullscreenButton;
    protected SeekBar mProgressBar;
    private TextView mCurrentTimeTextView;
    private TextView mTotalTimeTextView;
    public ViewGroup mBottomContainer;
    private ViewGroup mTextureViewContainer;
    public ViewGroup mTopContainer;
    protected int mScreenWidth;
    protected int mScreenHeight;
    public AudioManager mAudioManager;
    protected Handler mHandler;
    private boolean mTextureSizeChanged;
    private boolean mIsTryPlayOnError = false;

    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeLight;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected int mDownPosition;
    protected int mGestureDownVolume;
    protected int mGestureDownBrightness;
    protected int mSeekTimePosition;
    protected ViewDragHelper mDragHelper;
    protected boolean mTouchingProgressBar = false;

    public static long mLastAutoFullscreenTime = 0;

    public static AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            releaseAllVideos();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (MxMediaManager.getInstance().getPlayer().isPlaying()) {
                                MxMediaManager.getInstance().getPlayer().pause();
                            }
                            break;
                    }
                }
            };

    private ViewDragHelper.Callback mDragHelperCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return true;
        }
    };

    public MxVideoPlayer(Context context) {
        this(context, null);
    }

    public MxVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        initAttributeSet(context, attrs);
    }

    public void initView(Context context) {
        View.inflate(context, getLayoutId(), this);
        mPlayControllerButton = (ImageView) findViewById(R.id.mx_start);
        mFullscreenButton = (ImageView) findViewById(R.id.mx_fullscreen);
        mProgressBar = (SeekBar) findViewById(R.id.mx_progress);
        mCurrentTimeTextView = (TextView) findViewById(R.id.mx_current_time);
        mTotalTimeTextView = (TextView) findViewById(R.id.mx_total_time);
        mBottomContainer = (ViewGroup) findViewById(R.id.mx_layout_bottom);
        mTextureViewContainer = (ViewGroup) findViewById(R.id.mx_surface_container);
        mTopContainer = (ViewGroup) findViewById(R.id.mx_layout_top);

        mPlayControllerButton.setOnClickListener(this);
        if (mFullscreenButton != null) {
            mFullscreenButton.setOnClickListener(this);
        }
        mProgressBar.setOnSeekBarChangeListener(this);
        mBottomContainer.setOnClickListener(this);
        mTextureViewContainer.setOnClickListener(this);

        mTextureViewContainer.setOnTouchListener(this);
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler();
        mDragHelper = ViewDragHelper.create(this, mDragHelperCallback);
    }

    public boolean startPlay(String url, int screen, Object... objects) {
        if (!TextUtils.isEmpty(mPlayUrl) && TextUtils.equals(mPlayUrl, url)) {
            return false;
        }
        MxVideoPlayerManager.checkAndPutListener(this);
//        if (MxVideoPlayerManager.mCurScrollListener != null
//                && MxVideoPlayerManager.mCurScrollListener.get() != null) {
//            MxVideoPlayer mxVideoPlayer = (MxVideoPlayer) MxVideoPlayerManager.mCurScrollListener.get();
//            if (this == mxVideoPlayer && (mxVideoPlayer.mCurrentState == CURRENT_STATE_PLAYING) &&
//                    url.equals(MxMediaManager.getInstance().mCurrentUrl)) {
//                mxVideoPlayer.startWindowTiny();
//            }
//        }
        mPlayUrl = url;
        mCurrentScreen = screen;
        mObjects = objects;
        setUiPlayState(CURRENT_STATE_NORMAL);
        if (url.equals(MxMediaManager.getInstance().mCurrentUrl)) {
            MxVideoPlayerManager.putScrollListener(this);
        }
        return true;
    }

    public boolean startPlay(String url, int screen, Map<String, String> dataMap, Object... objects) {
        if (startPlay(url, screen, objects)) {
            mDataMap.clear();
            mDataMap.putAll(dataMap);
            return true;
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mCurrentScreen == SCREEN_WINDOW_FULLSCREEN || mCurrentScreen == SCREEN_WINDOW_TINY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int specWidth = MeasureSpec.getSize(widthMeasureSpec);
        int specHeight = (specWidth * 9) / 16;
        setMeasuredDimension(specWidth, specHeight);

        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, MeasureSpec.EXACTLY);
        getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.mx_start) {
            Log.i(TAG, "onClick: click start button and currentState=" + mCurrentState);
            if (TextUtils.isEmpty(mPlayUrl)) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (mCurrentState == CURRENT_STATE_NORMAL || mCurrentState == CURRENT_STATE_ERROR) {
                if (isShowNetworkStateDialog()) {
                    return;
                }
                preparePlayVideo();
                onActionEvent(mCurrentState != CURRENT_STATE_ERROR ? MxUserAction.ON_CLICK_START_ICON
                        : MxUserAction.ON_CLICK_START_ERROR);
            } else if (mCurrentState == CURRENT_STATE_PLAYING) {
                onActionEvent(MxUserAction.ON_CLICK_PAUSE);
                MxMediaManager.getInstance().getPlayer().pause();
                setUiPlayState(CURRENT_STATE_PAUSE);
            } else if (mCurrentState == CURRENT_STATE_PAUSE) {
                onActionEvent(MxUserAction.ON_CLICK_RESUME);
                MxMediaManager.getInstance().getPlayer().start();
                setUiPlayState(CURRENT_STATE_PLAYING);
            } else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
                onActionEvent(MxUserAction.ON_CLICK_START_AUTO_COMPLETE);
                preparePlayVideo();
            }
        } else if (id == R.id.mx_fullscreen) {
            Log.i(TAG, "onClick: click fullscreen button and currentState=" + mCurrentState);
            if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
                return;
            }
            if (mCurrentScreen == SCREEN_WINDOW_FULLSCREEN) {
                //quit fullscreen
                backPress();
            } else {
                onActionEvent(MxUserAction.ON_ENTER_FULLSCREEN);
                startWindowFullscreen();
            }
        } else if (id == R.id.mx_surface_container && mCurrentState == CURRENT_STATE_ERROR) {
            Log.i(TAG, "onClick: click surfaceContainer and currentState=" + mCurrentState);
            preparePlayVideo();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.mx_surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i(TAG, "onTouch: surfaceContainer actionDown [" + this.hashCode() + "] ");
                    mTouchingProgressBar = true;
                    mDownX = x;
                    mDownY = y;
                    mChangeLight = false;
                    mChangeVolume = false;
                    mChangePosition = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i(TAG, "onTouch: surfaceContainer actionMove [" + this.hashCode() + "] ");
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    if (mCurrentScreen == SCREEN_WINDOW_FULLSCREEN) {
                        if (!mChangePosition && !mChangeVolume && !mChangeLight) {
                            if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                                cancelProgressTimer();
                                if (absDeltaX >= THRESHOLD) { // adjust progress
                                    if (mCurrentState != CURRENT_STATE_ERROR) {
                                        mChangePosition = true;
                                        mDownPosition = getCurrentPositionWhenPlaying();
                                    }
                                } else {
                                    if (x <= MxMediaManager.mTextureView.getWidth() / 2) {  // adjust the volume
                                        mChangeVolume = true;
                                        mGestureDownVolume = mAudioManager.
                                                getStreamVolume(AudioManager.STREAM_MUSIC);
                                    } else {  // adjust the light
                                        mChangeLight = true;
                                        mGestureDownBrightness = MxUtils.getScreenBrightness((Activity) getContext());
                                    }
                                }
                            }
                        }
                    }
                    if (mChangePosition) {
                        int totalTimeDuration = getDuration();
                        mSeekTimePosition = (int) (mDownPosition + deltaX * 100);
                        if (mSeekTimePosition > totalTimeDuration) {
                            mSeekTimePosition = totalTimeDuration;
                        }
                        String seekTime = MxUtils.stringForTime(mSeekTimePosition);
                        String totalTime = MxUtils.stringForTime(totalTimeDuration);
                        showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
                    }
                    if (mChangeVolume) {
                        deltaY = -deltaY;  // up is -, down is +
                        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int deltaV = (int) (maxVolume * deltaY * 3 / mScreenHeight);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
                        int volumePercent = (int) (mGestureDownVolume * 100 / maxVolume + deltaY * 3 * 100 / mScreenHeight);
                        showVolumeDialog(-deltaY, volumePercent);
                    }
                    if (mChangeLight) {
                        deltaY = -deltaY;  // up is -, down is +
                        int deltaV = (int) (255 * deltaY * 3 / mScreenHeight);
                        MxUtils.setScreenManualMode(getContext());
                        int brightnessValue = mGestureDownBrightness + deltaV;
                        if (brightnessValue >= 0 && brightnessValue <= 255) {
                            MxUtils.setWindowBrightness((Activity) getContext(), brightnessValue);
                        }
                        int brightnessPercent = (int) (mGestureDownBrightness + deltaY * 255  * 3 / mScreenHeight);
                        showBrightnessDialog(-deltaY, brightnessPercent);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "onTouch: surfaceContainer actionUp [" + this.hashCode() + "] ");
                    mTouchingProgressBar = false;
                    dismissProgressDialog();
                    dismissVolumeDialog();
                    dismissBrightnessDialog();
                    if (mChangePosition) {
                        onActionEvent(MxUserAction.ON_TOUCH_SCREEN_SEEK_POSITION);
                        MxMediaManager.getInstance().getPlayer().seekTo(mSeekTimePosition);
                        int duration = getDuration();
                        int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
                        mProgressBar.setProgress(progress);
                    }
                    if (mChangeVolume) {
                        onActionEvent(MxUserAction.ON_TOUCH_SCREEN_SEEK_VOLUME);
                    }
                    if (mChangeLight) {
                        onActionEvent(MxUserAction.ON_TOUCH_SCREEN_SEEK_BRIGHTNESS);
                    }
                    startProgressTimer();
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    public void quitWindowTiny() {
        Log.i(TAG, "quitWindowTiny: [" + this.hashCode() + "]");
        if (mPlayUrl.equals(MxMediaManager.getInstance().mCurrentUrl)) {
            MxMediaPlayerListener currentListener = MxVideoPlayerManager.getCurrentListener();
            if (currentListener == null) {
                return;
            }
            if (currentListener.getScreenType() == SCREEN_WINDOW_TINY) {
                backPress();
            }
        }
    }

    public void startWindowTiny() {
        Log.i(TAG, "startWindowTiny: [" + this.hashCode() + "] ");
        if (!mPlayUrl.equals(MxMediaManager.getInstance().mCurrentUrl) ||
                mCurrentState == CURRENT_STATE_NORMAL) {
            Log.e(TAG, "startWindowTiny: url is not equal or video not start");
            return;
        }

        onActionEvent(MxUserAction.ON_ENTER_TINY_SCREEN);
        ViewGroup vp = (ViewGroup) scanForActivity(getContext())
                .findViewById(Window.ID_ANDROID_CONTENT);
        View oldView = vp.findViewById(R.id.mx_tiny_screen_id);
        if (oldView != null) {
            vp.removeView(oldView);
        }
        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        try {
            Constructor<MxVideoPlayer> constructor = (Constructor<MxVideoPlayer>) MxVideoPlayer.this.getClass().getConstructor(Context.class);
            MxVideoPlayer mxVideoPlayer = constructor.newInstance(getContext());
            mxVideoPlayer.setId(R.id.mx_tiny_screen_id);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(WINDOW_TINY_WIDTH, WINDOW_TINY_HEIGHT);
            params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
            vp.addView(mxVideoPlayer, params);
            mxVideoPlayer.startPlay(mPlayUrl, SCREEN_WINDOW_TINY, mObjects);
            mxVideoPlayer.addTextureView();
            MxVideoPlayerManager.putListener(mxVideoPlayer);
            mxVideoPlayer.setUiPlayState(mCurrentState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startWindowFullscreen() {
        CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
        hideSupportActionBar(getContext());
        MxUtils.getAppComptActivity(getContext()).setRequestedOrientation(FULLSCREEN_ORIENTATION);

        ViewGroup vp = (ViewGroup) (scanForActivity(getContext()))
                .findViewById(Window.ID_ANDROID_CONTENT);
        View oldView = vp.findViewById(R.id.mx_fullscreen_id);
        if (oldView != null) {
            vp.removeView(oldView);
        }
        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        try {
            Constructor<MxVideoPlayer> constructor = (Constructor<MxVideoPlayer>) MxVideoPlayer.this.getClass().getConstructor(Context.class);
            MxVideoPlayer mxVideoPlayer = constructor.newInstance(getContext());
            mxVideoPlayer.setId(R.id.mx_fullscreen_id);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
            vp.addView(mxVideoPlayer, params);
            mxVideoPlayer.startPlay(mPlayUrl, SCREEN_WINDOW_FULLSCREEN, mObjects);
            mxVideoPlayer.setUiPlayState(mCurrentState);
            mxVideoPlayer.addTextureView();
            MxVideoPlayerManager.putListener(mxVideoPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startFullscreen(Context context, Class _class, String url, Object... objects) {
        Log.i(TAG, "startFullscreen: ===manual fullscreen===");
        hideSupportActionBar(context);
        MxUtils.getAppComptActivity(context).setRequestedOrientation(FULLSCREEN_ORIENTATION);
        ViewGroup vp = (ViewGroup) (scanForActivity(context))
                .findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(R.id.mx_fullscreen_id);
        if (old != null) {
            vp.removeView(old);
        }
        try {
            Constructor<MxVideoPlayer> constructor = _class.getConstructor(Context.class);
            MxVideoPlayer mxVideoPlayer = constructor.newInstance(context);
            mxVideoPlayer.setId(R.id.mx_fullscreen_id);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            vp.addView(mxVideoPlayer, lp);

            mxVideoPlayer.startPlay(url, SCREEN_WINDOW_FULLSCREEN, objects);
            mxVideoPlayer.addTextureView();

            mxVideoPlayer.mPlayControllerButton.performClick();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean backPress() {
        MxMediaPlayerListener listener = MxVideoPlayerManager.getCurrentListener();
        return (listener != null && listener.quitFullscreenOrTinyListener());
    }

    public void setUiPlayState(int state) {
        mCurrentState = state;
        switch (mCurrentState) {
            case CURRENT_STATE_NORMAL:
                if (isCurrentMediaListener()) {
                    cancelProgressTimer();
                    MxMediaManager.getInstance().releaseMediaPlayer();
                }
                break;
            case CURRENT_STATE_PREPARING:
                resetProgressAndTime();
                break;
            case CURRENT_STATE_PLAYING:
            case CURRENT_STATE_PAUSE:
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                startProgressTimer();
                break;
            case CURRENT_STATE_ERROR:
                cancelProgressTimer();
                if (isCurrentMediaListener()) {
                    MxMediaManager.getInstance().releaseMediaPlayer();
                }
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                cancelProgressTimer();
                mProgressBar.setProgress(100);
                mCurrentTimeTextView.setText(mTotalTimeTextView.getText());
                break;
            default:
                break;
        }

    }

    private static void hideSupportActionBar(Context context) {
        ActionBar actionBar = MxUtils.getAppComptActivity(context).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setShowHideAnimationEnabled(false);
            actionBar.hide();
        }
        MxUtils.getAppComptActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        MxUtils.hideSystemUI(context);
    }

    private static void showSupportActionBar(Context context) {
        ActionBar actionBar = MxUtils.getAppComptActivity(context).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setShowHideAnimationEnabled(false);
            actionBar.show();
        }
        MxUtils.getAppComptActivity(context).getWindow().
                    clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        MxUtils.showSystemUI(context);
    }

    public void resetProgressAndTime() {
        mProgressBar.setProgress(0);
        mProgressBar.setSecondaryProgress(0);
        mCurrentTimeTextView.setText(MxUtils.stringForTime(0));
        mTotalTimeTextView.setText(MxUtils.stringForTime(0));
    }

    protected void startProgressTimer() {
        cancelProgressTimer();
        mUpdateProgressTimer = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        mUpdateProgressTimer.schedule(mProgressTimerTask, 0, 500);
    }

    protected void cancelProgressTimer() {
        if (mUpdateProgressTimer != null) {
            mUpdateProgressTimer.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    public static void setMxUserAction(MxUserAction userAction) {
        mUserAction = new WeakReference<>(userAction);
    }

    /**
     * collection user action
     *
     * @param type action type
     */
    public void onActionEvent(int type) {
        if (mUserAction != null && mUserAction.get() != null && isCurrentMediaListener()) {
            mUserAction.get().onActionEvent(type, mPlayUrl, mCurrentScreen, mObjects);
        }
    }

    /**
     * this method for different VideoPlayer with same url
     * when fullscreen or tiny screen
     *
     * @return true or false
     */
    private boolean isCurrentMediaListener() {
        return (MxVideoPlayerManager.getCurrentListener() != null
                && MxVideoPlayerManager.getCurrentListener() == this);
    }

    protected int getCurrentPositionWhenPlaying() {
        int pos = 0;
        if (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE
                || mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            try {
                pos = (int) MxMediaManager.getInstance().getPlayer().getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return pos;
            }
        }
        return pos;
    }

    public int getDuration() {
        int duration = 0;
        try {
            if (mCurrentState >= CURRENT_STATE_PLAYING && mCurrentState < CURRENT_STATE_ERROR) {
                duration = MxMediaManager.getInstance().getPlayer().getDuration();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    private void setTextAndProgress(int secProgress) {
        int position = getCurrentPositionWhenPlaying();
        int duration = getDuration();
        int progress = position * 100 / (duration == 0 ? 1 : duration);
        setProgressAndTime(progress, secProgress, position, duration);
    }

    public void setProgressAndTime(int progress, int secProgress, int currentTime, int totalTime) {
        if (!mTouchingProgressBar) {
            if (progress != 0) {
                mProgressBar.setProgress(progress);
            }
        }
        if (secProgress > 95) {
            secProgress = 100;
        }
        if (secProgress != 0) {
            mProgressBar.setSecondaryProgress(secProgress);
        }
        if (currentTime != 0) {
            mCurrentTimeTextView.setText(MxUtils.stringForTime(currentTime));
        }
        mTotalTimeTextView.setText(MxUtils.stringForTime(totalTime));
    }

    protected void preparePlayVideo() {
        Log.i(TAG, "prepare play video [" + this.hashCode() + "] ");
        mIsTryPlayOnError = mCurrentState == CURRENT_STATE_ERROR;
        MxVideoPlayerManager.completeAll();
        MxVideoPlayerManager.putListener(this);
        addTextureView();

        mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        // 禁止系统休眠
        scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        MxVideoPlayerManager.putScrollListener(this);
        MxMediaManager.getInstance().prepare(mPlayUrl, mDataMap, mLooping);
        setUiPlayState(CURRENT_STATE_PREPARING);
    }

    private void addTextureView() {
        Log.i(TAG, "addTextureView [" + this.hashCode() + "]");
        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }
        if (MxMediaManager.mTextureView == null) {
            MxMediaManager.mTextureView = new MxTextureView(getContext());
            Point videoSize = MxMediaManager.getInstance().getVideoSize();
            MxMediaManager.mTextureView.setVideoSize(videoSize);
        } else if (MxMediaManager.mTextureView.getParent() != null) {
            ((ViewGroup) MxMediaManager.mTextureView.getParent()).removeView(MxMediaManager.mTextureView);
        }

        MxMediaManager.mTextureView.setSurfaceTextureListener(this);


        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        mTextureViewContainer.addView(MxMediaManager.mTextureView, params);

    }

    /**
     * manual call release, only not fullscreen mode can call this
     */
    public void release() {
        if (mPlayUrl.equals(MxMediaManager.getInstance().mCurrentUrl) &&
                (System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            MxMediaPlayerListener currentListener = MxVideoPlayerManager.getCurrentListener();
            if (currentListener != null &&
                    currentListener.getScreenType() != SCREEN_WINDOW_FULLSCREEN) {
                Log.d(TAG, "manual release [" + this.hashCode() + "]");
                releaseAllVideos();
            }
        }
    }

    public static void releaseAllVideos() {
        Log.i(TAG, "releaseAllVideos===============");
        MxVideoPlayerManager.completeAll();
        MxMediaManager.getInstance().releaseMediaPlayer();
    }

    private void clearFullscreenLayout() {
        ViewGroup vp = (ViewGroup) (scanForActivity(getContext()))
                .findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(R.id.mx_fullscreen_id);
        View oldT = vp.findViewById(R.id.mx_tiny_screen_id);
        if (oldF != null) {
            vp.removeView(oldF);
        }
        if (oldT != null) {
            vp.removeView(oldT);
        }
        showSupportActionBar(getContext());
    }

    @Override
    public void onPrepared() {
        Log.i(TAG, "onPrepared====[" + this.hashCode() + "] ");
        if (mCurrentState != CURRENT_STATE_PREPARING) {
            return;
        }
        MxMediaManager.getInstance().getPlayer().start();
        setUiPlayState(CURRENT_STATE_PLAYING);
    }

    @Override
    public void onCompletion() {
        Log.i(TAG, "onCompletion====[" + this.hashCode() + "] ");
        setUiPlayState(CURRENT_STATE_NORMAL);
        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        MxMediaManager.getInstance().mCurVideoWidth = 0;
        MxMediaManager.getInstance().mCurVideoHeight = 0;
        // clean cache variable
        MxMediaManager.getInstance().bufferPercent = 0;

        mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if ((!mIsTryPlayOnError && mCurrentScreen == SCREEN_WINDOW_FULLSCREEN) ||
                mCurrentScreen == SCREEN_WINDOW_TINY) {
            clearFullscreenLayout();
        }
        MxUtils.getAppComptActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);
    }

    @Override
    public void onBufferingUpdate(int percent) {
        if (mCurrentState != CURRENT_STATE_NORMAL && mCurrentState != CURRENT_STATE_ERROR) {
            MxMediaManager.getInstance().bufferPercent = percent;
            setTextAndProgress(percent);
        }
    }

    @Override
    public void onSeekComplete() {
    }

    @Override
    public void onError(int what, int extra) {
        Log.e(TAG, "onError what : " + what + " extra : " + extra + " [" + this.hashCode() + "] ");
        if (what != -38 && what != 38) {
            setUiPlayState(CURRENT_STATE_ERROR);
        }
    }

    @Override
    public void onInfo(int what, int extra) {
        Log.i(TAG, "onInfo what : " + what + " extra : " + extra);
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            MxMediaManager.getInstance().mBackUpBufferState = mCurrentState;
            setUiPlayState(CURRENT_STATE_PLAYING_BUFFERING_START);
            Log.i(TAG, "MEDIA_INFO_BUFFERING_START");
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            if (MxMediaManager.getInstance().mBackUpBufferState != -1) {
                setUiPlayState(MxMediaManager.getInstance().mBackUpBufferState);
                MxMediaManager.getInstance().mBackUpBufferState = -1;
            }
            Log.i(TAG, "MEDIA_INFO_BUFFERING_END");
        }
    }

    @Override
    public void onVideoSizeChanged() {
        Log.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        Point videoSize = MxMediaManager.getInstance().getVideoSize();
        MxMediaManager.mTextureView.setVideoSize(videoSize);
    }

    @Override
    public void goBackNormalListener() {
        Log.i(TAG, "goBackNormalListener: [" + this.hashCode() + "] ");
        mCurrentState = MxMediaManager.getInstance().mLastState;
        setUiPlayState(mCurrentState);
        addTextureView();

        showSupportActionBar(getContext());
    }

    @Override
    public boolean quitFullscreenOrTinyListener() {
        Log.i(TAG, "quitFullscreenOrTinyListener: [" + this.hashCode() + "] ");
        mIsTryPlayOnError = false;
        MxUtils.getAppComptActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);
        if (mCurrentScreen == SCREEN_WINDOW_FULLSCREEN
                || mCurrentScreen == SCREEN_WINDOW_TINY) {
            onActionEvent(mCurrentScreen == SCREEN_WINDOW_FULLSCREEN ?
                    MxUserAction.ON_QUIT_FULLSCREEN : MxUserAction.ON_QUIT_TINY_SCREEN);
            if (MxVideoPlayerManager.mListenerList.size() == 1) {
                MxMediaPlayerListener popListener = MxVideoPlayerManager.popListener();
                if (popListener != null) {
                    popListener.onCompletion();
                }
                MxMediaManager.getInstance().releaseMediaPlayer();
                showSupportActionBar(getContext());
                return true;
            }
            ViewGroup vp = (ViewGroup) scanForActivity(getContext())
                    .findViewById(Window.ID_ANDROID_CONTENT);
            vp.removeView(this);
            MxMediaManager.getInstance().mLastState = mCurrentState;
            MxVideoPlayerManager.popListener();
            MxMediaPlayerListener currentListener = MxVideoPlayerManager.getCurrentListener();
            if (currentListener != null) {
                currentListener.goBackNormalListener();
                CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            } else {
                MxVideoPlayerManager.completeAll();
            }
            return true;
        }
        return false;
    }

    @Override
    public int getScreenType() {
        return mCurrentScreen;
    }

    @Override
    public String getUrl() {
        return mPlayUrl;
    }

    @Override
    public int getState() {
        return mCurrentState;
    }

    @Override
    public void onAutoCompletion() {
        Log.i(TAG, "onAutoCompletion " + " [" + this.hashCode() + "] " + "===current listener size=" +
                MxVideoPlayerManager.mListenerList.size());
        mIsTryPlayOnError = false;
        Runtime.getRuntime().gc();  // avoid memory increment when recycler play
        onActionEvent(MxUserAction.ON_AUTO_COMPLETE);
        dismissVolumeDialog();
        dismissProgressDialog();
        setUiPlayState(CURRENT_STATE_AUTO_COMPLETE);
        MxVideoPlayerManager.completeAll();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void autoFullscreen(float x) {
        Log.i(TAG, "autoFullscreen: [" + this.hashCode() + "] ");
        if (isCurrentMediaListener()
                && mCurrentState == CURRENT_STATE_PLAYING
                && mCurrentScreen != SCREEN_WINDOW_FULLSCREEN
                && mCurrentScreen != SCREEN_WINDOW_TINY) {
            int orientation = MxUtils.getAppComptActivity(getContext()).getRequestedOrientation();
            boolean isChangeOrientation = x > 0 ? orientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                    orientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            if (isChangeOrientation) {
                if (x > 0) {
                    MxUtils.getAppComptActivity(getContext()).setRequestedOrientation(
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    MxUtils.getAppComptActivity(getContext()).setRequestedOrientation(
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
                startWindowFullscreen();
            }
        }
    }

    @Override
    public void autoQuitFullscreen() {
        Log.i(TAG, "autoQuitFullscreen: [" + this.hashCode() + "] ");
        if ((System.currentTimeMillis() - mLastAutoFullscreenTime) > 2000
                && isCurrentMediaListener()
                && mCurrentState == CURRENT_STATE_PLAYING
                && mCurrentScreen == SCREEN_WINDOW_FULLSCREEN) {
            mLastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable [" + this.hashCode() + "] ");
        if (MxMediaManager.mSurface == null) {
            MxMediaManager.mSurface = surface;
        } else if (MxMediaManager.mTextureView != null) {
            MxMediaManager.mTextureView.setSurfaceTexture(MxMediaManager.mSurface);
        }
        MxMediaManager.getInstance().setDisPlay(new Surface(MxMediaManager.mSurface));
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureSizeChanged [" + this.hashCode() + "] ");
        mTextureSizeChanged = true;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.i(TAG, "onSurfaceTextureDestroyed: [" + this.hashCode() + "]");
        MxMediaManager.mSurface = null;
        surface.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (!mTextureSizeChanged) {
            MxMediaManager.mTextureView.setHasUpdated();
        } else {
            mTextureSizeChanged = false;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "onStartTrackingTouch: bottomProgress [" + this.hashCode() + "] ");
        cancelProgressTimer();
        ViewParent parent = getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
            parent = parent.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "onStopTrackingTouch: bottomProgress [" + this.hashCode() + "] ");
        onActionEvent(MxUserAction.ON_SEEK_POSITION);
        startProgressTimer();
        ViewParent viewParent = getParent();
        while (viewParent != null) {
            viewParent.requestDisallowInterceptTouchEvent(false);
            viewParent = viewParent.getParent();
        }
        if (mCurrentState != CURRENT_STATE_PLAYING
                && mCurrentState != CURRENT_STATE_PAUSE) {
            return;
        }
        int time = seekBar.getProgress() * getDuration() / 100;
        MxMediaManager.getInstance().getPlayer().seekTo(time);
        Log.i(TAG, "onStopTrackingTouch: seekTo " + time + " [" + this.hashCode() + "] ");
    }

    public class ProgressTimerTask extends TimerTask {

        @Override
        public void run() {
            if (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE
                    || mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setTextAndProgress(MxMediaManager.getInstance().bufferPercent);
                    }
                });
            }
        }
    }

    public static class MxAutoFullscreenListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) { // 可以得到传感器实时测量出来的变化值
            final float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            // 过滤掉用力过猛会有一个反向的大数值
            if (x > 7.5 || x < -7.5) {
                if ((System.currentTimeMillis() - mLastAutoFullscreenTime) > 2000) {
                    MxMediaPlayerListener currentListener = MxVideoPlayerManager.getCurrentListener();
                    if (currentListener != null) {
                        currentListener.autoFullscreen(x);
                    }
                    mLastAutoFullscreenTime = System.currentTimeMillis();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    protected abstract void showBrightnessDialog(float v, int brightnessPercent);

    protected abstract int getLayoutId();

    protected abstract void initAttributeSet(Context context, AttributeSet attrs);

    protected abstract boolean isShowNetworkStateDialog();

    protected abstract void showProgressDialog(float deltaX, String seekTime,
                                               int seekTimePosition, String totalTime, int totalTimeDuration);

    protected abstract void showVolumeDialog(float v, int volumePercent);

    protected abstract void dismissVolumeDialog();

    protected abstract void dismissBrightnessDialog();

    protected abstract void dismissProgressDialog();
}
