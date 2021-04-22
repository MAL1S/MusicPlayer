package com.example.musicplayer;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicplayer.Services.BackgroundMusicService;
import com.example.musicplayer.Services.OnClearFromRecentService;
import com.example.musicplayer.adapter.TrackAdapter;
import com.example.musicplayer.music.RadioActivity;
import com.example.musicplayer.music.SongActivity;
import com.example.musicplayer.music.Track;
import com.example.musicplayer.notification.CreateNotification;
import com.example.musicplayer.notification.Playable;

import java.io.File;

public class MainActivity extends AppCompatActivity implements Playable {
    private static final String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_PERMISSIONS = 12345;
    private static final int PERMISSIONS_COUNT = 2;
    private boolean isMusicPlayerInit = false;
    //public static int currentSong = 0;
    private final int REQUEST_CODE_PERMISSION_WRITE_STORAGE = 1;
    //public static String playablePath = "";
    Button prev, play, next;
    //public static Intent service;
    //public static int mediaPlayerCurrentPosition;
    TextView songName;
    ImageView imageView;
    //public static boolean isAnotherSong = false;
    //public static boolean isPlaying = false;
    //public static boolean wasSongSwitched = false;
    NotificationManager notificationManager;
    //public static List<Track> trackList = new ArrayList<>();
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mActivityTitle;
    Intent intentNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mActivityTitle = getTitle().toString();

        addDrawerItems();
        setupDrawer();

        if( getSupportActionBar() !=null ) {
            getSupportActionBar().setTitle("EarFeeder");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF362E")));
        }

