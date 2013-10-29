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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
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
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.dattasmoon.pebble.plugin.Constants.Mode;

public class NotificationService extends AccessibilityService {
    private class queueItem {
        public String title;
        public String body;

        public queueItem(String title, String body) {
            this.title = title;
            this.body = body;
        }
    }

    private Mode     mode                   = Mode.EXCLUDE;
    private boolean  notifications_only     = false;
    private boolean  no_ongoing_notifs      = false;
    private boolean  notification_extras    = false;
    private boolean  quiet_hours            = false;
    private boolean  notifScreenOn          = true;
    private JSONArray converts              = new JSONArray();
    private JSONArray ignores               = new JSONArray();
    private long     min_notification_wait  = 0 * 1000;
    private long     notification_last_sent = 0;
    private Date     quiet_hours_before     = null;
    private Date     quiet_hours_after      = null;
    private String[] packages               = null;
    private Handler  mHandler;
    private File     watchFile;
    private Long     lastChange;
    Queue<queueItem> queue;

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

        //handle quiet hours
        if(quiet_hours){

            Calendar c = Calendar.getInstance();
            Date now = new Date(0, 0, 0, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Checking quiet hours. Now: " + now.toString() + " vs " +
                        quiet_hours_before.toString() + " and " +quiet_hours_after.toString());
            }

