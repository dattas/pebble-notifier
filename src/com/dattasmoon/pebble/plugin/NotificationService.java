package com.dattasmoon.pebble.plugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.dattasmoon.pebble.plugin.Constants.Mode;

public class NotificationService extends AccessibilityService implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    private Mode              mode                   = Mode.EXCLUDE;
    private boolean           notifications_only     = false;
    private long              min_notification_wait  = 60 * 1000;
    private long              notification_last_sent = 0;
    private String[]          packages               = null;
    private Handler           mHandler;
    Queue<AccessibilityEvent> queue;
    SharedPreferences         sharedPreferences;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // if we are off, don't do anything.
        if (mode == Mode.OFF) {
            return;
        }

        // handle if they don't want toasts first, that way we don't put things
        // in queue that we won't actually use
        if (notifications_only) {
            Parcelable parcelable = event.getParcelableData();
            if (!(parcelable instanceof Notification)) {
                Log.i(Constants.LOG_TAG,
                        "Event is not a notification and notifications only is enabled. Clearing event andd checking queue");
                event = null;
            }
        }

        // queue functionality
        // if queue is not empty, queue this message up and see if we can send
        // an older one now
        if (!queue.isEmpty()) {
            if (event != null) {
                queue.add(event);
            }
            if (System.currentTimeMillis() - notification_last_sent < min_notification_wait) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onAccessibilityEvent(null);
                    }
                }, min_notification_wait);
                return;
            }
            event = queue.remove();
        }
        // if we've sent one too recently, queue it up and ask to be called
        // later
        if (System.currentTimeMillis() - notification_last_sent < min_notification_wait) {
            queue.add(event);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onAccessibilityEvent(null);
                }
            }, min_notification_wait);
            return;
        }
        if (event == null) {
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG,
                        "Event is null, this means the queue was empty and we removed the event that called us. Returning.");
            }
            return;
        }

        // main logic
        notification_last_sent = System.currentTimeMillis();
        PackageManager pm = getPackageManager();

        String eventPackageName = event.getPackageName().toString();
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "Service package list is: " + packages.toString());
        }

        if (mode == Mode.EXCLUDE) {
            // exclude functionality
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Mode is set to exclude");
            }

            for (String packageName : packages) {
                if (packageName.equalsIgnoreCase(eventPackageName)) {
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG, packageName + " == " + eventPackageName
                                + " which is on the exclude list. Returning.");
                    }
                    return;
                }
            }
        } else if (mode == Mode.INCLUDE) {
            // include only functionality
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Mode is set to include only");
            }
            boolean found = false;
            for (String packageName : packages) {
                if (packageName.equalsIgnoreCase(eventPackageName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Log.i(Constants.LOG_TAG, eventPackageName + " was not found in the include list. Returning.");
                return;
            }
        }
        String notificationText = event.getText().toString();
        // strip the first and last characters which are [ and ]
        notificationText = notificationText.substring(1, notificationText.length() - 1);
        // Create json object to be sent to Pebble
        final Map<String, Object> data = new HashMap<String, Object>();
        try {
            data.put("title", pm.getApplicationLabel(pm.getApplicationInfo(eventPackageName, 0)));
        } catch (NameNotFoundException e) {
            data.put("title", eventPackageName);
        }
        data.put("body", notificationText);
        final JSONObject jsonData = new JSONObject(data);
        final String notificationData = new JSONArray().put(jsonData).toString();

        // Create the intent to house the Pebble notification
        final Intent i = new Intent(Constants.INTENT_SEND_PEBBLE_NOTIFICATION);
        i.putExtra("messageType", Constants.PEBBLE_MESSAGE_TYPE_ALERT);
        i.putExtra("sender", getString(R.string.app_name));
        i.putExtra("notificationData", notificationData);

        // Send the alert to Pebble
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, event.toString());
            Log.i(Constants.LOG_TAG, event.getPackageName().toString());
            Log.i(Constants.LOG_TAG, notificationText);
            Log.d(Constants.LOG_TAG, "About to send a modal alert to Pebble: " + notificationData);
        }
        sendBroadcast(i);

    }

    @Override
    public void onInterrupt() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onServiceConnected() {
        // get inital preferences
        sharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
        int temp = sharedPreferences.getInt(Constants.PREFERENCE_MODE, -1);
        if (temp == -1) {
            if (!sharedPreferences.getBoolean(Constants.PREFERENCE_EXCLUDE_MODE, false)) {
                mode = Mode.EXCLUDE;
            } else {
                mode = Mode.INCLUDE;
            }
        } else {
            mode = Mode.values()[temp];
        }
        packages = sharedPreferences.getString(Constants.PREFERENCE_PACKAGE_LIST, "").split(",");
        notifications_only = sharedPreferences.getBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, false);
        min_notification_wait = sharedPreferences.getInt(Constants.PREFERENCE_MIN_NOTIFICATION_WAIT, 0) * 1000;

        // notify us if the preferences change
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        mHandler = new Handler();
        queue = new LinkedList<AccessibilityEvent>();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == Constants.PREFERENCE_EXCLUDE_MODE || key == Constants.PREFERENCE_MODE) {
            int temp = sharedPreferences.getInt(Constants.PREFERENCE_MODE, -1);
            if (temp == -1) {
                if (!sharedPreferences.getBoolean(Constants.PREFERENCE_EXCLUDE_MODE, false)) {
                    mode = Mode.EXCLUDE;
                } else {
                    mode = Mode.INCLUDE;
                }
            } else {
                mode = Mode.values()[temp];
            }
        } else if (key == Constants.PREFERENCE_PACKAGE_LIST) {
            packages = sharedPreferences.getString(Constants.PREFERENCE_PACKAGE_LIST, "").split(",");
        } else if (key == Constants.PREFERENCE_NOTIFICATIONS_ONLY) {
            notifications_only = sharedPreferences.getBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, false);
        } else if (key == Constants.PREFERENCE_MIN_NOTIFICATION_WAIT) {
            min_notification_wait = sharedPreferences.getInt(Constants.PREFERENCE_MIN_NOTIFICATION_WAIT, 0) * 1000;
        }
    }
}
