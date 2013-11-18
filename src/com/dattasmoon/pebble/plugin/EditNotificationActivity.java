/* 
Copyright (c) 2013 Dattas Moonchaser

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dattasmoon.pebble.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class EditNotificationActivity extends AbstractPluginActivity {

    ListView          lvPackages;
    TextView          tvTaskerNotice;
    Constants.Mode    mMode;
    Spinner           spMode;
    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_notification);

        lvPackages = (ListView) findViewById(R.id.listPackages);
        spMode = (Spinner) findViewById(R.id.spMode);
        tvTaskerNotice = (TextView) findViewById(R.id.tvTaskerNotice);
        findViewById(R.id.tvAccessibilityError).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.mode_choices,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMode.setAdapter(adapter);
        spMode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                mMode = Constants.Mode.values()[pos];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mMode = Constants.Mode.OFF;
                if (Constants.IS_LOGGABLE) {
                    Log.i(Constants.LOG_TAG, "Mode is: off");
                }
            }
        });



    }

    @Override
    public void onResume(){
        super.onResume();
        if (mode == Mode.STANDARD) {
            sharedPreferences = getSharedPreferences(Constants.LOG_TAG+"_preferences", MODE_MULTI_PROCESS | MODE_PRIVATE);
            if (sharedPreferences.getBoolean(Constants.PREFERENCE_TASKER_SET, false)) {
                tvTaskerNotice.setVisibility(View.VISIBLE);
            }
            spMode.setSelection(sharedPreferences.getInt(Constants.PREFERENCE_MODE, Constants.Mode.OFF.ordinal()));

            // legacy preference handler
            if (sharedPreferences.contains(Constants.PREFERENCE_EXCLUDE_MODE)) {
                if (sharedPreferences.getBoolean(Constants.PREFERENCE_EXCLUDE_MODE, false)) {
                    spMode.setSelection(Constants.Mode.INCLUDE.ordinal());
                } else {
                    spMode.setSelection(Constants.Mode.EXCLUDE.ordinal());
                }
            }
        } else if (mode == Mode.LOCALE) {
            if (localeBundle != null) {
                spMode.setSelection(localeBundle.getInt(Constants.BUNDLE_EXTRA_INT_MODE, Constants.Mode.OFF.ordinal()));
            }
        }
        checkAccessibilityService();

        if (findViewById(R.id.listPackages).isEnabled()) {
            new LoadAppsTask().execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_edit_notifications, menu);
        if(mode == Mode.LOCALE){
            menu.removeItem(R.id.btnSettings);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "Selected menu item id: " + String.valueOf(item.getItemId()));
        }
        switch (item.getItemId()) {
        case R.id.btnUncheckAll:
            AlertDialog dialog = new AlertDialog.Builder(this).create();
            dialog.setTitle(R.string.dialog_confirm_title);
            dialog.setMessage(getString(R.string.dialog_uncheck_message));
            dialog.setCancelable(false);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.confirm), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int buttonId) {
                    if (lvPackages == null || lvPackages.getAdapter() == null || ((packageAdapter) lvPackages.getAdapter()).selected == null){
                        //something went wrong
                        return;
                    }
                    ((packageAdapter) lvPackages.getAdapter()).selected.clear();
                    lvPackages.invalidateViews();
                }
            });
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.decline), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int buttonId) {
                    //do nothing!
                }
            });
            dialog.setIcon(android.R.drawable.ic_dialog_alert);
            dialog.show();

            return true;
        case R.id.btnSave:
            finish();
            return true;
        case R.id.btnDonate:
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(Constants.DONATION_URL));
            startActivity(i);
            return true;
        case R.id.btnSettings:
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        return onOptionsItemSelected(item);
    }

    public void checkAccessibilityService() {
        int accessibilityEnabled = 0;
        boolean accessibilityFound = false;
        try {
            accessibilityEnabled = Settings.Secure.getInt(this.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (SettingNotFoundException e) {
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();
                    if (accessabilityService.equalsIgnoreCase(Constants.ACCESSIBILITY_SERVICE)) {
                        accessibilityFound = true;
                        break;
                    }
                }
            }
        }
        if (!accessibilityFound) {
            findViewById(R.id.tvAccessibilityError).setVisibility(View.VISIBLE);
            findViewById(R.id.spMode).setVisibility(View.GONE);
            findViewById(R.id.tvMode).setVisibility(View.GONE);
            findViewById(android.R.id.empty).setVisibility(View.GONE);
            findViewById(R.id.listPackages).setEnabled(false);
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "The accessibility service is NOT on!");
            }

        } else {
            findViewById(R.id.tvAccessibilityError).setVisibility(View.GONE);
            findViewById(R.id.spMode).setVisibility(View.VISIBLE);
            findViewById(R.id.tvMode).setVisibility(View.VISIBLE);
            findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            findViewById(R.id.listPackages).setEnabled(true);
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "The accessibility service is on!");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        save();
        super.onSaveInstanceState(outState);
    }

    public void save() {
        String selectedPackages = "";
        ArrayList<String> tmpArray = new ArrayList<String>();
        if (lvPackages == null || lvPackages.getAdapter() == null) {
            return;
        }
        for (String strPackage : ((packageAdapter) lvPackages.getAdapter()).selected) {
            if (!strPackage.isEmpty()) {
                if (!tmpArray.contains(strPackage)) {
                    tmpArray.add(strPackage);
                    selectedPackages += strPackage + ",";
                }
            }
        }
        tmpArray.clear();
        tmpArray = null;
        if (!selectedPackages.isEmpty()) {
            selectedPackages = selectedPackages.substring(0, selectedPackages.length() - 1);
        }
        if (Constants.IS_LOGGABLE) {
            switch (mMode) {
            case OFF:
                Log.i(Constants.LOG_TAG, "Mode is: off");
                break;
            case EXCLUDE:
                Log.i(Constants.LOG_TAG, "Mode is: exclude");
                break;
            case INCLUDE:
                Log.i(Constants.LOG_TAG, "Mode is: include");
                break;
            }

            Log.i(Constants.LOG_TAG, "Package list is: " + selectedPackages);
        }

        if (mode == Mode.STANDARD) {

            Editor editor = sharedPreferences.edit();
            editor.putInt(Constants.PREFERENCE_MODE, mMode.ordinal());
            editor.putString(Constants.PREFERENCE_PACKAGE_LIST, selectedPackages);

            // we saved via the application, reset the variable if it exists
            editor.remove(Constants.PREFERENCE_TASKER_SET);

            // clear out legacy preference, if it exists
            editor.remove(Constants.PREFERENCE_EXCLUDE_MODE);

            // save!
            editor.commit();

            // notify service via file that it needs to reload the preferences
            File watchFile = new File(getFilesDir() + "PrefsChanged.none");
            if (!watchFile.exists()) {
                try {
                    watchFile.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            watchFile.setLastModified(System.currentTimeMillis());
        } else if (mode == Mode.LOCALE) {
            if (!isCanceled()) {
                final Intent resultIntent = new Intent();
                final Bundle resultBundle = new Bundle();

                // set the version, title and body
                resultBundle.putInt(Constants.BUNDLE_EXTRA_INT_VERSION_CODE,
                        Constants.getVersionCode(getApplicationContext()));
                resultBundle.putInt(Constants.BUNDLE_EXTRA_INT_TYPE, Constants.Type.SETTINGS.ordinal());
                resultBundle.putInt(Constants.BUNDLE_EXTRA_INT_MODE, mMode.ordinal());
                resultBundle.putString(Constants.BUNDLE_EXTRA_STRING_PACKAGE_LIST, selectedPackages);
                String blurb = "";
                switch (mMode) {
                case OFF:
                    blurb = getResources().getStringArray(R.array.mode_choices)[0];
                    break;
                case INCLUDE:
                    blurb = getResources().getStringArray(R.array.mode_choices)[2];
                    break;
                case EXCLUDE:
                    blurb = getResources().getStringArray(R.array.mode_choices)[1];
                }
                Log.i(Constants.LOG_TAG, resultBundle.toString());

                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
                resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);
                setResult(RESULT_OK, resultIntent);
            }
        }

    }

    @Override
    public void finish() {
        if (!lvPackages.isEnabled()) {
            // if the list is not enabled, we don't want to save settings
            super.finish();
            return;
        }
        save();
        super.finish();
    }

    private class LoadAppsTask extends AsyncTask<Void, Integer, Void> {
        public ArrayList<String> selected;
        List<PackageInfo> pkgAppsList;
        List<ApplicationInfo> appsList;

        @Override
        protected void onPreExecute(){
            PackageManager pm = getPackageManager();
            try{
                pkgAppsList = pm.getInstalledPackages(0);
            } catch (RuntimeException e){
                //this is usually thrown when people have too many things installed (or bloatware in the case of Samsung devices)
                pm = getPackageManager();
                appsList = pm.getInstalledApplications(0);
            }

        }

        @Override
        protected Void doInBackground(Void... unused) {
            if (pkgAppsList == null && appsList == null) {
                //something went really bad here
                return null;
            }
            if (appsList == null) {
                appsList = new ArrayList<ApplicationInfo>();
                for(PackageInfo pkg : pkgAppsList){
                    appsList.add(pkg.applicationInfo);
                }
            }
            AppComparator comparer = new AppComparator(EditNotificationActivity.this);
            Collections.sort(appsList, comparer);
            selected = new ArrayList<String>();
            String packageList;
            if (mode == Mode.LOCALE) {
                if(Constants.IS_LOGGABLE){
                    Log.i(Constants.LOG_TAG, "Locale mode");
                }
                if (localeBundle != null) {
                    packageList = localeBundle.getString(Constants.BUNDLE_EXTRA_STRING_PACKAGE_LIST);
                    if (packageList == null) {
                        // this can be null if it doesn't currently exist in the
                        // locale bundle, handle gracefully
                        packageList = "";
                        if(Constants.IS_LOGGABLE){
                            Log.i(Constants.LOG_TAG, "Package list from locale bundle is currently null");
                        }
                    }
                } else {
                    packageList = "";
                    if(Constants.IS_LOGGABLE){
                        Log.i(Constants.LOG_TAG, "Locale bundle is null");
                    }
                }
            } else {
                if(Constants.IS_LOGGABLE){
                    Log.i(Constants.LOG_TAG, "I am pulling from sharedPrefs");
                }
                packageList = sharedPreferences.getString(Constants.PREFERENCE_PACKAGE_LIST, "");
            }
            if(Constants.IS_LOGGABLE){
                Log.i(Constants.LOG_TAG, "Package list is: " + packageList);
            }
            for (String strPackage : packageList.split(",")) {
                // only add the ones that are still installed, providing cleanup
                // and faster speeds all in one!
                for (ApplicationInfo info : appsList) {
                    if (info.packageName.equalsIgnoreCase(strPackage)) {
                        selected.add(strPackage);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if (appsList == null) {
                //something went wrong
                return;
            }
            lvPackages.setAdapter(new packageAdapter(EditNotificationActivity.this, appsList
                    .toArray(new ApplicationInfo[appsList.size()]), selected));
            findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
    }

    private class packageAdapter extends ArrayAdapter<ApplicationInfo> implements OnCheckedChangeListener, OnClickListener {
        private final Context       context;
        private final PackageManager pm;
        private final ApplicationInfo[] packages;
        public ArrayList<String>    selected;

        public packageAdapter(Context context, ApplicationInfo[] packages, ArrayList<String> selected) {
            super(context, R.layout.list_application_item, packages);
            this.context = context;
            this.pm = context.getPackageManager();
            this.packages = packages;
            this.selected = selected;
        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent) {
            ListViewHolder viewHolder = null;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.list_application_item, parent, false);

                viewHolder = new ListViewHolder();

                viewHolder.textView = (TextView) rowView.findViewById(R.id.tvPackage);
                viewHolder.imageView = (ImageView) rowView.findViewById(R.id.ivIcon);
                viewHolder.chkEnabled = (CheckBox) rowView.findViewById(R.id.chkEnabled);
                viewHolder.chkEnabled.setOnCheckedChangeListener(this);

                rowView.setOnClickListener(this);
                rowView.setTag(viewHolder);
            } else {
                viewHolder = (ListViewHolder) rowView.getTag();
            }
            ApplicationInfo info = packages[position];

            String appName;
            try {
               appName = info.loadLabel(pm).toString();
            } catch (NullPointerException e ){
                appName = null;
            }

            if(appName != null){
                viewHolder.textView.setText(appName);
            } else {
                viewHolder.textView.setText("");
            }
            Drawable icon;
            try {
                icon = info.loadIcon(pm);
            } catch (NullPointerException e){
                icon = null;
            }
            if(icon != null){
                viewHolder.imageView.setImageDrawable(icon);
            }
            viewHolder.chkEnabled.setTag(info.packageName);

            boolean boolSelected = false;

            for (String strPackage : selected) {
                if (info.packageName.equalsIgnoreCase(strPackage)) {

                    boolSelected = true;
                    break;
                }
            }
            viewHolder.chkEnabled.setChecked(boolSelected);

            return rowView;
        }

        @Override
        public void onCheckedChanged(CompoundButton chkEnabled, boolean newState) {

            String strPackage = (String) chkEnabled.getTag();

            if (strPackage.isEmpty()) {
                return;
            }
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Check changed on " + strPackage);
            }
            if (newState) {
                if (!selected.contains(strPackage)) {
                    selected.add(strPackage);
                }
            } else {
                while (selected.contains(strPackage)) {
                    selected.remove(strPackage);
                }
            }
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Selected count is: " + String.valueOf(selected.size()));
            }

        }

        @Override
        public void onClick(View rowView) {
            ((CheckBox) rowView.findViewById(R.id.chkEnabled)).performClick();

        }
    }

    public static class ListViewHolder {
        public TextView  textView;
        public CheckBox  chkEnabled;
        public ImageView imageView;
    }

    public class AppComparator implements Comparator<ApplicationInfo> {
        final PackageManager pm;
        public AppComparator(Context context){
            this.pm = context.getPackageManager();
        }

        @Override
        public int compare(ApplicationInfo leftPackage, ApplicationInfo rightPackage) {

            String leftName = leftPackage.loadLabel(pm).toString();
            String rightName = rightPackage.loadLabel(pm).toString();

            return leftName.compareToIgnoreCase(rightName);
        }
    }
}
