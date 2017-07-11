package com.vincios.musicstreamer2.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vincios.musicstreamer2.connectors.Song;


public class SongDatabaseHelper extends SQLiteOpenHelper {


    public static final String SONGS_LINK_TABLE_NAME = "songs_link";
    public static final String FAVOURITE_SONGS_TABLE_NAME = "favourite_songs";
    public static final String DB_NAME = "Songs.db";

    private static final int DB_VERSION = 3;
    private static final String LOGTAG = "SongDbHelper";
    private static final String SQL_CREATE_FAVOURITE_SONG =
            "CREATE TABLE " + FAVOURITE_SONGS_TABLE_NAME + " (" +
                    Song._ID + " TEXT PRIMARY KEY," +
                    Song.TITLE + " TEXT," +
                    Song.ARTIST + " TEXT," +
                    Song.ALBUM + " TEXT," +
                    Song.LENGTH + " INTEGER," +
                    Song.BITRATE + " TEXT," +
                    Song.SIZE + " INTEGER," +
                    Song.HOST + " TEXT," +
                    Song.LINK + " TEXT, " +
                    Song.SAVED + " INTEGER DEFAULT 0, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)";

    private static final String SQL_CREATE_SONG_LINK =
            "CREATE TABLE " + SONGS_LINK_TABLE_NAME + " (" +
                    Song._ID + " TEXT PRIMARY KEY," +
                    Song.HOST + " TEXT DEFAULT NULL," +
                    Song.LINK + " TEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)";
    private static final String SQL_DELETE_SONG_LINK_TABLE = "DROP TABLE IF EXISTS " + SONGS_LINK_TABLE_NAME;
    private static final String SQL_DELETE_FAVOURITE_SONG_TABLE = "DROP TABLE IF EXISTS " + FAVOURITE_SONGS_TABLE_NAME;


    public SongDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(LOGTAG, "onCreate");

        db.execSQL(SQL_CREATE_FAVOURITE_SONG);
        db.execSQL(SQL_CREATE_SONG_LINK);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOGTAG, "Database upgrade. Old version: " + oldVersion + ". New version: " + newVersion);

        db.execSQL(SQL_DELETE_SONG_LINK_TABLE);
        db.execSQL(SQL_DELETE_FAVOURITE_SONG_TABLE);

        db.execSQL(SQL_CREATE_FAVOURITE_SONG);
        db.execSQL(SQL_CREATE_SONG_LINK);
    }


}
