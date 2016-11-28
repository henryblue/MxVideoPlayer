package hb.xvideoplayer;


import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import mxvideoplayer.app.com.xvideoplayer.R;

public class MxVideoPlayerWidget extends MxVideoPlayer {

    protected static Timer DISMISS_CONTROL_VIEW_TIMER;

    public ImageView mBackButton;
    public ProgressBar mBottomProgressBar, mLoadingProgressBar;
    public TextView mTitleTextView;
    public ImageView mThumbImageView;
    public ImageView mTinyBackImageView;

    protected DismissControlViewTimerTask mDismissControlViewTimerTask;

    public MxVideoPlayerWidget(Context context) {
        super(context);
    }

    public MxVideoPlayerWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void initView(Context context) {
        super.initView(context);
        mBottomProgressBar = (ProgressBar) findViewById(R.id.mx_bottom_progress);
        mTitleTextView = (TextView) findViewById(R.id.mx_title);
        mBackButton = (ImageView) findViewById(R.id.mx_back);
        mThumbImageView = (ImageView) findViewById(R.id.mx_thumb);
        mLoadingProgressBar = (ProgressBar) findViewById(R.id.mx_loading);
        mTinyBackImageView = (ImageView) findViewById(R.id.mx_quit_tiny);

        mThumbImageView.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
        mTinyBackImageView.setOnClickListener(this);
    }

    @Override
    public boolean setUp(String url, int screen, Object... objects) {
        if (objects.length == 0) {
            return false;
        }
        if (super.setUp(url, screen, objects)) {
            mTitleTextView.setText(objects[0].toString());
            if (mCurrentScreen == SCREEN_WINDOW_FULLSCREEN) {
                mFullscreenButton.setImageResource(R.drawable.mx_shrink);
                mBackButton.setVisibility(View.VISIBLE);
                mTinyBackImageView.setVisibility(View.INVISIBLE);
            } else if (mCurrentScreen == SCREEN_LAYOUT_LIST ||
                    mCurrentScreen == SCREEN_LAYOUT_NORMAL) {
                mFullscreenButton.setImageResource(R.drawable.mx_enlarge);
                mBackButton.setVisibility(View.GONE);
                mTinyBackImageView.setVisibility(View.INVISIBLE);
            } else if (mCurrentScreen == SCREEN_WINDOW_TINY) {
                mTinyBackImageView.setVisibility(View.VISIBLE);
                setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
            }
            return true;
        }
        return false;
    }

    @Override
    public void setUiStateAndScreen(int state) {
        super.setUiStateAndScreen(state);
        switch (mCurrentState) {
            case CURRENT_STATE_NORMAL:
                changeUiToNormal();
                break;
            case CURRENT_STATE_PREPARING:
                changeUiToPreparingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PLAYING:
                changeUiToPlayingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PAUSE:
                changeUiToPauseShow();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_ERROR:
                changeUiToError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                changeUiToCompleteShow();
                cancelDismissControlViewTimer();
                mBottomProgressBar.setProgress(100);
                break;
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                changeUiToPlayingBufferingShow();
                break;
            default:
                break;
        }
    }

    private void changeUiToPlayingBufferingShow() {
        switch (mCurrentScreen) {
            case SCREEN_LAYOUT_NORMAL:
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.INVISIBLE,
                        View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.INVISIBLE,
                        View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    private void changeUiToCompleteShow() {
        switch (mCurrentScreen) {
            case SCREEN_LAYOUT_NORMAL:
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    private void changeUiToError() {
        clearCacheImage();
        switch (mCurrentScreen) {
            case SCREEN_LAYOUT_NORMAL:
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    private void cancelDismissControlViewTimer() {
        if (DISMISS_CONTROL_VIEW_TIMER != null) {
            DISMISS_CONTROL_VIEW_TIMER.cancel();
        }
        if (mDismissControlViewTimerTask != null) {
            mDismissControlViewTimerTask.cancel();
        }
    }

    private void changeUiToPauseShow() {
        switch (mCurrentScreen) {
            case SCREEN_LAYOUT_NORMAL:
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    private void changeUiToPlayingShow() {
        switch (mCurrentScreen) {
            case SCREEN_LAYOUT_NORMAL:
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    private void startDismissControlViewTimer() {
        cancelDismissControlViewTimer();
        DISMISS_CONTROL_VIEW_TIMER = new Timer();
        mDismissControlViewTimerTask = new DismissControlViewTimerTask();
        DISMISS_CONTROL_VIEW_TIMER.schedule(mDismissControlViewTimerTask, 2500);
    }

    private void changeUiToPreparingShow() {
        switch (mCurrentScreen) {
            case SCREEN_LAYOUT_NORMAL:
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.VISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.VISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    private void changeUiToNormal() {
        switch (mCurrentState) {
            case SCREEN_LAYOUT_NORMAL:
            case SCREEN_LAYOUT_LIST:
                setAllControlsVisible(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisible(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
            default:
                break;
        }
    }

    private void updateStartImage() {
        if (mCurrentState == CURRENT_STATE_PLAYING) {
            mStartButton.setImageResource(R.drawable.mx_click_pause_selector);
        } else if (mCurrentState == CURRENT_STATE_ERROR) {
            mStartButton.setImageResource(R.drawable.mx_click_error_selector);
        } else {
            mStartButton.setImageResource(R.drawable.mx_click_play_selector);
        }
    }

    public void setAllControlsVisible(int topCon, int bottomCon, int startBtn, int loadingPro,
                                      int thumbImg, int bottomPro) {
        mTopContainer.setVisibility(topCon);
        mBottomContainer.setVisibility(bottomCon);
        mStartButton.setVisibility(startBtn);
        mLoadingProgressBar.setVisibility(loadingPro);
        if (thumbImg == View.VISIBLE) {
            mThumbImageView.setVisibility(thumbImg);
        } else {
            mThumbImageView.setVisibility(View.GONE);
        }
        mBottomProgressBar.setVisibility(bottomPro);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

    }

    @Override
    public void setProgressAndTime(int progress, int secProgress, int currentTime, int totalTime) {
        super.setProgressAndTime(progress, secProgress, currentTime, totalTime);
        if (progress != 0) {
            mBottomProgressBar.setProgress(progress);
        }
        if (secProgress != 0) {
            mBottomProgressBar.setSecondaryProgress(secProgress);
        }
    }

    @Override
    public void resetProgressAndTime() {
        super.resetProgressAndTime();
        mBottomProgressBar.setProgress(0);
        mBottomProgressBar.setSecondaryProgress(0);
    }

    @Override
    public int getLayoutId() {
        return R.layout.mx_video_layout_standard;
    }

    @Override
    public void showWifiDialog() {

    }

    public class DismissControlViewTimerTask extends TimerTask {

        @Override
        public void run() {
            if (mCurrentState != CURRENT_STATE_NORMAL
                    && mCurrentState != CURRENT_STATE_ERROR
                    && mCurrentState != CURRENT_STATE_AUTO_COMPLETE) {
                if (getContext() != null && getContext() instanceof Activity) {
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mBottomContainer.setVisibility(View.INVISIBLE);
                            mTopContainer.setVisibility(View.INVISIBLE);
                            mStartButton.setVisibility(View.INVISIBLE);
                            if (mCurrentScreen != SCREEN_WINDOW_TINY) {
                                mBottomProgressBar.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            }
        }
    }
}
