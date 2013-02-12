package com.dattasmoon.pebble.plugin;

import android.content.Context;
import android.util.Log;

public final class Constants {

    public static final String  LOG_TAG                         = "com.dattasmoon.pebble.plugin";
    public static final boolean IS_LOGGABLE                     = BuildConfig.DEBUG;
    public static final String  DONATION_URL                    = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=3W6PJ6EK6JVJY";

    // bundle extras
    public static final String  BUNDLE_EXTRA_INT_VERSION_CODE   = LOG_TAG + ".INT_VERSION_CODE";
    public static final String  BUNDLE_EXTRA_STRING_TITLE       = LOG_TAG + ".STRING_TITLE";
    public static final String  BUNDLE_EXTRA_STRING_BODY        = LOG_TAG + ".STRING_BODY";

    // Tasker bundle extras
    public static final String  BUNDLE_EXTRA_REPLACE_KEY        = "net.dinglisch.android.tasker.extras.VARIABLE_REPLACE_KEYS";
    public static final String  BUNDLE_EXTRA_REPLACE_VALUE      = BUNDLE_EXTRA_STRING_TITLE + " "
                                                                        + BUNDLE_EXTRA_STRING_BODY;

    // Shared preferences
    public static final String  PREFERENCE_EXCLUDE_MODE         = LOG_TAG + ".excludeMode";
    public static final String  PREFERENCE_PACKAGE_LIST         = LOG_TAG + ".packageList";

    // Intents
    public static final String  INTENT_SEND_PEBBLE_NOTIFICATION = "com.getpebble.action.SEND_NOTIFICATION";

    // Pebble specific items
    public static final String  PEBBLE_MESSAGE_TYPE_ALERT       = "PEBBLE_ALERT";

    // Accessibility specific items
    public static final String  ACCESSIBILITY_SERVICE           = "com.dattasmoon.pebble.plugin/com.dattasmoon.pebble.plugin.NotificationService";

    public static int getVersionCode(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (final UnsupportedOperationException e) {
            return 1;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void log(String message) {
        if (Constants.IS_LOGGABLE) {
            Log.d(LOG_TAG, message);
        }
    }

    private Constants() {
        throw new UnsupportedOperationException("This class is non-instantiable, so stop trying!");
    }
}
