package com.example.easytutomusicapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView noMusicTextView;
    ArrayList<AudioModel> songsList = new ArrayList<>();
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        noMusicTextView = findViewById(R.id.no_songs_text);
        songsList = new ArrayList<>();

        // Always add default song first
        addDefaultSong();

        // Check and request storage permission
        checkAndRequestPermission();
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above, we don't need storage permission for media
            loadSongs();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Show explanation if needed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, 
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, 
                            "Storage permission is needed to access your music files", 
                            Toast.LENGTH_LONG).show();
                }
                // Request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                loadSongs();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, load songs
                loadSongs();
            } else {
                // Permission denied, show message but keep the default song
                Toast.makeText(this, 
                        "Storage permission denied. Only sample song will be available.", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void addDefaultSong() {
        // Add multiple default songs to the list
        AudioModel[] defaultSongs = {
            new AudioModel(
                "android.resource://" + getPackageName() + "/raw/sample_song",
                "Song 1",
                "3:30"
            ),
            new AudioModel(
                "android.resource://" + getPackageName() + "/raw/sample_song2",
                "Old blues",
                "4:15"
            ),
            new AudioModel(
                "android.resource://" + getPackageName() + "/raw/sample_song3",
                "Bille Elish - Wildflower",
                "2:45"
            ),
            new AudioModel(
                "android.resource://" + getPackageName() + "/raw/sample_song4",
                "OneRepublic - Counting Stars",
                "5:00"
            ),
            new AudioModel(
                "android.resource://" + getPackageName() + "/raw/sample_song5",
                "Dua Lipa - New Rules",
                "3:55"
            ),
            new AudioModel(
                "android.resource://" + getPackageName() + "/raw/sample_song6",
                "Gotye - Somebody That I Used To Know",
                "4:20"
            )
        };
        
        // Add all default songs to the list
        for (AudioModel song : defaultSongs) {
            songsList.add(song);
        }
        
        // Update the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new MusicListAdapter(songsList, getApplicationContext()));
    }

    private void loadSongs() {
        try {
            Uri collection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            } else {
                collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            String[] projection = new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
            };

            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

            try (Cursor cursor = getApplicationContext().getContentResolver().query(
                    collection,
                    projection,
                    selection,
                    null,
                    null
            )) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        String title = cursor.getString(titleColumn);
                        String path = cursor.getString(pathColumn);
                        String duration = cursor.getString(durationColumn);

                        // Verify the file exists and is readable
                        File file = new File(path);
                        if (file.exists() && file.canRead()) {
                            AudioModel songData = new AudioModel(path, title, duration);
                            songsList.add(songData);
                        }
                    }
                }
            }

            // Update the RecyclerView with all songs
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new MusicListAdapter(songsList, getApplicationContext()));

            // Show/hide no songs message
            if (songsList.size() <= 1) { // Only default song
                noMusicTextView.setVisibility(View.VISIBLE);
            } else {
                noMusicTextView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading songs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(recyclerView != null) {
            recyclerView.setAdapter(new MusicListAdapter(songsList, getApplicationContext()));
        }
    }
}