            if(quiet_hours_before.after(quiet_hours_after)){
                if(now.after(quiet_hours_after) && now.before(quiet_hours_before)){
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG, "Time is during quiet time. Returning.");
                    }
                    return;
                }

            } else if(now.before(quiet_hours_before) || now.after(quiet_hours_after)){
                if (Constants.IS_LOGGABLE) {
                    Log.i(Constants.LOG_TAG, "Time is before or after the quiet hours time. Returning.");
                }
                return;
            }

        }

        // handle if they only want notifications
        if (notifications_only) {
            if (event != null) {
                Parcelable parcelable = event.getParcelableData();
                if (!(parcelable instanceof Notification)) {

                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG,
                                "Event is not a notification and notifications only is enabled. Returning.");
                    }
                    return;
                }
            }
        }
        if (no_ongoing_notifs){
            Parcelable parcelable = event.getParcelableData();
            if (parcelable instanceof Notification) {
                Notification notif = (Notification) parcelable;
                if (Constants.IS_LOGGABLE) {
                    Log.i(Constants.LOG_TAG,
                            "Looking at " +  String.valueOf(notif.flags) + " vs " + String.valueOf(Notification.FLAG_ONGOING_EVENT));
                }
                if ((notif.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT){
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG,
                                "Event is a notification, notification flag contains ongoing, and no ongoing notification is true. Returning.");
                    }
                    return;
                }
            } else {
                if (Constants.IS_LOGGABLE) {
                    Log.i(Constants.LOG_TAG,
                            "Event is not a notification.");
                }
            }
        }


        // Handle the do not disturb screen on settings
        PowerManager powMan = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (Constants.IS_LOGGABLE) {
            Log.d(Constants.LOG_TAG, "NotificationService.onAccessibilityEvent: notifScreenOn=" + notifScreenOn
                    + "  screen=" + powMan.isScreenOn());
        }
        if (!notifScreenOn && powMan.isScreenOn()) {
            return;
        }

        if (event == null) {
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Event is null. Returning.");
            }
            return;
        }
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "Event: " + event.toString());
        }

        // main logic
        PackageManager pm = getPackageManager();

        String eventPackageName;
        if (event.getPackageName() != null){
            eventPackageName = event.getPackageName().toString();
        } else {
            eventPackageName = "";
        }
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "Service package list is: ");
            for (String strPackage : packages) {
                Log.i(Constants.LOG_TAG, strPackage);
            }
            Log.i(Constants.LOG_TAG, "End Service package list");
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

        // get the title
        String title = "";
        try {
            title = pm.getApplicationLabel(pm.getApplicationInfo(eventPackageName, 0)).toString();
        } catch (NameNotFoundException e) {
            title = eventPackageName;
        }

        // get the notification text
        String notificationText = event.getText().toString();
        // strip the first and last characters which are [ and ]
        notificationText = notificationText.substring(1, notificationText.length() - 1);

        if (notification_extras) {
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Fetching extras from notification");
            }
            Parcelable parcelable = event.getParcelableData();
            if (parcelable instanceof Notification) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    notificationText += "\n" + getExtraBigData((Notification) parcelable, notificationText.trim());
                } else {
                    notificationText += "\n" + getExtraData((Notification) parcelable, notificationText.trim());
                }

            }
        }

        // Check ignore lists
        for(int i = 0; i < ignores.length(); i++){
            try{
                JSONObject ignore = ignores.getJSONObject(i);
                String app = ignore.getString("app");
                if((!app.equals("-1")) && (!eventPackageName.equalsIgnoreCase(app))){
                    //this rule doesn't apply to all apps and this isn't the app we're looking for.
                    continue;
                }
                String regex = "";
                if(!ignore.getBoolean("raw")){
                    regex = "(?i)" +Pattern.quote(ignore.getString("match"));
                } else {
                    regex = ignore.getString("match");
                }
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(notificationText);
                if(m.find()){
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG, "Notification text of '" + notificationText + "' matches: '" + regex +"'. Returning");
                    }
                    return;
                } else {
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG, "Notification text of '" + notificationText + "' does not match: '" + regex +"'. Returning");
                    }
                }

            } catch (JSONException e){
                continue;
            }
        }


        // Send the alert to Pebble

        sendToPebble(title, notificationText);

        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, event.toString());
            Log.i(Constants.LOG_TAG, event.getPackageName().toString());
        }
    }

    private void sendToPebble(String title, String notificationText) {
        title = title.trim();
        notificationText = notificationText.trim();
        if (title.trim().isEmpty() || notificationText.isEmpty()) {
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Detected empty title or notification text, skipping");
            }
            return;
        }
        for(int i = 0; i < converts.length(); i++){
            String from;
            String to;
            try{
                JSONObject convert = converts.getJSONObject(i);
                from = "(?i)" + Pattern.quote(convert.getString("from"));
                to = convert.getString("to");
            } catch (JSONException e){
                continue;
            }
            //not sure if the title should be replaced as well or not. I'm guessing not
            //title = title.replaceAll(from, to);
            notificationText = notificationText.replaceAll(from, to);
        }

        // Create json object to be sent to Pebble
        final Map<String, Object> data = new HashMap<String, Object>();

        data.put("title", title);

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
            Log.d(Constants.LOG_TAG, "About to send a modal alert to Pebble: " + notificationData);
        }
        sendBroadcast(i);
        notification_last_sent = System.currentTimeMillis();

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
        queue = new LinkedList<queueItem>();
    }

    private void loadPrefs() {
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "I am loading preferences");
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
        //if old preferences exist, convert them.
        if(sharedPreferences.contains(Constants.LOG_TAG + ".mode")){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(Constants.PREFERENCE_MODE, sharedPreferences.getInt(Constants.LOG_TAG + ".mode", Constants.Mode.OFF.ordinal()));
            editor.putString(Constants.PREFERENCE_PACKAGE_LIST, sharedPreferences.getString(Constants.LOG_TAG + ".packageList", ""));
            editor.putBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, sharedPreferences.getBoolean(Constants.LOG_TAG + ".notificationsOnly", true));
            editor.putBoolean(Constants.PREFERENCE_NOTIFICATION_EXTRA, sharedPreferences.getBoolean(Constants.LOG_TAG + ".fetchNotificationExtras", false));
            editor.commit();

            //clear out all old preferences
            editor = sharedPreferences.edit();
            editor.clear();
            editor.commit();
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Converted preferences to new format. Old ones should be completely gone.");
            }

        }

        mode = Mode.values()[sharedPref.getInt(Constants.PREFERENCE_MODE, Mode.OFF.ordinal())];

        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG,
                    "Service package list is: " + sharedPref.getString(Constants.PREFERENCE_PACKAGE_LIST, ""));
        }

        packages = sharedPref.getString(Constants.PREFERENCE_PACKAGE_LIST, "").split(",");
        notifications_only = sharedPref.getBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, true);
        no_ongoing_notifs = sharedPref.getBoolean(Constants.PREFERENCE_NO_ONGOING_NOTIF, false);
        min_notification_wait = sharedPref.getInt(Constants.PREFERENCE_MIN_NOTIFICATION_WAIT, 0) * 1000;
        notification_extras = sharedPref.getBoolean(Constants.PREFERENCE_NOTIFICATION_EXTRA, false);
        notifScreenOn = sharedPref.getBoolean(Constants.PREFERENCE_NOTIF_SCREEN_ON, true);
        quiet_hours = sharedPref.getBoolean(Constants.PREFERENCE_QUIET_HOURS, false);
        try{
            converts = new JSONArray(sharedPref.getString(Constants.PREFERENCE_CONVERTS, "[]"));
        } catch (JSONException e){
            converts = new JSONArray();
        }
        try{
            ignores = new JSONArray(sharedPref.getString(Constants.PREFERENCE_IGNORE, "[]"));
        } catch (JSONException e){
            converts = new JSONArray();
        }
        //we only need to pull this if quiet hours are enabled. Save the cycles for the cpu! (haha)
        if(quiet_hours){
            String[] pieces = sharedPref.getString(Constants.PREFERENCE_QUIET_HOURS_BEFORE, "00:00").split(":");
            quiet_hours_before= new Date(0, 0, 0, Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
            pieces = sharedPref.getString(Constants.PREFERENCE_QUIET_HOURS_AFTER, "23:59").split(":");
            quiet_hours_after = new Date(0, 0, 0, Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]));
        }

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
        try {
            ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
            views.reapply(getApplicationContext(), localView);
            return dumpViewGroup(0, localView, existing_text);
        } catch (android.content.res.Resources.NotFoundException e) {
            return "";
        } catch (RemoteViews.ActionException e) {
            return "";
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private String getExtraBigData(Notification notification, String existing_text) {
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "I am running extra big data");
        }
        RemoteViews views = null;
        try {
            views = notification.bigContentView;
        } catch (NoSuchFieldError e) {
            return getExtraData(notification, existing_text);
        }
        if (views == null) {
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "bigContentView was empty, running normal");
            }
            return getExtraData(notification, existing_text);
        }
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        try {
            ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
            views.reapply(getApplicationContext(), localView);
            return dumpViewGroup(0, localView, existing_text);
        } catch (android.content.res.Resources.NotFoundException e) {
            return "";
        }
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
                    Log.d(Constants.LOG_TAG, "I am going to skip this, but if I didn't, the text would be: "
                            + ((TextView) v).getText().toString());
                }
                if (existing_text.isEmpty() && v.getId() == android.R.id.title) {
                    if (Constants.IS_LOGGABLE) {
                        Log.d(Constants.LOG_TAG,
                                "I was going to skip this, but the existing text was empty, and I need something.");
                    }
                } else {
                    continue;
                }
            }

            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                if (tv.getText().toString() == "..." || tv.getText().toString() == "�"
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
