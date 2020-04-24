package com.harilee.locationalarm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;


import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.harilee.locationalarm.App.CHANNEL_ID;


public class MyBroadCastReceiver extends BroadcastReceiver {
    private String audioFile;

    @Override
    public void onReceive(Context context, Intent intent) {


        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            String locId = triggeringGeofences.get(0).getRequestId();
            Utility.setPreference(context, "LOCATION_ID", locId);

            // Get the transition details as a String.

            //sendNotification(locId, context);

            // Send notification and log the transition details.
            Intent serviceIntent = new Intent(context, LocationService.class);
            serviceIntent.putExtra("LOCID", locId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            }else{
                context.startService(serviceIntent);
            }
        } else {
            // Log the error.
            Log.e(TAG, "Error");
        }
    }

    private void sendNotification(String locId, Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_add_alarm_black_24dp)
                .setContentTitle("Location Reached")
                .setContentText(" you reached " + locId)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, builder.build());
    }


    private void getLocation(Context context, String locId) {
        FirebaseApp.initializeApp(context);
        FirebaseFirestore ref = FirebaseFirestore.getInstance();
        ref.collection("locations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            if (documentSnapshot.exists()) {
                                Toast.makeText(context, "sucess", Toast.LENGTH_SHORT).show();
                                if (locId.equalsIgnoreCase(String.valueOf(documentSnapshot.getData().get("lat")) + documentSnapshot.getData().get("lon"))) {
                                    audioFile = String.valueOf(documentSnapshot.getData().get("audio"));
                                    Log.e(TAG, "getLocation: " + audioFile);
                                    Intent serviceIntent = new Intent(context, LocationService.class);
                                    serviceIntent.putExtra("audio", audioFile);
                                    context.startService(serviceIntent);

                                  /*  MediaPlayer mediaPlayer = new MediaPlayer();
                                    mediaPlayer.setAudioAttributes(new AudioAttributes
                                            .Builder()
                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                            .build());
                                    mediaPlayer.setDataSource(audioFile);
                                    mediaPlayer.prepare(); // might take long! (for buffering, etc)
                                    mediaPlayer.start();*/
                                } else {
                                    Toast.makeText(context, "No such locations found", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }
                    }
                });
    }

    private String getGeofenceTransitionDetails(MyBroadCastReceiver myBroadCastReceiver
            , int geofenceTransition, List<Geofence> triggeringGeofences) {

        return triggeringGeofences.get(0).getRequestId();
    }


}
