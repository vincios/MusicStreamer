package com.vincios.musicstreamer2.ui.activities;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.RectF;
import android.graphics.drawable.PictureDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.vincios.musicstreamer2.PlayerService;
import com.vincios.musicstreamer2.PlayingSongsQueue;
import com.vincios.musicstreamer2.R;
import com.vincios.musicstreamer2.Utils;
import com.vincios.musicstreamer2.connectors.Song;
import com.vincios.musicstreamer2.ui.SongsQueueAdapter;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PlayerInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PlayerFragment} factory method to
 * create an instance of this fragment.
 */
public class PlayerFragment extends Fragment implements ViewSwitcher.ViewFactory{
    private static final String LOGTAG = "PlayerFragment";
    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private Context mContext;

    private PlayerInteractionListener mListener;
    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mMediaController;
    private android.support.v4.media.session.MediaControllerCompat.Callback mMediaSessionCallback;

    private CardView mMiniPlayerLayout;
    private RecyclerView mCurrentQueueList;

    private ImageButton mMiniPlayerPlayPauseButton;
    private TextView mTitleTextView;
    private TextView mArtistTextView;
    private boolean isMediaSessionCallbackRegistered = false;
    private com.vincios.musicstreamer2.PlayingSongsQueue mQueueHelper;
    private Toolbar mToolbar;
    private ImageSwitcher mBackgroundImage;
    private ImageButton mPlayerPlayPauseButton;
    private Animation mInAnimation;
    private Animation mOutAnimation;
    private SeekBar mSeekBar;
    private TextView mCurrentTimeText;
    private TextView mDurationTimeText;


