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
        videoPlayerWidget.startPlay("http://112.253.22.162/8/l/r/m/u/lrmuartyvcqytunfrqatzthrsrsmnm/" +
                        "hc.yinyuetai.com/A1460152D6652EB21A149B9DF5F7E92E.flv",
                MxVideoPlayer.SCREEN_LAYOUT_NORMAL, "LUV Apink");
        Button buttonList = (Button) findViewById(R.id.button_list);
        buttonList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MobileActivity.this, ListActivity.class);
                startActivity(intent);
            }
        });
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
