package com.example.musicplayer;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.Toast;

import androidx.room.Room;

import com.example.musicplayer.database.AppDatabase;
import com.example.musicplayer.database.Playlist;
import com.example.musicplayer.database.TrackPlaylist;
import com.example.musicplayer.music.SongActivity;
import com.example.musicplayer.database.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class App extends Application {
    private static App uniqueInstance;
    private Player player;
    private Intent playerService;
    private AppDatabase db;
    private LoadingDialog loading;
    private Activity currentActivity;

    public void setPlayerService(Intent playerService) {
        this.playerService = playerService;
    }

    public Intent getPlayerService() {
        return playerService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        uniqueInstance = this;
    }

    public static App getApp() {
        return uniqueInstance;
    }

    public synchronized AppDatabase getDb() {
        if (db == null){
            db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return db;
    }

    public synchronized Player getPlayer(){
        if (player == null){
            player = new Player();
        }
        return player;
    }

    //Loading circle
    public void createLoadingDialog(Activity activity) {
        loading = new LoadingDialog(activity);
    }

    public void dismissLoading() {
        this.loading.dismissDialog();
    }

    public LoadingDialog getLoadingDialog() {
        return loading;
    }

    public void nullLoading() {
        loading = null;
    }
    //Loading circle


    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }
}
