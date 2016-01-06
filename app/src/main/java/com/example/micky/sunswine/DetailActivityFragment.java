package com.example.micky.sunswine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.micky.sunswine.data.WeatherContract;
import com.example.micky.sunswine.data.WeatherContract.WeatherEntry;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private final String LOG_TAG = DetailActivityFragment.class.getSimpleName();

    public static final String BUNDLE_URI_KEY = "detailUri";

    private static final int DETAIL_LOADER = 0;
    private final String FORECAST_SHARE_HASHTAG = "#SunshineApp";
    private String mForecast;
    private Uri mUri;
    private ShareActionProvider mShareActionProvider;

    public ImageView mIconView;
    private TextView mDayView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mWindView;
    private TextView mPressureView;

    private static final String[] FORECAST_COLUMNS = {
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_HUMIDITY,
            WeatherEntry.COLUMN_WIND_SPEED,
            WeatherEntry.COLUMN_DEGREES,
            WeatherEntry.COLUMN_PRESSURE,
            WeatherEntry.COLUMN_WEATHER_ID
            };

    private static final int COL_WEATHER_ID = 0;
    private static final int COL_WEATHER_DATE = 1;
    private static final int COL_WEATHER_DESC = 2;
    private static final int COL_WEATHER_MAX_TEMP = 3;
    private static final int COL_WEATHER_MIN_TEMP = 4;
    private static final int COL_WEATHER_HUMIDITY = 5;
    private static final int COL_WEATHER_WIND_SPEED = 6;
    private static final int COL_WEATHER_DEGREES = 7;
    private static final int COL_WEATHER_PRESSURE = 8;
    private static final int COL_WEATHER_CONDITION_ID = 9;

    private static final String[] LOCATION_GEO_PROJECTION = {
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    private static final int INDEX_LAT = 0;
    private static final int INDEX_LONG = 1;

    public DetailActivityFragment() {
    }

    public static DetailActivityFragment createDetailFragment(Uri dateUri){
        DetailActivityFragment df = new DetailActivityFragment();

        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_URI_KEY, dateUri);
        df.setArguments(bundle);

        return df;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
    }

    public void onLocationChanged(String newLocation){
        Log.v(LOG_TAG, "In onLocationChanged()");
        Uri uri = mUri;
        if(uri != null){
            long date = WeatherEntry.getDateFromUri(uri);
            Uri newUri = WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = newUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detailfragment, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);

        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if(mForecast == null){
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
        else{
            Log.d(LOG_TAG, "Share Action Provider is null?");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        Bundle arguments = getArguments();
        if(arguments != null){
            mUri = arguments.getParcelable(BUNDLE_URI_KEY);
        }

        mIconView = (ImageView) view.findViewById(R.id.detail_icon_imageview);
        mDayView = (TextView) view.findViewById(R.id.detail_day_textview);
        mDateView = (TextView) view.findViewById(R.id.detail_date_textview);
        mDescriptionView = (TextView) view.findViewById(R.id.detail_forecast_textview);
        mHighTempView = (TextView) view.findViewById(R.id.detail_max_textview);
        mLowTempView = (TextView) view.findViewById(R.id.detail_min_textview);
        mHumidityView = (TextView) view.findViewById(R.id.detail_humidity_textview);
        mWindView = (TextView) view.findViewById(R.id.detail_wind_textview);
        mPressureView = (TextView) view.findViewById(R.id.detail_pressure_textview);

        return view;
    }

    private Intent createShareForecastIntent(){
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
        return intent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader()");

        if(mUri == null)
            return null;

        return new CursorLoader(
                getActivity(),
                mUri,
                FORECAST_COLUMNS,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG, "In onLoadFinished");
        if (data == null || !data.moveToFirst()) { return; }

        boolean isMetric = Utility.isMetric(getActivity());

        int weatherCondition = data.getInt(COL_WEATHER_CONDITION_ID);
        mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherCondition));

        long dateInMilliSeconds = data.getLong(COL_WEATHER_DATE);
        mDayView.setText(Utility.getDayName(getActivity(), dateInMilliSeconds));
        mDateView.setText(Utility.getFormattedMonthDay(getActivity(), dateInMilliSeconds));

        double high = data.getDouble(COL_WEATHER_MAX_TEMP);
        mHighTempView.setText(Utility.formatTemperature(getActivity(), high, isMetric));

        double low = data.getDouble(COL_WEATHER_MIN_TEMP);
        mLowTempView.setText(Utility.formatTemperature(getActivity(), low, isMetric));

        mDescriptionView.setText(data.getString(COL_WEATHER_DESC));

        double humidity = data.getDouble(COL_WEATHER_HUMIDITY);
        mHumidityView.setText(String.format(getActivity().getString(R.string.format_humidity), humidity));

        float windSpeed = data.getFloat(COL_WEATHER_WIND_SPEED);
        float degrees = data.getFloat(COL_WEATHER_DEGREES);
        mWindView.setText(Utility.getFormattedWind(getActivity(), windSpeed, degrees));

        float pressure = data.getFloat(COL_WEATHER_PRESSURE);
        mPressureView.setText(String.format(getActivity().getString(R.string.format_pressure), pressure));

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
