package com.commonsware.cwac.cam2;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by jason on 7/26/2016.
 */
public class DimScreenActivity extends Activity {

    public static final long DISCONNECT_TIMEOUT = 60000; // 5 min = 5 * 60 * 1000 ms
    //private static PowerManager.WakeLock wl;

    private Handler disconnectHandler = new Handler(){
        public void handleMessage(Message msg) {
        }
    };

    private Runnable disconnectCallback = new Runnable() {
        @Override
        public void run() {
            // Perform any required operation on disconnect

            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = 0;
            getWindow().setAttributes(lp);

        }
    };

    public void resetDisconnectTimer(){

        disconnectHandler.removeCallbacks(disconnectCallback);
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT);
        //..screen will stay on during this section..
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 255;
        getWindow().setAttributes(lp);

    }

    public void stopDisconnectTimer(){
        disconnectHandler.removeCallbacks(disconnectCallback);
    }

    @Override
    public void onUserInteraction(){
        super.onUserInteraction();

        resetDisconnectTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        resetDisconnectTimer();

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);

        //Put system into full screen mode
        if (hasFocus) {
            this.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    @Override
    public void onStop() {
        super.onStop();
        stopDisconnectTimer();
    }

}
