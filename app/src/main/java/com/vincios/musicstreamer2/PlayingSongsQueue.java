package com.vincios.musicstreamer2;


import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.vincios.musicstreamer2.connectors.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayingSongsQueue {
    private static final PlayingSongsQueue ourInstance = new PlayingSongsQueue();

    public static PlayingSongsQueue getInstance() {
        return ourInstance;
    }

    private QueueListener mListener;
    private List<MediaBrowserCompat.MediaItem> mCurrentPlayingQueue;
    private int mCurrentIndex;
    private String mQueueTitle;
    private Map<String, Song> songsCache;

    private PlayingSongsQueue() {
        mCurrentPlayingQueue = Collections.synchronizedList(new ArrayList<MediaBrowserCompat.MediaItem>());
        songsCache = new HashMap<>();
        mCurrentIndex = 0;
        mQueueTitle = "";
    }

    public String getQueueTitle() {
        return mQueueTitle;
    }

    public void setQueueTitle(String mQueueTitle) {
        this.mQueueTitle = mQueueTitle;
    }


    public void setListener(QueueListener listener){
        this.mListener = listener;
    }

    public void removeListener(QueueListener listener) {
        listener = null;
    }

    public void overwriteQueue(String title, List<Song> songs){
        mCurrentPlayingQueue.clear();
        for(Song s : songs)
            mCurrentPlayingQueue.add(adaptSong(s));

        mQueueTitle = title;

        if(mListener != null){
            mListener.onQueueChanged();
        }
    }

    public void overwriteQueue(String title, Song song){
        mCurrentPlayingQueue.clear();
        mCurrentPlayingQueue.add(adaptSong(song));
        mQueueTitle = title;
        if(mListener != null){
            mListener.onQueueChanged();
        }
        setCurrentPlaying(0);
    }

    public void addToQueue(List<Song> songs, boolean autoPlay){
        for(Song s : songs)
            mCurrentPlayingQueue.add(adaptSong(s));
        if(mListener != null){
            mListener.onQueueChanged();
        }
        if(autoPlay)
            setCurrentPlaying(0);
    }

    public void addToQueue(Song song, boolean autoPlay){
        addOnIndexQueue(song, mCurrentPlayingQueue.size(), autoPlay);
    }

    public void addOnTopQueue(Song s, boolean autoPlay){
        addOnIndexQueue(s, 0, autoPlay);
    }

    public void addOnIndexQueue(Song s, int index, boolean autoPlay){
        boolean found = setCurrentPlaying(s.getId());

        if(!found) {
            mCurrentPlayingQueue.add(index, adaptSong(s));
            if (mListener != null) {
                mListener.onQueueChanged();
            }
            if (autoPlay)
                setCurrentPlaying(index);
        }
    }

    public void replaceItemOnPosition(Song s, int index, boolean autoPlay){
        if(index == -1)
            index = searchItemPosition(s.getId());

        if(index == -1)
            return;

        mCurrentPlayingQueue.set(index, adaptSong(s));
        if(mListener != null)
            mListener.onQueueChanged();

        if(autoPlay)
            setCurrentPlaying(index);
    }

    private int searchItemPosition(String id) {
        int pos = 0;
        for (MediaBrowserCompat.MediaItem item : mCurrentPlayingQueue){
            if(id.equals(item.getMediaId()))
                return pos;
            pos++;
        }
        return -1;
    }


    public List<MediaSessionCompat.QueueItem> getCurrentQueue() {
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>(mCurrentPlayingQueue.size());
        int counter = 0;
        synchronized (mCurrentPlayingQueue) {
            for (MediaBrowserCompat.MediaItem song : mCurrentPlayingQueue) {
                queue.add(new MediaSessionCompat.QueueItem(song.getDescription(), counter++));
            }
        }
        return queue;
    }



    public void setCurrentPlaying(int index){
        if(index >= 0 && index < mCurrentPlayingQueue.size()) {
            this.mCurrentIndex = index;
            if (mListener != null)
                mListener.onCurrentIndexChanged(index);
        }
    }

    public boolean setCurrentPlaying(String mediaId){
        int index = 0;
        synchronized (mCurrentPlayingQueue) {
            for (MediaBrowserCompat.MediaItem item : mCurrentPlayingQueue) {
                if (mediaId.equals(item.getMediaId())) {
                    setCurrentPlaying(index);
                    return true;
                } else
                    index++;
            }
        }

        return false;
    }

    public void incrementCurrentPlaying(int amount){
        int index = mCurrentIndex + amount;

        if(index < 0)
            index = 0;
        else if(index > mCurrentPlayingQueue.size())
            index %= mCurrentPlayingQueue.size();

        setCurrentPlaying(index);
    }

    public int nextItemsCount(){
        return (mCurrentPlayingQueue.size() - 1) - mCurrentIndex;
    }

    public MediaSessionCompat.QueueItem getCurrentPlaying(){
        return new MediaSessionCompat.QueueItem(
                mCurrentPlayingQueue.get(mCurrentIndex).getDescription(),
                mCurrentIndex
        );
    }

    public MediaBrowserCompat.MediaItem getItemAtPosition(int index){
        return mCurrentPlayingQueue.get(index);
    }

    public MediaBrowserCompat.MediaItem getItemByMediaId(@NonNull String id){
        for (MediaBrowserCompat.MediaItem item : mCurrentPlayingQueue){
            if(id.equalsIgnoreCase(item.getMediaId())){
                return item;
            }
        }

        return null;
    }

    private MediaBrowserCompat.MediaItem adaptSong(Song songToAdapt){
        if(songToAdapt == null)
            return null;
        songsCache.put(songToAdapt.getId(), songToAdapt);

        MediaDescriptionCompat.Builder mediaItemBuilder = new MediaDescriptionCompat.Builder();

        mediaItemBuilder.setMediaId(songToAdapt.getId());
        mediaItemBuilder.setTitle(songToAdapt.getTitle());
        mediaItemBuilder.setSubtitle(songToAdapt.getArtist());
        mediaItemBuilder.setMediaUri(Uri.parse(songToAdapt.getLink()));

        Bundle bundle = new Bundle();
        bundle.putString(Song.BITRATE, songToAdapt.getBitrate());
        bundle.putString(Song.HOST, songToAdapt.getHost());
        if(songToAdapt.getAlbum() != null) bundle.putString(Song.ALBUM, songToAdapt.getAlbum());
        bundle.putInt(Song.LENGTH, songToAdapt.getLength());


        mediaItemBuilder.setExtras(bundle);

        return new MediaBrowserCompat.MediaItem(mediaItemBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    public Song extractSong(MediaBrowserCompat.MediaItem item){
        if(songsCache.containsKey(item.getMediaId()))
            return songsCache.get(item.getMediaId());

        return null;
    }

    public interface QueueListener{
        void onQueueChanged();
        void onCurrentIndexChanged(int index);
    }
}
