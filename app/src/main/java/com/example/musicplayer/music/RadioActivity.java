package com.example.musicplayer.music;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.App;
import com.example.musicplayer.LoadingDialog;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.Player;
import com.example.musicplayer.R;
import com.example.musicplayer.RecyclerItemClickListener;
import com.example.musicplayer.Services.OnClearFromRecentService;
import com.example.musicplayer.adapter.RadioAdapter;
import com.example.musicplayer.database.AppDatabase;
import com.example.musicplayer.database.Radio;
import com.example.musicplayer.database.Track;
import com.example.musicplayer.notification.CreateNotification;
import com.example.musicplayer.notification.Playable;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RadioActivity extends AppCompatActivity implements Playable {
    private static final String DEBUG_TAG = "NetworkStatusExample";
    Player player;
    Button setRadioButton, backButton;
    EditText radioUrl, radioTitle;
    RecyclerView radioList;
    RadioAdapter adapter;
    Button play, prev, next;
    TextView title;
    NotificationManager notificationManager;
    boolean running = false;
    public static int radioToRemove;
    AppDatabase db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);
        player = App.getApp().getPlayer();
        if (!running) {
            running = true;
            startTitleThread();
        }

        init();

        App.getApp().setCurrentActivity(this);

        if (player.isPlaying()) {
            play.setBackgroundResource(R.drawable.ic_pause);
        } else {
            play.setBackgroundResource(R.drawable.ic_play);
        }

        if (!player.getSource().equals(".") && player.getCurrentRadio() != -1) {
            title.setText(player.getCurrentRadioTrack().getName());
        }
        else if (player.getSource().equals(".") && player.getCurrentQueueTrack() != -1) {
            title.setText(player.getCurrentTitle());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
            registerReceiver(broadcastReceiver, new IntentFilter("TRACKS_TRACKS"));
            startService(new Intent(getBaseContext(), OnClearFromRecentService.class));
        }
    }

    void init() {
        setRadioButton = findViewById(R.id.setRadioButton);
        radioUrl = findViewById(R.id.radioUrl);
        radioList = findViewById(R.id.radioList);
        backButton = findViewById(R.id.backButton);
        radioTitle = findViewById(R.id.radioTitle);
        play = findViewById(R.id.play);
        title = findViewById(R.id.songName);
        prev = findViewById(R.id.previous);
        next = findViewById(R.id.next);
        title.setSelected(true);

        db = App.getApp().getDb();

        adapter = new RadioAdapter(RadioActivity.this, this);
        adapter.setData(db.radioDao().getAll());
        radioList.setAdapter(adapter);
        radioList.setLayoutManager(new LinearLayoutManager(this));
        radioList.setHasFixedSize(false);
        registerForContextMenu(radioList);

        setRadioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new createRadio().execute(radioUrl.getText().toString());
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.getMediaPlayer() == null) return;
                if (player.isPlaying()) {
                    if (player.getSource().equals(".")) {
                        createTrackNotification(R.drawable.ic_play);
                    }
                    else {
                        createRadioNotification(R.drawable.ic_play);
                    }

                    player.setIsPlaying(false);
                    play.setBackgroundResource(R.drawable.ic_play);
                    stopService(App.getApp().getPlayerService());
                } else {
                    if (player.getMediaPlayer() == null) player.setCurrentQueueTrack(0);
                    if (player.getSource().equals(".")) {
                        createTrackNotification(R.drawable.ic_pause);
                    }
                    else {
                        createRadioNotification(R.drawable.ic_pause);
                    }

                    player.setIsPlaying(true);
                    play.setBackgroundResource(R.drawable.ic_pause);
                    startService(App.getApp().getPlayerService());
                }
                player.setIsAnotherSong(false);
            }
        });
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.getMediaPlayer() == null) return;

                if (player.getSource().equals(".") && player.getCurrentQueueTrack() - 1 >= 0) {
                    moveTrack(-1);
                    createTrackNotification(R.drawable.ic_pause);
                }
                else if (!player.getSource().equals(".") && player.getCurrentRadio() - 1 >= 0) {
                    moveRadio(-1);
                    createRadioNotification(R.drawable.ic_pause);
                }
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.getMediaPlayer() == null) return;

                if (player.getSource().equals(".") && player.getCurrentQueueTrack() + 1 < player.getQueueSize()) {
                    moveTrack(1);
                    createTrackNotification(R.drawable.ic_pause);
                }
                else if (!player.getSource().equals(".") && player.getCurrentRadio() +1 < player.getRadioListSize()) {
                    moveRadio(1);
                    createRadioNotification(R.drawable.ic_pause);
                }
            }
        });
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!player.getSource().equals(".")) return;
                if (player.getCurrentPath().equals("")) return;
                if (player.isPlaying()) player.setMediaPlayerCurrentPosition(player.getCurrentPosition());
                Intent intent = new Intent(getApplicationContext(), SongActivity.class);
                startActivity(intent);
            }
        });
    }

    private class createRadio extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (!hasConnection()) {
                Toast.makeText(RadioActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                return;
            }

            App.getApp().createLoadingDialog(RadioActivity.this);
            App.getApp().getLoadingDialog().startLoadingAnimation();
        }

        @Override
        protected String doInBackground(String... urls) {
            URL url = null;
            try {
                url = new URL(urls[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("HEAD");
                con.connect();
                if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    con.disconnect();
                    throw new Exception();
                }
                con.disconnect();
                return url.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            App.getApp().dismissLoading();
            App.getApp().nullLoading();
            return null;
        }

        @Override
        protected void onPostExecute(String url) {
            if (url == null) {
                Toast.makeText(RadioActivity.this, "Wrong URL", Toast.LENGTH_SHORT).show();
                return;
            }
            player.addRadio(new Radio(player.getRadioIndex(), radioTitle.getText().toString(), radioUrl.getText().toString()));
            player.addRadioIndex(player.getRadioIndex());
            int index = player.getRadioIndex();
            player.incRadioIndex();

            stopService(App.getApp().getPlayerService());
            player.setSource(url.toString());
            player.setCurrentRadio(player.getRadioListSize() - 1);
            adapter.setData(db.radioDao().getAll());
            adapter.notifyDataSetChanged();
            player.setIsPlaying(true);
            player.setIsAnotherSong(true);
            startService(App.getApp().getPlayerService());

            for (Track track : db.trackDao().getAll()) db.trackDao().updatePlaying(track.getId(), false);

            for (Radio radio : db.radioDao().getAll()) {
                db.radioDao().updatePlaying(radio.getId(), false);
            }
            db.radioDao().updatePlaying(index, true);

            adapter.setData(db.radioDao().getAll());
            adapter.notifyDataSetChanged();
        }
    }

    void moveTrack(int direction) {
        player.setWasSongSwitched(true);
        player.setCurrentQueueTrack(player.getCurrentQueueTrack() + direction);
        stopService(App.getApp().getPlayerService());
        player.setIsAnotherSong(true);
        updateTitle();
        startService(App.getApp().getPlayerService());
    }

    void moveRadio(int direction) {
        App.getApp().createLoadingDialog(App.getApp().getCurrentActivity());
        App.getApp().getLoadingDialog().startLoadingAnimation();

        stopService(App.getApp().getPlayerService());
        player.setCurrentRadio(player.getCurrentRadio() + direction);
        player.setSource(player.getCurrentRadioTrack().getPath());
        player.setIsAnotherSong(true);
        player.setWasSongSwitched(true);
        updateTitle();
        startService(App.getApp().getPlayerService());
    }

    void updateTitle() {
        if (player.getSource().equals(".") && !title.getText().equals(player.getCurrentTitle())) {
            title.setText(player.getCurrentTitle());
        }
        else if (!player.getSource().equals(".") && !title.getText().equals(player.getCurrentRadioTrack().getName())) title.setText(player.getCurrentRadioTrack().getName());
    }

    void createTrackNotification(int index) {
        CreateNotification.createNotification(getApplicationContext(),
                player.getCurrentTrack(),
                index,
                player.getCurrentQueueTrack(),
                player.getQueueSize()-1);
    }

    void createRadioNotification(int index) {
        CreateNotification.createNotification(getApplicationContext(),
                new Track(player.getCurrentRadioTrack().getName(), player.getCurrentRadioTrack().getPath()),
                index,
                player.getCurrentRadio(),
                player.getRadioListSize() - 1);
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getTitle().equals("Delete")) {
            if (player.getCurrentRadio() >= 1) {
                if (player.getCurrentRadio() == radioToRemove) {
                    stopService(App.getApp().getPlayerService());
                    play.setBackgroundResource(R.drawable.ic_play);
                    player.setSource(".");
                    player.setCurrentRadio(-1);
                }
                if (player.getCurrentRadio() > radioToRemove) player.setCurrentRadio(player.getCurrentRadio() - 1);
            }
            else {
                stopService(App.getApp().getPlayerService());
                play.setBackgroundResource(R.drawable.ic_play);
                player.setSource(".");
            }

            player.removeRadio(player.getRadioIndexById(radioToRemove));
            player.removeRadioIndex(radioToRemove);
            adapter.setData(db.radioDao().getAll());
            adapter.notifyDataSetChanged();

            for (int item1 : player.getRadioIndexes()) {
                Log.d("testing", item1+"");
            }
        }
        return true;
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
                    if (!player.isPlaying()) onTrackPause();
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
        updateTitle();
        play.setBackgroundResource(R.drawable.ic_pause);
    }

    @Override
    public void onTrackPlay() {
        play.setBackgroundResource(R.drawable.ic_pause);
    }

    @Override
    public void onTrackPause() {
        play.setBackgroundResource(R.drawable.ic_play);
    }

    @Override
    public void onTrackNext() {
        updateTitle();
        play.setBackgroundResource(R.drawable.ic_pause);
    }

    private void startTitleThread() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable(){
                        public void run() {
                            if (player.getMediaPlayer() == null) return;
                            updateTitle();
                            if (player.isPlaying()) play.setBackgroundResource(R.drawable.ic_pause);
                            else play.setBackgroundResource(R.drawable.ic_play);
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }

    boolean hasConnection() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifiConn = false;
        boolean isMobileConn = false;
        for (Network network : connMgr.getAllNetworks()) {
            NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                isWifiConn |= networkInfo.isConnected();
            }
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                isMobileConn |= networkInfo.isConnected();
            }
        }
        return isWifiConn || isMobileConn;
    }
}