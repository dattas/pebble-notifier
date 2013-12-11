/* 
Copyright (c) 2013 Dattas Moonchaser

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.dattasmoon.sony.plugin;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;

import com.dattasmoon.pebble.plugin.Constants;
import com.dattasmoon.pebble.plugin.R;
import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

public class SonyRegistrationInformation extends RegistrationInformation {

    final Context mContext;

    /**
     * Create notification registration object
     * 
     * @param context
     *            The context
     */
    protected SonyRegistrationInformation(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        mContext = context;
    }

    @Override
    public int getRequiredNotificationApiVersion() {
        return 1;
    }

    @Override
    public int getRequiredWidgetApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredControlApiVersion() {
        return 0;
    }

    @Override
    public int getRequiredSensorApiVersion() {
        return 0;
    }

    @Override
    public ContentValues getExtensionRegistrationConfiguration() {
        String extensionIcon = ExtensionUtils.getUriString(mContext, R.drawable.icon_extension);
        String iconHostapp = ExtensionUtils.getUriString(mContext, R.drawable.ic_launcher);
        String extensionIcon48 = ExtensionUtils.getUriString(mContext, R.drawable.icon_extension_48);

        String configurationText = mContext.getString(R.string.pref_sony_activity_title);
        String extensionName = mContext.getString(R.string.extension_name);

        ContentValues values = new ContentValues();
        values.put(Registration.ExtensionColumns.CONFIGURATION_ACTIVITY, SonyPreferenceActivity.class.getName());
        values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT, configurationText);
        values.put(Registration.ExtensionColumns.EXTENSION_ICON_URI, extensionIcon);
        values.put(Registration.ExtensionColumns.EXTENSION_48PX_ICON_URI, extensionIcon48);

        values.put(Registration.ExtensionColumns.EXTENSION_KEY, Constants.EXTENSION_KEY);
        values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI, iconHostapp);
        values.put(Registration.ExtensionColumns.NAME, extensionName);
        values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION, getRequiredNotificationApiVersion());
        values.put(Registration.ExtensionColumns.PACKAGE_NAME, mContext.getPackageName());

        return values;
    }

    @Override
    public ContentValues[] getSourceRegistrationConfigurations() {
        List<ContentValues> bulkValues = new ArrayList<ContentValues>();
        bulkValues.add(getSourceRegistrationConfiguration(Constants.EXTENSION_SPECIFIC_ID));
        return bulkValues.toArray(new ContentValues[bulkValues.size()]);
    }

    /**
     * Get source configuration associated with extensions specific id
     * 
     * @param extensionSpecificId
     * @return The source configuration
     */
    public ContentValues getSourceRegistrationConfiguration(String extensionSpecificId) {
        ContentValues sourceValues = null;

        String iconSource1 = ExtensionUtils.getUriString(mContext, R.drawable.icn_30x30_message_notification);
        String iconSource2 = ExtensionUtils.getUriString(mContext, R.drawable.icn_18x18_message_notification);
        String iconBw = ExtensionUtils.getUriString(mContext, R.drawable.icn_18x18_black_white_message_notification);
        String textToSpeech = mContext.getString(R.string.text_to_speech);
        sourceValues = new ContentValues();
        sourceValues.put(Notification.SourceColumns.ENABLED, true);
        sourceValues.put(Notification.SourceColumns.ICON_URI_1, iconSource1);
        sourceValues.put(Notification.SourceColumns.ICON_URI_2, iconSource2);
        sourceValues.put(Notification.SourceColumns.ICON_URI_BLACK_WHITE, iconBw);
        sourceValues.put(Notification.SourceColumns.UPDATE_TIME, System.currentTimeMillis());
        sourceValues.put(Notification.SourceColumns.NAME, mContext.getString(R.string.source_name));
        sourceValues.put(Notification.SourceColumns.EXTENSION_SPECIFIC_ID, extensionSpecificId);
        sourceValues.put(Notification.SourceColumns.PACKAGE_NAME, mContext.getPackageName());
        sourceValues.put(Notification.SourceColumns.TEXT_TO_SPEECH, textToSpeech);
        // sourceValues.put(Notification.SourceColumns.ACTION_1,
        // mContext.getString(R.string.action_event_1));
        // sourceValues.put(Notification.SourceColumns.ACTION_2,
        // mContext.getString(R.string.action_event_2));
        // sourceValues.put(Notification.SourceColumns.ACTION_3,
        // mContext.getString(R.string.action_event_3));
        // sourceValues.put(Notification.SourceColumns.ACTION_ICON_1,
        // ExtensionUtils.getUriString(mContext, R.drawable.actions_1));
        // sourceValues.put(Notification.SourceColumns.ACTION_ICON_2,
        // ExtensionUtils.getUriString(mContext, R.drawable.actions_2));
        // sourceValues.put(Notification.SourceColumns.ACTION_ICON_3,
        // ExtensionUtils.getUriString(mContext, R.drawable.actions_3));
        return sourceValues;
    }
}
