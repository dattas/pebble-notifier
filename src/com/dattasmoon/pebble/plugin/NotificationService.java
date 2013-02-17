/* 
Copyright (c) 2013 Dattas Moonchaser

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dattasmoon.pebble.plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.dattasmoon.pebble.plugin.Constants.Mode;

public class NotificationService extends AccessibilityService {
    private Mode              mode                   = Mode.EXCLUDE;
    private boolean           notifications_only     = false;
    private long              min_notification_wait  = 60 * 1000;
    private long              notification_last_sent = 0;
    private String[]          packages               = null;
    private Handler           mHandler;
    private File              watchFile;
    private Long              lastChange;
    Queue<AccessibilityEvent> queue;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // handle the prefs changing, because of how accessibility services
        // work, sharedprefsonchange listeners don't work
        if (watchFile.lastModified() > lastChange) {
            loadPrefs();
        }
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "Service: Mode is: " + String.valueOf(mode.ordinal()));
        }
        // if we are off, don't do anything.
        if (mode == Mode.OFF) {
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Service: Mode is off, not sending any notifications");
            }
            return;
        }

        // handle if they don't want toasts first, that way we don't put things
        // in queue that we won't actually use
        if (notifications_only) {
            Parcelable parcelable = event.getParcelableData();
            if (!(parcelable instanceof Notification)) {
                if (Constants.IS_LOGGABLE) {
                    Log.i(Constants.LOG_TAG,
                            "Event is not a notification and notifications only is enabled. Clearing event and checking queue");
                }
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

        switch (mode) {
        case EXCLUDE:
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
            break;
        case INCLUDE:
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
            break;
        }

        String notificationText = event.getText().toString();
        // strip the first and last characters which are [ and ]
        notificationText = notificationText.substring(1, notificationText.length() - 1);
        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                notificationText += "\n" + getExtraBigData((Notification) parcelable, notificationText.trim());
            } else {
                notificationText += "\n" + getExtraData((Notification) parcelable, notificationText.trim());
            }

        }

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

    }

    @Override
    protected void onServiceConnected() {
        // get inital preferences

        watchFile = new File(getFilesDir() + "PrefsChanged.none");
        if (!watchFile.exists()) {
            try {
                watchFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            watchFile.setLastModified(System.currentTimeMillis());
        }
        loadPrefs();

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        mHandler = new Handler();
        queue = new LinkedList<AccessibilityEvent>();
    }

    private void loadPrefs() {
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "I am loading preferences");
        }
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_PRIVATE);

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
        lastChange = watchFile.lastModified();
    }

    private String getExtraData(Notification notification, String existing_text) {
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "I am running extra data");
        }
        RemoteViews views = notification.contentView;
        if (views == null) {
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "ContentView was empty, returning a blank string");
            }
            return "";
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
        views.reapply(getApplicationContext(), localView);
        return dumpViewGroup(0, localView, existing_text);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private String getExtraBigData(Notification notification, String existing_text) {
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "I am running extra big data");
        }
        RemoteViews views = notification.bigContentView;
        if (views == null) {
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "bigContentView was empty, running normal");
            }
            return getExtraData(notification, existing_text);
        }
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
        views.reapply(getApplicationContext(), localView);
        return dumpViewGroup(0, localView, existing_text);
    }

    private String dumpViewGroup(int depth, ViewGroup vg, String existing_text) {
        String text = "";
        Log.d(Constants.LOG_TAG, "root view, depth:" + depth + "; view: " + vg);
        for (int i = 0; i < vg.getChildCount(); ++i) {
            View v = vg.getChildAt(i);
            if (Constants.IS_LOGGABLE) {
                Log.d(Constants.LOG_TAG, "depth: " + depth + "; " + v.getClass().toString() + "; view: " + v);
            }
            if (v.getId() == android.R.id.title || v instanceof android.widget.Button
                    || v.getClass().toString().contains("android.widget.DateTimeView")) {
                if (Constants.IS_LOGGABLE) {
                    Log.d(Constants.LOG_TAG, "I am going to skip this");
                }
                continue;
            }

            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                if (tv.getText().toString() == "..." || tv.getText().toString() == "É"
                        || isInteger(tv.getText().toString())
                        || tv.getText().toString().trim().equalsIgnoreCase(existing_text)) {
                    if (Constants.IS_LOGGABLE) {
                        Log.d(Constants.LOG_TAG, "Text is: " + tv.getText().toString() + " but I am going to skip this");
                    }
                    continue;
                }
                text += tv.getText().toString() + "\n";
                if (Constants.IS_LOGGABLE) {
                    Log.i(Constants.LOG_TAG, tv.getText().toString());
                }
            }
            if (v instanceof ViewGroup) {
                text += dumpViewGroup(depth + 1, (ViewGroup) v, existing_text);
            }
        }
        return text;
    }

    public boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
