package com.app.mxvideoplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import hb.xvideoplayer.MxVideoPlayer;
import hb.xvideoplayer.MxVideoPlayerWidget;

public class ListAutoInsertActivity extends AppCompatActivity {

    private List<String> dataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ListView mListView = (ListView) findViewById(R.id.test_list_view);
        for (int i = 1; i <= 40; ++i) {
            dataList.add("item" + i);
        }
        View view = View.inflate(ListAutoInsertActivity.this, R.layout.head_normal_list, null);
        mListView.addHeaderView(view);
        final MxVideoPlayerWidget playerWidget = (MxVideoPlayerWidget) view.findViewById(R.id.mx_video_player);
        playerWidget.startPlay("http://1400299523.vod2.myqcloud.com/d457202dvodtranscq1400299523/b0d244d75285890811387532689/v.f30.mp4",
                MxVideoPlayer.SCREEN_LAYOUT_NORMAL, "明天你是否依然爱我");
        playerWidget.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                if (playerWidget.getState() == MxVideoPlayer.CURRENT_STATE_PLAYING) {
                    playerWidget.quitWindowTiny();
                }
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                if (playerWidget.getState() == MxVideoPlayer.CURRENT_STATE_PLAYING) {
                    playerWidget.startWindowTiny();
                }
            }
        });

        mListView.setAdapter(new ArrayAdapter<>(ListAutoInsertActivity.this,
                android.R.layout.simple_list_item_1, dataList));
    }

    @Override
    public void onBackPressed() {
        if (MxVideoPlayer.backPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MxVideoPlayer.releaseAllVideos();
    }
}
