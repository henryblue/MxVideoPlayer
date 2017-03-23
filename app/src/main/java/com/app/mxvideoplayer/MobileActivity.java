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
        videoPlayerWidget.autoStartPlay("http://140.207.204.156/185/36/24/letv-uts/14/ver_00_22-1032943424-avc-800171-aac-64001-1737360-189580210-07e49d10e171451346c343c19e966361-1458539290463.m3u8?crypt=63aa7f2e981&b=872&nlh=4096&nlt=60&bf=90&p2p=1&video_type=mp4&termid=0&tss=ios&platid=6&splatid=602&its=0&qos=3&fcheck=0&amltag=6602&mltag=6602&proxy=2362428569,2073904776,467476868&uid=989235994.rp&keyitem=GOw_33YJAAbXYE-cnQwpfLlv_b2zAkYctFVqe5bsXQpaGNn3T1-vhw..&ntm=1490268000&nkey=a7fa7af2d7fdd856993da1bd86fda469&nkey2=87d22028413e91ff18726a92d1a1773e&geo=CN-9-114-2&mmsid=49476265&tm=1490249840&key=e913a3db7fd70b9bfaa5b9b796dca1e7&orifrom=aHR0cCUzQS8vd3d3Lmd1aHVvLnR2Lw==&orivid=24914404&clientIp01=182.87.223.196&clientIp02=220.181.153.132&playid=0&vtype=22&cvid=89974894146&payff=0&errc=0&gn=1312&vrtmcd=102&buss=6602&cips=58.246.139.26",
                MxVideoPlayer.SCREEN_LAYOUT_NORMAL, "LUV Apink");

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
                Intent intent = new Intent(MobileActivity.this, AutoTinyWindowActivity.class);
                startActivity(intent);
            }
        });

        Button buttonSection = (Button) findViewById(R.id.button_play_subsection_video);
        buttonSection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MobileActivity.this, PlaySectionActivity.class);
                startActivity(intent);
            }
        });

        Button btnFullscreen = (Button) findViewById(R.id.button_auto_fullscreen);
        btnFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MxVideoPlayerWidget.startFullscreen(MobileActivity.this, MxVideoPlayerWidget.class,
                        "http://112.253.22.162/8/l/r/m/u/lrmuartyvcqytunfrqatzthrsrsmnm/" +
                                "hc.yinyuetai.com/A1460152D6652EB21A149B9DF5F7E92E.flv"
                , "LUV Apink");
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
