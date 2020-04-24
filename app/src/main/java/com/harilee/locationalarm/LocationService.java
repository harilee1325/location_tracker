package com.harilee.locationalarm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import static com.harilee.locationalarm.App.CHANNEL_ID;


public class LocationService extends Service {

    private static final String TAG = "Service";
    private String audioFile;
    private MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();
    private String locId;
    private String dateStr;
    private String username;


    public class LocalBinder extends Binder {
        LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate: ");
        // Toast.makeText(getApplicationContext(), "onCreate", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Looking for range", Toast.LENGTH_SHORT).show();
        locId = intent.getStringExtra("LOCID");
        username = Utility.getPreference(this, "USERNAME");
        mediaPlayer = new MediaPlayer();


        Intent notificationIntent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Alarm")
                .setContentText("You reached the location.")
                .setSmallIcon(R.drawable.ic_add_location_black_24dp)
                .setContentIntent(pendingIntent)
                .build();

        new DownloadFilesTask().execute();

        startForeground(1, notification);
        //do heavy work on a background thread
        //stopSelf();
        return START_NOT_STICKY;
    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadFilesTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            getLocation();
            return null;
        }


    }

    public void stopPlaying() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void getLocation() {
        FirebaseApp.initializeApp(this);
        FirebaseFirestore ref = FirebaseFirestore.getInstance();
        ref.collection("locations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            if (documentSnapshot.exists()) {
                                Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
                                try {
                                    if (locId.equalsIgnoreCase(String.valueOf(documentSnapshot.getData().get("lat")) + documentSnapshot.getData().get("lon"))) {
                                        audioFile = String.valueOf(documentSnapshot.getData().get("audio"));
                                        Log.e(TAG, "getLocation: " + audioFile);
                                        Toast.makeText(this, audioFile, Toast.LENGTH_SHORT).show();
                                        mediaPlayer.setAudioAttributes(new AudioAttributes
                                                .Builder()
                                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                .build());
                                        mediaPlayer.setDataSource(audioFile);
                                        mediaPlayer.prepare(); // might take long! (for buffering, etc)
                                        mediaPlayer.start();
                                        //sendNotification(String.valueOf(documentSnapshot.getData().get("location_name")), this);
                                        getCounter();

                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();

                                    Log.e(TAG, "getLocation: " + e.getLocalizedMessage());
                                }

                            }
                        }
                    }
                });
    }

    private void getCounter() {
        Log.e(TAG, "getCounter: ");
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm ");
        Date date = new Date();
        System.out.println(formatter.format(date));
        dateStr = formatter.format(date);
        FirebaseFirestore ref = FirebaseFirestore.getInstance();
        ref.collection("locations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            if (documentSnapshot.exists()) {
                                String loc = String.valueOf(documentSnapshot.getData().get("lat")) + documentSnapshot.getData().get("lon");
                                if (loc.equalsIgnoreCase(locId)) {
                                    if (documentSnapshot.getData().get("count") != null) {
                                        Utility.setPreference(this, "LOC_NAME", String.valueOf(documentSnapshot.getData().get("location_name")));
                                        getData();
                                        int count = Integer.parseInt(String.valueOf(documentSnapshot.getData().get("count")));
                                        count++;
                                        int finalCount = count;
                                        Map<String, Object> countMap = new HashMap<>();
                                        countMap.put("count", finalCount);
                                        ref.collection("locations").document(documentSnapshot.getId())
                                                .update(countMap).addOnSuccessListener(aVoid -> {
                                            // Toast.makeText(this, "Document added database", Toast.LENGTH_SHORT).show();

                                        }).addOnFailureListener(e -> {
                                            Log.e(TAG, "getCounter:1 " + e.getLocalizedMessage());
                                            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                        ;
                                        Map<String, Object> usersMap = new HashMap<>();
                                        usersMap.put("users", FieldValue.arrayUnion(username));
                                        ref.collection("locations").document(documentSnapshot.getId())
                                                .update(usersMap).addOnSuccessListener(aVoid -> {
                                            // Toast.makeText(this, "Documents added database", Toast.LENGTH_SHORT).show();
                                        }).addOnFailureListener(e -> {
                                            Log.e(TAG, "getCounter:2 " + e.getLocalizedMessage());
                                            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                        ;
                                    } else {
                                        Utility.setPreference(this, "AUDIO", String.valueOf(documentSnapshot.getData().get("audio")));
                                        Utility.setPreference(this, "LOC_NAME", String.valueOf(documentSnapshot.getData().get("location_name")));
                                        getData();
                                        Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
                                        //setUserDate();

                                        Map<String, Object> countMap = new HashMap<>();
                                        countMap.put("count", 1);
                                        ref.collection("locations").document(documentSnapshot.getId())
                                                .set(countMap, SetOptions.merge()).addOnSuccessListener(aVoid -> {
                                            // Toast.makeText(this, "Documents be added database", Toast.LENGTH_SHORT).show();

                                        }).addOnFailureListener(e -> {
                                            Log.e(TAG, "getCounter: 3" + e.getLocalizedMessage());
                                            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                        ;
                                        Map<String, Object> usersMap = new HashMap<>();
                                        usersMap.put("users", FieldValue.arrayUnion(username));
                                        ref.collection("locations").document(documentSnapshot.getId())
                                                .set(usersMap, SetOptions.merge()).addOnSuccessListener(aVoid -> {
                                            // Toast.makeText(this, "Document  added database", Toast.LENGTH_SHORT).show();

                                        }).addOnFailureListener(e -> {
                                            Log.e(TAG, "getCounter: 4" + e.getLocalizedMessage());
                                            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }


                            }
                        }
                    }
                }).addOnFailureListener(e -> {
            Log.e(TAG, "getCounter: 5" + e.getLocalizedMessage());
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
        //adding location to visited counter
        Map<String, Object> dateObj = new HashMap<>();
        dateObj.put(username, dateStr);
        ref.collection("date").document(locId).set(dateObj, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    //  Toast.makeText(this, "Document added to database", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
            // Toast.makeText(this, "Document could not be added database", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e -> {
            Log.e(TAG, "getCounter: 6" + e.getLocalizedMessage());
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
        ref.collection("counter").document(locId).get()
                .addOnCompleteListener(taskCounter -> {
                    if (taskCounter.isSuccessful()) {
                        if (taskCounter.getResult() != null && taskCounter.getResult().exists() && taskCounter.getResult().get(username) != null) {
                            Log.e(TAG, "setCounter: success");
                            int count = Integer.parseInt(String.valueOf(taskCounter.getResult().getData().get(username)));
                            count++;
                            Log.e(TAG, "setCounter: " + count);
                            Map<String, Object> user = new HashMap<>();
                            user.put(username, count);
                            ref.collection("counter").document(locId)
                                    .set(user, SetOptions.merge());
                        } else {
                            Log.e(TAG, "setCounter: not successful");
                            Map<String, Object> counterData = new HashMap<>();
                            counterData.put(username, "1");
                            ref.collection("counter").document(locId).set(counterData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        // Toast.makeText(this, "Document added to database", Toast.LENGTH_SHORT).show();
                                    }).addOnFailureListener(e -> {
                                //  Toast.makeText(this, "Document could not be added database", Toast.LENGTH_SHORT).show();

                            }).addOnFailureListener(e -> {
                                Log.e(TAG, "getCounter:7 " + e.getLocalizedMessage());
                                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            });


                        }
                    } else {
                        Log.e(TAG, "setCounter: " + "unsuccessful");
                        // Toast.makeText(this, "Document could not be found", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
            Log.e(TAG, "getCounter: 8" + e.getLocalizedMessage());
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
        ;

    }

    private void getData() {

        String locName = Utility.getPreference(this, "LOC_NAME");
        Log.e(TAG, "setUserDate: " + locName);
        Map<String, Object> locationData = new HashMap<>();
        locationData.put(locName, dateStr);
        FirebaseFirestore ref = FirebaseFirestore.getInstance();
        ref.collection("user_location_date").document(username).set(locationData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                }).addOnFailureListener(e -> {

        });
        ref.collection("user_location_count").document(username).get()
                .addOnCompleteListener(taskCounter -> {
                    if (taskCounter.isSuccessful()) {
                        if (taskCounter.getResult() != null && taskCounter.getResult().exists() && taskCounter.getResult().get(locName) != null) {
                            Log.e(TAG, "setCounter: success");
                            int count = Integer.parseInt(String.valueOf(taskCounter.getResult().getData().get(locName)));
                            count++;
                            Log.e(TAG, "setCounter: " + count);
                            Map<String, Object> user = new HashMap<>();
                            user.put(locName, count);
                            ref.collection("user_location_count").document(username)
                                    .set(user, SetOptions.merge());

                        } else {
                            Log.e(TAG, "setCounter: not successful");
                            Map<String, Object> counterData = new HashMap<>();
                            counterData.put(locName, "1");
                            ref.collection("user_location_count").document(username).set(counterData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                    }).addOnFailureListener(e -> {

                            });
                        }
                    } else {
                        Log.e(TAG, "setCounter: " + "unsuccessful");
                        // Toast.makeText(this, "Document could not be found", Toast.LENGTH_SHORT).show();
                    }
                });


    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "Location services are down", Toast.LENGTH_SHORT).show();
    }


}
