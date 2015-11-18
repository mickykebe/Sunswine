package com.example.micky.sunswine;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.example.micky.sunswine.data.WeatherContract;
import com.example.micky.sunswine.data.WeatherDbHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Micky on 11/10/2015.
 */
public class FetchWeatherTask extends AsyncTask<String, Void, Void> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    Context context;

    public FetchWeatherTask(Context context){
        this.context = context;
    }

    protected Void doInBackground(String... params){

        if(params.length == 0)
            return null;

        String locationQuery = params[0];

        // These two need to be declared outside the try/catch
// so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

// Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        int numDays = 14;

        Log.d(LOG_TAG, "FetchWeatherTask()");

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String CITYID_PARAM = "id";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String API_PARAM = "appid";

            Uri forecastUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(CITYID_PARAM, locationQuery)
                    .appendQueryParameter(FORMAT_PARAM, "json")
                    .appendQueryParameter(UNITS_PARAM, "metric")
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(API_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY).build();

            URL url = new URL(forecastUri.toString());

            Log.v(LOG_TAG, "Built URI" + forecastUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            forecastJsonStr = null;
        }  finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        final String OWM_LIST = "list";

        final String OWM_DATETIME = "dt";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        final String OWM_TEMP = "temp";
        final String OWM_MIN = "min";
        final String OWM_MAX = "max";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        try{
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONObject cityJSON = forecastJson.getJSONObject(OWM_CITY);

            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
            String cityName = cityJSON.getString(OWM_CITY_NAME);

            JSONObject cityCoord = cityJSON.getJSONObject(OWM_COORD);
            double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
            double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

            long locationId = addLocation(locationQuery, cityName, cityLatitude, cityLongitude);

            ArrayList<ContentValues> cVVector = new ArrayList<ContentValues>();
            for(int i = 0; i < weatherArray.length(); i++){
                JSONObject weatherJSON = weatherArray.getJSONObject(i);
                Date date = new Date(weatherJSON.getLong(OWM_DATETIME));

                JSONObject tempJSON = weatherJSON.getJSONObject(OWM_TEMP);
                double high = tempJSON.getDouble(OWM_MAX);
                double low = tempJSON.getDouble(OWM_MIN);

                double pressure = weatherJSON.getDouble(OWM_PRESSURE);
                double humidity = weatherJSON.getDouble(OWM_HUMIDITY);
                double windSpeed = weatherJSON.getDouble(OWM_WINDSPEED);
                double windDirection = weatherJSON.getDouble(OWM_WIND_DIRECTION);

                JSONArray moreWeatherArray = weatherJSON.getJSONArray(OWM_WEATHER);
                JSONObject weatherObj = moreWeatherArray.getJSONObject(0);
                int weatherId = weatherObj.getInt(OWM_WEATHER_ID);
                String description = weatherObj.getString(OWM_DESCRIPTION);

                ContentValues weatherValues = new ContentValues();
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATETEXT, WeatherContract.getDbDateString(date));
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
                weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);

                cVVector.add(weatherValues);
            }

            if(cVVector.size() > 0){
                ContentValues[] cVArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cVArray);

                context.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cVArray);
            }
            Log.d(LOG_TAG, "FetchWeatherTask Complete: " + cVVector.size() + " Inserted");
        }
        catch(JSONException e){
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return null;
    }

    private long addLocation(String locationSetting, String cityName, double lat, double lon){
        final Cursor cursor = context.getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ",
                new String[]{locationSetting},
                null);
        if(cursor.moveToFirst()){
            int index = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            return cursor.getLong(index);
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
        values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
        values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
        values.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

        Uri uri = context.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, values);
        return ContentUris.parseId(uri);
    }

    /*@Override
    protected void onPostExecute(String[] forecastStrs) {
        if(forecastStrs != null) {
            forecastAdapter.clear();
            for (String forecastStr : forecastStrs) {
                forecastAdapter.add(forecastStr);
            }
        }
    }


    *//* The date/time conversion code is going to be moved outside the asynctask later,
* so for convenience we're breaking it out into its own method now.
*//*
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }

    *//**
     * Prepare the weather high/lows for presentation.
     *//*
    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String tempunit = sharedPref.getString(getString(R.string.pref_tempunit_key), getString(R.string.pref_tempunit_metric));

        if(!tempunit.equals(getString(R.string.pref_tempunit_metric))){
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        }

        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    *//**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     *//*
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DATETIME = "dt";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime = dayForecast.getLong(OWM_DATETIME);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        for (String s : resultStrs){
            Log.v(LOG_TAG, "Forecast Entry: " + s);
        }

        return resultStrs;
    }*/
}