package com.aware.plugin.vahida;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Light;
import com.aware.Magnetometer;
import com.aware.providers.Light_Provider;
import com.aware.providers.Magnetometer_Provider;
import com.aware.utils.Aware_Plugin;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationRequest;

import java.util.Calendar;


public class Plugin extends Aware_Plugin implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private final String TAG = "MyAwesomeApp";
    private GoogleApiClient mGoogleApiClient;
    private PendingIntent pIntent;
    public static int detected_activity = -1;
    public static int activity_probability = -1;
    private static boolean dark_time = false;
    public static int magno_threshold;
    public static int light_threshold;
    public static int signal_threshold;
    private static double magneto_power = 0;
    private static double light_power = 0;
    private static double signal_strength = 0;
    private static String ACTION_AWARE_LOCATION_TYPE = "ACTION_AWARE_LOCATION_TYPE";
    private static String EXTRA_LOCATION = "Indoor";
    private static String location = "";
    private TelephonyManager telephonyManager;
    private MyPhoneStateListener signalListener;
    private static double inside_percentage = 0.00, outside_percentage = 0.00;
    private static SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        magno_threshold = Integer.valueOf(prefs.getString("edit_magno_threshold", "50"));
        light_threshold = Integer.valueOf(prefs.getString("edit_light_threshold", "400"));
        signal_threshold = Integer.valueOf(prefs.getString("edit_signal_threshold", "25"));
        //------------------------Activate sensors
        Aware.setSetting(this, Aware_Preferences.STATUS_LIGHT, true);
        Aware.setSetting(this, Aware_Preferences.FREQUENCY_LIGHT, 20000);

        Aware.setSetting(this, Aware_Preferences.STATUS_MAGNETOMETER, true);
        Aware.setSetting(this, Aware_Preferences.FREQUENCY_MAGNETOMETER, SensorManager.SENSOR_DELAY_NORMAL);

        signalListener = new MyPhoneStateListener();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, true);
        //------------------------Apply settings
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));

        IntentFilter filter = new IntentFilter();
        filter.addAction(Light.ACTION_AWARE_LIGHT);
        filter.addAction(Magnetometer.ACTION_AWARE_MAGNETOMETER);
        filter.addAction(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);

        registerReceiver(dataReceiver, filter);
        //------------------------
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        if (DEBUG) Log.d(TAG, "Template plugin running");

        int resp = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resp == ConnectionResult.SUCCESS) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
            Intent intent = new Intent(this, ActivityRecognitionService.class);
            pIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        } else {
            Toast.makeText(this, "Please install Google Play Service.", Toast.LENGTH_SHORT).show();
        }

        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                Intent context = new Intent(ACTION_AWARE_LOCATION_TYPE);
                context.putExtra(EXTRA_LOCATION, location);
                sendBroadcast(context);
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Aware.setSetting(this, Aware_Preferences.STATUS_LIGHT, false);
        Aware.setSetting(this, Aware_Preferences.STATUS_MAGNETOMETER, false);
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, false);

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, pIntent);
        }
        unregisterReceiver(dataReceiver);
        telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_NONE);
        if (DEBUG) Log.d(TAG, "Template plugin terminated");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient connection has failed");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Aware.startPlugin(this, getPackageName());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mGoogleApiClient.isConnected()) {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0, pIntent);
        }
        return START_STICKY;
    }

    private SensorDataReceiver dataReceiver = new SensorDataReceiver();

    public static class SensorDataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(System.currentTimeMillis());
            if (date.get(Calendar.HOUR_OF_DAY) > 18 || date.get(Calendar.HOUR_OF_DAY) < 6) {
                dark_time = true;
            } else {
                dark_time = false;
            }

            if (intent.getAction().equals(Magnetometer.ACTION_AWARE_MAGNETOMETER)) {
                Cursor magnetometer = context.getContentResolver().query
                        (Magnetometer_Provider.Magnetometer_Data.CONTENT_URI, null, null, null, Magnetometer_Provider.Magnetometer_Data.TIMESTAMP + " DESC LIMIT 1");
                if (magnetometer != null && magnetometer.moveToFirst()) {
                    double value_x = magnetometer.getDouble(magnetometer.getColumnIndex(Magnetometer_Provider.Magnetometer_Data.VALUES_0));
                    double value_y = magnetometer.getDouble(magnetometer.getColumnIndex(Magnetometer_Provider.Magnetometer_Data.VALUES_1));
                    double value_z = magnetometer.getDouble(magnetometer.getColumnIndex(Magnetometer_Provider.Magnetometer_Data.VALUES_2));
                    magneto_power = Math.sqrt((Math.pow(value_x, 2) + Math.pow(value_y, 2) + Math.pow(value_z, 2)));
                }
                if (magnetometer != null && !magnetometer.isClosed()) magnetometer.close();
            }
            if (intent.getAction().equals(Light.ACTION_AWARE_LIGHT)) {
                Cursor light = context.getContentResolver().query(Light_Provider.Light_Data.CONTENT_URI, null, null, null, Light_Provider.Light_Data.TIMESTAMP + " DESC LIMIT 1");
                if (light != null && light.moveToFirst()) {
                    light_power = light.getDouble(light.getColumnIndex(Light_Provider.Light_Data.LIGHT_LUX));
                }
                if (light != null && !light.isClosed()) light.close();
            }

            probability_compute();
            ContentValues data = new ContentValues();
            data.put(LocationProvider.Location_Data.TIMESTAMP, System.currentTimeMillis());
            data.put(LocationProvider.Location_Data.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
            data.put(LocationProvider.Location_Data.LOCATION, location);
            data.put(LocationProvider.Location_Data.INSIDE_PROBABILITY, inside_percentage);
            data.put(LocationProvider.Location_Data.OUTSIDE_PROBABILITY, outside_percentage);
            data.put(LocationProvider.Location_Data.LABEL, location);
            data.put(LocationProvider.Location_Data.MAGNETOMETER, magneto_power);
            data.put(LocationProvider.Location_Data.LIGHT, light_power);
            data.put(LocationProvider.Location_Data.SIGNAL, signal_strength);
            context.getContentResolver().insert(LocationProvider.Location_Data.CONTENT_URI, data);
        }
    }

    static private void probability_compute() {
        double sum_inside = 0, sum_outside = 0, sum = 0;
        double[] sum_diff = new double[3];
        // when it's night the algorithm for light should be different because in day if the light is a lot we
        // can say that we are inside but in night it is reverse
        if (dark_time) {
            light_threshold = 10;
            sum_diff[1] = light_power - light_threshold;
        } else {
            light_threshold = Integer.valueOf(prefs.getString("edit_light_threshold", "400"));
            sum_diff[1] = light_threshold - light_power;
        }
        sum_diff[0] = magneto_power - magno_threshold;
        sum_diff[2] = signal_threshold - signal_strength;

        sum = Math.abs(sum_diff[0]) + Math.abs(sum_diff[1]) + Math.abs(sum_diff[2]);

        for (double item : sum_diff) {
            if (item < 0) {
                sum_outside = sum_outside + Math.abs(item);
            } else {
                sum_inside = sum_inside + item;
            }
        }

        outside_percentage = Math.round(((sum_outside / sum) * 100) * 100.00) / 100.00;
        inside_percentage = Math.round(((sum_inside / sum) * 100) * 100.00) / 100.00;
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        /* Get the Signal strength from the provider, each tiome there is an update */
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            signal_strength = signalStrength.getGsmSignalStrength();
        }
    }

    ;

}


