package com.example.easytutomusicapp;

import android.media.MediaPlayer;

public class MyMediaPlayer {
    private static MediaPlayer instance;
    public static int currentIndex = -1;

    public static MediaPlayer getInstance() {
        if (instance == null) {
            instance = new MediaPlayer();
        }
        return instance;
    }

    public static void reset() {
        if (instance != null) {
            try {
                instance.stop();
                instance.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            instance = null;
        }
    }
}
