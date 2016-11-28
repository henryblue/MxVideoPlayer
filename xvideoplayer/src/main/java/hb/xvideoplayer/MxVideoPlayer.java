package hb.xvideoplayer;


import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Handler;
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
import tv.danmaku.ijk.media.player.IMediaPlayer;

import static hb.xvideoplayer.MxVideoPlayerManager.getFirst;


public abstract class MxVideoPlayer extends FrameLayout implements MxMediaPlayerListener, View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, View.OnTouchListener, TextureView.SurfaceTextureListener {

    public static final String TAG = "MxVideoPlayer";

    public static final int FULLSCREEN_ID = 446981;
    public static final int TINY_ID = 339961;
    public static final int FULL_SCREEN_NORMAL_DELAY = 500;

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

    private static final boolean WIFI_TIP_DIALOG_SHOWED = false;
    public static boolean ACTION_BAR_EXIST = true;
    public static boolean TOOL_BAR_EXIST = true;
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR; //由物理感应器决定显示方向
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;  //竖屏

    public int mCurrentState = -1;
    public int mCurrentScreen = -1;

    public String mPlayUrl;
    public Object[] mObjects = null;
    public boolean mLooping = false;
    public Map<String, String> mDataMap = new HashMap<>();

    protected static WeakReference<MxUserAction> mUserAction;
    protected static Timer mUpdateProgressTimer;
    public static long CLICK_QUIT_FULLSCREEN_TIME = 0;

    protected ProgressTimerTask mProgressTimerTask;

    public ImageView mStartButton;
    public ImageView mFullscreenButton;
    private SeekBar mProgressBar;
    private TextView mCurrentTimeTextView;
    private TextView mTotalTimeTextView;
    public ViewGroup mBottomContainer;
    private ViewGroup mTextureViewContainer;
    public ViewGroup mTopContainer;
    private MxImageView mCacheImageView;
    private Bitmap mPauseSwitchCoverBitmap = null;
    private int mScreenWidth;
    private int mScreenHeight;
    private AudioManager mAudioManager;
    private Handler mHandler;
    private Surface mSurface;
    private boolean mTextureSizeChanged;

    protected boolean mTouchingProgressBar = false;

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


    public MxVideoPlayer(Context context) {
        this(context, null);
    }

    public MxVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public void initView(Context context) {
        View.inflate(context, getLayoutId(), this);
        mStartButton = (ImageView) findViewById(R.id.mx_start);
        mFullscreenButton = (ImageView) findViewById(R.id.mx_fullscreen);
        mProgressBar = (SeekBar) findViewById(R.id.mx_progress);
        mCurrentTimeTextView = (TextView) findViewById(R.id.mx_current_time);
        mTotalTimeTextView = (TextView) findViewById(R.id.mx_total_time);
        mBottomContainer = (ViewGroup) findViewById(R.id.mx_layout_bottom);
        mTextureViewContainer = (ViewGroup) findViewById(R.id.mx_surface_container);
        mTopContainer = (ViewGroup) findViewById(R.id.mx_layout_top);
        mCacheImageView = (MxImageView) findViewById(R.id.mx_cache);

        mStartButton.setOnClickListener(this);
        mFullscreenButton.setOnClickListener(this);
        mProgressBar.setOnSeekBarChangeListener(this);
        mBottomContainer.setOnClickListener(this);
        mTextureViewContainer.setOnClickListener(this);

        mTextureViewContainer.setOnTouchListener(this);
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler();
    }

