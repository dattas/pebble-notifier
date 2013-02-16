package com.dattasmoon.pebble.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
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

            Type type = Type.values()[intent.getIntExtra(Constants.BUNDLE_EXTRA_INT_MODE, Type.NOTIFICATION.ordinal())];
            if (type == Type.NOTIFICATION) {
                String title = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_TITLE);
                String body = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_BODY);

                sendAlertToPebble(context, bundleVersionCode, title, body);
            } else if (type == Type.SETTINGS) {
                Mode mode = Mode.values()[intent.getIntExtra(Constants.BUNDLE_EXTRA_INT_MODE, Mode.OFF.ordinal())];
                boolean notificationsOnly = intent.getBooleanExtra(Constants.BUNDLE_EXTRA_BOOL_NOTIFICATIONS_ONLY,
                        false);
                String packageList = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_PACKAGE_LIST);
                setNotificationSettings(context, bundleVersionCode, mode, notificationsOnly, packageList);
            }
        }
    }

    public void setNotificationSettings(final Context context, int bundleVersionCode, Mode mode,
            boolean notificationsOnly, String packageList) {
        String selectedPackages = "";

        // ensure that all duplicates are removed before saving to ensure the
        // service is as efficient as possible
        ArrayList<String> tmpArray = new ArrayList<String>();
        for (String strPackage : packageList.split(",")) {
            if (!strPackage.isEmpty()) {
                if (!tmpArray.contains(strPackage)) {
                    tmpArray.add(strPackage);
                    selectedPackages += strPackage + ",";
                }
            }
        }
        tmpArray.clear();
        tmpArray = null;

        // remove extra , if there are any selected packages
        if (!selectedPackages.isEmpty()) {
            selectedPackages = selectedPackages.substring(0, selectedPackages.length() - 1);
        }
        if (Constants.IS_LOGGABLE) {
            if (mode == Mode.OFF) {
                Log.i(Constants.LOG_TAG, "Mode is: off");
            } else if (mode == Mode.INCLUDE) {
                Log.i(Constants.LOG_TAG, "Mode is: include only");
            } else if (mode == Mode.EXCLUDE) {
                Log.i(Constants.LOG_TAG, "Mode is: exclude");
            } else {
                Log.i(Constants.LOG_TAG, "Mode is: unknown (" + mode + ")");
            }
            if (notificationsOnly) {
                Log.i(Constants.LOG_TAG, "Only going to send notifications");
            } else {
                Log.i(Constants.LOG_TAG, "Sending all types of notifications, such as Toasts");
            }
            Log.i(Constants.LOG_TAG, "Package list is: " + selectedPackages);
        }

        Editor editor = context.getSharedPreferences(Constants.LOG_TAG,
                Context.MODE_MULTI_PROCESS | Context.MODE_PRIVATE).edit();
        editor.putInt(Constants.PREFERENCE_MODE, mode.ordinal());
        editor.putBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, notificationsOnly);
        editor.putString(Constants.PREFERENCE_PACKAGE_LIST, selectedPackages);
        editor.commit();
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
