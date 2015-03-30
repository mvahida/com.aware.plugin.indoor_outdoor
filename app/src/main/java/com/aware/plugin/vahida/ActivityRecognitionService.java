package com.aware.plugin.vahida;

/**
 * Created by Mitra on 3/10/2015.
 */

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionService extends IntentService {

    private final String TAG = "MyAwesomeApp";

    public ActivityRecognitionService() {
        super("My Activity Recognition Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbable = result.getMostProbableActivity();

            Intent i = new Intent("com.aware.plugin.vahida.ACTIVITY_RECOGNITION_DATA");
            i.putExtra("Activity", getType(result.getMostProbableActivity().getType()));
            i.putExtra("Confidence", result.getMostProbableActivity().getConfidence());
            sendBroadcast(i);

            Plugin.activity_probability = mostProbable.getConfidence();
            Plugin.detected_activity = mostProbable.getType();
            String activity_name = getType(Plugin.detected_activity);

            ContentValues data = new ContentValues();
            data.put(ActivityProvider.Activity_Data.TIMESTAMP, System.currentTimeMillis());
            data.put(ActivityProvider.Activity_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            data.put(ActivityProvider.Activity_Data.DETECTED_ACTIVITY, activity_name);
            data.put(ActivityProvider.Activity_Data.ACTIVITY_PROBABILITY, mostProbable.getConfidence());
            getContentResolver().insert(ActivityProvider.Activity_Data.CONTENT_URI, data);
        }
    }

    private String getType(int type) {
        if (type == DetectedActivity.UNKNOWN)
            return "Unknown";
        else if (type == DetectedActivity.IN_VEHICLE)
            return "In Vehicle";
        else if (type == DetectedActivity.ON_BICYCLE)
            return "On Bicycle";
        else if (type == DetectedActivity.ON_FOOT)
            return "On Foot";
        else if (type == DetectedActivity.STILL)
            return "Still";
        else if (type == DetectedActivity.TILTING)
            return "Tilting";
        else
            return "";
    }
}