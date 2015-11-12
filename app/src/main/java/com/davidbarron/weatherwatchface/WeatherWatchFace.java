package com.davidbarron.weatherwatchface;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class WeatherWatchFace extends CanvasWatchFaceService {
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int MSG_UPDATE_TIME = 0;


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,DataApi.DataListener, NodeApi.NodeListener {
        Bitmap mScaledWeatherBitmap;
        Paint mBackgroundPaint;
        Paint mHandPaint;
        Paint variablePaint;
        Paint mSecondHandPaint;
        Paint mTextPaint;
        Paint mSmallText;

        boolean mAmbient;
        int width, height, watchBatteryLevel=100,phoneBatteryLevel=100;
        Time mTime;
        public static final  String KEY_WEATHER_TEMPERATURE = "Temperature";
        public static final  String KEY_WEATHER_CONDITION   = "Condition";
        public static final String KEY_WEATHER_IMAGE = "Image";
        public static final String KEY_WEATHER_TIMESTAMP = "Timestamp";
        public static final String KEY_BATTERY_LEVEL = "Battery";
        public static final long VIBRATE_TIME = 250;
        String temperature = "00", condition="no connection", dateString="00/00/0000", updateTime="00:00";
        String TAG=WeatherWatchFace.this.getClass().getSimpleName();
        Resources resources = WeatherWatchFace.this.getResources();

        GoogleApiClient mGoogleApiClient;

        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        boolean mLowBitAmbient;

        final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                watchBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            }
        };
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            mGoogleApiClient = new GoogleApiClient.Builder(WeatherWatchFace.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
            mGoogleApiClient.connect();
            setWatchFaceStyle(new WatchFaceStyle.Builder(WeatherWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.TOP | Gravity.CENTER)
                    .setHotwordIndicatorGravity(Gravity.TOP | Gravity.CENTER)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());


            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.analog_background));

            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.analog_hands));
            mHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke));
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondHandPaint = new Paint();
            mSecondHandPaint.setColor(resources.getColor(R.color.analog_second_hand));
            mSecondHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_second_hand_stroke));
            mSecondHandPaint.setAntiAlias(true);
            mSecondHandPaint.setStrokeCap(Paint.Cap.ROUND);
            mTextPaint = new Paint();
            mTextPaint.setColor(resources.getColor(R.color.analog_hands));
            mTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(resources.getDimension(R.dimen.text_size));
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mSmallText = new Paint();
            mSmallText.setColor(resources.getColor(R.color.analog_hands));
            mSmallText.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            mSmallText.setAntiAlias(true);
            mSmallText.setTextSize(resources.getDimension(R.dimen.small_text));
            mSmallText.setTextAlign(Paint.Align.CENTER);
            variablePaint = new Paint();
            variablePaint.setColor(resources.getColor(R.color.analog_background));
            variablePaint.setAntiAlias(true);

            mTime = new Time();
            AlarmManager alarmManager = (AlarmManager) WeatherWatchFace.this.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(WeatherWatchFace.this,WeatherWatchFaceAlarmReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(WeatherWatchFace.this, 0, intent, 0);
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);

        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaint.setAntiAlias(!inAmbientMode);
                    mSecondHandPaint.setAntiAlias(!inAmbientMode);
                    mTextPaint.setAntiAlias(!inAmbientMode);
                    mSmallText.setAntiAlias(!inAmbientMode);
                    variablePaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

             updateTimer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            super.onSurfaceChanged(holder, format, w, h);
            width = w;
            height = h;
            Bitmap mWeatherBitmap = ((BitmapDrawable) resources.getDrawable(R.mipmap.weather_na)).getBitmap();
            if (mScaledWeatherBitmap == null || mScaledWeatherBitmap.getWidth() != width || mScaledWeatherBitmap.getHeight() != height) {
                mScaledWeatherBitmap = Bitmap.createScaledBitmap(mWeatherBitmap,width/3, height/3, true);
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();
            int arc_size=resources.getInteger(R.integer.arc_size);
            int arc_start=resources.getInteger(R.integer.arc_start);

            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);


            float centerX = width / 2f;
            float centerY = height / 2f;

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;
            if (!mAmbient) {
                canvas.drawBitmap(mScaledWeatherBitmap, (mScaledWeatherBitmap.getWidth()), (mScaledWeatherBitmap.getHeight() / 2), null);
                canvas.drawText(temperature, centerX, centerY + arc_size, mTextPaint);
                canvas.drawText(condition.toUpperCase(), centerX, centerY + arc_size*2, mTextPaint);
                canvas.drawText(dateString, centerX, centerY + arc_size * 3, mTextPaint);
                if(watchBatteryLevel > 75) {
                    variablePaint.setColor(resources.getColor(R.color.high_battery));
                }
                else if(watchBatteryLevel > 25 && watchBatteryLevel < 75) {
                    variablePaint.setColor(resources.getColor(R.color.mid_battery));
                }
                else if(watchBatteryLevel < 25) {
                    variablePaint.setColor(resources.getColor(R.color.low_battery));
                }
                canvas.drawArc(0, 0, arc_size, arc_size, arc_start, (float) (watchBatteryLevel * 1.8), true, variablePaint);
                if(phoneBatteryLevel > 75) {
                    variablePaint.setColor(resources.getColor(R.color.high_battery));
                }
                else if(phoneBatteryLevel > 25 && phoneBatteryLevel < 75) {
                    variablePaint.setColor(resources.getColor(R.color.mid_battery));
                }
                else if(phoneBatteryLevel < 25) {
                    variablePaint.setColor(resources.getColor(R.color.low_battery));
                }
                canvas.drawArc(0, 0, arc_size, arc_size, arc_start, ((float) (phoneBatteryLevel * 1.8) * -1), true, variablePaint);
                canvas.drawText(updateTime, centerX, arc_size / 20, mSmallText);
                variablePaint.setColor(resources.getColor(R.color.analog_background));
                canvas.drawCircle(arc_size/2, arc_size/2, 15, variablePaint);
            }
            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mHandPaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHandPaint);
            if (!mAmbient) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mSecondHandPaint);
                canvas.drawCircle(centerX, centerY, 5f, mSecondHandPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                mGoogleApiClient.connect();
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            }
            else {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    Wearable.NodeApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
                unregisterReceiver();
            }

            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WeatherWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            WeatherWatchFace.this.registerReceiver(batteryReceiver, intentFilter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WeatherWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
            WeatherWatchFace.this.unregisterReceiver(batteryReceiver);
        }


        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }


        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }


        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onConnected(Bundle bundle) {
            Wearable.NodeApi.addListener(mGoogleApiClient, this);
            Wearable.DataApi.addListener(mGoogleApiClient, this);
        }

        @Override
        public void onConnectionSuspended(int i) {
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for( DataEvent event : dataEventBuffer) {
                DataItem item = event.getDataItem();
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                temperature = dataMap.getString(KEY_WEATHER_TEMPERATURE);
                condition = dataMap.getString(KEY_WEATHER_CONDITION);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy");
                Date date = new Date(dataMap.getLong(KEY_WEATHER_TIMESTAMP));
                dateString = simpleDateFormat.format(date);
                simpleDateFormat = new SimpleDateFormat("kk:mm");
                updateTime = simpleDateFormat.format(date);
                phoneBatteryLevel = dataMap.getInt(KEY_BATTERY_LEVEL);
                String name = "weather_" + dataMap.getString(KEY_WEATHER_IMAGE);
                int id = resources.getIdentifier(name, "mipmap","com.davidbarron.weatherwatchface");
                Drawable b = resources.getDrawable(id);
                Bitmap mWeatherBitmap = ((BitmapDrawable) b).getBitmap();
                if (mScaledWeatherBitmap == null || mScaledWeatherBitmap.getWidth() != width || mScaledWeatherBitmap.getHeight() != height) {
                    mScaledWeatherBitmap = Bitmap.createScaledBitmap(mWeatherBitmap,width/3, height/3, true);
                }
            }
        }

        @Override
        public void onPeerConnected(Node node) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] intervals = {0,VIBRATE_TIME,VIBRATE_TIME,VIBRATE_TIME};
            vibrator.vibrate(intervals,-1);
        }

        @Override
        public void onPeerDisconnected(Node node) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_TIME);
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<WeatherWatchFace.Engine> mWeakReference;

        public EngineHandler(WeatherWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WeatherWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
