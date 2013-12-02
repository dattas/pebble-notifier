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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import com.dattasmoon.pebble.plugin.Constants;
import com.dattasmoon.pebble.plugin.R;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;

/**
 * The sample preference activity lets the user toggle start/stop of periodic
 * data insertion. It also allows the user to clear all events associated with
 * this extension.
 */
public class SonyPreferenceActivity extends PreferenceActivity {

    private static final int DIALOG_READ_ME = 1;

    private static final int DIALOG_CLEAR   = 2;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Handle clear all events
        Preference preference = findPreference(getString(R.string.pref_key_clear));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDialog(DIALOG_CLEAR);
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
        case DIALOG_CLEAR:
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
