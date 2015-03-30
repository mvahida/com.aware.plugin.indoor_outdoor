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
 * this provider save information about what the user is doing. walking - in car - still - on bike
 */
public class ActivityProvider extends ContentProvider {
    /**
     * Authority of this content provider
     */
    public static String AUTHORITY = "com.aware.plugin.vahida.provider";
    /**
     * ContentProvider database version. Increment every time you modify the database structure
     */
    public static final int DATABASE_VERSION = 2;

    public static final class Activity_Data implements BaseColumns {
        private Activity_Data() {
        }

        ;
        /**
         * Your ContentProvider table content URI.<br/>
         * The last segment needs to match your database table name
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/ACTIVITY");
        /**
         * How your data collection is identified internally in Android (vnd.android.cursor.dir). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.vahida";
        /**
         * How each row is identified individually internally in Android (vnd.android.cursor.item). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.vahida";
        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String DETECTED_ACTIVITY = "detected_activity";
        public static final String ACTIVITY_PROBABILITY = "activity_probability";
        // public static final String ELAPSED_DEVICE_OFF = "elapsed_device_off";
        // public static final String ELAPSED_DEVICE_OFF = "elapsed_device_off";
    }

    //ContentProvider query indexes
    private static final int ACTIVITY = 1;
    private static final int ACTIVITY_ID = 2;
    /**
     * Database stored in external folder: /AWARE/plugin_template.db
     */
    public static final String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/AWARE/ACTIVITY.db";
    /**
     * Database tables:<br/>
     * - plugin_phone_usage
     */
    public static final String[] DATABASE_TABLES = {"ACTIVITY"};
    /**
     * Database table fields
     */
    public static final String[] TABLES_FIELDS = {
            Activity_Data._ID + " integer primary key autoincrement," +
                    Activity_Data.TIMESTAMP + " real default 0," +
                    Activity_Data.DEVICE_ID + " text default ''," +
                    Activity_Data.DETECTED_ACTIVITY + " text default ''," +
                    Activity_Data.ACTIVITY_PROBABILITY + " integer," +
                    "UNIQUE (" + Activity_Data.TIMESTAMP + "," + Activity_Data.DEVICE_ID + ")"
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
            case ACTIVITY:
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
            case ACTIVITY:
                return Activity_Data.CONTENT_TYPE;
            case ACTIVITY_ID:
                return Activity_Data.CONTENT_ITEM_TYPE;
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
            case ACTIVITY:
                long _id = database.insert(DATABASE_TABLES[0],
                        Activity_Data.DEVICE_ID, values);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(
                            Activity_Data.CONTENT_URI, _id);
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
        //AUTHORITY = getContext().getPackageName() + ".provider.template";
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], ACTIVITY); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", ACTIVITY_ID); //URI for a single record
        tableMap = new HashMap<String, String>();
        tableMap.put(Activity_Data._ID, Activity_Data._ID);
        tableMap.put(Activity_Data.TIMESTAMP, Activity_Data.TIMESTAMP);
        tableMap.put(Activity_Data.DEVICE_ID, Activity_Data.DEVICE_ID);
        tableMap.put(Activity_Data.DETECTED_ACTIVITY, Activity_Data.DETECTED_ACTIVITY);
        tableMap.put(Activity_Data.ACTIVITY_PROBABILITY, Activity_Data.ACTIVITY_PROBABILITY);
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
            case ACTIVITY:
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
            case ACTIVITY:
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
