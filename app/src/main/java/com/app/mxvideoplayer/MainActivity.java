package com.app.mxvideoplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import hb.xvideoplayer.MxVideoPlayer;
import hb.xvideoplayer.MxVideoPlayerWidget;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MxVideoPlayerWidget videoPlayerWidget = (MxVideoPlayerWidget) findViewById(R.id.mpw_video_player);
        videoPlayerWidget.startPlay("http://112.253.22.159/29/g/b/u/u/gbuustuaqwiwhhsvwhmnfvlolfdqnc/" +
                "hc.yinyuetai.com/17F0015849548884B263584D615989A1.mp4",
                MxVideoPlayerWidget.SCREEN_LAYOUT_NORMAL, "TIAMO 完整版");
    }

    @Override
    protected void onPause() {
        super.onPause();
        MxVideoPlayer.releaseAllVideos();
    }

}
