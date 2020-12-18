package hb.xvideoplayer;

public interface MxMediaPlayerListener {
    void onPrepared();

    void onCompletion();

    void onBufferingUpdate(int percent);

    void onSeekComplete();

    void onError(int what, int extra);

    void onInfo(int what, int extra);

    void onVideoSizeChanged();

    void goBackNormalListener();

    boolean quitFullscreenOrTinyListener();

    int getScreenType();

    String getUrl();

    int getState();

    void onAutoCompletion();

    void autoFullscreen(float x);

    void autoQuitFullscreen();
}
