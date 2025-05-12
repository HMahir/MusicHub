package com.example.easytutomusicapp;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MusicPlayerActivity";
    TextView titleTv, currentTimeTv, totalTimeTv;
    SeekBar seekBar;
    ImageView pausePlay, nextBtn, previousBtn, musicIcon;
    ArrayList<AudioModel> songsList;
    AudioModel currentSong;
    MediaPlayer mediaPlayer;
    int x = 0;
    private Handler handler = new Handler();
    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                try {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    currentTimeTv.setText(convertToMMSS(mediaPlayer.getCurrentPosition() + ""));
                    if (mediaPlayer.isPlaying()) {
                        pausePlay.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
                        musicIcon.setRotation(x++);
                    } else {
                        pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
                        musicIcon.setRotation(0);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating seekbar: " + e.getMessage());
                }
            }
            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        initializeViews();
        setupListeners();
        loadSongList();
    }

    private void initializeViews() {
        titleTv = findViewById(R.id.song_title);
        currentTimeTv = findViewById(R.id.current_time);
        totalTimeTv = findViewById(R.id.total_time);
        seekBar = findViewById(R.id.seek_bar);
        pausePlay = findViewById(R.id.pause_play);
        nextBtn = findViewById(R.id.next);
        previousBtn = findViewById(R.id.previous);
        musicIcon = findViewById(R.id.music_icon_big);
        titleTv.setSelected(true);
    }

    private void setupListeners() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    try {
                        mediaPlayer.seekTo(progress);
                    } catch (Exception e) {
                        Log.e(TAG, "Error seeking: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadSongList() {
        try {
            songsList = (ArrayList<AudioModel>) getIntent().getSerializableExtra("LIST");
            if (songsList == null || songsList.isEmpty()) {
                Toast.makeText(this, "No songs available", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            initializeMediaPlayer();
            setResourcesWithMusic();
        } catch (Exception e) {
            Log.e(TAG, "Error loading song list: " + e.getMessage());
            Toast.makeText(this, "Error loading music player", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeMediaPlayer() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(mp -> {
                // Auto play next song when current song completes
                playNextSong();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MediaPlayer: " + e.getMessage());
            Toast.makeText(this, "Error initializing player", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateSeekBar);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateSeekBar);
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    void setResourcesWithMusic() {
        try {
            currentSong = songsList.get(MyMediaPlayer.currentIndex);
            titleTv.setText(currentSong.getTitle());
            totalTimeTv.setText(convertToMMSS(currentSong.getDuration()));

            pausePlay.setOnClickListener(v -> pausePlay());
            nextBtn.setOnClickListener(v -> playNextSong());
            previousBtn.setOnClickListener(v -> playPreviousSong());

            playMusic();
        } catch (Exception e) {
            Log.e(TAG, "Error setting resources: " + e.getMessage());
            Toast.makeText(this, "Error loading song", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void playMusic() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.reset();
            } else {
                mediaPlayer = new MediaPlayer();
            }

            String path = currentSong.getPath();
            if (path.startsWith("android.resource://")) {
                // Handle resource URI
                Uri uri = Uri.parse(path);
                try {
                    mediaPlayer.setDataSource(this, uri);
                } catch (Exception e) {
                    Log.e(TAG, "Error playing resource: " + e.getMessage());
                    Toast.makeText(this, "Error playing sample song", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                // Handle file path
                File file = new File(path);
                if (!file.exists() || !file.canRead()) {
                    throw new IOException("Cannot read file: " + path);
                }
                mediaPlayer.setDataSource(path);
            }

            // Set up error listener
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                Toast.makeText(this, "Error playing song: " + what, Toast.LENGTH_SHORT).show();
                finish();
                return true;
            });

            // Set up completion listener
            mediaPlayer.setOnCompletionListener(mp -> {
                playNextSong();
            });

            // Prepare and start
            mediaPlayer.prepare();
            mediaPlayer.start();
            seekBar.setProgress(0);
            seekBar.setMax(mediaPlayer.getDuration());
        } catch (IOException e) {
            Log.e(TAG, "Error playing music: " + e.getMessage());
            Toast.makeText(this, "Error playing song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage());
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void playNextSong() {
        if (MyMediaPlayer.currentIndex == songsList.size() - 1) {
            Toast.makeText(this, "Last song", Toast.LENGTH_SHORT).show();
            return;
        }
        MyMediaPlayer.currentIndex += 1;
        setResourcesWithMusic();
    }

    private void playPreviousSong() {
        if (MyMediaPlayer.currentIndex == 0) {
            Toast.makeText(this, "First song", Toast.LENGTH_SHORT).show();
            return;
        }
        MyMediaPlayer.currentIndex -= 1;
        setResourcesWithMusic();
    }

    private void pausePlay() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in pausePlay: " + e.getMessage());
            Toast.makeText(this, "Error controlling playback", Toast.LENGTH_SHORT).show();
        }
    }

    public static String convertToMMSS(String duration) {
        try {
            Long millis = Long.parseLong(duration);
            return String.format("%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
        } catch (NumberFormatException e) {
            return "00:00";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBar);
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaPlayer: " + e.getMessage());
            }
            mediaPlayer = null;
        }
    }
}