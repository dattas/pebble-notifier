/* 
Copyright (c) 2013 Dattas Moonchaser

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dattasmoon.pebble.plugin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.twofortyfouram.locale.BreadCrumber;

public abstract class AbstractPluginActivity extends Activity {
    public enum Mode {
        STANDARD, LOCALE
    };

    private boolean mIsCancelled = false;
    protected Mode  mode         = Mode.STANDARD;
    Bundle          localeBundle = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

        if (getIntent().getAction() == "com.twofortyfouram.locale.intent.action.EDIT_SETTING") {
            mode = Mode.LOCALE;
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Activity mode is set to locale");
            }
        } else {
            mode = Mode.STANDARD;
            if (Constants.IS_LOGGABLE) {
                Log.i(Constants.LOG_TAG, "Activity mode is set to standard");
            }
        }

        if (mode == Mode.LOCALE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                setupTitleApi11();
            } else {
                setTitle(BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(),
                        getString(R.string.app_name)));
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupTitleApi11() {
        CharSequence callingApplicationLabel = null;
        try {
            callingApplicationLabel = getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(getCallingPackage(), 0));
        } catch (final NameNotFoundException e) {
            if (Constants.IS_LOGGABLE) {
                Log.e(Constants.LOG_TAG, "Calling package couldn't be found", e);
            }
        }
        if (null != callingApplicationLabel) {
            setTitle(callingApplicationLabel);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mode == Mode.LOCALE) {
            getMenuInflater().inflate(R.menu.twofortyfouram_locale_help_save_dontsave, menu);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                setupActionBarApi11();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                setupActionBarApi14();
            }
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBarApi11() {
        getActionBar().setSubtitle(
                BreadCrumber.generateBreadcrumb(getApplicationContext(), getIntent(), getString(R.string.app_name)));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setupActionBarApi14() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
        try {
            getActionBar().setIcon(getPackageManager().getApplicationIcon(getCallingPackage()));
        } catch (final NameNotFoundException e) {
            if (Constants.IS_LOGGABLE) {
                Log.w(Constants.LOG_TAG, "An error occurred loading the host's icon", e);
            }
        }
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        if (Constants.IS_LOGGABLE) {
            Log.i(Constants.LOG_TAG, "Abstract plugin: Selected menu item id: " + String.valueOf(item.getItemId()));
        }
        final int id = item.getItemId();

        if (android.R.id.home == id) {
            finish();
            return true;
        } else if (R.id.twofortyfouram_locale_menu_dontsave == id) {
            mIsCancelled = true;
            finish();
            return true;
        } else if (R.id.twofortyfouram_locale_menu_save == id) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected boolean isCanceled() {
        return mIsCancelled;
    }
}
