package com.vincios.musicstreamer2.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vincios.musicstreamer2.PlayingSongsQueue;
import com.vincios.musicstreamer2.R;
import com.vincios.musicstreamer2.Utils;
import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.connectors.SongLinkValue;
import com.vincios.musicstreamer2.connectors.tasks.ConnectorsResultListener;
import com.vincios.musicstreamer2.connectors.tasks.SongLinkRequestTask;
import com.vincios.musicstreamer2.connectors.tasks.SongsSearchTask;
import com.vincios.musicstreamer2.database.SongDatabaseHandler;
import com.vincios.musicstreamer2.ui.SongListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends SlidingPlayerBaseActivity implements ConnectorsResultListener {

    private static final String LOGTAG = "MainActivity";
    public static final String SEARCH_QUERY = "query";
    private RecyclerView searchResultList;
    private SongDatabaseHandler songDatabase;
    private EditText searchQueryEditText;
    private Map<String, Song> lastPlayedSongs;
    private FloatingActionButton mSearchButton;
    //private PlayerFragment mPlayerFragment;



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_open_favourites:
                launchFavouritesActivity();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout toolbarLayout = (LinearLayout) findViewById(R.id.toolbarLayout);
        Toolbar toolbar = (Toolbar) toolbarLayout.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.inflateMenu(R.menu.activity_main);

        searchResultListInitialize();
        buttonSearchInitialize();
        searchQueryEditText = (EditText) findViewById(R.id.searchEditText);
        searchQueryEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchButton.show();
            }
        });

        searchQueryEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchQueryEditText.setImeActionLabel("Done", KeyEvent.KEYCODE_ENTER);
        searchQueryEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH){
                    searchSongs();
                    return true;
                }
                return false;
            }
        });


        songDatabase = new SongDatabaseHandler(this);
        lastPlayedSongs = new HashMap<>();
    }

    private void searchResultListInitialize() {
        searchResultList = (RecyclerView) findViewById(R.id.searchResultList);
        searchResultList.setHasFixedSize(true);
        searchResultList.setLayoutManager(new LinearLayoutManager(this));
        SongListAdapter adapter = new SongListAdapter(true);
        adapter.setOnItemClickListener(new SongListAdapter.ItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Song song = ((SongListAdapter) searchResultList.getAdapter()).getItemAtPosition(position);
                String songId = song.getId();

                //search if song already played in this session, so no need to start a new link request
                if(lastPlayedSongs.containsKey(songId)) {
                    Song s = lastPlayedSongs.get(songId);
                    if(s.getLink() != null){
                        playSong(s);
                        return;
                    }
                }

                String songLink = songDatabase.getSongLink(songId);

                if(songLink == null){
                    SongLinkRequestTask task = new SongLinkRequestTask(MainActivity.this);
                    lastPlayedSongs.put(songId, song);
                    task.execute(song);
                }else {
                    Log.d(LOGTAG, "Found link in database for id " + song.getId());
                    Log.d(LOGTAG, "Link: " + songLink);
                    song.setLink(songLink);
                    lastPlayedSongs.put(song.getId(), song);
                    playSong(song);
                }
            }

            @Override
            public void onSongSave(int position) {
                Log.d(LOGTAG, "onSongSave: Saving song as favourite..." );
                Song song = ((SongListAdapter) searchResultList.getAdapter()).getItemAtPosition(position);
                if(!songDatabase.isSavedLink(song.getId())){
                    Log.d(LOGTAG, "onSongSave: No link found for this song. Searching link...");
                    SongLinkRequestTask task = new SongLinkRequestTask(new ConnectorsResultListener() {
                        @Override public void onSearchResult(List<Song> songs) {}
                        @Override public void onFail(Exception exception) {}
                        @Override
                        public void onLinkRequest(SongLinkValue value) {
                            songDatabase.saveSongLink(value.getSongId(), value.getLink(), false);
                            Log.d(LOGTAG, "onSongSave: Saved link");
                        }
                    });
                    task.execute(song);
                }
                songDatabase.saveFavouriteSong(song);
                Log.d(LOGTAG, "onSongSave: Saved song");
            }

            @Override
            public void onSavedSongRemove(int position) {
                Song song = ((SongListAdapter) searchResultList.getAdapter()).getItemAtPosition(position);
                songDatabase.deleteSong(song);
            }

            @Override
            public void onItemLongClick(int position) {
            }
        });

        searchResultList.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOGTAG, "onStart");
        Intent i = getIntent();
        if(i != null && Utils.CONSTANTS.ACTION_SEARCH.equals(i.getAction())){
            searchQueryEditText.setText(i.getStringExtra(SEARCH_QUERY));
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOGTAG, "onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOGTAG, "onPause");
        /*FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.remove(mPlayerFragment);
        transaction.commit();*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOGTAG, "onResume");

    }

    private void launchFavouritesActivity() {
        Intent intent = new Intent(this, FavouriteSongsActivity.class);
//        Intent intent = new Intent(this, DemoActivity.class);

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void buttonSearchInitialize() {
        mSearchButton = (FloatingActionButton) findViewById(R.id.searchButton);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchSongs();
            }
        });

    }

    private void searchSongs(){
        String query = searchQueryEditText.getText().toString();
        Log.d(LOGTAG, "Searching: " + query);
        if(!query.trim().isEmpty()) {
            SongsSearchTask searchTask = new SongsSearchTask(this);
            //TODO: Aggiungere campo 'artista' nella ricerca
            searchTask.execute(query);

            //Close keyboard after search
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(
                    (null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }else{
            searchQueryEditText.setError(getResources().getString(R.string.empty_serach_edit));
        }
    }

    @Override
    public void onSearchResult(List<Song> songs) {

        if(songs.isEmpty())
            Toast.makeText(this, getResources().getString(R.string.search_no_result), Toast.LENGTH_SHORT).show();
        else
            populateResultList(songs);
    }

    private void populateResultList(List<Song> songs) {
        List<Song> toAdd = new ArrayList<>(songs.size());
        for(Song s : songs){
            if(songDatabase.isSavedSong(s.getId()))
                s.setSaved(true);
            toAdd.add(s);
        }

        ((SongListAdapter) searchResultList.getAdapter()).setItems(toAdd);
    }

    @Override
    public void onFail(Exception exception) {
        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLinkRequest(SongLinkValue value) {
        songDatabase.saveSongLink(value.getSongId(), value.getLink(), true);
        Log.d(LOGTAG, "Saved link in database for id " + value.getLink());

        Song s = lastPlayedSongs.get(value.getSongId());
        s.setLink(value.getLink());
        lastPlayedSongs.put(value.getSongId(), s);

        playSong(s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        songDatabase.close();
    }

    @Override
    public void reloadSongLink(MediaBrowserCompat.MediaItem itemToReload) {
        final Song s = PlayingSongsQueue.getInstance().extractSong(itemToReload);

        SongLinkRequestTask task = new SongLinkRequestTask(new ConnectorsResultListener() {
            @Override public void onSearchResult(List<Song> songs) {}
            @Override public void onFail(Exception exception) {}
            @Override
            public void onLinkRequest(SongLinkValue value) {
                songDatabase.saveSongLink(value.getSongId(), value.getLink(), true);
                PlayingSongsQueue.getInstance().replaceItemOnPosition(s, -1, true);
            }
        });

        task.execute(s);
    }
}
