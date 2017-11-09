package org.noobs2d.coneplayer.sample;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.noobs2d.coneplayer.ConePlayer;
import org.noobs2d.coneplayer.ConePlayerFactory;
import org.noobs2d.coneplayer.Media;
import org.noobs2d.coneplayer.ui.ConePlayerView;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class MainActivity
        extends AppCompatActivity
        implements ConePlayer.PlayerStateChangeListener, ConePlayer.VideoListener {

    public static final String URL = "http://download.blender.org/peach/bigbuckbunny_movies/big_buck_bunny_720p_h264.mov";
    public static final String STATES[] = {"STATE_IDLE", "STATE_PLAYING", "STATE_PAUSE", "STATE_STOP", "STATE_ENDED"};

    private ConePlayerView conePlayerView;
    private ImageView previewImageView;
    private TextView timeTextView;
    private TextView stateTextView;
    private ConePlayer player;
    private long time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        conePlayerView = findViewById(R.id.cone_player_view);
        previewImageView = findViewById(R.id.preview_image_view);
        timeTextView = findViewById(R.id.time_text_view);
        stateTextView = findViewById(R.id.state_text_view);
    }

    @Override
    public void onStart() {
        super.onStart();
        initializePlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player == null)
            initializePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void initializePlayer() {
        Uri uri = Uri.parse(URL);

        player = ConePlayerFactory.newConePlayer(this);
        player.prepare(new Media(uri));
        conePlayerView.setPlayer(player);
        player.addPlayerStateChangeListener(this);
        player.addVideoListener(this);
        player.play();
        player.seekTo(time);
    }

    private void releasePlayer() {
        if (player != null) {
            time = player.getCurrentPosition();

            conePlayerView.setPlayer(null);
            player.removeVideoListener(this);
            player.removePlayerStateChangeListener(this);
            player.release();

            player = null;
        }
    }

    @Override
    public void onPlayerStateChanged(int state) {
        long millis = player.getCurrentPosition();
        String position = String.format("%02d:%02d",
                MILLISECONDS.toMinutes(millis),
                MILLISECONDS.toSeconds(millis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(millis))
        );

        millis = player.getDuration();
        String duration = String.format("%02d:%02d",
                MILLISECONDS.toMinutes(millis),
                MILLISECONDS.toSeconds(millis) - MINUTES.toSeconds(MILLISECONDS.toMinutes(millis))
        );

        timeTextView.setText(getString(R.string.time, position, duration));
        stateTextView.setText(getString(R.string.state, STATES[state - 1]));
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {

    }

    @Override
    public void onRenderedFirstFrame() {
        previewImageView.setVisibility(View.GONE);
    }
}