package com.aware.plugin.vahida;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String STATUS_PLUGIN_TEMPLATE = "status_plugin_template";
    public static final String STATUS_PLUGIN_MAGNETOMETER = "edit_magno_threshold";
    public static final String STATUS_PLUGIN_LIGHT = "edit_light_threshold";
    public static final String STATUS_PLUGIN_SIGNAL = "edit_signal_threshold";
    public static final String STATUS_CHART_MAGNETOMETER = "status_chart_magnetometer";
    public static final String STATUS_CHART_LIGHT = "status_chart_light";
    public static final String STATUS_CHART_SIGNAL = "status_chart_signal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs.registerOnSharedPreferenceChangeListener(this);

        syncSettings();
    }

    private void syncSettings() {
        //Make sure to load the latest values
        CheckBoxPreference status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_TEMPLATE);
        status.setChecked(Aware.getSetting(this, STATUS_PLUGIN_TEMPLATE).equals("true"));
        EditTextPreference edit_light_threshold = (EditTextPreference) findPreference(STATUS_PLUGIN_LIGHT);
        EditTextPreference edit_magno_threshold = (EditTextPreference) findPreference(STATUS_PLUGIN_MAGNETOMETER);
        EditTextPreference edit_signal_threshold = (EditTextPreference) findPreference(STATUS_PLUGIN_SIGNAL);

        //...
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = (Preference) findPreference(key);

        if( setting.getKey().equals(STATUS_PLUGIN_TEMPLATE) ) {
            boolean is_active = sharedPreferences.getBoolean(key, false);
            Aware.setSetting(this, key, is_active);
            if( is_active ) {
                Aware.startPlugin(this, getPackageName());
            } else {
                Aware.stopPlugin(this, getPackageName());
            }
        }
        else if(setting.getKey().equals(STATUS_PLUGIN_MAGNETOMETER))
        {
            int magno_threshold = Integer.valueOf(sharedPreferences.getString(key, "40"));
            Plugin.magno_threshold = magno_threshold;
        }
        else if(setting.getKey().equals(STATUS_PLUGIN_LIGHT))
        {
            int light_threshold = Integer.valueOf(sharedPreferences.getString(key, "400"));
            Plugin.light_threshold = light_threshold;
        }
        else if(setting.getKey().equals(STATUS_PLUGIN_SIGNAL))
        {
            int signal_threshold = Integer.valueOf(sharedPreferences.getString(key, "25"));
            Plugin.signal_threshold = signal_threshold;
        }
        else if(setting.getKey().equals(STATUS_CHART_MAGNETOMETER)) {
            boolean is_active = sharedPreferences.getBoolean(key, true);
            ContextCard.is_magnetometer_enabled = is_active;
        }
        else if(setting.getKey().equals(STATUS_CHART_LIGHT)) {
            boolean is_active = sharedPreferences.getBoolean(key, true);
            ContextCard.is_light_enabled = is_active;
        }
        else if(setting.getKey().equals(STATUS_CHART_SIGNAL)) {
            boolean is_active = sharedPreferences.getBoolean(key, true);
            ContextCard.is_signal_enabled = is_active;
        }
        //Apply the new settings
        Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
        sendBroadcast(apply);
    }
}
