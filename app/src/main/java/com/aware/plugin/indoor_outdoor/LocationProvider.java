package com.aware.plugin.indoor_outdoor;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

/**
 * Created by Mitra on 2/17/2015.
 * this provider is for saving information of the sensors and the detected environment + probability of it
 */
public class LocationProvider extends ContentProvider {
    /**
     * Authority of this content provider
     */
    public static String AUTHORITY = "com.aware.plugin.indoor_outdoor.provider.location";
    /**
     * ContentProvider database version. Increment every time you modify the database structure
     */
    public static final int DATABASE_VERSION = 3;

    public static final class Location_Data implements BaseColumns {
        private Location_Data() {
        }

        ;
        /**
         * Your ContentProvider table content URI.<br/>
         * The last segment needs to match your database table name
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/plugin_indoor_outdoor_location");
        /**
         * How your data collection is identified internally in Android (vnd.android.cursor.dir). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.indoor_outdoor";
        /**
         * How each row is identified individually internally in Android (vnd.android.cursor.item). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.indoor_outdoor";
        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String LOCATION = "location";
        public static final String INSIDE_PROBABILITY = "inside_probability";
        public static final String OUTSIDE_PROBABILITY = "outside_probability";
        public static final String LABEL = "label";
        public static final String MAGNETOMETER = "magnetometer";
        public static final String LIGHT = "light";
        public static final String SIGNAL = "signal";
    }

    //ContentProvider query indexes
    private static final int LOCATION = 1;
    private static final int LOCATION_ID = 2;
    /**
     * Database stored in external folder: /AWARE/plugin_template.db
     */
    public static final String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/AWARE/plugin_indoor_outdoor_location.db";
    /**
     * Database tables:<br/>
     * - plugin_phone_usage
     */
    public static final String[] DATABASE_TABLES = {"plugin_indoor_outdoor_location"};
    /**
     * Database table fields
     */
    public static final String[] TABLES_FIELDS = {
            Location_Data._ID + " integer primary key autoincrement," +
                    Location_Data.TIMESTAMP + " real default 0," +
                    Location_Data.DEVICE_ID + " text default ''," +
                    Location_Data.LOCATION + " text default ''," + //indoor/outdoor
                    Location_Data.INSIDE_PROBABILITY + " real default 0.00," +
                    Location_Data.OUTSIDE_PROBABILITY + " real default 0.00," +
                    Location_Data.LABEL + " text default ''," + // I was planning to use it for finding signature for each place
                    Location_Data.MAGNETOMETER + " real default 0," +
                    Location_Data.LIGHT + " real default 0," +
                    Location_Data.SIGNAL + " real default 0," +
                    "UNIQUE (" + Location_Data.TIMESTAMP + "," + Location_Data.DEVICE_ID + ")"
    };
    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> tableMap = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        }
        if (databaseHelper != null && (database == null || !database.isOpen())) {
            database = databaseHelper.getWritableDatabase();
        }
        return (database != null && databaseHelper != null);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w(AUTHORITY, "Database unavailable...");
            return 0;
        }
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case LOCATION:
                count = database.delete(DATABASE_TABLES[0], selection,
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case LOCATION:
                return Location_Data.CONTENT_TYPE;
            case LOCATION_ID:
                return Location_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues new_values) {
        if (!initializeDB()) {
            Log.w(AUTHORITY, "Database unavailable...");
            return null;
        }
        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();
        switch (sUriMatcher.match(uri)) {
            case LOCATION:
                long _id = database.insert(DATABASE_TABLES[0],
                        Location_Data.DEVICE_ID, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(
                            Location_Data.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null);
                    return dataUri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], LOCATION); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", LOCATION_ID); //URI for a single record
        tableMap = new HashMap<String, String>();
        tableMap.put(Location_Data._ID, Location_Data._ID);
        tableMap.put(Location_Data.TIMESTAMP, Location_Data.TIMESTAMP);
        tableMap.put(Location_Data.DEVICE_ID, Location_Data.DEVICE_ID);
        tableMap.put(Location_Data.LOCATION, Location_Data.LOCATION);
        tableMap.put(Location_Data.INSIDE_PROBABILITY, Location_Data.INSIDE_PROBABILITY);
        tableMap.put(Location_Data.OUTSIDE_PROBABILITY, Location_Data.OUTSIDE_PROBABILITY);
        tableMap.put(Location_Data.LABEL, Location_Data.LABEL);
        tableMap.put(Location_Data.MAGNETOMETER, Location_Data.MAGNETOMETER);
        tableMap.put(Location_Data.LIGHT, Location_Data.LIGHT);
        tableMap.put(Location_Data.SIGNAL, Location_Data.SIGNAL);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        if (!initializeDB()) {
            Log.w(AUTHORITY, "Database unavailable...");
            return null;
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case LOCATION:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(tableMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w(AUTHORITY, "Database unavailable...");
            return 0;
        }
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case LOCATION:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            default:
                database.close();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
