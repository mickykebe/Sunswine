package com.example.micky.sunswine;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.example.micky.sunswine.data.WeatherContract.WeatherEntry;
import com.example.micky.sunswine.data.WeatherContract.LocationEntry;
import com.example.micky.sunswine.data.WeatherDbHelper;

import junit.framework.Test;

import java.util.Map;
import java.util.Set;

/**
 * Created by Micky on 11/8/2015.
 */
public class TestDb extends AndroidTestCase {
    public static final String LOG_TAG = TestDb.class.getSimpleName();

    public void testCreateDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }

    public String TEST_CITY_NAME = "North Pole";

    ContentValues getLocationContentValues(){
        String testLocationSetting = "99705";
        double testLatitude = 64.772;
        double testLongitude = -147.355;
        ContentValues values = new ContentValues();
        values.put(LocationEntry.COLUMN_CITY_NAME, TEST_CITY_NAME);
        values.put(LocationEntry.COLUMN_LOCATION_SETTING, testLocationSetting);
        values.put(LocationEntry.COLUMN_COORD_LAT, testLatitude);
        values.put(LocationEntry.COLUMN_COORD_LONG, testLongitude);
        return values;
    }

    ContentValues getWeatherContentValues(long locationRowId){
        ContentValues values = new ContentValues();
        values.put(WeatherEntry.COLUMN_DATETEXT, "20141205");
        values.put(WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
        values.put(WeatherEntry.COLUMN_LOC_KEY, locationRowId);
        values.put(WeatherEntry.COLUMN_WEATHER_ID, 321);
        values.put(WeatherEntry.COLUMN_MIN_TEMP, 65);
        values.put(WeatherEntry.COLUMN_MAX_TEMP, 75);
        values.put(WeatherEntry.COLUMN_HUMIDITY, 1.2);
        values.put(WeatherEntry.COLUMN_PRESSURE, 1.3);
        values.put(WeatherEntry.COLUMN_WIND_SPEED, 5.5);
        values.put(WeatherEntry.COLUMN_DEGREES, 1.1);
        return values;
    }

    public static void validateCursor(ContentValues expectedValues, Cursor valueCursor){
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for(Map.Entry<String, Object> entry : valueSet){
            String columnName = entry.getKey();
            int index = valueCursor.getColumnIndex(columnName);
            assertFalse(index == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals(expectedValue, valueCursor.getString(index));
        }
    }

    public void testInsertReadDb(){

        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = getLocationContentValues();

        long locationRowId;
        locationRowId = db.insert(LocationEntry.TABLE_NAME, null, values);

        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New row id: " + locationRowId);

        Cursor cursor = db.query(
                LocationEntry.TABLE_NAME,
                null,
                null, //Columns for the 'where' clause
                null, //Values for the 'where' clause
                null, //Columns to group by
                null, //Columns to filter by row groups
                null //Sort order
        );

        if(cursor.moveToFirst()){
            validateCursor(values, cursor);

            ContentValues weatherValues = getWeatherContentValues(locationRowId);

            long weatherRowId;
            weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);

            assertTrue(weatherRowId != -1);

            Log.d(LOG_TAG, "New Weather row Id: " + weatherRowId);

            Cursor weatherCursor = db.query(
                    WeatherEntry.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            if(weatherCursor.moveToFirst()){
                validateCursor(weatherValues, weatherCursor);
            }
            else{
                fail("Weather Cursor: No values returned");
            }

        }
        else{
            fail("No values returned :(");
        }
    }

}
