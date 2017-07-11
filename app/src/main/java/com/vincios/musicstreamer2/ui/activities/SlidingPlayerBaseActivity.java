package com.vincios.musicstreamer2.ui.activities;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.vincios.musicstreamer2.R;
import com.vincios.musicstreamer2.connectors.Song;


/**
 * Base activity for all activities that wants the player as sliding up layout
 * NOTE: An Activity that extends SlidingPlayerBaseActivity MUST have a root layout of type
 * com.sothree.slidinguppanel.SlidingUpPanelLayout with id 'sliding_root_layout' and MUST have as
 * root layout's second child a FrameLayout with id 'bottomPanel'
 */
public class SlidingPlayerBaseActivity extends AppCompatActivity implements PlayerFragment.PlayerInteractionListener {

    private SlidingUpPanelLayout.PanelSlideListener mPanelSlideListener = new SlidingUpPanelLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
            if(mPlayerFragment != null) {
                mPlayerFragment.setMiniPlayerVisibility(View.VISIBLE);
                mPlayerFragment.changeAlpha(1 - slideOffset);
            }
        }

        @Override
        public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
            if(newState == SlidingUpPanelLayout.PanelState.EXPANDED){
                mSlidingRootLayout.setTouchEnabled(false);
                mPlayerFragment.setMiniPlayerVisibility(View.GONE);
            }else{
                mSlidingRootLayout.setTouchEnabled(true);
                mPlayerFragment.setMiniPlayerVisibility(View.VISIBLE);
            }
        }
    };


    private PlayerFragment mPlayerFragment;
    private SlidingUpPanelLayout mSlidingRootLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if(mPlayerFragment == null)
            mPlayerFragment = new PlayerFragment();

        transaction.replace(R.id.bottomPanel, mPlayerFragment);
        transaction.commit();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        mSlidingRootLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_root_layout);
        /*mSlidingRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
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

        mSlidingRootLayout.addPanelSlideListener(mPanelSlideListener);
        mSlidingRootLayout.setParallaxOffset(100);
    }

    @Override
    public void onBackPressed() {
        if(mSlidingRootLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            super.onBackPressed();
        }else{
            mSlidingRootLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
    }

    protected void playSong(Song s){
        if(mPlayerFragment != null)
            mPlayerFragment.playSong(s);
    }

    @Override
    public void onBackIconPressed() {
        onBackPressed();
    }
}
