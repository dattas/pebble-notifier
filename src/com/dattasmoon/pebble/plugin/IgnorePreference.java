/*
Copyright (c) 2013 Dattas Moonchaser

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dattasmoon.pebble.plugin;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class IgnorePreference extends DialogPreference {
    JSONArrayAdapter arrayAdapter;
    ListView lvIgnore;
    EditText etMatch;
    Button btnAdd;
    CheckBox chkRawRegex;
    CheckBox chkCaseInsensitive;
    AutoCompleteTextView actvApplications;
    Spinner spnApplications;
    Spinner spnMode;
    public IgnorePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_ignore);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }
    @Override
    protected void onBindDialogView(View view) {


        btnAdd = (Button)view.findViewById(R.id.btnAdd);
        etMatch = (EditText)view.findViewById(R.id.etMatch);
        chkRawRegex = (CheckBox)view.findViewById(R.id.chkRawRegex);
        chkCaseInsensitive = (CheckBox)view.findViewById(R.id.chkCaseInsensitive);
        actvApplications = (AutoCompleteTextView)view.findViewById(R.id.actvApplications);

        spnApplications = (Spinner)view.findViewById(R.id.spnApplications);
        spnMode = (Spinner)view.findViewById(R.id.spnMode);
        lvIgnore = (ListView) view.findViewById(R.id.lvIgnore);
        lvIgnore.setAdapter(arrayAdapter);
        lvIgnore.setEmptyView(view.findViewById(android.R.id.empty));
        new LoadAppsTask().execute();

        lvIgnore.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
                AdapterView.AdapterContextMenuInfo contextInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
                int position = contextInfo.position;
                long id = contextInfo.id;
                // the child view who's info we're viewing (should be equal to v)
                final View v = contextInfo.targetView;
                MenuInflater inflater = new MenuInflater(getContext());
                inflater.inflate(R.menu.preference_ignore_context, menu);

                //we have to do this mess because DialogPreference doesn't allow for onMenuItemSelected or onOptionsItemSelected. Bleh
                menu.findItem(R.id.btnEdit).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        final int arrayPosition = (Integer) v.getTag();
                        final String text = ((TextView) v.findViewById(R.id.tvItem)).getText().toString();
                        JSONArray temp = new JSONArray();
                        for (int i = 0; i < arrayAdapter.getJSONArray().length(); i++) {
                            try {
                                JSONObject ignore = arrayAdapter.getJSONArray().getJSONObject(i);
                                if (i == arrayPosition) {
                                    etMatch.setText(ignore.getString("match"));
                                    chkRawRegex.setChecked(ignore.getBoolean("raw"));
                                    chkCaseInsensitive.setChecked(ignore.optBoolean("insensitive", true));
                                    String app = ignore.getString("app");
                                    if(app == "-1"){
                                        actvApplications.setText(getContext().getString(R.string.ignore_any));
                                    } else {
                                        actvApplications.setText(app);
                                    }
                                    boolean exclude = ignore.optBoolean("exclude", true);
                                    if(exclude){
                                        spnMode.setSelection(Constants.IgnoreMode.EXCLUDE.ordinal());
                                    } else {
                                        spnMode.setSelection(Constants.IgnoreMode.INCLUDE.ordinal());
                                    }
                                    continue;
                                }

                                temp.put(ignore);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        arrayAdapter.setJSONArray(temp);

                        arrayAdapter.notifyDataSetChanged();
                        return true;
                    }
                });
                menu.findItem(R.id.btnDelete).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());


                        final int arrayPosition = (Integer) v.getTag();
                        final String text = ((TextView) v.findViewById(R.id.tvItem)).getText().toString();
                        builder.setMessage(getContext().getResources().getString(R.string.confirm_delete) + " '" + text + "' ?")
                                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        JSONArray temp = new JSONArray();
                                        for (int i = 0; i < arrayAdapter.getJSONArray().length(); i++) {
                                            if (i == arrayPosition) {
                                                continue;
                                            }
                                            try {
                                                temp.put(arrayAdapter.getJSONArray().getJSONObject(i));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        arrayAdapter.setJSONArray(temp);

                                        arrayAdapter.notifyDataSetChanged();
                                    }
                                })
                                .setNegativeButton(R.string.decline, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User cancelled the dialog
                                    }
                                });
                        builder.create().show();
                        return true;
                    }
                });
            }
        });
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject item = new JSONObject();
                try {
                    item.put("match", etMatch.getText().toString());
                    item.put("raw", chkRawRegex.isChecked());
                    item.put("insensitive", chkCaseInsensitive.isChecked());
                    if (actvApplications.getText().toString().equalsIgnoreCase(getContext().getString(R.string.ignore_any))) {
                        item.put("app", "-1");
                    } else {
                        item.put("app", actvApplications.getText().toString());
                    }
                    if (spnMode.getSelectedItemPosition() == Constants.IgnoreMode.INCLUDE.ordinal()) {
                        item.put("exclude", false);
                    } else {
                        item.put("exclude", true);
                    }
                    if (Constants.IS_LOGGABLE) {
                        Log.i(Constants.LOG_TAG, "Item is: " + item.toString());
                    }
                    arrayAdapter.getJSONArray().put(item);
                    etMatch.setText("");
                    arrayAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        actvApplications.setText(getContext().getString(R.string.ignore_any));
        actvApplications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actvApplications.showDropDown();
            }
        });
        actvApplications.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ApplicationInfo pkg = (ApplicationInfo) parent.getItemAtPosition(position);
                if (pkg == null) {
                    actvApplications.setText(getContext().getString(R.string.ignore_any));
                } else {
                    actvApplications.setText(pkg.packageName);
                }
            }
        });
        actvApplications.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    actvApplications.showDropDown();
                } else {
                    if (actvApplications.getText().length() == 0){
                        actvApplications.setText(getContext().getString(R.string.ignore_any));
                    }
                }
            }
        });
        super.onBindDialogView(view);

    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String tempValue;
        JSONArray mCurrentValue;
        if (restoreValue) {
            if (defaultValue == null) {
                tempValue = getPersistedString("[]");
            } else {
                tempValue = getPersistedString(defaultValue.toString());
            }
        } else {
            tempValue = defaultValue.toString();
        }
        try{
            mCurrentValue = new JSONArray(tempValue);
        } catch (JSONException e){
            mCurrentValue = new JSONArray();
        }
        arrayAdapter = new JSONArrayAdapter(getContext(), mCurrentValue);
    }
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            if(etMatch.getText().toString().trim().length() > 0){
                btnAdd.performClick();
            }
            String tempValue = arrayAdapter.getJSONArray().toString();
            if(Constants.IS_LOGGABLE){
                Log.i(Constants.LOG_TAG, "Setting preference to be: " + tempValue);
            }
            persistString(tempValue);
        }
    }

    private class LoadAppsTask extends AsyncTask<Void, Integer, Void> {
        List<PackageInfo> pkgAppsList;
        List<ApplicationInfo> appsList;

        @Override
        protected void onPreExecute(){
            PackageManager pm = getContext().getPackageManager();
            try {
                pkgAppsList = pm.getInstalledPackages(0);
            } catch (RuntimeException e){
                //this is usually thrown when people have too many things installed (or bloatware in the case of Samsung devices)
                pm = getContext().getPackageManager();
                appsList = pm.getInstalledApplications(0);
            }

        }

        @Override
        protected Void doInBackground(Void... unused) {
            if(pkgAppsList == null && appsList == null){
                //something went really bad here
                return null;
            }
            if(appsList == null){
                appsList = new ArrayList<ApplicationInfo>();
                for(PackageInfo pkg : pkgAppsList){
                    appsList.add(pkg.applicationInfo);
                }
            }

            AppComparator comparer = new AppComparator(getContext());
            Collections.sort(appsList, comparer);
            appsList.add(0, null);
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if(appsList == null){
                //something went wrong
                return;
            }
            ArrayList<ApplicationInfo> tmp = new ArrayList<ApplicationInfo>();
            for(ApplicationInfo pkg : appsList){
                tmp.add(pkg);
            }
            packageAdapter adapter = new packageAdapter(getContext(), tmp);
            //spnApplications.setAdapter(adapter);
            actvApplications.setAdapter(adapter);

        }
    }

    private class packageAdapter extends ArrayAdapter<ApplicationInfo> implements Filterable {
        private final Context       context;
        private ArrayList<ApplicationInfo> packages;
        private ArrayList<ApplicationInfo> mOriginalValues;
        private ArrayFilter mFilter;

        public packageAdapter(Context context, ArrayList<ApplicationInfo> packages) {
            super(context, R.layout.list_convert_item, packages);
            this.context = context;
            this.packages = packages;
            this.mOriginalValues = new ArrayList<ApplicationInfo>(packages);
        }
        @Override
        public int getCount(){
            return packages.size();
        }
        @Override
        public ApplicationInfo getItem(int position) {
            return packages.get(position);
        }
        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new ArrayFilter();
            }
            return mFilter;
        }
        @Override
        public View getDropDownView(int position, View convertView,ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent) {
            return getCustomView(position, rowView, parent);
        }
        private View getCustomView(int position, View rowView, ViewGroup parent){
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.list_filter_item, parent, false);

            }

            ApplicationInfo info = packages.get(position);

            TextView tvText = (TextView)rowView.findViewById(R.id.tvItem);
            TextView tvSubText = (TextView)rowView.findViewById(R.id.tvSubItem);
            ImageView iv = (ImageView)rowView.findViewById(R.id.ivIcon);
            if(info == null){
                tvText.setText(context.getString(R.string.ignore_any));
                tvSubText.setText("");
                rowView.setTag("-1");
                iv.setImageDrawable(null);
            } else {
                tvText.setText(info.loadLabel(getContext().getPackageManager()).toString());
                tvSubText.setText("("+info.packageName+")");
                iv.setImageDrawable(info.loadIcon(getContext().getPackageManager()));
                rowView.setTag(info.packageName);
            }

            return rowView;
        }
        private class ArrayFilter extends Filter {
            private Object lock;

            @Override
            protected FilterResults performFiltering(CharSequence prefix) {
                FilterResults results = new FilterResults();

                if (mOriginalValues == null) {
                    synchronized (lock) {
                        mOriginalValues = new ArrayList<ApplicationInfo>(packages);
                    }
                }

                if (prefix == null || prefix.length() == 0) {
                    synchronized (lock) {
                        ArrayList<ApplicationInfo> list = new ArrayList<ApplicationInfo>(mOriginalValues);
                        results.values = list;
                        results.count = list.size();
                    }
                } else {
                    final String prefixString = prefix.toString().toLowerCase();

                    ArrayList<ApplicationInfo> values = mOriginalValues;
                    int count = values.size();

                    ArrayList<ApplicationInfo> newValues = new ArrayList<ApplicationInfo>();

                    for (int i = 0; i < count; i++) {
                        ApplicationInfo pkg = values.get(i);
                        if(pkg == null){
                            continue;
                        }
                        String item = pkg.packageName;
                        if (pkg.packageName.toLowerCase().contains(prefixString) || pkg.loadLabel(context.getPackageManager()).toString().toLowerCase().contains(prefixString)) {
                            newValues.add(pkg);
                        }

                    }

                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                if(results.values!=null){
                    packages = (ArrayList<ApplicationInfo>) results.values;
                }else{
                    packages = new ArrayList<ApplicationInfo>();
                }
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        }
    }

    public class JSONArrayAdapter extends BaseAdapter {

        private ViewGroup group;
        private JSONArray items;
        private Context context;

        public JSONArrayAdapter(Context context, JSONArray items){
            super();
            this.items = items;
            this.context = context;
        }

        public JSONArray getJSONArray(){
            return items;
        }
        public void setJSONArray(JSONArray items){
            this.items = items;
        }

        public int getCount() {
            return items.length();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent)  {
            View view = convertView;
            group = parent;

            if (view == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = vi.inflate(R.layout.list_convert_item, null);
            }

            String itemText = "";

            try {
                JSONObject jsonObject = items.getJSONObject(position);
                view.setTag(position);


                if(jsonObject.getBoolean("raw")){
                    itemText = "(Regex) ";
                }
                String pkg = jsonObject.getString("app");
                if(pkg.equals("-1")){
                    itemText += "["+context.getString(R.string.ignore_any)+"] ";
                } else {
                    itemText += "[" + pkg + "] ";
                }
                if(!jsonObject.optBoolean("exclude", true)){
                    itemText += "NOT ";
                }
                itemText += jsonObject.getString("match");
            } catch (JSONException e) {

            }

            if (itemText != null){

                TextView name = (TextView) view.findViewById(R.id.tvItem);

                if (name != null)
                    name.setText(itemText);
            }


            return view;
        }
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
