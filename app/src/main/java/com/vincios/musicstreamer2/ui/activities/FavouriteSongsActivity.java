package com.vincios.musicstreamer2.ui.activities;

import android.app.DialogFragment;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.vincios.musicstreamer2.PlayingSongsQueue;
import com.vincios.musicstreamer2.R;
import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.connectors.SongLinkValue;
import com.vincios.musicstreamer2.connectors.tasks.ConnectorsResultListener;
import com.vincios.musicstreamer2.connectors.tasks.SongLinkRequestTask;
import com.vincios.musicstreamer2.database.DatabaseReadyListener;
import com.vincios.musicstreamer2.database.SongDatabaseHandler;
import com.vincios.musicstreamer2.ui.widgets.MyDialogFragment;
import com.vincios.musicstreamer2.ui.SongListAdapter;

import java.util.List;

public class FavouriteSongsActivity extends SlidingPlayerBaseActivity {
    private static final String LOGTAG = "FavouriteSongActivity";
    private RelativeLayout rootview;
    private RecyclerView songListView;
    private SongDatabaseHandler database;
    private SongListAdapter adapter;
    private Song lastDeletedSong;
    private int lastDeletedSongPosition;

    private SongListAdapter.ItemClickListener mAdapterClickListener = new SongListAdapter.ItemClickListener() {
        @Override
        public void onItemClick(int position) {
            launchPlayerActivity(position);
        }
        @Override public void onSongSave(int position) {}
        @Override public void onSavedSongRemove(int position) {}
        @Override
        public void onItemLongClick(final int position) {
            MyDialogFragment dialog = new MyDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putString(MyDialogFragment.DIALOG_MESSAGE, getResources().getString(R.string.delete_favourite_dialog_message));
            dialog.setArguments(arguments);

            dialog.setListener(new MyDialogFragment.MyDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialog) {
                    deleteSong(position);
                }
                @Override public void onDialogNegativeClick(DialogFragment dialog) {}
            });

            dialog.show(getFragmentManager(), "favourite_delete");
        }
    };

    private PlayerFragment fragment;
    private SlidingUpPanelLayout mSlidingRootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite_songs);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.activity_main);

        adapter = new SongListAdapter(false);
        adapter.setOnItemClickListener(mAdapterClickListener);

        songListView = (RecyclerView) findViewById(R.id.favouritesList);
        songListView.setLayoutManager(new LinearLayoutManager(this));
        songListView.setAdapter(adapter);

        database = new SongDatabaseHandler(this);
        database.setDatabaseReadyListener(new DatabaseReadyListener() {
            @Override
            public void databaseReady() {
                Log.d(LOGTAG, "databaseReady");
                List<Song> songs = database.getSongs();
                adapter.setItems(songs);
            }
        });

        lastDeletedSong = null;

        /*FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        fragment = new PlayerFragment();

        transaction.add(R.id.favouriteActivityBottomPanel, fragment);
        transaction.commit();

        mSlidingRootLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_root_layout);
        mSlidingRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                Rect r = new Rect();
                mSlidingRootLayout.getWindowVisibleDisplayFrame(r);
                int screenHeight = mSlidingRootLayout.getRootView().getHeight();

                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;

                Log.d(LOGTAG, "keypadHeight = " + keypadHeight);

                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    if(mSlidingRootLayout.getPanelState() != SlidingUpPanelLayout.PanelState.HIDDEN){
                        mSlidingRootLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                    }
                }
                else {
                    if(mSlidingRootLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN){
                        mSlidingRootLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    }
                }
            }
        });*/

    }

    private void launchPlayerActivity(int position) {
        Song song = ((SongListAdapter) songListView.getAdapter()).getItemAtPosition(position);
        if(song.getLink() == null){
            String link = database.getSongLink(song.getId());
            song.setLink(link);
        }
        Log.d(LOGTAG, "song id = " + song.getId());
        /*Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.SONG_TO_PLAY, song);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);*/


        //fragment.playSong(song);
        super.playSong(song);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOGTAG, "onDestroy");
        database.close();
    }

    private void deleteSong(final int position){
        final Song s = adapter.removeItemAtPosition(position);
        //lastDeletedSong = s;
        //lastDeletedSongPosition = position;
        database.deleteSong(s);
        Snackbar.make(findViewById(R.id.sliding_root_layout), R.string.favourite_song_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        restoreDeletedSong(s, position);
                        v.setVisibility(View.GONE);
                    }
                })
                .show();
    }

    private void restoreDeletedSong(Song s, int position) {
        database.saveFavouriteSong(s);
        adapter.insertItemAtPosition(s, position);
    }

    @Override
    public void reloadSongLink(MediaBrowserCompat.MediaItem itemToReload) {
        final Song s = PlayingSongsQueue.getInstance().extractSong(itemToReload);

        SongLinkRequestTask task = new SongLinkRequestTask(new ConnectorsResultListener() {
            @Override public void onSearchResult(List<Song> songs) {}
            @Override public void onFail(Exception exception) {}
            @Override
            public void onLinkRequest(SongLinkValue value) {
                database.saveSongLink(value.getSongId(), value.getLink(), true);
                PlayingSongsQueue.getInstance().replaceItemOnPosition(s, -1, true);
            }
        });

        task.execute(s);
    }
}
