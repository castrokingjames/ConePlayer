package org.noobs2d.coneplayer;

import android.view.SurfaceView;
import android.view.TextureView;

public interface ConePlayer {

    int STATE_IDLE = 1;

    int STATE_PLAYING = 2;

    int STATE_PAUSE = 3;

    int STATE_STOP = 4;

    int STATE_ENDED = 5;

    void prepare(Media media);

    void play();

    void pause();

    void repeat();

    void stop();

    void release();

    void seekTo(long positionMs);

    void addVideoListener(VideoListener videoListener);

    void removeVideoListener(VideoListener videoListener);

    void clearVideoListeners();

    void addPlayerStateChangeListener(PlayerStateChangeListener playerStateChangeListener);

    void removePlayerStateChangeListener(PlayerStateChangeListener playerStateChangeListener);

    void clearPlayerStateChangeListeners();

    void addCompletionListener(CompletionListener listener);

    void removeCompletionListener(CompletionListener listener);

    void clearCompletionListener();

    void setVideoSurfaceView(SurfaceView surfaceView);

    void setVideoTextureView(TextureView textureView);

    void clearVideoTextureView(TextureView surfaceView);

    void clearVideoSurfaceView(SurfaceView surfaceView);

    int getPlaybackState();

    long getDuration();

    long getCurrentPosition();

    interface VideoListener {

        void onVideoSizeChanged(int width, int height);

        void onRenderedFirstFrame();
    }

    interface PlayerStateChangeListener {

        void onPlayerStateChanged(int state);
    }

    interface CompletionListener {

        void onComplete();
    }
}