        App.getInstance();
        if (!arePermissionsDenied()) {
            play = findViewById(R.id.play);
            prev = findViewById(R.id.previous);
            next = findViewById(R.id.next);
            songName = findViewById(R.id.songName);
            songName.setSelected(true);
            songName.setText("");

            if (!App.getCurrentPath().equals("")) songName.setText(App.getCurrentTitle());

            if (App.isPlaying()) {
                play.setBackgroundResource(R.drawable.ic_pause);
            } else {
                play.setBackgroundResource(R.drawable.ic_play);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannel();
                registerReceiver(broadcastReceiver, new IntentFilter("TRACKS_TRACKS"));
                intentNotification = new Intent(getBaseContext(), OnClearFromRecentService.class);
                startService(intentNotification);
            }

            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (App.getCurrentPath().equals("")) return;
                    App.setIsAnotherSong(false);
                    if (App.isPlaying()) {
                        //stopService(MainActivity.service);
                        //play.setBackgroundResource(R.drawable.ic_play);
                        onTrackPause();
                    } else {
                        //startService(MainActivity.service);
                        //play.setBackgroundResource(R.drawable.ic_pause);
                        onTrackPlay();
                    }
                }
            });
            prev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (App.getCurrentPath().equals("")) return;
                    if (App.getCurrentSong() - 1 >= 0) {
//                        wasSongSwitched = true;
//                        playablePath = trackList.get(--currentSong).getPath();
//                        songName.setText(playablePath.substring(playablePath.lastIndexOf('/')+1));
//                        stopService(service);
//                        isAnotherSong = true;
//                        startService(service);
                        onTrackPrevious();
                    }
                }
            });
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (App.getCurrentPath().equals("")) return;
                    if (App.getCurrentSong() + 1 < App.getTrackListSize()) {
//                        wasSongSwitched = true;
//                        playablePath = trackList.get(++currentSong).getPath();
//                        songName.setText(playablePath.substring(playablePath.lastIndexOf('/')+1));
//                        stopService(service);
//                        isAnotherSong = true;
//                        startService(service);
                        onTrackNext();
                    }
                }
            });
            songName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (App.getCurrentPath().equals("")) return;
                    if (App.isPlaying()) App.setMediaPlayerCurrentPosition(App.getPlayer().getCurrentPosition());
                    Intent intent = new Intent(MainActivity.this, SongActivity.class);
                    //stopService(intentNotification);
                    startActivity(intent);
                }
            });
            if (App.getPlayerService() == null) App.setPlayerService(new Intent(this, BackgroundMusicService.class));
            onResume();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                REQUEST_CODE_PERMISSION_WRITE_STORAGE);
        }
    }

    private void addDrawerItems() {
        String[] osArray = { "Радио", "Плейлисты" };
        //mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);
        mAdapter = new ArrayAdapter<String>(this, R.layout.left_menu_textview, osArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        Intent intent = new Intent(MainActivity.this, RadioActivity.class);
                        startActivity(intent);
                        break;
                    case 1:
                        Toast.makeText(MainActivity.this, "1", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close);

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NewApi")
    private boolean arePermissionsDenied() {
        for (int i = 0; i < PERMISSIONS_COUNT; i++) {
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (arePermissionsDenied()) {
//            ((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
//            recreate();
//        } else {
//            onResume();
//        }
         switch (requestCode) {
             case REQUEST_CODE_PERMISSION_WRITE_STORAGE:
                 if (grantResults.length > 0
                 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onResume();
                 } else {
                    Toast.makeText(this, "NO", Toast.LENGTH_SHORT).show();
                 }
         }
    }

    private void addMusicFilesFrom(String dirPath) {
        final File musicDir = new File(dirPath);
        if (!musicDir.exists()) {
            musicDir.mkdir();
            return;
        }
        final File[] files = musicDir.listFiles();
        for (File file : files) {
            final String path = file.getAbsolutePath();
            if (path.endsWith(".mp3") || path.endsWith(".wav")) {
                App.addTrack(new Track(file.getName().replace(".mp3", "").replace(".wav", ""),
                        path));
            }
        }
    }

    @SuppressLint("NewApi")
    private void fillMusicList() {
        App.clearTrackList();
        addMusicFilesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS)));
        addMusicFilesFrom(String.valueOf(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
        if (App.isPlaying()) {
            play.setBackgroundResource(R.drawable.ic_pause);
        } else {
            play.setBackgroundResource(R.drawable.ic_play);
        }
        if (!isMusicPlayerInit) {
            final ListView listView = findViewById(R.id.listView);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (App.isPlaying()) stopService(App.getPlayerService());
                    App.setIsAnotherSong(true);
                    play.setBackgroundResource(R.drawable.ic_pause);
                    App.setCurrentSong(position);
                    songName.setText(App.getCurrentTitle());
                    startService(App.getPlayerService());
                    //creating notification
                    CreateNotification.createNotification(MainActivity.this,
                            App.getCurrentTrack(),
                            R.drawable.ic_pause, 1, App.getTrackListSize()-1);
                }
            });
            fillMusicList();
            final TrackAdapter trackAdapter = new TrackAdapter();
            trackAdapter.setData(App.getTrackList());
            listView.setAdapter(trackAdapter);

            isMusicPlayerInit = true;
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CreateNotification.CHANNEL_ID,
                    "KOD DEV", NotificationManager.IMPORTANCE_LOW);

            notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("actionName");

            switch (action) {
                case CreateNotification.ACTION_PREVIOUS:
                    onTrackPrevious();
                    break;
                case CreateNotification.ACTION_PLAY:
                    if (App.isPlaying()) onTrackPause();
                    else onTrackPlay();
                    break;
                case CreateNotification.ACTION_NEXT:
                    onTrackNext();
                    break;
            }
        }
    };

    @Override
    public void onTrackPrevious() {
        App.setWasSongSwitched(true);
        App.setCurrentSong(App.getCurrentSong()-1);
        stopService(App.getPlayerService());
        App.setIsAnotherSong(true);
        startService(App.getPlayerService());

        CreateNotification.createNotification(MainActivity.this,
                App.getCurrentTrack(),
                R.drawable.ic_pause,
                App.getCurrentSong(),
                App.getTrackListSize()-1);
        songName.setText(App.getCurrentTitle());
    }

    @Override
    public void onTrackPlay() {
        CreateNotification.createNotification(MainActivity.this,
                App.getCurrentTrack(),
                R.drawable.ic_pause,
                App.getCurrentSong(),
                App.getTrackListSize()-1);
        App.setIsPlaying(true);
        App.setIsAnotherSong(false);
        startService(App.getPlayerService());
        play.setBackgroundResource(R.drawable.ic_pause);
        songName.setText(App.getCurrentTitle());
    }

    @Override
    public void onTrackPause() {
        CreateNotification.createNotification(MainActivity.this,
                App.getCurrentTrack(),
                R.drawable.ic_play,
                App.getCurrentSong(),
                App.getTrackListSize()-1);
        App.setIsPlaying(false);
        App.setIsAnotherSong(false);
        stopService(App.getPlayerService());
        play.setBackgroundResource(R.drawable.ic_play);
        songName.setText(App.getCurrentTitle());
    }

    @Override
    public void onTrackNext() {
        App.setWasSongSwitched(true);
        App.setCurrentSong(App.getCurrentSong()+1);
        stopService(App.getPlayerService());
        App.setIsAnotherSong(true);
        startService(App.getPlayerService());

        CreateNotification.createNotification(MainActivity.this,
                App.getCurrentTrack(),
                R.drawable.ic_pause,
                App.getCurrentSong(),
                App.getTrackListSize()-1);
        songName.setText(App.getCurrentTitle());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.cancelAll();
        }
        unregisterReceiver(broadcastReceiver);
    }
}