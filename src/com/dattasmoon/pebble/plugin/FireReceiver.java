/* 
Copyright (c) 2013 Dattas Moonchaser
Parts Copyright (c) 2013 Robin Sheat

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dattasmoon.pebble.plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dattasmoon.pebble.plugin.Constants.Mode;
import com.dattasmoon.pebble.plugin.Constants.Type;

public class FireReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            // fetch this for later, we may need it in case we change things
            // around and we need to know what version of the code they were
            // running when they saved the action
            int bundleVersionCode = intent.getIntExtra(Constants.BUNDLE_EXTRA_INT_VERSION_CODE, 1);

            Type type = Type.values()[intent.getIntExtra(Constants.BUNDLE_EXTRA_INT_TYPE, Type.NOTIFICATION.ordinal())];
            PowerManager pm;
            switch (type) {
            case NOTIFICATION:
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                boolean notifScreenOn = sharedPref.getBoolean(SettingsActivity.PREF_NOTIF_SCREEN_ON, true);
                pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (Constants.IS_LOGGABLE) {
                    Log.d(Constants.LOG_TAG, "FireReceiver.onReceive: notifScreenOn=" + notifScreenOn + "  screen="
                            + pm.isScreenOn());
                }
                if (!notifScreenOn && pm.isScreenOn()) {
                    break;
                }

                String title = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_TITLE);
                String body = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_BODY);

                sendAlertToPebble(context, bundleVersionCode, title, body);
                break;
            case SETTINGS:
                Mode mode = Mode.values()[intent.getIntExtra(Constants.BUNDLE_EXTRA_INT_MODE, Mode.OFF.ordinal())];
                boolean notificationsOnly = intent.getBooleanExtra(Constants.BUNDLE_EXTRA_BOOL_NOTIFICATIONS_ONLY,
                        false);
                boolean detailedNotifications = intent.getBooleanExtra(Constants.BUNDLE_EXTRA_BOOL_NOTIFICATION_EXTRAS,
                        false);
                String packageList = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_PACKAGE_LIST);

                setNotificationSettings(context, bundleVersionCode, mode, notificationsOnly, detailedNotifications,
                        packageList);
                break;
            }
        }
    }

    public void setNotificationSettings(final Context context, int bundleVersionCode, Mode mode,
            boolean notificationsOnly, boolean detailedNotifications, String packageList) {

        if (Constants.IS_LOGGABLE) {
            switch (mode) {
            case OFF:
                Log.i(Constants.LOG_TAG, "Mode is: off");
                break;
            case INCLUDE:
                Log.i(Constants.LOG_TAG, "Mode is: include only");
                break;
            case EXCLUDE:
                Log.i(Constants.LOG_TAG, "Mode is: exclude");
                break;
            default:
                Log.i(Constants.LOG_TAG, "Mode is: unknown (" + mode + ")");
            }
            if (notificationsOnly) {
                Log.i(Constants.LOG_TAG, "Only going to send notifications");
            } else {
                Log.i(Constants.LOG_TAG, "Sending all types of notifications, such as Toasts");
            }
            if (detailedNotifications) {
                Log.i(Constants.LOG_TAG, "Going to fetch detailed notifications");
            } else {
                Log.i(Constants.LOG_TAG, "Not going to fetch detailed notifications");
            }
            Log.i(Constants.LOG_TAG, "Package list is: " + packageList);
        }

        Editor editor = context.getSharedPreferences(Constants.LOG_TAG,
                Context.MODE_MULTI_PROCESS | Context.MODE_PRIVATE).edit();
        editor.putInt(Constants.PREFERENCE_MODE, mode.ordinal());
        editor.putBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, notificationsOnly);
        editor.putBoolean(Constants.PREFERENCE_NOTIFICATION_EXTRA, detailedNotifications);
        editor.putBoolean(Constants.PREFERENCE_TASKER_SET, true);
        editor.putString(Constants.PREFERENCE_PACKAGE_LIST, packageList);
        if (!editor.commit()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            editor.commit();
        }
        File watchFile = new File(context.getFilesDir() + "PrefsChanged.none");
        if (!watchFile.exists()) {
            try {
                watchFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        watchFile.setLastModified(System.currentTimeMillis());
    }

    public void sendAlertToPebble(final Context context, int bundleVersionCode, String title, String body) {
        // Create json object to be sent to Pebble
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", title);
        data.put("body", body);
        final JSONObject jsonData = new JSONObject(data);
        final String notificationData = new JSONArray().put(jsonData).toString();

        // Create the intent to house the Pebble notification
        final Intent i = new Intent(Constants.INTENT_SEND_PEBBLE_NOTIFICATION);
        i.putExtra("messageType", Constants.PEBBLE_MESSAGE_TYPE_ALERT);
        i.putExtra("sender", context.getString(R.string.app_name));
        i.putExtra("notificationData", notificationData);

        // Send the alert to Pebble
        if (Constants.IS_LOGGABLE) {
            Log.d(Constants.LOG_TAG, "About to send a modal alert to Pebble: " + notificationData);
        }
        context.sendBroadcast(i);
    }
}
