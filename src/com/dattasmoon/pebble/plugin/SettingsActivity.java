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

import java.io.File;
import java.io.IOException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;

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
        // if old preferences exist, convert them.
        if (sharedPreferences.contains(Constants.LOG_TAG + ".mode")) {
            SharedPreferences sharedPref = getSharedPreferences(Constants.LOG_TAG + "_preferences", MODE_MULTI_PROCESS
                    | MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(Constants.PREFERENCE_MODE,
                    sharedPreferences.getInt(Constants.LOG_TAG + ".mode", Constants.Mode.OFF.ordinal()));
            editor.putString(Constants.PREFERENCE_PACKAGE_LIST,
                    sharedPreferences.getString(Constants.LOG_TAG + ".packageList", ""));
            editor.putBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY,
                    sharedPreferences.getBoolean(Constants.LOG_TAG + ".notificationsOnly", true));
            editor.putBoolean(Constants.PREFERENCE_NOTIFICATION_EXTRA,
                    sharedPreferences.getBoolean(Constants.LOG_TAG + ".fetchNotificationExtras", false));
            editor.commit();

            // clear out all old preferences
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
            pref_version.setSummary(packageInfo.versionName + " - build " + packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        pc.addPreference(pref_version);
        pref_version.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                pref_version_clicks++;
                if (pref_version_clicks > 5) {
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
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // send intent
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(Constants.DONATION_URL));
                startActivity(i);
                return true;
            }
        });

        // Handle clear all events
        Preference preference = findPreference(getString(R.string.pref_key_clear));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDialog(Constants.DIALOG_CLEAR);
                return true;
            }
        });

        // Remove preferences that are not supported by the accessory
        if (!ExtensionUtils.supportsHistory(getIntent())) {
            preference = findPreference(getString(R.string.pref_key_clear));
            getPreferenceScreen().removePreference(preference);
        }
    }

    @Override
    protected void onPause() {
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

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;

        switch (id) {
        case Constants.DIALOG_CLEAR:
            dialog = createClearDialog();
            break;
        default:
            Log.w(Constants.LOG_TAG, "Not a valid dialog id: " + id);
            break;
        }

        return dialog;
    }

    /**
     * Create the Clear events dialog
     * 
     * @return the Dialog
     */
    private Dialog createClearDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.pref_option_clear_txt).setTitle(R.string.pref_option_clear)
                .setIcon(android.R.drawable.ic_input_delete)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        new ClearEventsTask().execute();
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }

    /**
     * Clear all messaging events
     */
    private class ClearEventsTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Integer doInBackground(Void... params) {
            int nbrDeleted = 0;
            nbrDeleted = NotificationUtil.deleteAllEvents(SettingsActivity.this);
            return nbrDeleted;
        }

        @Override
        protected void onPostExecute(Integer id) {
            if (id != NotificationUtil.INVALID_ID) {
                Toast.makeText(SettingsActivity.this, R.string.clear_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SettingsActivity.this, R.string.clear_failure, Toast.LENGTH_SHORT).show();
            }
        }

    }

}
