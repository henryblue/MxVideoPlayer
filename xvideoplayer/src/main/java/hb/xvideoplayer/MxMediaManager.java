package hb.xvideoplayer;


import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import java.lang.reflect.Method;
import java.util.Map;


public class MxMediaManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnInfoListener {

    private static String TAG = "MxVideoPlayer";

    private static MxMediaManager mxMediaManager;
    private MediaPlayer mMediaPlayer;
    private Handler mMediaHandler;
    private Handler mainThreadHandler;
    public static MxTextureView mTextureView;
    public static SurfaceTexture mSurface;

    public int mLastState;
    public boolean mIsShowBottomProgressBar = true;
    public int mCurVideoWidth = 0;
    public int mCurVideoHeight = 0;
    public int bufferPercent = 0;
    public int mBackUpBufferState = -1;
    public String mCurrentUrl = "";

    private MxMediaManager() {
        mMediaPlayer = new MediaPlayer();
        HandlerThread mMediaHandlerThread = new HandlerThread(TAG);
        mMediaHandlerThread.start();
        mMediaHandler = new Handler(mMediaHandlerThread.getLooper());
        mainThreadHandler = new Handler();
    }

    public static MxMediaManager getInstance() {
        if (mxMediaManager == null) {
            synchronized (MxMediaManager.class) {
                if (mxMediaManager == null) {
                    mxMediaManager = new MxMediaManager();
                }
            }
        }
        return mxMediaManager;
    }

    public void setDisPlay(final Surface surface) {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setSurface(surface);
                }
            }
        });
    }

    public MediaPlayer getPlayer() {
        return mMediaPlayer;
    }

    public void prepare(final String url, final Map<String, String> mapHeapData, final boolean loop) {
        if (!TextUtils.isEmpty(url)) {
            mMediaHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCurVideoWidth = 0;
                        mCurVideoHeight = 0;
                        if (mMediaPlayer != null) {
                            mMediaPlayer.release();
                            mMediaPlayer = null;
                        }
                        mMediaPlayer = new MediaPlayer();
                        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mMediaPlayer.setScreenOnWhilePlaying(true);
                        mMediaPlayer.setLooping(loop);
                        mMediaPlayer.setOnPreparedListener(MxMediaManager.this);
                        mMediaPlayer.setOnCompletionListener(MxMediaManager.this);
                        mMediaPlayer.setOnBufferingUpdateListener(MxMediaManager.this);
                        mMediaPlayer.setOnSeekCompleteListener(MxMediaManager.this);
                        mMediaPlayer.setOnErrorListener(MxMediaManager.this);
                        mMediaPlayer.setOnInfoListener(MxMediaManager.this);
                        mMediaPlayer.setOnVideoSizeChangedListener(MxMediaManager.this);
                        Class<MediaPlayer> clazz = MediaPlayer.class;
                        mCurrentUrl = url;
                        Method method = clazz.getDeclaredMethod("setDataSource", String.class, Map.class);
                        method.invoke(mMediaPlayer, url, mapHeapData);
                        mMediaPlayer.prepareAsync();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "handleMessage: prepare video error: " + e.getMessage());
                    }
                }
            });
        }
    }

    public void releaseMediaPlayer() {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer != null) {
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
            }
        });
    }

    public void seekTo(final int msec) {
        mMediaHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer != null) {
                    mMediaPlayer.seekTo(msec);
                }
            }
        });
    }

    @Override
    public void onPrepared(MediaPlayer MediaPlayer) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                MxMediaPlayerListener listener = MxVideoPlayerManager.getCurrentListener();
                if (listener != null) {
                    listener.onPrepared();
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(MediaPlayer MediaPlayer, final int percent) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                MxMediaPlayerListener listener = MxVideoPlayerManager.getCurrentListener();
                if (listener != null) {
                    listener.onBufferingUpdate(percent);
                }
            }
        });
    }

    @Override
    public void onCompletion(MediaPlayer MediaPlayer) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                MxMediaPlayerListener listener = MxVideoPlayerManager.getCurrentListener();
                if (listener != null) {
                    listener.onAutoCompletion();
                }
            }
        });
    }

    @Override
    public boolean onError(MediaPlayer MediaPlayer, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                MxMediaPlayerListener listener = MxVideoPlayerManager.getCurrentListener();
                if (listener != null) {
                    listener.onError(what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer MediaPlayer, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                MxMediaPlayerListener listener = MxVideoPlayerManager.getCurrentListener();
                if (listener != null) {
                    listener.onInfo(what, extra);
                }
            }
        });
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer MediaPlayer) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                MxMediaPlayerListener listener = MxVideoPlayerManager.getCurrentListener();
                if (listener != null) {
                    listener.onSeekComplete();
                }
            }
        });
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        mCurVideoWidth = mp.getVideoWidth();
        mCurVideoHeight = mp.getVideoHeight();
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                MxMediaPlayerListener listener = MxVideoPlayerManager.getCurrentListener();
                if (listener != null) {
                    listener.onVideoSizeChanged();
                }
            }
        });
    }

    public Point getVideoSize() {
        if (mCurVideoWidth != 0 && mCurVideoHeight != 0) {
            return new Point(mCurVideoWidth, mCurVideoHeight);
        } else {
            return null;
        }
    }
}
