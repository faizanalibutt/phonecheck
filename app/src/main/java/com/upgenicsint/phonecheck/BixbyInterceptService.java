package com.upgenicsint.phonecheck;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Toast;

import com.upgenicsint.phonecheck.activities.ButtonsTestActivity;
import com.upgenicsint.phonecheck.activities.MainActivity;
import com.upgenicsint.phonecheck.broadcastreceiver.BixbyButtonReceiver;

import java.lang.ref.WeakReference;

/**
 * Created by zohai on 12/18/2017.
 */

/*extends AccessibilityService*/
public class BixbyInterceptService  {

    /*private static final int KEYCODE_BIXBY = 1082;

    Boolean MODE_TALK_BACK_SCREEN = false;
    private static final String TAG = BixbyInterceptService.class.getSimpleName();
    private static final String BIXBY_PACKAGE = "com.samsung.android.app.spage";

    WeakReference activity;
    @Override
    protected void onServiceConnected() {
        Log.v(TAG, "onServiceConnected");
//        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        setupCameraIfNeeded();
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String activeWindowPackage = getActiveWindowPackage();

        Log.v(TAG, String.format(
                "onAccessibilityEvent: [type] %s [time] %s [activeWindowPackage] %s",
                AccessibilityEvent.eventTypeToString(event.getEventType()), event.getEventTime(), activeWindowPackage));

        if (!BIXBY_PACKAGE.equals(activeWindowPackage)) {
            return;
        }

        String packageName = String.valueOf(event.getEventType());
        ButtonsTestActivity.isBixbyPressed = true;
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(BixbyButtonReceiver.ACTION));
        this.performGlobalAction(GLOBAL_ACTION_BACK);
       // new DelayedBackButtonTask(this).execute();
//        if(event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED){
//        }
    }

    @Override
    public void onInterrupt() {
        Log.v(TAG, "onInterrupt");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind");
        return false;
    }

    private String getActiveWindowPackage() {
        AccessibilityNodeInfo rootInActiveWindow = getRootInActiveWindow();
        return rootInActiveWindow != null ? rootInActiveWindow.getPackageName().toString() : null;
    }

    private static class DelayedBackButtonTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<BixbyInterceptService> activityReference;

        DelayedBackButtonTask(BixbyInterceptService context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Log.v(TAG, "interrupted");
            }
            BixbyInterceptService service = activityReference.get();
            if (service != null) {
                ButtonsTestActivity.isBixbyPressed = true;
                LocalBroadcastManager.getInstance(service.getApplicationContext()).sendBroadcast(new Intent(BixbyButtonReceiver.ACTION));
                service.performGlobalAction(GLOBAL_ACTION_BACK);
            }
            return null;
        }
    }*/
}

