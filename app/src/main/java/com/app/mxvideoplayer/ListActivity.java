package com.app.mxvideoplayer;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import hb.xvideoplayer.MxVideoPlayer;

public class ListActivity extends AppCompatActivity {

    private SensorManager mSensorManager;
    private MxVideoPlayer.MxAutoFullscreenListener mSensorEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ListView mListView = (ListView) findViewById(R.id.test_list_view);
        mListView.setAdapter(new VideoListAdapter(ListActivity.this));
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorEventListener = new MxVideoPlayer.MxAutoFullscreenListener();
    }

    @Override
    public void onBackPressed() {
        if (MxVideoPlayer.backPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(mSensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mSensorEventListener);
        MxVideoPlayer.releaseAllVideos();
    }
}
