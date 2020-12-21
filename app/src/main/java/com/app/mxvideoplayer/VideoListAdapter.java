package com.app.mxvideoplayer;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.app.mxvideoplayer.util.playUtils;
import com.bumptech.glide.Glide;

import hb.xvideoplayer.MxVideoPlayer;
import hb.xvideoplayer.MxVideoPlayerWidget;


public class VideoListAdapter extends BaseAdapter {

    private final Context mContext;
    int[] mVideoIds = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public VideoListAdapter(Context context) {
        mContext = context;
    }
    @Override
    public int getCount() {
        return mVideoIds.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (null == convertView) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.layout_item_list, null);
            holder.mPlayerWidget = (MxVideoPlayerWidget) convertView.findViewById(R.id.list_video_player);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        String url = playUtils.videoUrls[position];
        holder.mPlayerWidget.startPlay(url, MxVideoPlayer.SCREEN_LAYOUT_LIST,
                playUtils.videoTitles[position]);

        return convertView;
    }

    private class ViewHolder {
        MxVideoPlayerWidget mPlayerWidget;
    }
}
