package hb.xvideoplayer_lib;


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import mxvideoplayer.app.com.xvideoplayer.R;



public abstract class MxVideoPlayer extends FrameLayout implements MxMediaPlayerListener, View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, View.OnTouchListener, TextureView.SurfaceTextureListener {

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

    public int mCurrentState = -1;
    public int mCurrentScreen = -1;

    public String mPlayUrl;
    public Object[] mObjects = null;
    protected static WeakReference<MxUserAction> mUserAction;
    private ImageView mStartButton;
    private ImageView mFullscreenButton;
    private SeekBar mProgressBar;
    private TextView mCurrentTimeTextView;
    private TextView mTotalTimeTextView;
    private ViewGroup mBottomContainer;
    private ViewGroup mTextureViewContainer;
    private ViewGroup mTopContainer;
    private MxImageView mCacheImageView;
    private int mScreenWidth;
    private int mScreenHeight;
    private AudioManager mAudioManager;
    private Handler mHandler;

    public MxVideoPlayer(Context context) {
        this(context, null);
    }

    public MxVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
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
        return true;
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }


    private void startWindowTiny() {
    }

    public void setUiStateAndScreen(int state) {
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
        return (MxVideoPlayerManager.getFirst() != null
                && MxVideoPlayerManager.getFirst() == this);
    }

    @Override
    public void onPrepared() {

    }

    @Override
    public void onCompletion() {

    }

    @Override
    public void onBufferingUpdate(int percent) {

    }

    @Override
    public void onSeekComplete() {

    }

    @Override
    public void onError(int what, int extra) {

    }

    @Override
    public void onInfo(int what, int extra) {

    }

    @Override
    public void onVideoSizeChanged() {

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

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

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

    public abstract int getLayoutId();
}
