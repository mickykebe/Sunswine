package com.example.micky.sunswine;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import com.example.micky.sunswine.data.WeatherContract;
import com.example.micky.sunswine.data.WeatherContract.LocationEntry;
import com.example.micky.sunswine.data.WeatherContract.WeatherEntry;
import com.example.micky.sunswine.data.WeatherDbHelper;
import com.example.micky.sunswine.data.WeatherProvider;

import java.util.Map;
import java.util.Set;

/**
 * Created by Micky on 11/8/2015.
 */
public class TestProvider extends AndroidTestCase {
    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    /*public void testDeleteDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }*/
    public void testDeleteAllRecords(){
        mContext.getContentResolver().delete(WeatherEntry.CONTENT_URI, null, null);
        mContext.getContentResolver().delete(LocationEntry.CONTENT_URI, null, null);

        Cursor cursor = mContext.getContentResolver().query(WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null);
        assertEquals(cursor.getCount(), 0);
        cursor.close();

        cursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null);
        assertEquals(cursor.getCount(), 0);
        cursor.close();
    }

    public String TEST_CITY_NAME = "North Pole";
    public String TEST_DATETEXT = "20141205";
    public String TEST_LOCATION_SETTING = "99705";

    ContentValues getLocationContentValues(){
        double testLatitude = 64.772;
        double testLongitude = -147.355;
        ContentValues values = new ContentValues();
        values.put(LocationEntry.COLUMN_CITY_NAME, TEST_CITY_NAME);
        values.put(LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION_SETTING);
        values.put(LocationEntry.COLUMN_COORD_LAT, testLatitude);
        values.put(LocationEntry.COLUMN_COORD_LONG, testLongitude);
        return values;
    }

    ContentValues getWeatherContentValues(long locationRowId){
        ContentValues values = new ContentValues();
        values.put(WeatherEntry.COLUMN_DATETEXT, TEST_DATETEXT);
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

    public void testInsertReadProvider() throws NullPointerException{

        //Insert Location
        ContentValues values = getLocationContentValues();

        Uri locUri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI, values);

        long locationRowId = ContentUris.parseId(locUri);

        //Test: Location main
        Cursor cursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if(cursor.moveToFirst()){
            validateCursor(values, cursor);
        }
        else{
            fail("No values returned :(");
        }

        // Test: Location with ID
        Cursor locCursor = mContext.getContentResolver().query(LocationEntry.buildLocationUri(locationRowId),
                null,
                null,
                null,
                null);

        if(locCursor.moveToFirst()){
            validateCursor(values, locCursor);
        }
        else{
            fail("Loc Cursor: No values returned");
        }

        // Insert Weather
        ContentValues weatherValues = getWeatherContentValues(locationRowId);

        Uri weatherUri = mContext.getContentResolver().insert(WeatherEntry.CONTENT_URI, weatherValues);
        long weatherRowId = ContentUris.parseId(weatherUri);

        // Test: Weather main
        Cursor weatherCursor = mContext.getContentResolver().query(WeatherEntry.CONTENT_URI,
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

        //Test: Weather + Location via Location setting + StartDate
        Cursor locDateBothCursor = mContext.getContentResolver().query(WeatherEntry.buildWeatherLocationWithStartDate(TEST_LOCATION_SETTING, TEST_DATETEXT),
                null, null, null, null);

        ContentValues joinedValues = new ContentValues();

        joinedValues.putAll(weatherValues);
        joinedValues.putAll(values);

        if(locDateBothCursor.moveToFirst()){
            validateCursor(joinedValues, locDateBothCursor);
        }
        else{
            fail("locStartDateBoth Cursor: No Values returned");
        }

        //Test: Weather + Location via Location setting
        Cursor locBothCursor = mContext.getContentResolver().query(WeatherEntry.buildWeatherLocation(TEST_LOCATION_SETTING),
                null, null, null, null);

        if(locBothCursor.moveToFirst()){
            validateCursor(joinedValues, locBothCursor);
        }
        else{
            fail("locBoth Cursor: No Values returned");
        }

        //Test: Weather + Location via Location Setting + Date
        cursor = mContext.getContentResolver().query(WeatherEntry.buildWeatherLocationWithDate(TEST_LOCATION_SETTING, TEST_DATETEXT),
                null, null, null, null);

        if(cursor.moveToFirst()){
            validateCursor(joinedValues, cursor);
        }
        else{
            fail("locDateBoth Cursor: No Values Returned");
        }

        //Test: Delete Everything Again
        testDeleteAllRecords();
    }

    public void testUpdateLocation(){
        testDeleteAllRecords();

        //Insert New Row and Test
        ContentValues values = getLocationContentValues();
        Uri uri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI, values);
        long insertedRowId = ContentUris.parseId(uri);
        assert(insertedRowId != -1);

        //Update row and Test
        ContentValues values2 = new ContentValues(values);
        values2.put(LocationEntry.COLUMN_CITY_NAME, "Santa's Village");

        int updatedRows = mContext.getContentResolver().update(LocationEntry.CONTENT_URI,
                values2,
                LocationEntry._ID + " = ?",
                new String[]{Long.toString(insertedRowId)});
        assertEquals(updatedRows, 1);

        Cursor cursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        if(cursor.moveToFirst()){
            validateCursor(values2, cursor);
        }
        else{
            fail("testUpdateLocation: No values returned");
        }

        cursor.close();
    }

    public void testGetType() {
        String type = mContext.getContentResolver().getType(WeatherEntry.CONTENT_URI);
        assertEquals(type, WeatherEntry.CONTENT_TYPE);

        String testLocation = "344979";
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocation(testLocation));
        assertEquals(type, WeatherEntry.CONTENT_TYPE);

        String testDate = "20140612";
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));
        assertEquals(type, WeatherEntry.CONTENT_ITEM_TYPE);

        type = mContext.getContentResolver().getType(LocationEntry.CONTENT_URI);
        assertEquals(type, LocationEntry.CONTENT_TYPE);

        type = mContext.getContentResolver().getType(LocationEntry.buildLocationUri(1L));
        assertEquals(type, LocationEntry.CONTENT_ITEM_TYPE);
    }
}
