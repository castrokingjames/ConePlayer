package org.noobs2d.coneplayer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.noobs2d.R;
import org.noobs2d.coneplayer.ConePlayer;

public class ConePlayerView
        extends FrameLayout {

    private static final int SURFACE_TYPE_NONE = 0;
    private static final int SURFACE_TYPE_SURFACE_VIEW = 1;
    private static final int SURFACE_TYPE_TEXTURE_VIEW = 2;

    private final AspectRatioFrameLayout contentFrame;
    private final ComponentListener componentListener;
    private final PlaybackControlView controller;
    private final View surfaceView;

    private ConePlayer player;
    private boolean useController;
    private int controllerShowTimeoutMs;

    public ConePlayerView(@NonNull Context context) {
        this(context, null);
    }

    public ConePlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConePlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (isInEditMode()) {
            contentFrame = null;
            surfaceView = null;
            componentListener = null;
            controller = null;
            useController = false;
            return;
        }

        boolean useController = true;
        int playerLayoutId = R.layout.cone_player_view;
        int surfaceType = SURFACE_TYPE_SURFACE_VIEW;
        int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
        int controllerShowTimeoutMs = PlaybackControlView.DEFAULT_SHOW_TIMEOUT_MS;

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.ConePlayerView, 0, 0);
            try {
                playerLayoutId = a.getResourceId(R.styleable.ConePlayerView_player_layout_id,
                        playerLayoutId);
                useController = a.getBoolean(R.styleable.ConePlayerView_use_controller, useController);
                surfaceType = a.getInt(R.styleable.ConePlayerView_surface_type, surfaceType);
                resizeMode = a.getInt(R.styleable.ConePlayerView_resize_mode, resizeMode);
                controllerShowTimeoutMs = a.getInt(R.styleable.ConePlayerView_show_timeout,
                        controllerShowTimeoutMs);
            } finally {
                a.recycle();
            }
        }

        LayoutInflater.from(context).inflate(playerLayoutId, this);
        componentListener = new ComponentListener();
        contentFrame = findViewById(R.id.cone_content_frame);
        contentFrame.setResizeMode(resizeMode);

        if (contentFrame != null && surfaceType != SURFACE_TYPE_NONE) {
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            surfaceView = surfaceType == SURFACE_TYPE_TEXTURE_VIEW ? new TextureView(context)
                    : new SurfaceView(context);
            surfaceView.setLayoutParams(params);
            contentFrame.addView(surfaceView, 0);
        } else {
            surfaceView = null;
        }

        View controllerPlaceholder = findViewById(R.id.cone_controller_placeholder);
        if (controllerPlaceholder != null) {
            this.controller = new PlaybackControlView(context, attrs, defStyleAttr);
            controller.setLayoutParams(controllerPlaceholder.getLayoutParams());
            ViewGroup parent = ((ViewGroup) controllerPlaceholder.getParent());
            int controllerIndex = parent.indexOfChild(controllerPlaceholder);
            parent.removeView(controllerPlaceholder);
            parent.addView(controller, controllerIndex);
            controller.setShowTimeoutMs(controllerShowTimeoutMs);
        } else {
            this.controller = null;
        }

        this.controllerShowTimeoutMs = controller != null ? controllerShowTimeoutMs : 0;
        this.useController = useController && controller != null;
        hideController();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!useController || player == null) {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (controller.isVisible()) {
                controller.hide();
            } else {
                controller.show();
            }
        }

        return true;
    }

    public void hideController() {
        if (controller != null) {
            controller.hide();
        }
    }

    private void showController() {
        if (controller != null) {
            controller.show();
        }
    }

    public void setPlayer(ConePlayer player) {
        if (this.player == player) {
            return;
        }
        if (this.player != null) {

            if (surfaceView instanceof TextureView) {
                this.player.clearVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                this.player.clearVideoSurfaceView((SurfaceView) surfaceView);
            }
        }

        this.player = player;
        if (useController) {
            controller.setPlayer(player);
        }

        if (player != null) {
            if (surfaceView instanceof TextureView) {
                player.setVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                player.setVideoSurfaceView((SurfaceView) surfaceView);
            }

            player.addVideoListener(componentListener);
        }
    }

    public void setResizeMode(int resizeMode) {
        if (contentFrame != null)
            contentFrame.setResizeMode(resizeMode);
    }

    public int getControllerShowTimeoutMs() {
        return controllerShowTimeoutMs;
    }

    public void setControllerShowTimeoutMs(int controllerShowTimeoutMs) {
        this.controllerShowTimeoutMs = controllerShowTimeoutMs;
        if (controller != null)
            controller.setShowTimeoutMs(controllerShowTimeoutMs);
    }

    private final class ComponentListener implements ConePlayer.VideoListener {

        @Override
        public void onVideoSizeChanged(int width, int height) {
            if (contentFrame != null) {
                double aspectRatio = (double) width / height;
                contentFrame.setAspectRatio((float) aspectRatio);
            }
        }

        @Override
        public void onRenderedFirstFrame() {

        }
    }
}