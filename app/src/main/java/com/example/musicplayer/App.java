package com.example.musicplayer;

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

    private static boolean isPlaying;
    private static int currentSong = -1;
    private static boolean isAnotherSong;
    private static Intent playerService;
    private static int currentDuration;
    private static int playerId;
    private static int mediaPlayerCurrentPosition = 0;
    private static boolean wasSongSwitched;
    private final static List<Track> trackList = new ArrayList<>();
    private final static List<Track> radioList = new ArrayList<>();
    private static MediaPlayer player;
    private static String source = ".";
    private static int currentRadio = -1;
    private final static List<Track> queue = new ArrayList<>();
    private static boolean isRepeated = false;
    private static boolean isShuffled = false;
    private static AppDatabase db;
    private static int playlistIndex = 0;
    private static int playlistToView;
    private static List<Integer> playlistIndexes = new ArrayList<>();
    private static List<Integer> selected = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        uniqueInstance = this;
        db = Room.databaseBuilder(this, AppDatabase.class, "database")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
        radioList.add(new Track("Chill-out radio", "http://air.radiorecord.ru:8102/chil_320"));
        radioList.add(new Track("Pop radio", "http://ice-the.musicradio.com/CapitalXTRANationalMP3"));
        radioList.add(new Track("Anime radio", "http://pool.anison.fm:9000/AniSonFM(320)?nocache=0.98"));
        radioList.add(new Track("Rock radio", "http://galnet.ru:8000/hard"));
        radioList.add(new Track("Dubstep radio", "http://air.radiorecord.ru:8102/dub_320"));

        if (db.playlistDao().ifExist()) {
            clearPlaylistIndexes();
            List<Playlist> list = db.playlistDao().getAll();
            for (Playlist playlist : list) {
                App.addPlaylistIndex(playlist.getId());
            }
            playlistIndex = list.get(list.size() - 1).getId();
            incPlaylistIndex();
        }
    }

    public static App getInstance() {
        return uniqueInstance;
    }

    public static AppDatabase getDb() {
        return db;
    }

    public static void addSelected(int id) {
        selected.add(id);
    }

    public static void removeSelected(int id) {
        selected.remove((Integer)id);
    }

    public static List<Integer> getSelected() {
        return selected;
    }

    public static int getSelectedIndex(int id) {
        return selected.get(id);
    }

    public static void clearSelected(int id) {
        selected.clear();
    }

    public static void addPlaylistIndex(int id) {
        playlistIndexes.add(id);
    }

    public static void removePlaylistIndex(int id) {
        playlistIndexes.remove(id);
    }

    public static int getPlaylistIndexById(int id) {
        return playlistIndexes.get(id);
    }

    public static void clearPlaylistIndexes() {
        playlistIndexes.clear();
    }

    public static List<Integer> getPlaylistIndexes() {
        return playlistIndexes;
    }

    public static int getPlaylistToView() {
        return playlistToView;
    }

    public static void setPlaylistToView(int playlistToView) {
        App.playlistToView = playlistToView;
    }

    public static int getPlaylistIndex() {
        return playlistIndex;
    }

    public static void setPlaylistIndex(int playlistIndex) {
        App.playlistIndex = playlistIndex;
    }

    public static void incPlaylistIndex() {
        playlistIndex++;
    }

    public static void setIsRepeated(boolean temp) {
        isRepeated = temp;
    }

    public static boolean isRepeated() {
        return isRepeated;
    }

    public static void setIsShuffled(boolean temp) {
        isShuffled = temp;
    }

    public static boolean isShuffled() {
        return isShuffled;
    }

    public static void addToQueue(Track track) {
        queue.add(track);
    }

    public static void clearQueue() {
        queue.clear();
    }

    public static Track getCurrentRadioTrack() {
        return radioList.get(currentRadio);
    }

    public static void setCurrentRadio(int index) {
        currentRadio = index;
    }

    public static int getCurrentRadio() {
        return currentRadio;
    }

    public static void addRadio(Track track) {
        radioList.add(track);
    }

    public static void removeRadio(int index) {
        radioList.remove(index);
    }

    public static List<Track> getRadioList() {
        return radioList;
    }

    public static int getRadioListSize() {
        return radioList.size();
    }

    public static void setSource(String source) {
        App.source = source;
    }

    public static String getSource() {
        return source;
    }

    public static void createEmptyPlayer() {
        player = new MediaPlayer();
    }

    public static int getCurrentDuration() {
        return currentDuration;
    }

    public static void setCurrentDuration(int currentDuration) {
        App.currentDuration = currentDuration;
    }

    public static void addTrack(Track track) {
        trackList.add(track);
    }

    public static void removeTrack(int index) {
        trackList.remove(index);
    }

    public static void clearTrackList() {
        trackList.clear();
    }

    public static int getQueueSize() {
        return queue.size();
    }

    public static Track getCurrentTrack() {
        if (currentSong == -1) return null;
        return queue.get(currentSong);
    }

    public static int getTrackListSize() {
        return trackList.size();
    }

    public static Track getTrackFromQueue(int index) {
        return queue.get(index);
    }

    public static Track getTrack(int index) {
        return trackList.get(index);
    }

    public static String getCurrentPath() {
        if (currentSong == -1) return "";
        return queue.get(currentSong).getPath();
    }

    public static String getCurrentTitle() {
        if (currentSong == -1) return "";
        return queue.get(currentSong).getName();
    }

    public static int getPlayerId() {
        return playerId;
    }

    public static void setPlayerId(int playerId) {
        App.playerId = playerId;
    }

    public static boolean isPlaying() {
        return isPlaying;
    }

    public static void setIsPlaying(boolean isPlaying) {
        App.isPlaying = isPlaying;
    }

    public static int getCurrentSong() {
        return currentSong;
    }

    public static void setCurrentSong(int currentSong) {
        App.currentSong = currentSong;
    }

    public static boolean isAnotherSong() {
        return isAnotherSong;
    }

    public static void setIsAnotherSong(boolean isAnotherSong) {
        App.isAnotherSong = isAnotherSong;
    }

    public static Intent getPlayerService() {
        return playerService;
    }

    public static void setPlayerService(Intent playerService) {
        App.playerService = playerService;
    }

    public static int getMediaPlayerCurrentPosition() {
        return mediaPlayerCurrentPosition;
    }

    public static void setMediaPlayerCurrentPosition(int mediaPlayerCurrentPosition) {
        App.mediaPlayerCurrentPosition = mediaPlayerCurrentPosition;
    }

    public static boolean wasSongSwitched() {
        return wasSongSwitched;
    }

    public static void setWasSongSwitched(boolean wasSongSwitched) {
        App.wasSongSwitched = wasSongSwitched;
    }

    public static List<Track> getTrackList() {
        return trackList;
    }

    public static synchronized MediaPlayer getPlayer() {
        return player;
    }

    public static void setPlayer(MediaPlayer player) {
        App.player = player;
    }
}
