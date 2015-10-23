package com.davidbarron.weatherwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class WeatherWatchFaceAlarmReceiver extends BroadcastReceiver {
    private static final String TAG="AlarmReceiver";
    public static final String PATH_WEATHER_REQUIRE = "/WeatherService/Require";
    @Override
    public void onReceive(Context context, Intent intent) {

        final GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        Log.i(TAG,"Getting weather");
        new Thread(new Runnable() {
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                for (Node node : nodes.getNodes()) {
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), PATH_WEATHER_REQUIRE, null)
                            .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                    Log.d(TAG, "SendRequireMessage:" + sendMessageResult.getStatus());
                                }
                            });
                }
            }
        }).start();
    }

}
