package com.davidbarron.weatherwatchface;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WearReceiverService extends WearableListenerService{
    String TAG = WearReceiverService.this.getClass().getSimpleName();
    public static final String PATH_WEATHER_INFO = "/WeatherWatchFace/WeatherInfo";
    private GoogleApiClient mGoogleApiClient;
    public static final  String KEY_WEATHER_TEMPERATURE = "Temperature";
    public static final  String KEY_WEATHER_CONDITION   = "Condition";
    public static final String KEY_WEATHER_IMAGE = "Image";
    public static final String KEY_WEATHER_TIMESTAMP = "Timestamp";
    public static final String KEY_BATTERY_LEVEL = "Battery";
    public String path;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG,"onMessageReceived: " + messageEvent);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
        }

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        path=messageEvent.getPath();
        DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(messageEvent.getPath());
        putDataMapRequest.getDataMap().putString(KEY_WEATHER_TEMPERATURE,dataMap.getString(KEY_WEATHER_TEMPERATURE));
        putDataMapRequest.getDataMap().putString(KEY_WEATHER_CONDITION,dataMap.getString(KEY_WEATHER_CONDITION));
        putDataMapRequest.getDataMap().putString(KEY_WEATHER_IMAGE,dataMap.getString(KEY_WEATHER_IMAGE));
        putDataMapRequest.getDataMap().putLong(KEY_WEATHER_TIMESTAMP, System.currentTimeMillis());
        putDataMapRequest.getDataMap().putInt(KEY_BATTERY_LEVEL,dataMap.getInt(KEY_BATTERY_LEVEL));
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest()).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d(TAG, "SaveConfig: " + dataItemResult.getStatus());
                mGoogleApiClient.disconnect();
            }
        });
    }
}
