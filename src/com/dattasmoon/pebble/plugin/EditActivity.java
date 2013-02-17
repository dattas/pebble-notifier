package com.dattasmoon.pebble.plugin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

public class EditActivity extends AbstractPluginActivity {
    EditText txtTitle;
    EditText txtBody;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        txtTitle = (EditText) findViewById(R.id.txtTitle);
        txtBody = (EditText) findViewById(R.id.txtBody);
        final Bundle localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        if (null == savedInstanceState && null != localeBundle) {
            if (localeBundle.containsKey(Constants.BUNDLE_EXTRA_STRING_TITLE)
                    && localeBundle.containsKey(Constants.BUNDLE_EXTRA_STRING_BODY)) {
                txtTitle.setText(localeBundle.getString(Constants.BUNDLE_EXTRA_STRING_TITLE));
                txtBody.setText(localeBundle.getString(Constants.BUNDLE_EXTRA_STRING_BODY));
            }
        }
    }

    @Override
    public void finish() {
        if (!isCanceled()) {
            final Intent resultIntent = new Intent();
            final Bundle resultBundle = new Bundle();
            final String title = txtTitle.getText().toString();
            final String body = txtBody.getText().toString();
            final String blurb = generateBlurb(getApplicationContext(), "Notification with title: \"" + title
                    + "\" and body: \"" + body + "\"");

            // provide string to Tasker to replace any variables in the title or
            // body
            resultBundle.putString(Constants.BUNDLE_EXTRA_REPLACE_KEY, Constants.BUNDLE_EXTRA_REPLACE_VALUE);

            // set the version, title and body
            resultBundle.putInt(Constants.BUNDLE_EXTRA_INT_VERSION_CODE,
                    Constants.getVersionCode(getApplicationContext()));
            resultBundle.putInt(Constants.BUNDLE_EXTRA_INT_TYPE, Constants.Type.NOTIFICATION.ordinal());
            resultBundle.putString(Constants.BUNDLE_EXTRA_STRING_TITLE, title);
            resultBundle.putString(Constants.BUNDLE_EXTRA_STRING_BODY, body);

            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);
            setResult(RESULT_OK, resultIntent);
        }
        super.finish();
    }

    static String generateBlurb(final Context context, final String message) {
        final int maxBlurbLength = 45;

        if (message.length() > maxBlurbLength) {
            return message.substring(0, maxBlurbLength);
        }

        return message;
    }
}