    private PlaybackStateCompat mLastPlaybackState;
    private int mProgressUpdateIteration = 0;
    private Handler mHandler = new Handler();
    private Runnable mProgressUpdateTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };
    private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mScheduleFuture;
    private ResultReceiver mBufferedPositionReceiver = new BufferedPositionReceiver(null);

    public PlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getContext();

        Log.d(LOGTAG, "onCreate");
        mMediaSessionCallback = new MySessionCallback();

        mMediaBrowser = new MediaBrowserCompat(mContext,
                new ComponentName(mContext, PlayerService.class),
                mConnectionCallback, //Connection callback
                null); //Extras bundle
        mQueueHelper = PlayingSongsQueue.getInstance();

        mInAnimation = AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in);
        mOutAnimation = AnimationUtils.loadAnimation(mContext, android.R.anim.fade_out);

    }


    private void updateMetadataUI(MediaMetadataCompat metadata){
        if(metadata == null)
            metadata = mMediaController.getMetadata();

        if(metadata != null) {
            mArtistTextView.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            mTitleTextView.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            if(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART) != null){
                Bitmap image = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART);
                //image.
                //mBackgroundImage.setImageDrawable(new BitmapDrawable(getResources(), Utils.blur(mContext, image, 15f)));
                mBackgroundImage.setImageDrawable(new PictureDrawable(pictureFromBitmap(image)));
            }else{
                mBackgroundImage.setImageResource(R.drawable.gradient_bg_fullscreen);
            }
            int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            Log.d(LOGTAG, "New metadata duration = " +duration);
            mSeekBar.setMax(duration);
            mDurationTimeText.setText(DateUtils.formatElapsedTime(duration/1000));
        }else {
            mArtistTextView.setText("");
            mTitleTextView.setText(getResources().getText(R.string.player_no_song));
        }


    }

    private Picture pictureFromBitmap(Bitmap bitmap) {

        Picture picture = new Picture();
        Canvas canvas = picture.beginRecording(bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(bitmap, null, new RectF(0f, 0f, (float) bitmap.getWidth(), (float) bitmap.getHeight()), null);
        picture.endRecording();
        return picture;

    }

    private void updatePlaybackStateUI(PlaybackStateCompat state){
        if(state == null)
            state = mMediaController.getPlaybackState();

        if(mMiniPlayerPlayPauseButton != null) {
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_BUFFERING:
                case PlaybackStateCompat.STATE_NONE:
                case PlaybackStateCompat.STATE_STOPPED:
                case PlaybackStateCompat.STATE_PAUSED:
                    mMiniPlayerPlayPauseButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    stopSeekBarUpdate();
                    break;
                case PlaybackStateCompat.STATE_PLAYING:
                    mMiniPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause_black_24dp);
                    scheduleSeekBarUpdate();
                    break;
            }
        }

        if(mPlayerPlayPauseButton != null) {
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PAUSED:
                    mPlayerPlayPauseButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    break;
                case PlaybackStateCompat.STATE_PLAYING:
                    mPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause_black_24dp);
                    break;
            }
        }

        mSeekBar.setProgress((int)state.getPosition());
        mSeekBar.setSecondaryProgress((int) state.getBufferedPosition());

        long activeQueueId = state.getActiveQueueItemId();
        if(activeQueueId != -1)
            updateQueueUI(null, activeQueueId);

    }

    private void updateQueueUI(List<MediaSessionCompat.QueueItem> queue, long currentPlayingQueueId){
        if(queue == null && mMediaController != null)
            queue = mMediaController.getQueue();


        ((SongsQueueAdapter) mCurrentQueueList.getAdapter()).setQueue(queue);
        ((SongsQueueAdapter) mCurrentQueueList.getAdapter()).setmCurrentPlayingId(currentPlayingQueueId);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(LOGTAG,"onCreatedView");

        View v = inflater.inflate(R.layout.fragment_player, container, false);

        mMiniPlayerLayout = (CardView) v.findViewById(R.id.miniPlayerLayout);
        // mPlayerLayout = (RelativeLayout) v.findViewById(R.id.playerLayout);
        mToolbar = (Toolbar) v.findViewById(R.id.playerToolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        mMiniPlayerPlayPauseButton = (ImageButton) v.findViewById(R.id.miniPlayerPlayPauseButton);
        mTitleTextView = (TextView) v.findViewById(R.id.miniPlayerTitleText);
        mArtistTextView = (TextView) v.findViewById(R.id.miniPlayerArtistText);
        mPlayerPlayPauseButton = (ImageButton) v.findViewById(R.id.playerPlayPauseButton);
        mBackgroundImage = (ImageSwitcher) v.findViewById(R.id.playerBackgroundImage);
        mSeekBar = (SeekBar) v.findViewById(R.id.playerSeekBar);
        mCurrentTimeText = (TextView) v.findViewById(R.id.playerCurrentTimeText);
        mDurationTimeText = (TextView) v.findViewById(R.id.playerDurationText);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null)
                    mListener.onBackIconPressed();
            }
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MediaControllerCompat controller = MediaControllerCompat.getMediaController(PlayerFragment.this.getActivity());
                int state = controller.getPlaybackState().getState();
                if(state == PlaybackStateCompat.STATE_PAUSED)
                    controller.getTransportControls().play();
                else if(state == PlaybackStateCompat.STATE_PLAYING)
                    controller.getTransportControls().pause();
            }
        };

        mMiniPlayerPlayPauseButton.setOnClickListener(listener);
        mPlayerPlayPauseButton.setOnClickListener(listener);


        mBackgroundImage.setInAnimation(mInAnimation);
        mBackgroundImage.setOutAnimation(mOutAnimation);
        mBackgroundImage.setFactory(this);
        mBackgroundImage.setImageResource(R.color.bg_color);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                mCurrentTimeText.setText(DateUtils.formatElapsedTime(progress/1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekBarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(LOGTAG, "Seekbar progress changed: " + seekBar.getProgress());
                seekBar.setSecondaryProgress(seekBar.getProgress());
                mMediaController.getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekBarUpdate();
            }
        });
        initializeCurrentQueueList(v);
        return v;
    }



    private void initializeCurrentQueueList(View v){
        mCurrentQueueList = (RecyclerView) v.findViewById(R.id.playerCurrentQueue);
        mCurrentQueueList.setLayoutManager(new LinearLayoutManager(mContext));

        mCurrentQueueList.setAdapter(new SongsQueueAdapter(new SongsQueueAdapter.QueueItemClickListener() {
            @Override
            public void onQueueItemClick(int position) {
                MediaSessionCompat.QueueItem item =
                        ((SongsQueueAdapter) mCurrentQueueList.getAdapter()).getItemAtPosition(position);

                String mediaId = item.getDescription().getMediaId();
                mQueueHelper.setCurrentPlaying(mediaId);
            }
        }));
    }

    public void playSong(Song s){
        mQueueHelper.addToQueue(s, true);
        mMediaController.getTransportControls().play();
    }

    private void scheduleSeekBarUpdate(){
        stopSeekBarUpdate();
        if(!mExecutorService.isShutdown()){
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mProgressUpdateTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekBarUpdate(){
        if(mScheduleFuture != null)
            mScheduleFuture.cancel(false);
    }
    private void updateProgress() {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        if (mLastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();

            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }
        mSeekBar.setProgress((int) currentPosition);
        mProgressUpdateIteration++;

        if(mProgressUpdateIteration % 3 == 0 && mSeekBar.getSecondaryProgress() < mSeekBar.getMax()){
            mMediaController.sendCommand(Utils.CONSTANTS.ACTION_UPDATE_BUFFERED_POSITION, null, mBufferedPositionReceiver);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PlayerInteractionListener) {
            mListener = (PlayerInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement PlayerInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        Log.d(LOGTAG, "onDetach");
        super.onDetach();
        mListener = null;
        if(mMediaBrowser.isConnected())
            mMediaBrowser.disconnect();
        if(isMediaSessionCallbackRegistered) {
            mMediaController.unregisterCallback(mMediaSessionCallback);
            isMediaSessionCallbackRegistered = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mExecutorService.shutdown();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mMediaBrowser.isConnected())
            mMediaBrowser.connect();

        if(mMediaBrowser.isConnected())
            updateQueueUI(null, MediaSessionCompat.QueueItem.UNKNOWN_ID);
    }

    public void setMiniPlayerVisibility(int visibility){
        if(mMiniPlayerLayout.getVisibility() != visibility){
            mMiniPlayerLayout.setVisibility(visibility);
        }


    }
    public void changeAlpha(float alpha){
        mMiniPlayerLayout.setAlpha(alpha);
        mToolbar.setAlpha(1-alpha);
    }

    @Override
    public View makeView() {
        ImageView imageView = new ImageView(mContext);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setLayoutParams(new ImageSwitcher.LayoutParams(
                LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT));
        return imageView;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface PlayerInteractionListener {
        void onBackIconPressed();

        void reloadSongLink(MediaBrowserCompat.MediaItem itemToReload);
    }

    private class BufferedPositionReceiver extends ResultReceiver{
        public BufferedPositionReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if(resultCode == 1){
                long bufferedPosition = resultData.getLong(Utils.CONSTANTS.BUFFERED_POSITION);
                mSeekBar.setSecondaryProgress((int) bufferedPosition);
            }
        }
    }
    private class MySessionCallback extends MediaControllerCompat.Callback{
        public MySessionCallback() {
            super();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            Log.d(LOGTAG, "onPlaybackStateChanged. State = " + state);

            if(state.getState() == PlaybackStateCompat.STATE_BUFFERING){
                Toast.makeText(mContext, getResources().getText(R.string.player_buffering), Toast.LENGTH_SHORT).show();
            }

            if(state.getState() == PlaybackStateCompat.STATE_ERROR){
                handlePlaybackError(state);
            }else {
                mLastPlaybackState = state;
                updatePlaybackStateUI(state);
            }

        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            Log.d(LOGTAG, "onMetadataChanged. metadata="+metadata);
            updateMetadataUI(metadata);
        }

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            super.onQueueChanged(queue);
            Log.d(LOGTAG, "Queue changed! CurrentQueue = ");
            updateQueueUI(queue, -1);
        }

    }

    private void handlePlaybackError(PlaybackStateCompat state) {
        int errorCode = state.getErrorCode();
        String message = state.getErrorMessage().toString();

        Toast.makeText(mContext, getText(R.string.song_connection_failed) + ": " + message, Toast.LENGTH_LONG).show();
        MediaBrowserCompat.MediaItem errorItem = PlayingSongsQueue.getInstance().getItemAtPosition((int) state.getActiveQueueItemId());
        switch (errorCode){
            case PlaybackStateCompat.ERROR_CODE_NOT_SUPPORTED:
                Log.d(LOGTAG, "Trying to reload song link...");
                mListener.reloadSongLink(errorItem);
                break;
        }
    }

    private MediaBrowserCompat.ConnectionCallback mConnectionCallback = new MediaBrowserCompat.ConnectionCallback(){

        @Override
        public void onConnected() {
            super.onConnected();
            Log.d(LOGTAG, "onConnected");
            MediaSessionCompat.Token mediaSessionToken = mMediaBrowser.getSessionToken();
            MediaControllerCompat mediaController = null;
            try {
                mediaController = new MediaControllerCompat(mContext, mediaSessionToken);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            FragmentActivity activity = PlayerFragment.this.getActivity();
            String activityName = (activity != null) ? activity.toString() : "null";
            Log.d(LOGTAG, "Current fragment activity: "+ activityName);

            MediaControllerCompat.setMediaController(PlayerFragment.this.getActivity(), mediaController);
            mMediaController = MediaControllerCompat.getMediaController(PlayerFragment.this.getActivity());

            PendingIntent sessionActivity = mMediaController.getSessionActivity();
            String sessionActivityString = (sessionActivity != null) ? sessionActivity.toString() : "null";
            Log.d(LOGTAG, "Current session activity: "+ sessionActivityString);

            Log.d(LOGTAG, mMediaController.toString());

            updateMetadataUI(null);
            updatePlaybackStateUI(null);
            updateQueueUI(null, MediaSessionCompat.QueueItem.UNKNOWN_ID);

            if(!isMediaSessionCallbackRegistered) {
                mMediaController.registerCallback(mMediaSessionCallback);
                isMediaSessionCallbackRegistered = true;
            }
        }

        @Override
        public void onConnectionSuspended() {
            super.onConnectionSuspended();
            Log.d(LOGTAG, "onConnectionSuspended");
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();

            Log.d(LOGTAG, "onConnectionFailed");
        }
    };


}
