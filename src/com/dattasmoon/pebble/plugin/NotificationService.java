package com.dattasmoon.pebble.plugin;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class NotificationService extends AccessibilityService implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    private boolean   mode     = false;
    private String[]  packages = null;
    SharedPreferences sharedPreferences;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        PackageManager pm = getPackageManager();

        String eventPackageName = event.getPackageName().toString();
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "Service package list is: " + packages.toString());
        }

        if (!mode) {
            // exclude functionality
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Mode is set to exclude");
            }

            for (String packageName : packages) {
                if (packageName.equalsIgnoreCase(eventPackageName)) {
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG, packageName + " == " + eventPackageName + " Returning.");
                    }
                    return;
                }
            }
        } else {
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
                Log.i(Constants.LOG_TAG, eventPackageName + " was not found in the list. returning.");
                return;
            }
        }
        String text = event.getText().toString();
        // strip the first and last characters which are [ and ]
        text = text.substring(1, text.length() - 1);
        // Create json object to be sent to Pebble
        final Map<String, Object> data = new HashMap<String, Object>();
        try {
            data.put("title", pm.getApplicationLabel(pm.getApplicationInfo(eventPackageName, 0)));
        } catch (NameNotFoundException e) {
            data.put("title", eventPackageName);
        }
        data.put("body", text);
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
            Log.i(Constants.LOG_TAG, text);
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
        // get preferences
        sharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
        mode = sharedPreferences.getBoolean(Constants.PREFERENCE_EXCLUDE_MODE, false);
        packages = sharedPreferences.getString(Constants.PREFERENCE_PACKAGE_LIST, "").split(",", 0);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.notificationTimeout = 100;
        setServiceInfo(info);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == Constants.PREFERENCE_EXCLUDE_MODE) {
            mode = sharedPreferences.getBoolean(Constants.PREFERENCE_EXCLUDE_MODE, false);
        } else if (key == Constants.PREFERENCE_PACKAGE_LIST) {
            packages = sharedPreferences.getString(Constants.PREFERENCE_PACKAGE_LIST, "").split(",", 0);
        }
    }
}
