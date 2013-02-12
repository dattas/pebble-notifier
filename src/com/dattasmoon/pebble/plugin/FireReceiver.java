package com.dattasmoon.pebble.plugin;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FireReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
            // fetch this for later, we may need it in case we change things
            // around and we need to know what version of the code they were
            // running when they saved the action
            int bundleVersionCode = intent.getIntExtra(Constants.BUNDLE_EXTRA_INT_VERSION_CODE, 1);

            String title = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_TITLE);
            String body = intent.getStringExtra(Constants.BUNDLE_EXTRA_STRING_BODY);
            sendAlertToPebble(context, bundleVersionCode, title, body);
        }
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
