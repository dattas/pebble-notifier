package com.dattasmoon.pebble.plugin;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsTaskerActivity extends AbstractPluginActivity {
    List<Preference> preferences;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_tasker);
        try {
            parsePreferenceXml();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ListView lv = (ListView) findViewById(R.id.lvSettings);
        lv.setAdapter(new settingsAdapter(SettingsTaskerActivity.this, preferences, null));


    }
    private void parsePreferenceXml() throws XmlPullParserException, IOException {
        XmlResourceParser parser = getResources().getXml(R.xml.preferences);
        preferences = new ArrayList<Preference>();

        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_DOCUMENT) {
                Log.d(Constants.LOG_TAG, "Start document");
            } else if (eventType == XmlPullParser.END_DOCUMENT) {
                Log.d(Constants.LOG_TAG, "End document");
            } else if (eventType == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                Log.d(Constants.LOG_TAG, "Start tag \"" + tag +"\"");
                Map<String,String> attributes = getAttributes(parser);
                if(tag.equalsIgnoreCase("PreferenceScreen")){
                    eventType = parser.next();
                    continue;
                } else if(tag.equalsIgnoreCase("PreferenceCategory")){
                    Log.d(Constants.LOG_TAG, "android:key is " + attributes.get("key"));
                    if(attributes.get("key").equalsIgnoreCase("About")){
                        break;
                    }
                    PreferenceCategory pc = new PreferenceCategory(this);
                    if(attributes.containsKey("title")){
                        String title = getStringFromResource(attributes.get("title"));
                        pc.setTitle(title);
                        Log.d(Constants.LOG_TAG, "Title is: " + title );
                    }
                    if(attributes.containsKey("summary")){
                        String summary = getStringFromResource(attributes.get("summary"));
                        pc.setSummary(summary);
                        Log.d(Constants.LOG_TAG, "Summary is: " + summary);
                    }

                    //preferences.add(pc);

                } else if(tag.equalsIgnoreCase("CheckboxPreference")){
                    if(attributes.containsKey("dependency")){
                        eventType = parser.next();
                        continue;
                    }
                    Preference pref = new CheckBoxPreference(this);
                    pref.setKey(attributes.get("key"));
                    if(pref.getKey().equalsIgnoreCase("pref_dnd_time_enabled")){
                        eventType = parser.next();
                        continue;
                    }
                    if(attributes.containsKey("title")){
                        String title = getStringFromResource(attributes.get("title"));
                        pref.setTitle(title);
                        Log.d(Constants.LOG_TAG, "Title is: " + title );
                    }
                    if(attributes.containsKey("summary")){
                        String summary = getStringFromResource(attributes.get("summary"));
                        pref.setSummary(summary);
                        Log.d(Constants.LOG_TAG, "Summary is: " + summary);
                    }
                    preferences.add(pref);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                Log.d(Constants.LOG_TAG, "End tag " + parser.getName());
            } else if (eventType == XmlPullParser.TEXT) {
                Log.d(Constants.LOG_TAG, "Text " + parser.getText());
            }

            eventType = parser.next();

        }
    }
    private Map<String,String> getAttributes(XmlPullParser parser) throws XmlPullParserException, IOException {
        Map<String,String> attrs = null;
        int count = parser.getAttributeCount();
        if(count != -1) {
            Log.d(Constants.LOG_TAG,"Attributes for ["+parser.getName()+"]");
            attrs = new HashMap<String,String>(count);
            for(int index=0; index < count; index++) {
                Log.d(Constants.LOG_TAG,"\t["+parser.getAttributeName(index)+"] = " + "\""+parser.getAttributeValue(index)+"\"");
                attrs.put(parser.getAttributeName(index), parser.getAttributeValue(index));
            }
        }
        return attrs;
    }
    private String getStringFromResource(String id){
        String ret = null;
        if(id.startsWith("@")){
            id = id.replace("@", "");
            int int_id = Integer.parseInt(id);
            ret = getResources().getString(int_id);
        } else {
            ret = id;
        }
        return ret;
    }
    private class settingsAdapter extends ArrayAdapter<Preference> implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
        private final Context context;
        private final List<Preference> preferences;
        public ArrayList<String>    selected;

        public settingsAdapter(Context context, List<Preference> preferences, ArrayList<String> selected) {
            super(context, R.layout.list_application_item, preferences);
            this.context = context;
            this.preferences = preferences;
            this.selected = selected;
        }

        @Override
        public View getView(int position, View rowView, ViewGroup parent) {
            ListViewHolder viewHolder = null;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.list_setting_item, parent, false);

                viewHolder = new ListViewHolder();

                viewHolder.textView = (TextView) rowView.findViewById(R.id.tvSummary);
                viewHolder.chkEnabled = (CheckBox) rowView.findViewById(R.id.chkEnabled);
                viewHolder.chkSetting = (CheckBox) rowView.findViewById(R.id.chkSetting);
                viewHolder.chkEnabled.setOnCheckedChangeListener(this);

                //rowView.setOnClickListener(this);
                rowView.setTag(viewHolder);
            } else {
                viewHolder = (ListViewHolder) rowView.getTag();
            }
            Preference pref = preferences.get(position);
            viewHolder.chkEnabled.setText(pref.getTitle());
            viewHolder.textView.setText(pref.getSummary());

            viewHolder.chkSetting.setEnabled(viewHolder.chkEnabled.isChecked());

            return rowView;
        }

        @Override
        public void onCheckedChanged(CompoundButton chkEnabled, boolean newState) {
            View v = (View) chkEnabled.getParent();
            v.findViewById(R.id.chkSetting).setEnabled(newState);
            if(!newState){
                ((CheckBox)v.findViewById(R.id.chkSetting)).setChecked(false);
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
        public CheckBox  chkSetting;
    }
}
