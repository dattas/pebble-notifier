/* 
Copyright (c) 2013 Dattas Moonchaser

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.dattasmoon.sony.plugin;

import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;

import com.dattasmoon.pebble.plugin.Constants;
import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

/**
 * The sample extension service handles extension registration and inserts data
 * into the notification database.
 */
public class SonyExtensionService extends ExtensionService {

    public SonyExtensionService() {
        super(Constants.EXTENSION_KEY);
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.LOG_TAG, "onCreate");
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Service#onStartCommand()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int retVal = super.onStartCommand(intent, flags, startId);
        if (intent != null && Constants.INTENT_ACTION_ADD.equals(intent.getAction())) {
            Log.d(Constants.LOG_TAG, "onStart action: INTENT_ACTION_ADD");
            Bundle extras = intent.getExtras();
            addData((String) extras.get("title"), (String) extras.get("body"));
            stopSelfCheck();
        }

        return retVal;
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.LOG_TAG, "onDestroy");
    }

    /**
     * Add some "random" data
     */
    private void addData(String name, String message) {
        long time = System.currentTimeMillis();
        long sourceId = NotificationUtil.getSourceId(this, Constants.EXTENSION_SPECIFIC_ID);
        if (sourceId == NotificationUtil.INVALID_ID) {
            Log.e(Constants.LOG_TAG, "Failed to insert data");
            return;
        }
        ContentValues eventValues = new ContentValues();
        eventValues.put(Notification.EventColumns.EVENT_READ_STATUS, false);
        eventValues.put(Notification.EventColumns.DISPLAY_NAME, name);
        eventValues.put(Notification.EventColumns.MESSAGE, message);
        eventValues.put(Notification.EventColumns.PERSONAL, 1);
        eventValues.put(Notification.EventColumns.PUBLISHED_TIME, time);
        eventValues.put(Notification.EventColumns.SOURCE_ID, sourceId);

        try {
            getContentResolver().insert(Notification.Event.URI, eventValues);
        } catch (IllegalArgumentException e) {
            Log.e(Constants.LOG_TAG, "Failed to insert event", e);
        } catch (SecurityException e) {
            Log.e(Constants.LOG_TAG, "Failed to insert event, is Live Ware Manager installed?", e);
        } catch (SQLException e) {
            Log.e(Constants.LOG_TAG, "Failed to insert event", e);
        }
    }

    @Override
    protected void onViewEvent(Intent intent) {
        // String action =
        // intent.getStringExtra(Notification.Intents.EXTRA_ACTION);
        // String hostAppPackageName = intent
        // .getStringExtra(Registration.Intents.EXTRA_AHA_PACKAGE_NAME);
        // boolean advancedFeaturesSupported =
        // DeviceInfoHelper.isSmartWatch2ApiAndScreenDetected(
        // this, hostAppPackageName);
        //
        // int eventId = intent.getIntExtra(Notification.Intents.EXTRA_EVENT_ID,
        // -1);
        // if (Notification.SourceColumns.ACTION_1.equals(action)) {
        // doAction1(eventId);
        // } else if (Notification.SourceColumns.ACTION_2.equals(action)) {
        // // Here we can take different actions depending on the device.
        // if (advancedFeaturesSupported) {
        // Toast.makeText(this, "Action 2 API level 2",
        // Toast.LENGTH_LONG).show();
        // } else {
        // Toast.makeText(this, "Action 2", Toast.LENGTH_LONG).show();
        // }
        // } else if (Notification.SourceColumns.ACTION_3.equals(action)) {
        // Toast.makeText(this, "Action 3", Toast.LENGTH_LONG).show();
        // }
    }

    @Override
    protected void onRefreshRequest() {
        // Do nothing here, only relevant for polling extensions, this
        // extension is always up to date
    }

    // /**
    // * Show toast with event information
    // *
    // * @param eventId The event id
    // */
    // public void doAction1(int eventId) {
    // Log.d(LOG_TAG, "doAction1 event id: " + eventId);
    // Cursor cursor = null;
    // try {
    // String name = "";
    // String message = "";
    // cursor = getContentResolver().query(Notification.Event.URI, null,
    // Notification.EventColumns._ID + " = " + eventId, null, null);
    // if (cursor != null && cursor.moveToFirst()) {
    // int nameIndex =
    // cursor.getColumnIndex(Notification.EventColumns.DISPLAY_NAME);
    // int messageIndex =
    // cursor.getColumnIndex(Notification.EventColumns.MESSAGE);
    // name = cursor.getString(nameIndex);
    // message = cursor.getString(messageIndex);
    // }
    //
    // String toastMessage = getText(R.string.action_event_1) + ", Event: " +
    // eventId
    // + ", Name: " + name + ", Message: " + message;
    // Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    // } catch (SQLException e) {
    // Log.e(LOG_TAG, "Failed to query event", e);
    // } catch (SecurityException e) {
    // Log.e(LOG_TAG, "Failed to query event", e);
    // } catch (IllegalArgumentException e) {
    // Log.e(LOG_TAG, "Failed to query event", e);
    // } finally {
    // if (cursor != null) {
    // cursor.close();
    // }
    // }
    // }

    /**
     * Called when extension and sources has been successfully registered.
     * Override this method to take action after a successful registration.
     */
    @Override
    public void onRegisterResult(boolean result) {
        super.onRegisterResult(result);
        Log.d(Constants.LOG_TAG, "onRegisterResult");
    }

    @Override
    protected RegistrationInformation getRegistrationInformation() {
        return new SonyRegistrationInformation(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sonyericsson.extras.liveware.aef.util.ExtensionService#
     * keepRunningWhenConnected()
     */
    @Override
    protected boolean keepRunningWhenConnected() {
        return false;
    }
}
