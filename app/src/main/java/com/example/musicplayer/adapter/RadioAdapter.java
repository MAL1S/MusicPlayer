package com.example.musicplayer.adapter;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.example.musicplayer.App;
import com.example.musicplayer.Player;
import com.example.musicplayer.R;
import com.example.musicplayer.database.AppDatabase;
import com.example.musicplayer.database.Radio;
import com.example.musicplayer.database.Track;
import com.example.musicplayer.music.RadioActivity;
import com.example.musicplayer.notification.CreateNotification;

import java.util.ArrayList;
import java.util.List;

public class RadioAdapter extends Adapter<RadioAdapter.ViewHolder> {
    private List<Radio> data = new ArrayList<>();
    Activity activity;
    Context context;
    Player player;
    AppDatabase db;

    public RadioAdapter(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;

        player = App.getApp().getPlayer();
        db = App.getApp().getDb();
    }

    public void setData(List<Radio> mData) {
        data.clear();
        data.addAll(mData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        RadioAdapter.ViewHolder holder = new RadioAdapter.ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.info.setText(data.get(position).getName());
        holder.info.setSelected(true);
        holder.info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasConnection()) {
                    Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (player.getCurrentRadio() == position) return;

                App.getApp().createLoadingDialog(activity);
                App.getApp().getLoadingDialog().startLoadingAnimation();

                player.setCurrentRadio(position);
                context.stopService(App.getApp().getPlayerService());
                player.setIsPlaying(true);
                player.setIsAnotherSong(true);
                player.setSource(player.getCurrentRadioTrack().getPath());

                for (Radio radio : db.radioDao().getAll()) {
                    Log.d("testing", radio.getId() + " " + radio.getPath());
                }

                context.startService(App.getApp().getPlayerService());
                createRadioNotification(R.drawable.ic_pause);
                for (Track track : db.trackDao().getAll()) db.trackDao().updatePlaying(track.getId(), false);

                for (Radio radio : db.radioDao().getAll()) {
                    db.radioDao().updatePlaying(radio.getId(), false);
                    if (data.get(position).getId() == radio.getId()) db.radioDao().updatePlaying(radio.getId(), true);
                }

                setData(db.radioDao().getAll());
                notifyDataSetChanged();

                player.setIsShuffled(false);
            }
        });
        holder.info.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                RadioActivity.radioToRemove = position;
                return false;
            }
        });

        if (data.get(position).isPlaying()) holder.image.setImageResource(R.drawable.ic_play);
        else holder.image.setImageResource(R.drawable.ic_music);
    }

    void createRadioNotification(int index) {
        CreateNotification.createNotification(context,
                new Track(player.getCurrentRadioTrack().getName(), player.getCurrentRadioTrack().getPath()),
                index,
                player.getCurrentRadio(),
                player.getRadioListSize() - 1);
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView info;
        ImageView image;

        ViewHolder(View view) {
            super(view);
            this.info = view.findViewById(R.id.txtSongName);
            this.image = view.findViewById(R.id.imgSong);
        }
    }

    boolean hasConnection() {

        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
