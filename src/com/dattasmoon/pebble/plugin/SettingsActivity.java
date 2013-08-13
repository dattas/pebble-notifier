/*
Copyright (c) 2013 Robin Sheat and Dattas Moonchaser

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

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/**
 * This activity handles any logic for the settings screen (of which there is\
 * currently none.)
 * 
 * @author robin
 */
public class SettingsActivity extends PreferenceActivity {
    int pref_version_clicks = 0;

    // Using the deprecated methods because that supports android < 3 without
    // too much hassle.
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
        //if old preferences exist, convert them.
        if(sharedPreferences.contains(Constants.LOG_TAG + ".mode")){
            SharedPreferences sharedPref = getSharedPreferences(Constants.LOG_TAG + "_preferences", MODE_MULTI_PROCESS | MODE_PRIVATE);
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

        PreferenceCategory pc = (PreferenceCategory) findPreference("About");
        Preference pref_version = new Preference(this);
        pref_version.setTitle("Version");
        PackageInfo packageInfo;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            pref_version.setSummary(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        pc.addPreference(pref_version);
        pref_version.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                pref_version_clicks++;
                if(pref_version_clicks > 5){
                    final Dialog easterDialog = new Dialog(SettingsActivity.this);
                    easterDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                    easterDialog.setContentView(getLayoutInflater().inflate(R.layout.dialog_easter_hidden, null));
                    easterDialog.findViewById(R.id.btnOK).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            easterDialog.dismiss();
                        }
                    });
                    easterDialog.show();
                    pref_version_clicks = 0;
                }
                return true;


            }
        });

        Preference pref_donate = findPreference("pref_donate");
        pref_donate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                //send intent
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(Constants.DONATION_URL));
                startActivity(i);
                return true;
            }
        });


    }

    @Override
    protected void onPause(){
        File watchFile = new File(getFilesDir() + "PrefsChanged.none");
        if (!watchFile.exists()) {
            try {
                watchFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            watchFile.setLastModified(System.currentTimeMillis());
        }
        super.onPause();
    }

}
