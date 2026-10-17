package com.kuugo.chargenotice;

import android.content.Context;
import android.content.SharedPreferences;

final class AppPrefs {
    private static final String FILE = "charge_notice_prefs";
    private static final String ROOT_ENABLED = "root_enabled";
    private static final String NOTIFIED_THIS_CHARGE = "notified_this_charge";

    private AppPrefs() {}

    private static SharedPreferences prefs(Context context) {
        Context storage = context.createDeviceProtectedStorageContext();
        return storage.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean isRootEnabled(Context context) {
        return prefs(context).getBoolean(ROOT_ENABLED, false);
    }

    static void setRootEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(ROOT_ENABLED, value).apply();
    }

    static boolean wasNotifiedThisCharge(Context context) {
        return prefs(context).getBoolean(NOTIFIED_THIS_CHARGE, false);
    }

    static void setNotifiedThisCharge(Context context, boolean value) {
        prefs(context).edit().putBoolean(NOTIFIED_THIS_CHARGE, value).apply();
    }
}
