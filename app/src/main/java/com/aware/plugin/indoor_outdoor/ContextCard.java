package com.aware.plugin.indoor_outdoor;


import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.ui.Stream_UI;
import com.aware.utils.IContextCard;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

//TODO: show daily plot of indoor/outdoor time stacked bar plot

public class ContextCard implements IContextCard {
    private XYSeries magno = new XYSeries("Magnotometer");
    private XYSeries light = new XYSeries("Light");
    private XYSeries signal = new XYSeries("Signal");
    //Set how often your card needs to refresh if the stream is visible (in milliseconds)
    private int refresh_interval = 1 * 1000; //1 second = 1000 milliseconds
    //DEMO: we are demo'ing a counter incrementing in real-time
    private int counter = 0;
    private TextView locationText, InfoMagneto, InfoLight, InfoSignal;
    private ImageView img_activity;
    private GraphicalView chartView;
    private LinearLayout chartLyt;
    private static CheckBox signal_button, light_button, magnetometer_button;
    public static boolean is_signal_enabled, is_light_enabled, is_magnetometer_enabled;
    private static SharedPreferences prefs;
    private static Button btn_yes, btn_no;
    private Context sContext;
    //Declare here all the UI elements you'll be accessing
    private View card;
    //Used to load your context card
    private LayoutInflater sInflater;
    private Handler uiRefresher = new Handler(Looper.getMainLooper());

    //Empty constructor used to instantiate this card
    public ContextCard() {

    }

