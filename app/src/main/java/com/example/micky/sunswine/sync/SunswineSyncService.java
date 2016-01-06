package com.example.micky.sunswine.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SunswineSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static SunswineSyncAdapter sSunswineSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("SunshineSyncService", "onCreate - SunshineSyncService");
        synchronized (sSyncAdapterLock) {
            if (sSunswineSyncAdapter == null) {
                sSunswineSyncAdapter = new SunswineSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSunswineSyncAdapter.getSyncAdapterBinder();
    }
}