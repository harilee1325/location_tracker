package com.harilee.locationalarm;

import android.Manifest;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


public class LocationService extends Service {


    final static int REQUEST_CODE = 1;
    private static final String TAG = "Service";
    private static final String CHANNEL_ID = "1";
    public LocationManager lm;
    double alarm_location_latitude = 0;
    double alarm_location_longitutde = 0;
    double current_location_latitude = 0;
    double current_location_longitutde = 0;
    Circle circle;
    final int delay = 5000; //milliseconds
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private boolean state = false;
    private String lat, lon, rad;
    private ArrayList<String> latList = new ArrayList<>();
    private ArrayList<String> lngList = new ArrayList<>();
    private ArrayList<String> audio = new ArrayList<>();
    private String audioFile;


    private void setAlarm() {
        state = false;

        try {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_add_alarm_black_24dp)
                    .setContentTitle("Location Reached")
                    .setContentText("You have reached one of the locations")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    // Set the intent that will fire when the user taps the notification
//                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(1, builder.build());


            Uri notification = Uri.parse(audioFile);
//            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            final MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(getApplicationContext(), notification);
            mediaPlayer.prepare();
            mediaPlayer.start();

            /*Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.stop();
                }
            }, 5000);*/
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "setAlarm: " + e.getLocalizedMessage());
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }

        // Toast.makeText(this, "setting alarm new", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "setAlarm: ");
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent alarmIntent = new Intent(this, MyBroadCastReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                    + (100), 3000, pendingIntent);
        }
    }

    public boolean IsInCircle() {
        //Toast.makeText(this, " also radius" + rad, Toast.LENGTH_SHORT).show();

        float[] distance = {0, 0, 0};
        for (int i = 0; i < latList.size(); i++) {
            Log.e(TAG, "IsInCircle: " + latList.get(i));
            Location.distanceBetween(current_location_latitude, current_location_longitutde,
                    Double.parseDouble(latList.get(i)), Double.parseDouble(lngList.get(i)), distance);
            if (!(distance[0] > Double.parseDouble(rad))) {
                audioFile = audio.get(i);
                return true;
            }
        }
        return false;
    }


    private void getMyLocation() {
        Log.e(TAG, "getMyLocation: ");
        mLocationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                current_location_latitude = location.getLatitude();
                current_location_longitutde = location.getLongitude();

                // Toast.makeText(getBaseContext(), current_location_latitude + "-" + current_location_longitutde, Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnownLocation != null) {
                    current_location_latitude = lastKnownLocation.getLatitude();
                    current_location_longitutde = lastKnownLocation.getLongitude();
                }

            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 500, 10, mLocationListener);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: ");

        Toast.makeText(getApplicationContext(), "onCreate", Toast.LENGTH_SHORT).show();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Looking for range", Toast.LENGTH_SHORT).show();
        latList.addAll(intent.getStringArrayListExtra("LAT"));
        lngList.addAll(intent.getStringArrayListExtra("LON"));
        audio.addAll(intent.getStringArrayListExtra("AUDIO"));
        rad = intent.getStringExtra("RAD");

        state = true;
        final Handler handler = new Handler();
        final int delay = 1000; //milliseconds
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    if (state) {
                        getMyLocation();
                        if (IsInCircle()) {
                            setAlarm();
                            state = false;
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "onHandleIntent: " + e.getLocalizedMessage());
                }
                handler.postDelayed(this, delay);
            }
        }, delay);

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        Toast.makeText(getApplicationContext(), "Location services are down", Toast.LENGTH_SHORT).show();
        mLocationManager.removeUpdates(mLocationListener);

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
//        Intent intent = new Intent("com.android.ServiceStopped");
//        intent.putExtra("LAT", lat);
//        intent.putExtra("LON", lon);
//        Log.e("TAG", "setAlarm: "+circle.getRadius() );
//        intent.putExtra("RAD", String.valueOf(rad));
//        sendBroadcast(intent);
    }
}
