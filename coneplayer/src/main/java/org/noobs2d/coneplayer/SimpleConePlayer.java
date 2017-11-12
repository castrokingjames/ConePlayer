package org.noobs2d.coneplayer;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.MediaPlayer;

import java.util.concurrent.CopyOnWriteArraySet;

public class SimpleConePlayer
        implements ConePlayer,
        Handler.Callback,
        IVLCVout.Callback,
        MediaPlayer.EventListener {

    private static final int MSG_PREPARE = 0;
    private static final int MSG_PLAY = 1;
    private static final int MSG_PAUSE = 2;
    private static final int MSG_SEEK = 3;
    private static final int MSG_REPEAT = 4;
    private static final int MSG_STOP = 5;
    private static final int MSG_RELEASE = 6;
    private static final int MSG_ATTACH_VIEW = 7;

    private final CopyOnWriteArraySet<VideoListener> videoListeners;
    private final CopyOnWriteArraySet<PlayerStateChangeListener> playerStateChangeListeners;
    private final CopyOnWriteArraySet<CompletionListener> completionListeners;
    private final HandlerThread internalPlaybackThread;
    private final Handler handler;
    private ComponentListener componentListener;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private IVLCVout vlcVout;
    private View videoView;
    private int state = STATE_IDLE;
    private boolean onRenderFirstFrame = false;

    public SimpleConePlayer(LibVLC libVLC) {
        this.libVLC = libVLC;
        mediaPlayer = new MediaPlayer(libVLC);
        vlcVout = mediaPlayer.getVLCVout();

        videoListeners = new CopyOnWriteArraySet<>();
        playerStateChangeListeners = new CopyOnWriteArraySet<>();
        completionListeners = new CopyOnWriteArraySet<>();
        componentListener = new ComponentListener();

        internalPlaybackThread = new HandlerThread("VLCPlayer:Handler",
                Process.THREAD_PRIORITY_AUDIO);
        internalPlaybackThread.start();
        handler = new Handler(internalPlaybackThread.getLooper(), this);
        mediaPlayer.setEventListener(this);
        vlcVout.addCallback(this);
    }

    @Override
    public boolean handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case MSG_PREPARE: {
                    Media media = (Media) msg.obj;
                    prepareInternal(media);
                    return true;
                }
                case MSG_PLAY: {
                    playInternal();
                    return true;
                }
                case MSG_PAUSE: {
                    pauseInternal();
                    return true;
                }
                case MSG_SEEK: {
                    seekToInternal((Long) msg.obj);
                    return true;
                }
                case MSG_REPEAT: {
                    repeatInternal();
                    return true;
                }
                case MSG_STOP: {
                    stopInternal();
                    return true;
                }
                case MSG_RELEASE: {
                    releaseInternal();
                    return true;
                }
                case MSG_ATTACH_VIEW: {
                    Bundle bundle = (Bundle) msg.obj;
                    attachViewInternal(bundle);
                    return true;
                }
                default:
                    return false;
            }
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public void prepare(Media media) {
        handler.obtainMessage(MSG_PREPARE, media).sendToTarget();
    }

    @Override
    public void play() {
        handler.obtainMessage(MSG_PLAY).sendToTarget();
    }

    @Override
    public void pause() {
        handler.obtainMessage(MSG_PAUSE).sendToTarget();
    }

    @Override
    public void repeat() {
        handler.obtainMessage(MSG_REPEAT).sendToTarget();
    }

    @Override
    public void stop() {
        handler.obtainMessage(MSG_STOP).sendToTarget();
    }

    @Override
    public void release() {
        handler.obtainMessage(MSG_RELEASE).sendToTarget();
    }

    @Override
    public void seekTo(long positionMs) {
        handler.obtainMessage(MSG_SEEK, positionMs).sendToTarget();
    }

    @Override
    public void addVideoListener(VideoListener videoListener) {
        videoListeners.add(videoListener);
    }

    @Override
    public void removeVideoListener(VideoListener videoListener) {
        videoListeners.remove(videoListener);
    }

    @Override
    public void clearVideoListeners() {
        videoListeners.clear();
    }

    @Override
    public void addPlayerStateChangeListener(PlayerStateChangeListener playerStateChangeListener) {
        playerStateChangeListeners.add(playerStateChangeListener);
    }

    @Override
    public void removePlayerStateChangeListener(PlayerStateChangeListener playerStateChangeListener) {
        playerStateChangeListeners.remove(playerStateChangeListener);
    }

    @Override
    public void clearPlayerStateChangeListeners() {
        playerStateChangeListeners.clear();
    }

    @Override
    public void addCompletionListener(CompletionListener listener) {
        completionListeners.add(listener);
    }

    @Override
    public void removeCompletionListener(CompletionListener listener) {
        completionListeners.remove(listener);
    }

    @Override
    public void clearCompletionListener() {
        completionListeners.clear();
    }

    @Override
    public void setVideoSurfaceView(SurfaceView surfaceView) {
        if (surfaceView == videoView)
            return;
        removeVideoViewListener();
        videoView = surfaceView;
        if (surfaceView != null) {

            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            Surface surface = surfaceHolder.getSurface();
            if (surface != null && surface.isValid()) {
                setSurfaceInternal(surface, surfaceView.getWidth(), surfaceView.getHeight());
            }

            surfaceHolder.addCallback(componentListener);
        }
    }

    @Override
    public void setVideoTextureView(TextureView textureView) {
        if (textureView == videoView)
            return;
        removeVideoViewListener();
        videoView = textureView;
        if (textureView != null) {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            if (surfaceTexture != null) {
                Surface surface = new Surface(surfaceTexture);
                setSurfaceInternal(surface, textureView.getWidth(), textureView.getHeight());
            }

            textureView.setSurfaceTextureListener(componentListener);
        }
    }

    private void removeVideoViewListener() {
        if (videoView == null)
            return;

        if (videoView instanceof SurfaceView) {
            SurfaceView surfaceView = (SurfaceView) videoView;
            surfaceView.getHolder().removeCallback(componentListener);
        } else if (videoView instanceof TextureView) {
            TextureView textureView = (TextureView) videoView;
            textureView.setSurfaceTextureListener(null);
        }
    }

    @Override
    public void clearVideoTextureView(TextureView textureView) {
        if (textureView != null && videoView == textureView) {
            textureView.setSurfaceTextureListener(null);
            videoView = null;
            vlcVout.detachViews();
        }
    }

    @Override
    public void clearVideoSurfaceView(SurfaceView surfaceView) {
        if (surfaceView != null && videoView == surfaceView) {
            surfaceView.getHolder().removeCallback(componentListener);
            videoView = null;
            vlcVout.detachViews();
        }
    }

    @Override
    public int getPlaybackState() {
        return state;
    }

    @Override
    public long getDuration() {
        return mediaPlayer.getMedia().getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return mediaPlayer.getTime();
    }

    @Override
    public void onNewLayout(IVLCVout vlcVout,
                            int width,
                            int height,
                            int visibleWidth,
                            int visibleHeight,
                            int sarNum,
                            int sarDen) {
        for (VideoListener videoListener : videoListeners)
            videoListener.onVideoSizeChanged(width, height);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {

    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {

    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Opening:
                state = STATE_IDLE;
                onIdle();
                break;

            case MediaPlayer.Event.Playing:
                state = STATE_PLAYING;
                break;

            case MediaPlayer.Event.Paused:
                state = STATE_PAUSE;
                break;

            case MediaPlayer.Event.EndReached:
                state = STATE_ENDED;
                onEndReached();
                break;

            case MediaPlayer.Event.Stopped:
                state = STATE_STOP;
                break;

            case MediaPlayer.Event.PositionChanged:
                onRenderedFirstFrame();
                break;
        }

        for (PlayerStateChangeListener playerStateChangeListener : playerStateChangeListeners)
            playerStateChangeListener.onPlayerStateChanged(state);

    }

    private void onIdle() {
        onRenderFirstFrame = false;
    }

    private void onRenderedFirstFrame() {
        if (onRenderFirstFrame)
            return;

        for (VideoListener videoListener : videoListeners)
            videoListener.onRenderedFirstFrame();

        onRenderFirstFrame = true;
    }

    private void onEndReached() {
        for (CompletionListener completionListener : completionListeners)
            completionListener.onComplete();
    }

    private void prepareInternal(Media media) {
        org.videolan.libvlc.Media libVlcMedia = new org.videolan.libvlc.Media(libVLC, media.getPath());
        mediaPlayer.setMedia(libVlcMedia);
        libVlcMedia.release();
    }

    private void playInternal() {
        mediaPlayer.play();
    }

    private void pauseInternal() {
        mediaPlayer.pause();
    }

    private void seekToInternal(long positionMs) {
        mediaPlayer.setTime(positionMs);
    }

    private void repeatInternal() {
        org.videolan.libvlc.Media libVlcMedia = mediaPlayer.getMedia();
        mediaPlayer.setMedia(libVlcMedia);
        mediaPlayer.play();
        libVlcMedia.release();
    }

    private void releaseInternal() {
        mediaPlayer.release();
        libVLC.release();
    }

    private void attachViewInternal(Bundle bundle) {
        int width = bundle.getInt("width");
        int height = bundle.getInt("height");
        Surface surface = bundle.getParcelable("surface");
        vlcVout.setWindowSize(width, height);
        vlcVout.setVideoSurface(surface, null);
        vlcVout.attachViews();
    }

    private void setSurfaceInternal(Surface surface, int width, int height) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("surface", surface);
        bundle.putInt("width", width);
        bundle.putInt("height", height);
        handler.obtainMessage(MSG_ATTACH_VIEW, bundle).sendToTarget();
    }

    private void stopInternal() {
        mediaPlayer.stop();
    }

    private final class ComponentListener
            implements TextureView.SurfaceTextureListener,
            SurfaceHolder.Callback {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setSurfaceInternal(new Surface(surface), width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Rect rect = holder.getSurfaceFrame();
            setSurfaceInternal(holder.getSurface(), rect.width(), rect.height());
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }
}