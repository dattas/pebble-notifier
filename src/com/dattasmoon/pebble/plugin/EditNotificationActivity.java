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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
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
    Handler           mHandler;
    JSONArray         arrayRenames;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_notification);
        mHandler = new Handler();

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
    public void onResume() {
        super.onResume();
        if (mode == Mode.STANDARD) {
            sharedPreferences = getSharedPreferences(Constants.LOG_TAG + "_preferences", MODE_MULTI_PROCESS
                    | MODE_PRIVATE);
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
        if (mode == Mode.LOCALE) {
            menu.removeItem(R.id.btnSettings);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "Selected menu item id: " + String.valueOf(item.getItemId()));
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v;
        ListViewHolder viewHolder;
        AdapterView.AdapterContextMenuInfo contextInfo;
        String app_name;
        final String package_name;
        switch (item.getItemId()) {
        case R.id.btnUncheckAll:

            builder.setTitle(R.string.dialog_confirm_title);
            builder.setMessage(getString(R.string.dialog_uncheck_message));
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int buttonId) {
                    if (lvPackages == null || lvPackages.getAdapter() == null
                            || ((packageAdapter) lvPackages.getAdapter()).selected == null) {
                        // something went wrong
                        return;
                    }
                    ((packageAdapter) lvPackages.getAdapter()).selected.clear();
                    lvPackages.invalidateViews();
                }
            });
            builder.setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int buttonId) {
                    // do nothing!
                }
            });
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.show();

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
        case R.id.btnRename:
            contextInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            int position = contextInfo.position;
            long id = contextInfo.id;
            // the child view who's info we're viewing (should be equal to v)
            v = contextInfo.targetView;
            app_name = ((TextView) v.findViewById(R.id.tvPackage)).getText().toString();
            viewHolder = (ListViewHolder) v.getTag();
            if (viewHolder == null || viewHolder.chkEnabled == null) {
                // failure
                return true;
            }
            package_name = (String) viewHolder.chkEnabled.getTag();
            builder.setTitle(R.string.dialog_title_rename_notification);
            final EditText input = new EditText(this);
            input.setHint(app_name);
            builder.setView(input);
            builder.setPositiveButton(R.string.confirm, null);
            builder.setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // do nothing
                }
            });
            final AlertDialog d = builder.create();
            d.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
                    if (b != null) {
                        b.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // can't be nothing
                                if (input.getText().length() > 0) {
                                    if (Constants.IS_LOGGABLE) {
                                        Log.i(Constants.LOG_TAG,
                                                "Adding rename for " + package_name + " to " + input.getText());
                                    }
                                    JSONObject rename = new JSONObject();
                                    try {
                                        rename.put("pkg", package_name);
                                        rename.put("to", input.getText());
                                        arrayRenames.put(rename);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    ((packageAdapter) lvPackages.getAdapter()).notifyDataSetChanged();

                                    d.dismiss();
                                } else {
                                    input.setText(R.string.error_cant_be_blank);
                                }

                            }
                        });
                    }
                }
            });

            d.show();

            return true;
        case R.id.btnRemoveRename:
            contextInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            // the child view who's info we're viewing (should be equal to v)
            v = contextInfo.targetView;
            app_name = ((TextView) v.findViewById(R.id.tvPackage)).getText().toString();
            viewHolder = (ListViewHolder) v.getTag();
            if (viewHolder == null || viewHolder.chkEnabled == null) {
                if (Constants.IS_LOGGABLE) {
                    Log.i(Constants.LOG_TAG, "Viewholder is null or chkEnabled is null");
                }
                // failure
                return true;
            }
            package_name = (String) viewHolder.chkEnabled.getTag();
            builder.setTitle(getString(R.string.dialog_title_remove_rename) + app_name + " (" + package_name + ")?");
            builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG, "Before remove is: " + String.valueOf(arrayRenames.length()));
                    }
                    JSONArray tmp = new JSONArray();
                    try {
                        for (int i = 0; i < arrayRenames.length(); i++) {
                            if (!arrayRenames.getJSONObject(i).getString("pkg").equalsIgnoreCase(package_name)) {
                                tmp.put(arrayRenames.getJSONObject(i));
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    arrayRenames = tmp;
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG, "After remove is: " + String.valueOf(arrayRenames.length()));
                    }
                    ((packageAdapter) lvPackages.getAdapter()).notifyDataSetChanged();
                }
            });
            builder.setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // do nothing
                }
            });
            builder.show();
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
            editor.putString(Constants.PREFERENCE_PKG_RENAMES, arrayRenames.toString());

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
                resultBundle.putString(Constants.BUNDLE_EXTRA_STRING_PKG_RENAMES, arrayRenames.toString());
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
        List<PackageInfo>        pkgAppsList;
        List<ApplicationInfo>    appsList;
        JSONArray                jsonRenames;

        @Override
        protected void onPreExecute() {
            PackageManager pm = getPackageManager();
            try {
                pkgAppsList = pm.getInstalledPackages(0);
            } catch (RuntimeException e) {
                // this is usually thrown when people have too many things
                // installed (or bloatware in the case of Samsung devices)
                pm = getPackageManager();
                appsList = pm.getInstalledApplications(0);
            }

        }

        @Override
        protected Void doInBackground(Void... unused) {
            if (pkgAppsList == null && appsList == null) {
                // something went really bad here
                return null;
            }
            if (appsList == null) {
                appsList = new ArrayList<ApplicationInfo>();
                for (PackageInfo pkg : pkgAppsList) {
                    appsList.add(pkg.applicationInfo);
                }
            }
            AppComparator comparer = new AppComparator(EditNotificationActivity.this);
            Collections.sort(appsList, comparer);
            selected = new ArrayList<String>();
            String packageList;
            String packageRenames;
            if (mode == Mode.LOCALE) {
                if (Constants.IS_LOGGABLE) {
                    Log.i(Constants.LOG_TAG, "Locale mode");
                }
                if (localeBundle != null) {
                    packageRenames = localeBundle.getString(Constants.BUNDLE_EXTRA_STRING_PKG_RENAMES);
                    packageList = localeBundle.getString(Constants.BUNDLE_EXTRA_STRING_PACKAGE_LIST);
                    if (packageList == null) {
                        // this can be null if it doesn't currently exist in the
                        // locale bundle, handle gracefully
                        packageList = "";
                        if (Constants.IS_LOGGABLE) {
                            Log.i(Constants.LOG_TAG, "Package list from locale bundle is currently null");
                        }
                    }
                    if (packageRenames == null) {
                        packageRenames = "[]";
                    }
                } else {
                    packageList = "";
                    packageRenames = "[]";
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG, "Locale bundle is null");
                    }
                }
            } else {
                if (Constants.IS_LOGGABLE) {
                    Log.i(Constants.LOG_TAG, "I am pulling from sharedPrefs");
                }
                packageList = sharedPreferences.getString(Constants.PREFERENCE_PACKAGE_LIST, "");
                packageRenames = sharedPreferences.getString(Constants.PREFERENCE_PKG_RENAMES, "[]");
            }
            if (Constants.IS_LOGGABLE) {
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
            try {
                jsonRenames = new JSONArray(packageRenames);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if (appsList == null) {
                // something went wrong
                return;
            }
            if (jsonRenames == null) {
                arrayRenames = new JSONArray();
            } else {
                arrayRenames = jsonRenames;
            }
            findViewById(android.R.id.empty).setVisibility(View.GONE);
            lvPackages.setAdapter(new packageAdapter(EditNotificationActivity.this, appsList
                    .toArray(new ApplicationInfo[appsList.size()]), selected));

            lvPackages.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
                    AdapterView.AdapterContextMenuInfo contextInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
                    int position = contextInfo.position;
                    long id = contextInfo.id;
                    // the child view who's info we're viewing (should
                    // be equal to v)
                    View v = contextInfo.targetView;
                    MenuInflater inflater = getMenuInflater();
                    inflater.inflate(R.menu.list_application_menu, menu);
                    ListViewHolder viewHolder = (ListViewHolder) v.getTag();
                    if (viewHolder.renamed) {
                        menu.findItem(R.id.btnRename).setVisible(false);
                        menu.findItem(R.id.btnRemoveRename).setVisible(true);
                    }

                }
            });

        }
    }

    private class packageAdapter extends ArrayAdapter<ApplicationInfo> implements OnCheckedChangeListener,
            OnClickListener {
        private final Context           context;
        private final PackageManager    pm;
        private final ApplicationInfo[] packages;
        public ArrayList<String>        selected;

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
                rowView = inflater.inflate(R.layout.list_application_item, null, false);

                viewHolder = new ListViewHolder();

                viewHolder.textView = (TextView) rowView.findViewById(R.id.tvPackage);
                viewHolder.imageView = (ImageView) rowView.findViewById(R.id.ivIcon);
                viewHolder.chkEnabled = (CheckBox) rowView.findViewById(R.id.chkEnabled);
                viewHolder.chkEnabled.setOnCheckedChangeListener(this);

                rowView.setOnClickListener(this);
                // really wish we didn't have to do this, but if we don't the
                // rowview will gobble this event up.
                rowView.setOnCreateContextMenuListener(null);
                rowView.setTag(viewHolder);
            } else {
                viewHolder = (ListViewHolder) rowView.getTag();
            }
            ApplicationInfo info = packages[position];

            String appName = null;
            viewHolder.renamed = false;
            try {
                for (int i = 0; i < arrayRenames.length(); i++) {
                    if (arrayRenames.getJSONObject(i).getString("pkg").equalsIgnoreCase(info.packageName)) {
                        viewHolder.renamed = true;
                        appName = arrayRenames.getJSONObject(i).getString("to");
                        viewHolder.textView.setTag(appName);
                        break;
                    }
                }
                if (!viewHolder.renamed) {
                    appName = info.loadLabel(pm).toString();
                }
            } catch (NullPointerException e) {
                appName = null;
            } catch (JSONException e) {
                appName = null;
            }

            if (appName != null) {
                viewHolder.textView.setText(appName);
            } else {
                viewHolder.textView.setText("");
            }
            Drawable icon;
            try {
                icon = info.loadIcon(pm);
            } catch (NullPointerException e) {
                icon = null;
            }
            if (icon != null) {
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
        public boolean   renamed;
        public TextView  textView;
        public CheckBox  chkEnabled;
        public ImageView imageView;

        public ListViewHolder() {
            renamed = false;
        }
    }

    public class AppComparator implements Comparator<ApplicationInfo> {
        final PackageManager pm;

        public AppComparator(Context context) {
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
