package com.sundarram;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.rtp.AudioGroup;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.sundarram.R;

public class InCallActivity extends Activity
{
    public static final int DIALLED = 1;
    public static final int RECEIVED = 2;
    private Integer requestedByActivity;
    private String target;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incall);

        mTimer = ((TextView)findViewById(R.id.timer));
        mTimer.setText(R.string.ringing);
        //TODO: Should do when this activity starts
        //set localInetAddress.
        //set remoteInetAddress or outsource to other activities.
        //start localAudio
        //send Audioport
        //send ready signal
        //TODO: The following should happend once a READY signal is received:
        // Hold and Mute enabled.
        // Timer should start.
        // Set inCall
        // Listen for changes to receivedSignal
        // the hold and mute buttons must only be enabled after the call connects. Timer should only start then.
        initTimer();

        Bundle localBundle = getIntent().getExtras();
        requestedByActivity = ((Integer)localBundle.get("requestCode"));
        target = ((String)localBundle.get("target"));
        ((TextView)findViewById(R.id.peers_ip)).setText(this.target);

        findViewById(R.id.end).setOnClickListener(this.onEndListener);
        findViewById(R.id.hold).setOnClickListener(this.onHoldListener);
        findViewById(R.id.mute).setOnClickListener(this.onMuteListener);
    }

    private View.OnClickListener onEndListener = new View.OnClickListener() {
        public void onClick(View paramView) {
            mHandler.removeCallbacks(mUpdateTimeTask);
            finish();
            mService.send(VoiceService.END, VoiceService.SHORT_SIGNAL_RECEIVE_PORT);
        }
    };

    private View.OnClickListener onHoldListener = new View.OnClickListener() {
        public void onClick(View paramView) {
            Button holdButton = (Button)findViewById(R.id.hold);
            Button muteButton = (Button)findViewById(R.id.mute);
            if(mService.isAudioGroupSet()) {
                int curMode = mService.getAudioGroupMode();
                if(curMode != AudioGroup.MODE_ON_HOLD) {
                    // will hold the AudioGroup
                    mService.holdGroup(true);
                    holdButton.setText(R.string.text_button_resume);
                    muteButton.setEnabled(false);
                    mService.send(VoiceService.HOLD, VoiceService.SHORT_SIGNAL_RECEIVE_PORT);
                }
                else {
                    // will unhold the AudioGroup - set it to NORMAL
                    mService.holdGroup(false);
                    holdButton.setText(R.string.text_button_hold);
                    muteButton.setEnabled(true);
                    mService.send(VoiceService.UNHOLD, VoiceService.SHORT_SIGNAL_RECEIVE_PORT);
                }

            }
        }
    };

    private View.OnClickListener onMuteListener = new View.OnClickListener() {
        public void onClick(View paramView) {
            Button holdButton = (Button)findViewById(R.id.hold);
            Button muteButton = (Button)findViewById(R.id.mute);
            if(mService.isAudioGroupSet()) {
                int curMode = mService.getAudioGroupMode();
                if(curMode != AudioGroup.MODE_MUTED) {
                    // will mute the AudioGroup
                    mService.muteGroup(true);
                    muteButton.setText(R.string.text_button_unmute);
                    holdButton.setEnabled(false);
                    mService.send(VoiceService.MUTE, VoiceService.SHORT_SIGNAL_RECEIVE_PORT);
                }
                else {
                    // will unmute the AudioGroup
                    mService.muteGroup(false);
                    muteButton.setText(R.string.text_button_mute);
                    holdButton.setEnabled(true);
                    mService.send(VoiceService.UNMUTE, VoiceService.SHORT_SIGNAL_RECEIVE_PORT);
                }

            }
        }
    };

    /** Timer related data members */
    Handler mHandler;
    long mStartTime;
    TextView mTimer;

    /** Initializes timer data memebers and starts the handler */
    private void initTimer() {
        mTimer = ((TextView)findViewById(R.id.timer));
        mStartTime = SystemClock.uptimeMillis();
        mHandler = new Handler();
        mHandler.removeCallbacks(mUpdateTimeTask);
        mHandler.postDelayed(mUpdateTimeTask, 100L);
    }

    /** Runnable that updates the timer every second */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            final long start = mStartTime;
            long millis = SystemClock.uptimeMillis() - start;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds     = seconds % 60;

            if (seconds < 10) {
                mTimer.setText("" + minutes + ":0" + seconds);
            } else {
                mTimer.setText("" + minutes + ":" + seconds);
            }

            mHandler.postAtTime(this,
                    start + (((minutes * 60) + seconds + 1) * 1000));
        }
    };

    @Override
    public void finish() {
        super.finish();
    }

    /** The following code provides a connection to VoiceService*/
    VoiceService mService;
    boolean mBound = false;

    @Override
    public void onStart() {
        super.onStart();
        //Bind to VoiceService.
        Intent intent = new Intent(this, VoiceService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
    /** Binds this activity to the VoiceService */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            VoiceService.VoiceBinder binder = (VoiceService.VoiceBinder) iBinder;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

}