    public boolean setUp(String url, int screen, Object... objects) {
        if (!TextUtils.isEmpty(mPlayUrl) && TextUtils.equals(mPlayUrl, url)) {
            return false;
        }
        MxVideoPlayerManager.checkAndPutListener(this);
        if (MxVideoPlayerManager.mCurScrollListener != null
                && MxVideoPlayerManager.mCurScrollListener.get() != null) {
            MxVideoPlayer mxVideoPlayer = (MxVideoPlayer) MxVideoPlayerManager.mCurScrollListener.get();
            if (this == mxVideoPlayer && (mxVideoPlayer.mCurrentState == CURRENT_STATE_PLAYING) &&
                    url.equals(MxMediaManager.getInstance().getPlayer().getDataSource())) {
                mxVideoPlayer.startWindowTiny();
            }
        }
        mPlayUrl = url;
        mCurrentScreen = screen;
        mObjects = objects;
        setUiStateAndScreen(CURRENT_STATE_NORMAL);
        if (url.equals(MxMediaManager.getInstance().getPlayer().getDataSource())) {
            MxVideoPlayerManager.putScrollListener(this);
        }
        Log.d(TAG, "setUp: url===" + mPlayUrl);
        return true;
    }

    public boolean setUp(String url, int screen, Map<String, String> dataMap, Object... objects) {
        if (setUp(url, screen, objects)) {
            mDataMap.clear();
            mDataMap.putAll(dataMap);
            return true;
        }
        return false;
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
                if (!mPlayUrl.startsWith("file") && !MxUtils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog();
                    return;
                }
                preparePlayVideo();
                onActionEvent(mCurrentState != CURRENT_STATE_ERROR ? MxUserAction.ON_CLICK_START_ICON
                        : MxUserAction.ON_CLICK_START_ERROR);
            } else if (mCurrentState == CURRENT_STATE_PLAYING) {
                obtainCache();
                onActionEvent(MxUserAction.ON_CLICK_PAUSE);
                MxMediaManager.getInstance().getPlayer().pause();
                setUiStateAndScreen(CURRENT_STATE_PAUSE);
                refreshCache();
            } else if (mCurrentState == CURRENT_STATE_PAUSE) {
                onActionEvent(MxUserAction.ON_CLICK_RESUME);
                MxMediaManager.getInstance().getPlayer().start();
                setUiStateAndScreen(CURRENT_STATE_PLAYING);
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
            preparePlayVideo();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }


    private void startWindowTiny() {
    }


