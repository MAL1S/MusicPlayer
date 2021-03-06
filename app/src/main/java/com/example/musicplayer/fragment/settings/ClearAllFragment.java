package com.example.musicplayer.fragment.settings;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.musicplayer.player.Player;
import com.example.musicplayer.R;
import com.example.musicplayer.app.App;
import com.example.musicplayer.database.AppDatabase;
import com.example.musicplayer.database.entities.Track;

public class ClearAllFragment extends Fragment {
    Button clear;
    AppDatabase db;
    Player player;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clear_all, container, false);

        init(view);

        return view;
    }

    private void init(View view) {
        clear = view.findViewById(R.id.clearButton);
        player = App.getApp().getPlayer();

        db = App.getApp().getDb();

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContext().stopService(App.getApp().getPlayerService());

                db.radioDao().deleteAll();
                player.clearRadioIndexes();
                player.setCurrentRadio(-1);

                db.trackPlaylistDao().deleteAll();

                db.playlistDao().deleteAll();
                player.clearPlaylistIndexes();

                for (Track track : db.trackDao().getAll()) {
                    db.trackDao().updateLiked(track.getId(), false);
                    if (track.isLiked()) Log.d("testing", track.getId() + " " + track.getName() + " " + track.isLiked());
                }
            }
        });
    }
}