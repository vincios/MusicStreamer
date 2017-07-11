package com.vincios.musicstreamer2.ui;


import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.vincios.musicstreamer2.R;
import com.vincios.musicstreamer2.connectors.Song;

import java.util.ArrayList;
import java.util.List;

public class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.SongViewHolder>{

    private static final String LOGTAG = "SongListAdapt";
    private List<Song> songs;
    private ItemClickListener listener;
    private boolean favouriteIconEnabled;



    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView artist;
        TextView length;
        TextView bitrate;
        TextView host;
        TextView size;
        ImageView star;

        public SongViewHolder(View itemView, final ItemClickListener listener) {
            super(itemView);
            title = (TextView)itemView.findViewById(R.id.songItemTitle);
            artist = (TextView)itemView.findViewById(R.id.songItemArtist);
            length = (TextView)itemView.findViewById(R.id.songItemLength);
            bitrate = (TextView)itemView.findViewById(R.id.songItemBitrate);
            host = (TextView)itemView.findViewById(R.id.songItemHost);
            size = (TextView)itemView.findViewById(R.id.songItemSize);
            star = (ImageView) itemView.findViewById(R.id.songItemStar);

            CardView cw = (CardView) itemView.findViewById(R.id.card_view);
            cw.setCardElevation(2f);


            star.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOGTAG, "Star clicked. State = " + star.getTag());
                    if(listener != null) {
                        if (Song.SongSaved.YES.equals(star.getTag())) {
                            //star.setColorFilter(null);
                            star.setImageResource(R.drawable.ic_star_transparent_24dp);
                            star.setTag(Song.SongSaved.NO);
                            listener.onSavedSongRemove(getAdapterPosition());
                        } else {
                            star.setImageResource(R.drawable.ic_star_colored_24dp);
                            // star.setColorFilter(R.color.starEnabled);
                            star.setTag(Song.SongSaved.YES);
                            listener.onSongSave(getAdapterPosition());
                        }
                    }
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null)
                        listener.onItemClick(getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(listener != null) {
                        listener.onItemLongClick(getAdapterPosition());
                        return true;
                    }
                    return false;
                }
            });
        }


    }

    public interface ItemClickListener{
        void onItemClick(int position);
        void onSongSave(int position);
        void onSavedSongRemove(int position);
        void onItemLongClick(int position);
    }

    public SongListAdapter(boolean favouriteIconEnabled) {
        songs = new ArrayList<>();
        this.favouriteIconEnabled = favouriteIconEnabled;
        this.listener = null;
    }

    public void setOnItemClickListener(ItemClickListener listener){
        this.listener = listener;
    }

    public void setItems(List<Song> songs){
        if(this.songs.size() != 0){
            this.songs.clear();
        }
        this.songs.addAll(songs);
        this.notifyDataSetChanged();
    }

    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_search, parent, false);
        return new SongViewHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, int position) {

        holder.title.setText(songs.get(position).getTitle());
        holder.artist.setText(songs.get(position).getArtist());
        holder.bitrate.setText(songs.get(position).getBitrate());
        holder.host.setText(songs.get(position).getHost());
        //holder.size.setText(songs.get(position).getSize());
        holder.size.setText("");
        holder.length.setText(songs.get(position).getLengthString());

        if(!favouriteIconEnabled)
            holder.star.setVisibility(View.INVISIBLE);
        else if(songs.get(position).isSaved()) {
            holder.star.setTag(Song.SongSaved.YES);
            holder.star.setImageResource(R.drawable.ic_star_colored_24dp);
        }else{
            holder.star.setTag(Song.SongSaved.NO);
            holder.star.setImageResource(R.drawable.ic_star_transparent_24dp);
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public Song getItemAtPosition(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return ((long) songs.get(position).getId().hashCode());
    }

    public Song removeItemAtPosition(int position){
        Song removed = songs.remove(position);
        this.notifyItemRemoved(position);
        return removed;
    }

    public void insertItemAtPosition(Song s, int position){
        songs.add(position, s);
        notifyItemInserted(position);
    }
}
