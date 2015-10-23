package com.davidbarron.weatherwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class MobileReceiverService extends WearableListenerService {
    String TAG = MobileReceiverService.this.getClass().getSimpleName();
    private LocationManager mLocationManager;
    private Location mLocation;
    public static final String PATH_SERVICE_REQUIRE = "/WeatherService/Require";
    public static final String PATH_WEATHER_INFO = "/WeatherWatchFace/WeatherInfo";
    public static final  String KEY_WEATHER_TEMPERATURE = "Temperature";
    public static final  String KEY_WEATHER_CONDITION   = "Condition";
    public static final String KEY_WEATHER_IMAGE = "Image";
    public static final String KEY_BATTERY_LEVEL = "Battery";
    String nodeId;
    int batteryLevel=100;
    final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, intentFilter);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(TAG, "MessageReceived: " + messageEvent.getPath());
        nodeId = messageEvent.getSourceNodeId();
        if (messageEvent.getPath().equals(PATH_SERVICE_REQUIRE)) {
            startTask();
        }
    }

    private void startTask() {
        Log.d(TAG, "Start Weather AsyncTask");
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (mLocation == null) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            mLocationManager.removeUpdates(this);
                            mLocation = location;
                            ConnectivityManager connMgr = (ConnectivityManager)  getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                            if (networkInfo != null && networkInfo.isConnected()) {
                                new FetchWeatherTask().execute(mLocation);
                            }
                        }

                        @Override
                        public void onStatusChanged( String provider, int status, Bundle extras )
                        {

                        }

                        @Override
                        public void onProviderEnabled( String provider )
                        {

                        }

                        @Override
                        public void onProviderDisabled( String provider )
                        {

                        }
                    }
            );
        }
        else
        {
            ConnectivityManager connMgr = (ConnectivityManager)  getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                new FetchWeatherTask().execute(mLocation);
            }
        }
    }


    public class FetchWeatherTask extends AsyncTask<Location,Void,WeatherItem> {

        @Override
        protected WeatherItem doInBackground(Location...params) {
            if(params!=null && params.length>0) {
                Location loc = params[0];
                return new WeatherFetcher().fetchWeather(loc);
            }
            return null;
        }

        @Override
        protected void onPostExecute(WeatherItem item) {
            if(item != null) {
                final GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(MobileReceiverService.this).addApi(Wearable.API).build();
                mGoogleApiClient.connect();

                DataMap config = new DataMap();
                config.putString(KEY_WEATHER_TEMPERATURE, item.getTemp_value());
                config.putString(KEY_WEATHER_CONDITION, item.getWeather_value());
                config.putString(KEY_WEATHER_IMAGE, item.getWeather_number());
                config.putInt(KEY_BATTERY_LEVEL,batteryLevel);
                Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, PATH_WEATHER_INFO, config.toByteArray()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.d(TAG, "SendUpdateMessage: " + sendMessageResult.getStatus());
                        mGoogleApiClient.disconnect();
                    }
                });
            }
        }
    }
}
