package org.noobs2d.coneplayer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import org.noobs2d.coneplayer.ConePlayer;
import org.noobs2d.R;

public class PlaybackControlView
        extends FrameLayout {

    public static final int DEFAULT_FAST_FORWARD_MS = 15000;
    public static final int DEFAULT_REWIND_MS = 5000;
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 5000;

    public static final long TIME_UNSET = Long.MIN_VALUE + 1;

    private ConePlayer player;
    private ComponentListener componentListener;
    private View playButton;
    private View pauseButton;
    private View rewindButton;
    private View fastForwardButton;
    private View repeatButton;
    private boolean isAttachedToWindow;
    private long hideAtMs;
    private int rewindMs;
    private int fastForwardMs;
    private int showTimeoutMs;

    private final Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public PlaybackControlView(@NonNull Context context) {
        this(context, null);
    }

    public PlaybackControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlaybackControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int controllerLayoutId = R.layout.cone_playback_control_view;
        rewindMs = DEFAULT_REWIND_MS;
        fastForwardMs = DEFAULT_FAST_FORWARD_MS;
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS;

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.PlaybackControlView, 0, 0);
            try {
                rewindMs = a.getInt(R.styleable.PlaybackControlView_rewind_increment, rewindMs);
                fastForwardMs = a.getInt(R.styleable.PlaybackControlView_fastforward_increment,
                        fastForwardMs);
                showTimeoutMs = a.getInt(R.styleable.PlaybackControlView_show_timeout, showTimeoutMs);
                controllerLayoutId = a.getResourceId(R.styleable.PlaybackControlView_controller_layout_id,
                        controllerLayoutId);
            } finally {
                a.recycle();
            }
        }

        LayoutInflater.from(context).inflate(controllerLayoutId, this);
        componentListener = new ComponentListener();

        playButton = findViewById(R.id.cone_play);
        pauseButton = findViewById(R.id.cone_pause);
        fastForwardButton = findViewById(R.id.cone_fast_forward);
        rewindButton = findViewById(R.id.cone_rewind);
        repeatButton = findViewById(R.id.cone_repeat);

        // Create dummy view if view can't find on custom layout
        if (playButton == null)
            playButton = new View(context);
        if(pauseButton == null)
            pauseButton = new View(context);
        if(fastForwardButton == null)
            fastForwardButton = new View(context);
        if(rewindButton == null)
            rewindButton = new View(context);
        if(repeatButton == null)
            repeatButton = new View(context);

        playButton.setOnClickListener(componentListener);
        pauseButton.setOnClickListener(componentListener);
        fastForwardButton.setOnClickListener(componentListener);
        rewindButton.setOnClickListener(componentListener);
        repeatButton.setOnClickListener(componentListener);
    }

    public void hide() {
        if (isVisible()) {
            setVisibility(GONE);
            removeCallbacks(hideAction);
            hideAtMs = TIME_UNSET;
        }
    }

    private void hideAfterTimeout() {
        removeCallbacks(hideAction);
        if (showTimeoutMs > 0) {
            hideAtMs = SystemClock.uptimeMillis() + showTimeoutMs;
            if (isAttachedToWindow) {
                postDelayed(hideAction, showTimeoutMs);
            }
        } else {
            hideAtMs = TIME_UNSET;
        }
    }

    public void show() {
        if (!isVisible()) {
            setVisibility(VISIBLE);

            updateAll();
        }
        // Call hideAfterTimeout even if already visible to reset the timeout.
        hideAfterTimeout();
    }


    public void setPlayer(ConePlayer player) {
        if (this.player == player) {
            return;
        }
        if (this.player != null) {
            this.player.removePlayerStateChangeListener(componentListener);
        }
        this.player = player;
        if (player != null) {
            player.addPlayerStateChangeListener(componentListener);
        }
        updateAll();
    }

    private void updateAll() {
        updateButtons();
    }

    private void updateButtons() {
        if (!isVisible() || !isAttachedToWindow) {
            return;
        }
        int state = player.getPlaybackState();
        if (state == ConePlayer.STATE_PLAYING) {
            playButton.setVisibility(GONE);
            pauseButton.setVisibility(VISIBLE);
            repeatButton.setVisibility(GONE);
        } else if (state == ConePlayer.STATE_ENDED || state == ConePlayer.STATE_STOP) {
            playButton.setVisibility(GONE);
            pauseButton.setVisibility(GONE);
            repeatButton.setVisibility(VISIBLE);
        } else {
            playButton.setVisibility(VISIBLE);
            pauseButton.setVisibility(GONE);
            repeatButton.setVisibility(GONE);
        }
    }

    private void seekTo(long positionMs) {
        player.seekTo(positionMs);
    }

    /**
     * Returns whether the controller is currently visible.
     */
    public boolean isVisible() {
        return getVisibility() == VISIBLE;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
        if (hideAtMs != TIME_UNSET) {
            long delayMs = hideAtMs - SystemClock.uptimeMillis();
            if (delayMs <= 0) {
                hide();
            } else {
                postDelayed(hideAction, delayMs);
            }
        }
        updateAll();
    }

    public void setShowTimeoutMs(int showTimeoutMs) {
        this.showTimeoutMs = showTimeoutMs;
    }

    public void play() {
        player.play();
    }

    public void pause() {
        player.pause();
    }

    public void fastForward() {
        if (fastForwardMs <= 0) {
            return;
        }

        long durationMs = player.getDuration();
        long seekPositionMs = player.getCurrentPosition() + fastForwardMs;
        if (durationMs != TIME_UNSET) {
            seekPositionMs = Math.min(seekPositionMs, durationMs);
        }
        seekTo(seekPositionMs);
    }

    public void rewind() {
        if (rewindMs <= 0) {
            return;
        }

        seekTo(Math.max(player.getCurrentPosition() - rewindMs, 0));
    }

    public void repeat() {
        player.repeat();
    }

    private final class ComponentListener
            implements ConePlayer.PlayerStateChangeListener,
            View.OnClickListener {

        @Override
        public void onPlayerStateChanged(int state) {
            updateAll();
        }

        @Override
        public void onClick(View v) {
            if (v == playButton) play();
            else if (v == pauseButton) pause();
            else if (v == fastForwardButton) fastForward();
            else if (v == rewindButton) rewind();
            else if (v == repeatButton) repeat();
        }
    }
}