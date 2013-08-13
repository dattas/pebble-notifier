/*
Copyright (c) 2013 Robin Sheat

Permission is hereby granted, free of charge, to any person obtaining a copy 
of this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to 
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
the Software, and to permit persons to whom the Software is furnished to do so, 
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
SOFTWARE.
 */
package com.dattasmoon.pebble.plugin;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * This activity handles any logic for the settings screen (of which there is\
 * currently none.)
 * 
 * @author robin
 */
public class SettingsActivity extends PreferenceActivity {

    // Using the deprecated methods because that supports android < 3 without
    // too much hassle.
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
        //if old preferences exist, convert them.
        if(sharedPreferences.contains(Constants.LOG_TAG + ".mode")){
            SharedPreferences sharedPref = getSharedPreferences(Constants.LOG_TAG+"_preferences", MODE_MULTI_PROCESS | MODE_PRIVATE);
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
            Toast.makeText(this, "Converted your old settings", Toast.LENGTH_SHORT).show();
        }
        addPreferencesFromResource(R.xml.preferences);
    }

}
