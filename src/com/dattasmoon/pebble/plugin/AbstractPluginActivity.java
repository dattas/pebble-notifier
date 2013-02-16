package com.dattasmoon.pebble.plugin;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
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

        localeBundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

        if (null == savedInstanceState && null != localeBundle) {
            mode = Mode.LOCALE;
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
        }

        return true;
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
