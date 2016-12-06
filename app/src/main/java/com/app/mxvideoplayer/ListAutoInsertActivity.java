package com.app.mxvideoplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AbsListView;
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
        MxVideoPlayerWidget playerWidget = (MxVideoPlayerWidget) view.findViewById(R.id.mx_video_player);
        playerWidget.startPlay("http://112.253.22.162/8/l/r/m/u/lrmuartyvcqytunfrqatzthrsrsmnm/" +
                        "hc.yinyuetai.com/A1460152D6652EB21A149B9DF5F7E92E.flv",
                MxVideoPlayer.SCREEN_LAYOUT_NORMAL, "LUV Apink");

        mListView.setAdapter(new ArrayAdapter<>(ListAutoInsertActivity.this, android.R.layout.simple_list_item_1, dataList));
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                MxVideoPlayer.onScroll();
            }
        });
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
