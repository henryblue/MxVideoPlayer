package com.app.mxvideoplayer;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;

import hb.xvideoplayer.MxTvPlayerWidget;
import hb.xvideoplayer.MxVideoPlayer;


public class TvActivity extends AppCompatActivity {

    private MxTvPlayerWidget mVideoPlayerWidget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv);
        View decorView = getWindow().getDecorView();
        int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(option);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mVideoPlayerWidget = (MxTvPlayerWidget) findViewById(R.id.mpw_video_player);
        mVideoPlayerWidget.startPlay("http://1400299523.vod2.myqcloud.com/d457202dvodtranscq1400299523/7cfe01a45285890811342490244/v.f30.mp4",
                "汽车");

        mVideoPlayerWidget.setOnPlayStateListener(new MxTvPlayerWidget.OnPlayStateListener() {
            @Override
            public void onPlayPrepared() {
            }

            @Override
            public void onPlayBufferingUpdate(int percent) {
            }

            @Override
            public void OnPlayCompletion() {
            }
        });

        mVideoPlayerWidget.addOnKeyListener(new MxTvPlayerWidget.IOnKeyListener() {
            @Override
            public boolean onKeyDown(int keyCode, KeyEvent event) {
                return false;
            }

            @Override
            public boolean onKeyUp(int keyCode, KeyEvent event) {
                return false;
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mVideoPlayerWidget.requestKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mVideoPlayerWidget.requestKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MxVideoPlayer.releaseAllVideos();
    }
}
