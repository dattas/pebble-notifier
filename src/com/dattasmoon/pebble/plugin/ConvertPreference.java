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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ConvertPreference extends DialogPreference {
    JSONArrayAdapter arrayAdapter;
    EditText etFrom;
    EditText etTo;
    ListView lvConvert;
    //JSONArray currentValue;
    public ConvertPreference(Context context, AttributeSet attrs){
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_covert);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }
    @Override
    protected void onBindDialogView(View view) {
        Button btnAdd = (Button)view.findViewById(R.id.btnAdd);
        etFrom = (EditText)view.findViewById(R.id.etFrom);
        etTo = (EditText)view.findViewById(R.id.etTo);
        lvConvert = (ListView) view.findViewById(R.id.lvConvert);
        lvConvert.setEmptyView(view.findViewById(android.R.id.empty));
        lvConvert.setAdapter(arrayAdapter);
        lvConvert.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
                AdapterView.AdapterContextMenuInfo contextInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
                int position = contextInfo.position;
                long id = contextInfo.id;
                // the child view who's info we're viewing (should be equal to v)
                View v = contextInfo.targetView;
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());


                final int arrayPosition = (Integer) v.getTag();
                final String text = ((TextView)v.findViewById(R.id.tvItem)).getText().toString();
                builder.setMessage("Do you want to delete " + text + "?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                JSONArray temp = new JSONArray();
                                for(int i = 0; i < arrayAdapter.getJSONArray().length(); i++){
                                    if(i == arrayPosition){
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
                        .setNegativeButton("no", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                builder.create().show();
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject item = new JSONObject();
                try {
                    item.put("from", etFrom.getText().toString());
                    item.put("to", etTo.getText().toString());
                    arrayAdapter.getJSONArray().put(item);
                    etFrom.setText("");
                    etTo.setText("");
                    arrayAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
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
            String tempValue = arrayAdapter.getJSONArray().toString();
            if(Constants.IS_LOGGABLE){
                Log.i(Constants.LOG_TAG, "Setting preference to be: " + tempValue);
            }
            persistString(tempValue);
        }
    }
    public class JSONArrayAdapter extends BaseAdapter implements View.OnTouchListener,View.OnLongClickListener {

        private ViewGroup group;
        private JSONArray items;
        private Context context;
        private String selectedItemID;
        private int selectedItemPosition;

        public JSONArrayAdapter(Context context, JSONArray items){
            super();
            this.items = items;
            this.context = context;
            this.selectedItemPosition = -1;
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
                //view.setOnLongClickListener(this);
                //view.setLongClickable(true);
                //view.setClickable(true);
                //view.setOnTouchListener(this);
            }

            String itemText = null;
            String itemID = null;

            try {
                JSONObject jsonObject = items.getJSONObject(position);
                view.setTag(position);
                itemText = jsonObject.getString("from") + " -> " + jsonObject.getString("to");
            } catch (JSONException e) {

            }

            if (itemText != null){

                TextView name = (TextView) view.findViewById(R.id.tvItem);

                if (name != null)
                    name.setText(itemText);
            }


            return view;
        }


        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                Context mContext = getContext();
                Resources res = mContext.getResources();
                //TransitionDrawable transition = (TransitionDrawable) res.getDrawable(android.R.anim.);
                //v.setBackgroundDrawable(transition);
                //LongClick took approx. 510-530 milliseconds to register after OnTouch. So I set the duration at 500 millis.
                //transition.startTransition(ViewConfiguration.getLongPressTimeout());
            } else if(event.getAction() == MotionEvent.ACTION_UP){
                v.setBackgroundColor(Color.TRANSPARENT);
            }





            return false;
        }

        @Override
        public boolean onLongClick(View v) {


            return false;
        }
    }
}
