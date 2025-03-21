package ru.thelv.warningnotifier;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class Settings {
    private static final String KEY_URL = "url";
    private static final String KEY_CHECK_INTERVAL = "check_interval";
    private static final String KEY_OFFLINE_THRESHOLD = "offline_threshold";
    private static final String KEY_MONITORING_ENABLED = "monitoring_enabled";
    private static final String KEY_LAST_SUCCESS_TIME = "last_success_time";

    private final SharedPreferences prefs;

    public Settings(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getUrl() {
        return prefs.getString(KEY_URL, "https://example.com");
    }

    public void setUrl(String url) {
        prefs.edit().putString(KEY_URL, url).apply();
    }

    public int getCheckInterval() {
        return prefs.getInt(KEY_CHECK_INTERVAL, 1); // Default 1 hour
    }

    public void setCheckInterval(int hours) {
        prefs.edit().putInt(KEY_CHECK_INTERVAL, hours).apply();
    }

    public int getOfflineThreshold() {
        return prefs.getInt(KEY_OFFLINE_THRESHOLD, 24); // Default 24 hours
    }

    public void setOfflineThreshold(int hours) {
        prefs.edit().putInt(KEY_OFFLINE_THRESHOLD, hours).apply();
    }

    public boolean isMonitoringEnabled() {
        return prefs.getBoolean(KEY_MONITORING_ENABLED, false);
    }

    public void setMonitoringEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply();
    }

    public long getLastSuccessTime() {
        return prefs.getLong(KEY_LAST_SUCCESS_TIME, 0);
    }

    public void setLastSuccessTime(long timestamp) {
        prefs.edit().putLong(KEY_LAST_SUCCESS_TIME, timestamp).apply();
    }
} 