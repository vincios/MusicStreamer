package com.vincios.musicstreamer2.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.connectors.SongBase;

import java.util.ArrayList;
import java.util.List;

public class SongDatabaseHandler implements TaskListener {


    private static final String LOGTAG = "SongDatabaseHandler";
    private SQLiteDatabase database;
    private DatabaseReadyListener listener;


    public SongDatabaseHandler(Context context) {
        SongDatabaseHelper dbHelper = new SongDatabaseHelper(context);
        this.database = null;
        this.listener = null;

        DBTask task = new DBTask(this);
        task.execute(dbHelper);
    }

    public void setDatabaseReadyListener(DatabaseReadyListener listener) {
        this.listener = listener;
    }

    public void saveSongLink(String id, String link, boolean overwriteIfExist){
        if(database == null || !database.isOpen())
            throw new SQLException("Database not opened");

        if(isSavedLink(id)) {
            if (overwriteIfExist)
                removeSongLink(id);
            else
                return;
        }

        ContentValues values = new ContentValues(2);
        values.put(Song._ID, id);
        values.put(Song.LINK, link);

        database.insert(SongDatabaseHelper.SONGS_LINK_TABLE_NAME, null, values);
    }

    public void removeSongLink(String id){
        if(database == null || !database.isOpen())
            throw new SQLException("Database not opened");

        String whereClause = Song._ID + "=?";
        String[] whereArgs = {id};

        database.delete(SongDatabaseHelper.SONGS_LINK_TABLE_NAME,
                whereClause,
                whereArgs
        );
    }

    public void saveFavouriteSong(Song song){
        if(database == null || !database.isOpen())
            throw new SQLException("Database not opened");

        ContentValues values = new ContentValues();
        values.put(Song._ID, song.getId());
        values.put(Song.TITLE, song.getTitle());
        values.put(Song.ARTIST, song.getArtist());
        values.put(Song.ALBUM, song.getAlbum());
        values.put(Song.BITRATE, song.getBitrate());
        values.put(Song.LENGTH, song.getLength());
        values.put(Song.SIZE, song.getSize());
        values.put(Song.LINK, song.getLink());
        values.put(Song.HOST, song.getHost());

        database.insert(SongDatabaseHelper.FAVOURITE_SONGS_TABLE_NAME, null, values);
    }

    public String getSongLink(String id){
        if(database == null || !database.isOpen())
            throw new SQLException("Database not opened");

        String[] columns = {Song.LINK};
        String selection = Song._ID + " = ?";
        String[] selectionArgs = {id};
        Cursor cursor = null;
        String link = null;

        try {

            cursor = database.query(
                    SongDatabaseHelper.SONGS_LINK_TABLE_NAME,
                    columns,
                    selection,
                    selectionArgs, null, null, null);

            while (cursor.moveToNext()) {
                link = cursor.getString(cursor.getColumnIndex(Song.LINK));
            }
        }finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return link;
    }

    public boolean isSavedSong(String id){
        if(database == null || !database.isOpen())
            throw new SQLException("Database not opened");

        String[] columns = {Song._ID};
        String selecion = Song._ID + "=?";
        String[] selectionArgs = {id};
        Cursor cursor = null;

        try{
            cursor = database.query(
                    SongDatabaseHelper.FAVOURITE_SONGS_TABLE_NAME,
                    columns,
                    selecion,
                    selectionArgs,
                    null, null, null
            );

            return cursor.getCount() != 0;

        }finally {
            if(cursor != null)
                cursor.close();
        }
    }

    public boolean isSavedLink(String id){
        if(database == null || !database.isOpen())
            throw new SQLException("Database not opened");

        String[] columns = {Song._ID};
        String selecion = Song._ID + "=?";
        String[] selectionArgs = {id};
        Cursor cursor = null;

        try{
            cursor = database.query(
                    SongDatabaseHelper.SONGS_LINK_TABLE_NAME,
                    columns,
                    selecion,
                    selectionArgs,
                    null, null, null
            );

            return cursor.getCount() != 0;

        }finally {
            if(cursor != null)
                cursor.close();
        }
    }

    public void deleteSong(Song song) {

        if(database == null || !database.isOpen())
            throw new SQLException("Database not opened");

        String whereClause = Song._ID + "=?";
        String[] whereArgs = {song.getId()};

        database.delete(SongDatabaseHelper.FAVOURITE_SONGS_TABLE_NAME,
                whereClause,
                whereArgs
        );

    }

    public List<Song> getSongs(){
        if(database == null || !database.isOpen())
            throw new SQLException("Database not opened");

        String[] columns = {Song._ID, Song.TITLE, Song.ARTIST, Song.ALBUM, Song.BITRATE, Song.LENGTH, Song.SIZE, Song.LINK, Song.HOST};
        Cursor cursor = null;
        List<Song> songs = new ArrayList<>();

        try {
            cursor = database.query(
                    SongDatabaseHelper.FAVOURITE_SONGS_TABLE_NAME,
                    columns,
                    null, null, null, null,
                    "created_at");

            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(Song._ID));
                String title = cursor.getString(cursor.getColumnIndex(Song.TITLE));
                String artist = cursor.getString(cursor.getColumnIndex(Song.ARTIST));
                String album = cursor.getString(cursor.getColumnIndex(Song.ALBUM));
                String bitrate = cursor.getString(cursor.getColumnIndex(Song.BITRATE));
                int length = cursor.getInt(cursor.getColumnIndex(Song.LENGTH));
                String size = cursor.getString(cursor.getColumnIndex(Song.SIZE));
                String link = cursor.getString(cursor.getColumnIndex(Song.LINK));
                String host = cursor.getString(cursor.getColumnIndex(Song.HOST));

                Song song = new SongBase(id,artist,title,length,bitrate,size,album,link,host);
                songs.add(song);
            }
        }finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return songs;
    }

    public void close(){

        if(database == null || !database.isOpen())
            throw new SQLException("Database not opened");

        database.close();
    }


    @Override
    public void onFinish(SQLiteDatabase db) {
        this.database = db;
        if(listener != null)
            listener.databaseReady();
    }


    private class DBTask extends AsyncTask<SongDatabaseHelper, Void, SQLiteDatabase>{

        TaskListener listener;

        public DBTask(TaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected SQLiteDatabase doInBackground(SongDatabaseHelper... params) {
            return params[0].getWritableDatabase();
        }

        @Override
        protected void onPostExecute(SQLiteDatabase sqLiteDatabase) {
            listener.onFinish(sqLiteDatabase);
        }
    }

}

interface TaskListener{
    void onFinish(SQLiteDatabase db);
}
