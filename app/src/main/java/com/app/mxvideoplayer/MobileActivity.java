package com.app.mxvideoplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import hb.xvideoplayer.MxVideoPlayer;
import hb.xvideoplayer.MxVideoPlayerWidget;


public class MobileActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile);

        MxVideoPlayerWidget videoPlayerWidget = (MxVideoPlayerWidget) findViewById(R.id.mpw_video_player);
        videoPlayerWidget.autoStartPlay(
                "http://1400299523.vod2.myqcloud.com/d457202dvodtranscq1400299523/7cfe01a45285890811342490244/v.f30.mp4",
                MxVideoPlayer.SCREEN_LAYOUT_NORMAL,
                "汽车"
        );


        Button buttonList = (Button) findViewById(R.id.button_list);
        buttonList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MobileActivity.this, ListActivity.class);
                startActivity(intent);
            }
        });

        Button buttonAutoList = (Button) findViewById(R.id.button_auto_list);
        buttonAutoList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MobileActivity.this, ListAutoInsertActivity.class);
                startActivity(intent);
            }
        });

        Button btnFullscreen = (Button) findViewById(R.id.button_auto_fullscreen);
        btnFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MxVideoPlayerWidget.startFullscreen(MobileActivity.this, MxVideoPlayerWidget.class,
                        "http://1400299523.vod2.myqcloud.com/d457202dvodtranscq1400299523/7cfe01a45285890811342490244/v.f30.mp4"
                , "汽车");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MxVideoPlayer.releaseAllVideos();
    }

    @Override
    public void onBackPressed() {
        if (MxVideoPlayer.backPress()) {
            return;
        }
        super.onBackPressed();
    }
}
