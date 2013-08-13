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
import java.util.Calendar;
import java.util.Date;
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

                // handle screen DND
                boolean notifScreenOn = sharedPref.getBoolean(Constants.PREFERENCE_NOTIF_SCREEN_ON, true);
                pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (Constants.IS_LOGGABLE) {
                    Log.d(Constants.LOG_TAG, "FireReceiver.onReceive: notifScreenOn=" + notifScreenOn + "  screen="
                            + pm.isScreenOn());
                }
                if (!notifScreenOn && pm.isScreenOn()) {
                    break;
                }

                //handle quiet hours DND
                boolean quiet_hours = sharedPref.getBoolean(Constants.PREFERENCE_QUIET_HOURS, false);
                //we only need to pull this if quiet hours are enabled. Save the cycles for the cpu! (haha)
                if(quiet_hours){
                    String[] pieces = sharedPref.getString(Constants.PREFERENCE_QUIET_HOURS_BEFORE, "00:00").split(":");
                    Date quiet_hours_before= new Date(0, 0, 0, Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
                    pieces = sharedPref.getString(Constants.PREFERENCE_QUIET_HOURS_AFTER, "23:59").split(":");
                    Date quiet_hours_after = new Date(0, 0, 0, Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
                    Calendar c = Calendar.getInstance();
                    Date now = new Date(0, 0, 0, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG, "Checking quiet hours. Now: " + now.toString() + " vs " +
                                quiet_hours_before.toString() + " and " +quiet_hours_after.toString());
                    }
                    if(now.before(quiet_hours_before) || now.after(quiet_hours_after)){
                        if (Constants.IS_LOGGABLE) {
                            Log.i(Constants.LOG_TAG, "Time is before or after the quiet hours time. Returning.");
                        }
                        break;
                    }
                }

                String title = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_TITLE);
                String body = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_BODY);

                sendAlertToPebble(context, bundleVersionCode, title, body);
                break;
            case SETTINGS:
                Mode mode = Mode.values()[intent.getIntExtra(Constants.BUNDLE_EXTRA_INT_MODE, Mode.OFF.ordinal())];
                String packageList = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_PACKAGE_LIST);

                setNotificationSettings(context, bundleVersionCode, mode, packageList);
                break;
            }
        }
    }

    public void setNotificationSettings(final Context context, int bundleVersionCode, Mode mode,String packageList) {

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
            Log.i(Constants.LOG_TAG, "Package list is: " + packageList);
        }

        Editor editor = context.getSharedPreferences(Constants.LOG_TAG+"_preferences", context.MODE_MULTI_PROCESS | context.MODE_PRIVATE).edit();
        editor.putInt(Constants.PREFERENCE_MODE, mode.ordinal());
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
