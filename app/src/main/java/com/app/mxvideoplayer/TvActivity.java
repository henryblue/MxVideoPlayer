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
        mVideoPlayerWidget.startPlay("http://112.253.22.162/8/l/r/m/u/lrmuartyvcqytunfrqatzthrsrsmnm/" +
                "hc.yinyuetai.com/A1460152D6652EB21A149B9DF5F7E92E.flv", "LUV Apink");

        mVideoPlayerWidget.setOnPlayStateListener(new MxTvPlayerWidget.OnPlayStateListener() {
            @Override
            public void onPlayPrepared() {
            }

            @Override
            public void onPlayBufferingUpdate(int percent) {
            }

            @Override
            public void OnPlayCompletion() {
                mVideoPlayerWidget.autoStartPlay("http://112.253.22.156/14/j/s/s/d/" +
                        "jssdpypuuzgutqiolfvbxizywfjzjd/hc.yinyuetai.com/F9640146F51C894E3B31592989D7AE28.flv",
                        "One More 完整版");
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
