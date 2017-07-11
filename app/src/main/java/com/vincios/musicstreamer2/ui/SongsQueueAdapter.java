package com.vincios.musicstreamer2.ui;

import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vincios.musicstreamer2.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SongsQueueAdapter extends RecyclerView.Adapter<SongsQueueAdapter.QueueViewHolder> {

    private List<MediaSessionCompat.QueueItem> mCurrentQueue;
    private QueueItemClickListener mListener;
    private long mCurrentPlayingId;

    public void setQueue(List<MediaSessionCompat.QueueItem> queue) {
        if(queue != null) {
            this.mCurrentQueue.clear();
            this.mCurrentQueue.addAll(queue);
            notifyDataSetChanged();
        }
    }

    public void setmCurrentPlayingId(long currentPlayingId) {
        this.mCurrentPlayingId = currentPlayingId;
       notifyDataSetChanged();
    }


    public interface QueueItemClickListener{
        void onQueueItemClick(int position);
    }

    public SongsQueueAdapter(QueueItemClickListener clickListener) {
        this.mCurrentQueue = Collections.synchronizedList(new ArrayList<MediaSessionCompat.QueueItem>());
        this.mListener = clickListener;
    }

    @Override
    public QueueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue, parent, false);
        return new QueueViewHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(QueueViewHolder holder, int position) {
        MediaSessionCompat.QueueItem item = mCurrentQueue.get(position);

        holder.title.setText(item.getDescription().getTitle());
        holder.artist.setText(item.getDescription().getSubtitle());
        if(mCurrentPlayingId == item.getQueueId()){
            holder.playingImage.setImageResource(R.drawable.ic_play_arrow_black_24dp);
        }else{
            holder.playingImage.setImageResource(R.mipmap.ic_note_circle);
        }

    }

    @Override
    public int getItemCount() {
        return mCurrentQueue.size();
    }


    public MediaSessionCompat.QueueItem getItemAtPosition(int position) {
        return mCurrentQueue.get(position);
    }

    public static class QueueViewHolder extends RecyclerView.ViewHolder{

        public TextView title;
        TextView artist;
        ImageView playingImage;

        public QueueViewHolder(View itemView, final QueueItemClickListener listener) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.queueItemTitle);
            artist = (TextView) itemView.findViewById(R.id.queueItemArtist);
            playingImage = (ImageView) itemView.findViewById(R.id.queueItemImage);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onQueueItemClick(getAdapterPosition());
                }
            });
        }
    }
}