    ;
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {
            //Modify card's content here once it's initialized
            if (card != null) {
                //DEMO display the counter value

            }
            //Reset timer and schedule the next card refresh
            uiRefresher.postDelayed(uiChanger, refresh_interval);
        }
    };

    @Override
    public View getContextCard(final Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        is_magnetometer_enabled = prefs.getBoolean("status_chart_magnetometer", true);
        is_light_enabled = prefs.getBoolean("status_chart_light", true);
        is_signal_enabled = prefs.getBoolean("status_chart_signal", true);

        //Tell Android that you'll monitor the stream statuses
        sContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_OPEN);
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_CLOSED);
        context.registerReceiver(streamObs, filter);
        //Load card information to memory
        sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        card = sInflater.inflate(R.layout.card, null);


        chartLyt = (LinearLayout) card.findViewById(R.id.chart);


        locationText = (TextView) card.findViewById(R.id.location);
        InfoMagneto = (TextView) card.findViewById(R.id.InfoMagneto);
        InfoLight = (TextView) card.findViewById(R.id.InfoLight);
        InfoSignal = (TextView) card.findViewById(R.id.InfoSignal);
        img_activity = (ImageView) card.findViewById(R.id.img_activity);
        magnetometer_button = (CheckBox) card.findViewById(R.id.magnetometer_button);
        light_button = (CheckBox) card.findViewById(R.id.light_button);
        signal_button = (CheckBox) card.findViewById(R.id.signal_button);
        btn_yes = (Button) card.findViewById(R.id.btn_yes);
        btn_no = (Button) card.findViewById(R.id.btn_no);

        magnetometer_button.setChecked(is_magnetometer_enabled);
        light_button.setChecked(is_light_enabled);
        signal_button.setChecked(is_signal_enabled);

        magnetometer_button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prefs.edit().putBoolean("status_chart_magnetometer", true).apply();
                } else

                {
                    prefs.edit().putBoolean("status_chart_magnetometer", false).apply();
                }

            }
        });
        light_button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prefs.edit().putBoolean("status_chart_light", true).apply();
                } else

                {
                    prefs.edit().putBoolean("status_chart_light", false).apply();
                }

            }
        });
        signal_button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prefs.edit().putBoolean("status_chart_signal", true).apply();
                } else

                {
                    prefs.edit().putBoolean("status_chart_signal", false).apply();
                }

            }
        });

        btn_yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = "Correct data: " + locationText.getText().toString() + "," +
                        InfoMagneto.getText().toString() + "," +
                        InfoLight.getText().toString() + "," +
                        InfoSignal.getText().toString() + '\n' + '\n';
                Save save = new Save(context, data);
                save.execute();
            }
        });
        btn_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = "Wrong data: " + locationText.getText().toString() + "," +
                        InfoMagneto.getText().toString() + "," +
                        InfoLight.getText().toString() + "," +
                        InfoSignal.getText().toString() + '\n' + '\n';
                Save save = new Save(context, data);
                save.execute();
            }
        });

        Cursor action = context.getContentResolver().query
                (ActivityProvider.Activity_Data.CONTENT_URI, null, null, null, ActivityProvider.Activity_Data.TIMESTAMP + " DESC LIMIT 1");
        if (action != null && action.moveToFirst()) {
            String str_activity = action.getString(action.getColumnIndex(ActivityProvider.Activity_Data.DETECTED_ACTIVITY));
            if (str_activity.contains("In Vehicle")) {
                img_activity.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_vehicle));
            } else if (str_activity.contains("On Bicycle")) {
                img_activity.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_biking));
            } else if (str_activity.contains("On Foot")) {
                img_activity.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_walking));
            } else if (str_activity.contains("Still")) {
                img_activity.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_still));
            }
        }
        if (action != null && !action.isClosed()) action.close();

        if (!magnetometer_button.isChecked() && !light_button.isChecked() && !signal_button.isChecked()) {
            Cursor location_cursor = context.getContentResolver().query
                    (LocationProvider.Location_Data.CONTENT_URI, null, null, null, LocationProvider.Location_Data.TIMESTAMP + " DESC LIMIT 5");
            if (location_cursor != null && location_cursor.moveToFirst()) {
                double probability_inside = location_cursor.getDouble(location_cursor.getColumnIndex(LocationProvider.Location_Data.INSIDE_PROBABILITY));
                double probability_outside = location_cursor.getDouble(location_cursor.getColumnIndex(LocationProvider.Location_Data.OUTSIDE_PROBABILITY));
                double magneto_power = location_cursor.getDouble(location_cursor.getColumnIndex(LocationProvider.Location_Data.MAGNETOMETER));
                double light_power = location_cursor.getDouble(location_cursor.getColumnIndex(LocationProvider.Location_Data.LIGHT));
                double signal_power = location_cursor.getDouble(location_cursor.getColumnIndex(LocationProvider.Location_Data.SIGNAL));
                if (probability_inside < probability_outside) {
                    locationText.setText("You are outside " + probability_outside + "% and "
                            + probability_inside + "% inside");
                } else {
                    locationText.setText("You are Inside " + probability_inside + "% and "
                            + probability_outside + "% outside");
                }
                InfoMagneto.setText("Magnetometer value is: " + Math.round(magneto_power * 100.00) / 100.00);
                InfoLight.setText("Light value is: " + Math.round(light_power * 100.00) / 100.00);
                InfoSignal.setText("GSM signal value is: " + Math.round(signal_power * 100.00) / 100.00);
            }
            if (location_cursor != null && !location_cursor.isClosed()) location_cursor.close();
        } else {
            Cursor location_cursor = context.getContentResolver().query
                    (LocationProvider.Location_Data.CONTENT_URI, null, null, null, LocationProvider.Location_Data.TIMESTAMP + " DESC LIMIT 5");
            if (location_cursor != null && location_cursor.moveToFirst()) {
                do {
                    double probability_inside = location_cursor.getDouble(location_cursor.getColumnIndex(LocationProvider.Location_Data.INSIDE_PROBABILITY));
                    double probability_outside = location_cursor.getDouble(location_cursor.getColumnIndex(LocationProvider.Location_Data.OUTSIDE_PROBABILITY));
                    double magneto_power = location_cursor.getDouble(location_cursor.getColumnIndex(LocationProvider.Location_Data.MAGNETOMETER));
                    double light_power = location_cursor.getDouble(location_cursor.getColumnIndex(LocationProvider.Location_Data.LIGHT));
                    double signal_power = location_cursor.getDouble(location_cursor.getColumnIndex(LocationProvider.Location_Data.SIGNAL));
                    if (probability_inside < probability_outside) {
                        locationText.setText("You are outside " + probability_outside + "% and " + probability_inside + "% inside");
                    } else {
                        locationText.setText("You are Inside " + probability_inside + "% and " + probability_outside + "% outside");
                    }
                    if (magnetometer_button.isChecked()) {
                        magno.add(location_cursor.getPosition(), Math.round(magneto_power * 100.00) / 100.00);
                    }
                    if (light_button.isChecked()) {
                        light.add(location_cursor.getPosition(), Math.round(light_power * 100.00) / 100.00);
                    }
                    if (signal_button.isChecked()) {
                        signal.add(location_cursor.getPosition(), Math.round(signal_power * 100.00) / 100.00);
                    }
                    InfoMagneto.setText("Magnetometer value is: " + Math.round(magneto_power * 100.00) / 100.00);
                    InfoLight.setText("Light value is: " + Math.round(light_power * 100.00) / 100.00);
                    InfoSignal.setText("GSM signal value is: " + Math.round(signal_power * 100.00) / 100.00);

                } while (location_cursor.moveToNext());
            }
            if (location_cursor != null && !location_cursor.isClosed()) location_cursor.close();
            chartView = DrawChart();
            //chartLyt.addView(chartView, 0);
            chartLyt.addView(chartView, new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 500));
        }

        uiRefresher.postDelayed(uiChanger, refresh_interval);
        return card;
    }

    private GraphicalView DrawChart() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(magno);
        dataset.addSeries(light);
        dataset.addSeries(signal);

        XYSeriesRenderer magnoRenderer = new XYSeriesRenderer();
        magnoRenderer.setLineWidth(2);
        magnoRenderer.setColor(sContext.getResources().getColor(R.color.aware_blue));
        magnoRenderer.setDisplayBoundingPoints(true);
        magnoRenderer.setPointStyle(PointStyle.CIRCLE);
        magnoRenderer.setPointStrokeWidth(3);

        XYSeriesRenderer lightRenderer = new XYSeriesRenderer();
        lightRenderer.setLineWidth(2);
        lightRenderer.setColor(sContext.getResources().getColor(R.color.aware_pink));
        lightRenderer.setDisplayBoundingPoints(true);
        lightRenderer.setPointStyle(PointStyle.DIAMOND);
        lightRenderer.setPointStrokeWidth(3);

        XYSeriesRenderer signalRenderer = new XYSeriesRenderer();
        signalRenderer.setLineWidth(2);
        signalRenderer.setColor(sContext.getResources().getColor(R.color.aware_green));
        signalRenderer.setDisplayBoundingPoints(true);
        signalRenderer.setPointStyle(PointStyle.SQUARE);
        signalRenderer.setPointStrokeWidth(3);

        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        mRenderer.addSeriesRenderer(magnoRenderer);
        mRenderer.addSeriesRenderer(lightRenderer);
        mRenderer.addSeriesRenderer(signalRenderer);
        mRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00)); // transparent margins
        // Disable Pan on two axis
        mRenderer.setPanEnabled(false, false);
        mRenderer.setZoomEnabled(false);
        mRenderer.setZoomRate(6.0f);
        mRenderer.setShowLabels(true);
        mRenderer.setFitLegend(true);
        mRenderer.setInScroll(true);
        mRenderer.setYAxisMax(350);
        mRenderer.setYAxisMin(0);
        mRenderer.setShowGrid(true); // we show the grid
        GraphicalView chartView = ChartFactory.getLineChartView(sContext, dataset, mRenderer);
        return chartView;
    }

    private StreamObs streamObs = new StreamObs();

    public class StreamObs extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_OPEN)) {
                //start refreshing when user enters the stream
                uiRefresher.postDelayed(uiChanger, refresh_interval);
                //DEMO only, reset the counter every time the user opens the stream
                counter = 0;
            }
            if (intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_CLOSED)) {
                //stop refreshing when user leaves the stream
                uiRefresher.removeCallbacks(uiChanger);
                uiRefresher.removeCallbacksAndMessages(null);
            }
        }
    }

    private boolean writeToFile(String data) {
        String path = Environment.getExternalStorageDirectory() + "/AWARE/DataAnalyze.txt";
        try {
            File file = new File(path);
            // If file does not exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.append(data);
            bw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private class Save extends AsyncTask<Void, Void, Boolean> {
        private ProgressDialog progress = null;
        Context context;
        String strValue;

        public Save(Context inContext, String strValue) {
            this.context = inContext;
            this.strValue = strValue;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO Auto-generated method stub
            return writeToFile(strValue);
        }

        @Override
        protected void onPreExecute() {
            //progress = ProgressDialog.show(context, null,
            //"Please wait"); // for some reason sContext is null and cause crash.
            // I couldn't find a solution for that and I didn't have
            // enough time. I'll do it later :(
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            if (result) {
                Toast.makeText(
                        context,
                        "Saved in /sdcard/AWARE/DataAnalyze.txt",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context,
                        "Oops! The record did not save", Toast.LENGTH_LONG)
                        .show();
            }
            //progress.dismiss();
            super.onPostExecute(result);
        }

    }
}
