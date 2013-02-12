package com.dattasmoon.pebble.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class EditNotificationActivity extends Activity {
    ListView          lvPackages;
    ToggleButton      tbMode;
    CheckBox          chkNotificationsOnly;
    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_notification);

        lvPackages = (ListView) findViewById(R.id.listPackages);
        tbMode = (ToggleButton) findViewById(R.id.tbMode);
        chkNotificationsOnly = (CheckBox) findViewById(R.id.chkNotificationsOnly);
        sharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
        tbMode.setChecked(sharedPreferences.getBoolean(Constants.PREFERENCE_EXCLUDE_MODE, false));
        chkNotificationsOnly.setChecked(sharedPreferences.getBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, false));

        checkAccessibilityService();

        if (findViewById(R.id.listPackages).isEnabled()) {
            new LoadAppsTask().execute();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_edit_notifications, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.btnUncheckAll:
            ((packageAdapter) lvPackages.getAdapter()).selected.clear();
            lvPackages.invalidateViews();
            return true;
        case R.id.btnSave:
            finish();
            return true;
        case R.id.btnDonate:
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(Constants.DONATION_URL));
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);

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
            findViewById(R.id.tbMode).setVisibility(View.GONE);
            findViewById(R.id.tvMode).setVisibility(View.GONE);
            findViewById(R.id.chkNotificationsOnly).setVisibility(View.GONE);
            findViewById(R.id.listPackages).setEnabled(false);
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "The accessibility service is NOT on!");
            }
        } else {
            findViewById(R.id.tvAccessibilityError).setVisibility(View.GONE);
            findViewById(R.id.tbMode).setVisibility(View.VISIBLE);
            findViewById(R.id.tvMode).setVisibility(View.VISIBLE);
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
        for (String strPackage : ((packageAdapter) lvPackages.getAdapter()).selected) {
            if (!strPackage.isEmpty()) {
                if (!tmpArray.contains(strPackage)) {
                    tmpArray.add(strPackage);
                    selectedPackages += strPackage + ",";
                }
            }
        }
        tmpArray.clear();
        if (!selectedPackages.isEmpty()) {
            selectedPackages = selectedPackages.substring(0, selectedPackages.length() - 1);
        }
        if (Constants.IS_LOGGABLE) {
            if (tbMode.isChecked()) {
                Log.i(Constants.LOG_TAG, "Mode is is: include only");
            } else {
                Log.i(Constants.LOG_TAG, "Mode is is: exclude");
            }
            if (chkNotificationsOnly.isChecked()) {
                Log.i(Constants.LOG_TAG, "Only going to send notifications");
            } else {
                Log.i(Constants.LOG_TAG, "Sending all types of notifications, such as Toasts");
            }
            Log.i(Constants.LOG_TAG, "Package list is: " + selectedPackages);
        }

        Editor editor = sharedPreferences.edit();
        editor.putBoolean(Constants.PREFERENCE_EXCLUDE_MODE, tbMode.isChecked());
        editor.putBoolean(Constants.PREFERENCE_NOTIFICATIONS_ONLY, chkNotificationsOnly.isChecked());
        editor.putString(Constants.PREFERENCE_PACKAGE_LIST, selectedPackages);
        editor.commit();

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

    private class LoadAppsTask extends AsyncTask<Void, Integer, List<PackageInfo>> {
        public ArrayList<String> selected;

        @Override
        protected List<PackageInfo> doInBackground(Void... unused) {
            final List<PackageInfo> pkgAppsList = getPackageManager().getInstalledPackages(0);
            PackageComparator comparer = new PackageComparator();
            Collections.sort(pkgAppsList, comparer);
            selected = new ArrayList<String>();
            for (String strPackage : sharedPreferences.getString(Constants.PREFERENCE_PACKAGE_LIST, "").split(",", 0)) {
                // only add the ones that are still installed, providing cleanup
                // and faster speeds all in one!
                for (PackageInfo info : pkgAppsList) {
                    if (info.packageName.equalsIgnoreCase(strPackage)) {
                        selected.add(strPackage);
                    }
                }
            }

            return pkgAppsList;
        }

        @Override
        protected void onPostExecute(List<PackageInfo> pkgAppsList) {

            lvPackages.setAdapter(new packageAdapter(EditNotificationActivity.this, pkgAppsList
                    .toArray(new PackageInfo[pkgAppsList.size()]), selected));

        }
    }

    private class packageAdapter extends ArrayAdapter<PackageInfo> implements OnCheckedChangeListener, OnClickListener {
        private final Context       context;
        private final PackageInfo[] packages;
        public ArrayList<String>    selected;

        public packageAdapter(Context context, PackageInfo[] packages, ArrayList<String> selected) {
            super(context, R.layout.list_application_item, packages);
            this.context = context;
            this.packages = packages;
            this.selected = selected;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.list_application_item, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.tvPackage);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.ivIcon);
            CheckBox chkEnabled = (CheckBox) rowView.findViewById(R.id.chkEnabled);
            chkEnabled.setOnCheckedChangeListener(this);
            rowView.setOnClickListener(this);

            PackageInfo info = packages[position];
            textView.setText(info.applicationInfo.loadLabel(getPackageManager()).toString());
            imageView.setImageDrawable(info.applicationInfo.loadIcon(getPackageManager()));
            chkEnabled.setChecked(false);
            chkEnabled.setTag(info.packageName);
            for (String strPackage : selected) {
                if (info.packageName.equalsIgnoreCase(strPackage)) {
                    chkEnabled.setChecked(true);
                    break;
                }
            }
            return rowView;
        }

        @Override
        public void onCheckedChanged(CompoundButton chkEnabled, boolean newState) {
            String strPackage = (String) chkEnabled.getTag();
            if (strPackage.isEmpty()) {
                return;
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

        }

        @Override
        public void onClick(View rowView) {
            ((CheckBox) rowView.findViewById(R.id.chkEnabled)).performClick();

        }
    }

    public class PackageComparator implements Comparator<PackageInfo> {

        @Override
        public int compare(PackageInfo leftPackage, PackageInfo rightPackage) {

            String leftName = leftPackage.applicationInfo.loadLabel(getPackageManager()).toString();
            String rightName = rightPackage.applicationInfo.loadLabel(getPackageManager()).toString();

            return leftName.compareToIgnoreCase(rightName);
        }
    }
}
