package com.example.micky.sunswine;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    private final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    private final String FORECAST_SHARE_HASHTAG = "#SunshineApp";
    private String mForecastStr;

    public DetailActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_detailfragment, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);

        ShareActionProvider mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if(mShareActionProvider == null){
            Log.d(LOG_TAG, "Share Action Provider is null?");
            return;
        }

        mShareActionProvider.setShareIntent(createShareForecastIntent());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        Intent intent = getActivity().getIntent();

        if(intent == null || intent.getStringExtra(Intent.EXTRA_TEXT) == ""){
            return rootView;
        }

        mForecastStr = intent.getStringExtra(Intent.EXTRA_TEXT);
        TextView detailForecastTextview = (TextView) rootView.findViewById(R.id.detailforecast_textview);

        detailForecastTextview.setText(mForecastStr);

        return rootView;
    }

    private Intent createShareForecastIntent(){
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mForecastStr + FORECAST_SHARE_HASHTAG);
        return intent;
    }
}
