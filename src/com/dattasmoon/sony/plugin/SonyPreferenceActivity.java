/* 
Copyright (c) 2013 Dattas Moonchaser

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.dattasmoon.sony.plugin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.widget.Toast;

import com.dattasmoon.pebble.plugin.Constants;
import com.dattasmoon.pebble.plugin.R;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;

/**
 * Create the few preferences needed for the Sony preference activity that is
 * launched via Sony Smart Connect
 */
public class SonyPreferenceActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add the minimal set of preferences specific to Sony Smart Connect
        addPreferencesFromResource(R.xml.sony_preferences);

        // Add a preference to launch the main app for primary configuration
        Preference preference = new Preference(this);
        preference.setTitle(R.string.pref_option_launch_app);
        preference.setSummary(R.string.pref_option_launch_app_txt);
        PreferenceCategory pc = (PreferenceCategory) findPreference("Sony SW2");
        pc.addPreference(preference);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.dattasmoon.pebble.plugin");
                startActivity(LaunchIntent);
                return true;
            }
        });

        // Handle clear all events
        preference = findPreference(getString(R.string.pref_key_clear));
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
            nbrDeleted = NotificationUtil.deleteAllEvents(SonyPreferenceActivity.this);
            return nbrDeleted;
        }

        @Override
        protected void onPostExecute(Integer id) {
            if (id != NotificationUtil.INVALID_ID) {
                Toast.makeText(SonyPreferenceActivity.this, R.string.clear_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SonyPreferenceActivity.this, R.string.clear_failure, Toast.LENGTH_SHORT).show();
            }
        }

    }
}