    private void startWindowFullscreen() {
        obtainCache();
        CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
        hideSupportActionBar(getContext());
        MxUtils.getAppComptActivity(getContext()).setRequestedOrientation(FULLSCREEN_ORIENTATION);

        ViewGroup vp = (ViewGroup)(MxUtils.scanForActivity(getContext()))
                .findViewById(Window.ID_ANDROID_CONTENT);
        View oldView = vp.findViewById(FULLSCREEN_ID);
        if (oldView != null) {
            vp.removeView(oldView);
        }
        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        try {
            Constructor<MxVideoPlayer> constructor = (Constructor<MxVideoPlayer>) MxVideoPlayer.this.getClass().getConstructor(Context.class);
            MxVideoPlayer mxVideoPlayer = constructor.newInstance(getContext());
            mxVideoPlayer.setId(FULLSCREEN_ID);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(mxVideoPlayer, params);
            mxVideoPlayer.setUp(mPlayUrl, SCREEN_WINDOW_FULLSCREEN, mObjects);
            mxVideoPlayer.setUiStateAndScreen(mCurrentState);
            mxVideoPlayer.addTextureView();
            MxVideoPlayerManager.putListener(mxVideoPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        refreshCache();
    }

    public static void startFullscreen(Context context, Class _class, String url, Object... objects) {

        hideSupportActionBar(context);
        MxUtils.getAppComptActivity(context).setRequestedOrientation(FULLSCREEN_ORIENTATION);
        ViewGroup vp = (ViewGroup) (MxUtils.scanForActivity(context))
                .findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(FULLSCREEN_ID);
        if (old != null) {
            vp.removeView(old);
        }
        try {
            Constructor<MxVideoPlayer> constructor = _class.getConstructor(Context.class);
            MxVideoPlayer jcVideoPlayer = constructor.newInstance(context);
            jcVideoPlayer.setId(FULLSCREEN_ID);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            vp.addView(jcVideoPlayer, lp);

            jcVideoPlayer.setUp(url, SCREEN_WINDOW_FULLSCREEN, objects);
            jcVideoPlayer.addTextureView();

            jcVideoPlayer.mStartButton.performClick();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean backPress() {
        MxMediaPlayerListener listener = getFirst();
        return (listener != null && listener.backToOtherListener());
    }

    public void setUiStateAndScreen(int state) {
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

    private void obtainCache() {
        Point videoSize = MxMediaManager.getInstance().getVideoSize();
        if (videoSize != null) {
            Bitmap bitmap = MxMediaManager.mTextureView.getBitmap(videoSize.x, videoSize.y);
            if (bitmap != null) {
                mPauseSwitchCoverBitmap = bitmap;
            }
        }
    }

    private void refreshCache() {
        if (mPauseSwitchCoverBitmap != null) {
            MxVideoPlayer mxVideoPlayer = (MxVideoPlayer) getFirst();
            if (mxVideoPlayer != null) {
                mxVideoPlayer.mCacheImageView.setImageBitmap(mPauseSwitchCoverBitmap);
                mxVideoPlayer.mCacheImageView.setVisibility(View.VISIBLE);
            }
        }
    }

    public void clearCacheImage() {
        mPauseSwitchCoverBitmap = null;
        mCacheImageView.setImageBitmap(null);
    }

    private static void hideSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar actionBar = MxUtils.getAppComptActivity(context).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setShowHideAnimationEnabled(false);
                actionBar.hide();
            }
        }
        if (TOOL_BAR_EXIST) {
            MxUtils.getAppComptActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private static void showSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar actionBar = MxUtils.getAppComptActivity(context).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setShowHideAnimationEnabled(false);
                actionBar.show();
            }
        }
        if (TOOL_BAR_EXIST) {
            MxUtils.getAppComptActivity(context).getWindow().
                    clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public void resetProgressAndTime() {
        mProgressBar.setProgress(0);
        mProgressBar.setSecondaryProgress(0);
        mCurrentTimeTextView.setText(MxUtils.stringForTime(0));
        mTotalTimeTextView.setText(MxUtils.stringForTime(0));
    }

    private void startProgressTimer() {
        cancelProgressTimer();
        mUpdateProgressTimer = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        mUpdateProgressTimer.schedule(mProgressTimerTask, 0, 300);
    }

    private void cancelProgressTimer() {
        if (mUpdateProgressTimer != null) {
            mUpdateProgressTimer.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    /**
     * collection user action
     * @param type action type
     */
    public void onActionEvent(int type) {
        if (mUserAction != null && mUserAction.get() != null && isCurrentMediaListener()) {
            mUserAction.get().onActionEvent(type, mPlayUrl, mCurrentScreen, mObjects);
        }
    }

    private boolean isCurrentMediaListener() {
        return (getFirst() != null
                && getFirst() == this);
    }

    private int getCurrentPositionWhenPlaying() {
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

    private int getDuration() {
        int duration = 0;
        try {
            duration = (int) MxMediaManager.getInstance().getPlayer().getDuration();
        }catch (IllegalStateException e) {
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

    private void preparePlayVideo() {
        Log.i(TAG, "prepare play video [" + this.hashCode() + "] ");
        MxVideoPlayerManager.completeAll();
        MxVideoPlayerManager.putListener(this);
        addTextureView();

        AudioManager audioManager = (AudioManager) getContext().
                getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        MxMediaManager.getInstance().prepare(mPlayUrl, mDataMap, mLooping);
        setUiStateAndScreen(CURRENT_STATE_PREPARING);
    }

    private void addTextureView() {
        Log.i(TAG, "addTextureView [" + this.hashCode() + "]");
        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }
        MxMediaManager.mTextureView = null;
        MxMediaManager.mTextureView = new MxTextureView(getContext());
        MxMediaManager.mTextureView.setVideoSize(MxMediaManager.getInstance().getVideoSize());
        MxMediaManager.mTextureView.setRotation(MxMediaManager.getInstance().mVideoRotation);
        MxMediaManager.mTextureView.setSurfaceTextureListener(this);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        mTextureViewContainer.addView(MxMediaManager.mTextureView, params);
        mCacheImageView.setVideoSize(MxMediaManager.getInstance().getVideoSize());
        mCacheImageView.setRotation(MxMediaManager.getInstance().mVideoRotation);
    }

    public static void releaseAllVideos() {
        Log.i(TAG, "releaseAllVideos===========");
        MxVideoPlayerManager.completeAll();
        MxMediaManager.getInstance().releaseMediaPlayer();
    }

    private void clearFullscreenLayout() {
        ViewGroup vp = (ViewGroup) (MxUtils.scanForActivity(getContext()))
                .findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(FULLSCREEN_ID);
        View oldT = vp.findViewById(TINY_ID);
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
        Log.i(TAG, "onPrepared================");
        if (mCurrentState != CURRENT_STATE_PREPARING) {
            return;
        }
        MxMediaManager.getInstance().getPlayer().start();
        setUiStateAndScreen(CURRENT_STATE_PLAYING);
    }

    @Override
    public void onCompletion() {
        Log.i(TAG, "onCompletion===============");
        setUiStateAndScreen(CURRENT_STATE_NORMAL);
        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        MxMediaManager.getInstance().mCurVideoWidth = 0;
        MxMediaManager.getInstance().mCurVideoHeight = 0;
        // clean cache variable
        MxMediaManager.getInstance().bufferPercent = 0;
        MxMediaManager.getInstance().mVideoRotation = 0;

        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        MxUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        clearFullscreenLayout();
        MxUtils.getAppComptActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);
        clearCacheImage();
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
            setUiStateAndScreen(CURRENT_STATE_ERROR);
        }
    }

    @Override
    public void onInfo(int what, int extra) {
        Log.i(TAG, "onInfo what : " + what + " extra : " + extra);
        if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
            MxMediaManager.getInstance().mBackUpBufferState = mCurrentState;
            setUiStateAndScreen(CURRENT_STATE_PLAYING_BUFFERING_START);
            Log.i(TAG, "MEDIA_INFO_BUFFERING_START");
        } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
            if (MxMediaManager.getInstance().mBackUpBufferState != -1) {
                setUiStateAndScreen(MxMediaManager.getInstance().mBackUpBufferState);
                MxMediaManager.getInstance().mBackUpBufferState = -1;
            }
            Log.i(TAG, "MEDIA_INFO_BUFFERING_END");
        } else if (what == IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED) {
            MxMediaManager.getInstance().mVideoRotation = extra;
            MxMediaManager.mTextureView.setRotation(extra);
            mCacheImageView.setRotation(MxMediaManager.getInstance().mVideoRotation);
            Log.i(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED");
        }
    }

    @Override
    public void onVideoSizeChanged() {
        Log.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        Point videoSize = MxMediaManager.getInstance().getVideoSize();
        MxMediaManager.mTextureView.setVideoSize(videoSize);
        mCacheImageView.setVideoSize(videoSize);
    }

    @Override
    public void goBackThisListener() {

    }

    @Override
    public boolean backToOtherListener() {
        return false;
    }

    @Override
    public void onScrollChange() {

    }

    @Override
    public int getScreenType() {
        return 0;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public void onAutoCompletion() {

    }

    @Override
    public void autoFullscreen(float x) {

    }

    @Override
    public void autoQuitFullscreen() {

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable [" + this.hashCode() + "] ");
        mSurface = new Surface(surface);
        MxMediaManager.getInstance().setDisplay(mSurface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureSizeChanged [" + this.hashCode() + "] ");
        mTextureSizeChanged = true;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        surface.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (!mTextureSizeChanged) {
            mCacheImageView.setVisibility(View.INVISIBLE);
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

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

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

    public abstract int getLayoutId();
    public abstract void showWifiDialog();
}
