package com.harilee.locationalarm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.internal.Constants;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.Maps;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import io.grpc.okhttp.internal.Util;

import static android.content.ContentValues.TAG;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    final static int REQUEST_CODE = 1;
    Circle circle;
    public LocationManager lm;
    double current_location_latitude = 0;
    double current_location_longitutde = 0;
    private GeofencingClient geofencingClient;
    private static final String CHANNEL_ID = "1";
    private FirebaseFirestore ref;
    private ArrayList<LocationModel> locationModels = new ArrayList<>();
    private Dialog dialog;
    private MediaPlayer mediaPlayer;
    private AlarmManager am;
    private LocationManager mLocationManager;
    private ArrayList<String> latList = new ArrayList<>();
    private ArrayList<String> lngList = new ArrayList<>();
    private ArrayList<String> audio = new ArrayList<>();
    private LocationListener mLocationListener;
    private String radius;
    private Intent serviceIntent;
    private List<Geofence> geofenceList = new ArrayList<>();
    private PendingIntent geofencePendingIntent;
    private GoogleApiClient googleApiClient;
    private Marker currentLocationMarker;
    private MarkerOptions markerOptions;
    private boolean moveCamer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        dialog = new Dialog(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Location";
            String description = "Location arrived";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        geofencingClient = LocationServices.getGeofencingClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION
                            , Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}
                    , REQUEST_CODE);
        }
        Utility.showGifPopup(this, true, dialog);
        getLocationList();

        mediaPlayer = new MediaPlayer();
        if (getIntent().hasExtra("PLAY")) {
            String play = getIntent().getStringExtra("PLAY");
            if (play != null && play.equalsIgnoreCase("yes")) {
                setCounter();

            }
        }

    }

    private void playAudio() {


        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(Utility.getPreference(this, "AUDIO"));
            mediaPlayer.prepare(); // might take long! (for buffering, etc)
            mediaPlayer.start();

            removeGeofence();

        } catch (Exception e) {
            // TODO: handle exception
            Log.e(TAG, "onReceive: " + e.getLocalizedMessage());
        }


    }

    private void setCounter() {

        String username = Utility.getPreference(this, "USERNAME");
        String locId = Utility.getPreference(this, "LOCATION_ID");
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm ");
        Date date = new Date();
        System.out.println(formatter.format(date));
        String dateStr = formatter.format(date);
        Log.e(TAG, "setCounter: " + username);
        Log.e(TAG, "setCounter: " + locId);
        List<String> users = new ArrayList<>();
        ref = FirebaseFirestore.getInstance();
        ref.collection("locations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            if (documentSnapshot.exists()) {
                                String loc = String.valueOf(documentSnapshot.getData().get("lat")) + documentSnapshot.getData().get("lon");
                                Utility.setPreference(this, "AUDIO", String.valueOf(documentSnapshot.getData().get("audio")));
                                playAudio();
                                Log.e(TAG, "setCounter:  " + loc + " " + locId);
                                if (loc.equalsIgnoreCase(locId)) {
                                    int count = Integer.parseInt(String.valueOf(documentSnapshot.getData().get("count")));
                                    count++;
                                    int finalCount = count;
                                    ref.collection("locations").document(documentSnapshot.getId())
                                            .update("count", finalCount).addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, username + " visit count has been changed to " + finalCount, Toast.LENGTH_SHORT).show();
                                    });
                                    ref.collection("locations").document(documentSnapshot.getId())
                                            .update("users", FieldValue.arrayUnion(username)).addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, username + " visited a new location", Toast.LENGTH_SHORT).show();
                                    });

                                }
                            }
                        }
                    }
                });

        //adding location to visited counter
        Map<String, Object> dateObj = new HashMap<>();
        dateObj.put(username, dateStr);
        ref.collection("date").document(locId).set(dateObj, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Document added to database", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
            Toast.makeText(this, "Document could not be added database", Toast.LENGTH_SHORT).show();

        });


        ref.collection("counter").document(locId).get()
                .addOnCompleteListener(taskCounter -> {
                    if (taskCounter.isSuccessful()) {
                        if (taskCounter.getResult() != null && taskCounter.getResult().exists() && taskCounter.getResult().get(username) != null) {
                            Log.e(TAG, "setCounter: success");
                            int count = Integer.parseInt(String.valueOf(taskCounter.getResult().getData().get(username)));
                            count++;
                            Log.e(TAG, "setCounter: " + count);
                            ref.collection("counter").document(locId)
                                    .update(username, count);
                        } else {
                            Log.e(TAG, "setCounter: not successful");
                            Map<String, Object> counterData = new HashMap<>();
                            counterData.put(username, "1");
                            ref.collection("counter").document(locId).set(counterData, SetOptions.merge())
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Document added to database", Toast.LENGTH_SHORT).show();
                                    }).addOnFailureListener(e -> {
                                Toast.makeText(this, "Document could not be added database", Toast.LENGTH_SHORT).show();

                            });

                        }
                    } else {
                        Log.e(TAG, "setCounter: " + "unsuccessful");
                        Toast.makeText(this, "Document could not be found", Toast.LENGTH_SHORT).show();
                    }
                });


    }


    private void getLocationList() {

        ref = FirebaseFirestore.getInstance();
        ref.collection("radius").get().addOnCompleteListener(task1 -> {
            for (QueryDocumentSnapshot documentSnapshot1 : task1.getResult()) {
                radius = String.valueOf(documentSnapshot1.getData().get("value"));
                Log.e(TAG, "onCreate: " + radius);
                ref.collection("locations")
                        .get()
                        .addOnCompleteListener(task -> {
                            Utility.showGifPopup(MapsActivity.this, false, dialog);

                            if (task.isSuccessful() && task.getResult() != null) {
                                LocationModel locationModel;
                                locationModels.clear();

                                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                    if (documentSnapshot.exists()) {
                                        if (String.valueOf(documentSnapshot.getData().get("playable")).equalsIgnoreCase("true")) {
                                            locationModel = new LocationModel();
                                            Log.e("TAG", "onComplete: " + (documentSnapshot.getData().get("location_name")));
                                            locationModel.setCount(String.valueOf(documentSnapshot.getData().get("count")));
                                            locationModel.setLat(String.valueOf(documentSnapshot.getData().get("lat")));
                                            locationModel.setLon(String.valueOf(documentSnapshot.getData().get("lon")));
                                            locationModel.setLocationName(String.valueOf(documentSnapshot.getData().get("location_name")));
                                            locationModel.setAudioFile(String.valueOf(documentSnapshot.getData().get("audio")));
                                            geofenceList.add(new Geofence.Builder()
                                                    // Set the request ID of the geofence. This is a string to identify this
                                                    // geofence.
                                                    .setRequestId(locationModel.getLat() + locationModel.getLon())

                                                    .setCircularRegion(
                                                            Double.parseDouble(locationModel.getLat()),
                                                            Double.parseDouble(locationModel.getLon()),
                                                            (float) Double.parseDouble(radius)
                                                    )
                                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                                                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                                    .setNotificationResponsiveness(1000)
                                                    .build());

                                            circle = mMap.addCircle(new CircleOptions()
                                                    .center(new LatLng(Double.parseDouble(locationModel.getLat())
                                                            , Double.parseDouble(locationModel.getLon())))
                                                    .radius(Double.parseDouble(radius))
                                                    .fillColor(getResources().getColor(R.color.colorGrey)));
                                            locationModels.add(locationModel);
                                        }
                                    }
                                }
                            }
                        });

            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "onCreate: failure");

        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        current_location_longitutde = loc.getLongitude();
        current_location_latitude = loc.getLatitude();
        LatLng latLng = new LatLng(current_location_latitude, current_location_longitutde);

        mMap = googleMap;
        moveCamer = true;
        googleMap.setMyLocationEnabled(true);


    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(this, MyBroadCastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }


    private void goToCurrentLocation() {

        if (googleApiClient.isConnected()) {
            Log.d(TAG, "start location monitor");
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(2000)
                    .setFastestInterval(1000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new com.google.android.gms.location.LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (moveCamer) {
                            moveCamer = false;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude())
                                    , 18.0f));
                        }

                        if (currentLocationMarker != null) {
                            currentLocationMarker.remove();
                        }
                        markerOptions = new MarkerOptions();
                        markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
                        markerOptions.title("Current Location");
                        currentLocationMarker = mMap.addMarker(markerOptions);
                        Log.d(TAG, "Location Change Lat Lng " + location.getLatitude() + " " + location.getLongitude());
                    }
                });
            } catch (SecurityException e) {
                Log.d(TAG, e.getMessage());
            }  // zoom in
        }
    }

    public void setAlarm(View view) {
        addGeofence();

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (this.googleApiClient != null) {
            this.googleApiClient.connect();
        }
    }

    private void addGeofence() {
        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, aVoid -> {
                    Toast.makeText(getApplicationContext(), "Geofencing has started", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(getApplicationContext(), "Geofencing failed", Toast.LENGTH_SHORT).show();

                });
    }

    private void removeGeofence() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(this, aVoid -> {
                    Toast.makeText(getApplicationContext(), "Geofencing has been removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(this, e -> {
                    Toast.makeText(getApplicationContext(), "Geofencing could not be removed", Toast.LENGTH_SHORT).show();
                });
        Toast.makeText(getApplicationContext(), "Audio canceled", Toast.LENGTH_SHORT).show();
    }

    public void cancelAlarm(View view) {

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        removeGeofence();

    }

    public void getMyLocation(View view) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        current_location_longitutde = loc.getLongitude();
        current_location_latitude = loc.getLatitude();
        LatLng latLng = new LatLng(current_location_latitude, current_location_longitutde);
    }

    public void getReport(View view) {

        startActivity(new Intent(this, GetReport.class));
    }

    public void logOut(View view) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        startActivity(new Intent(this, LoginPage.class));
    }

    public void refreshData(View view) {
        Utility.showGifPopup(MapsActivity.this, true, dialog);

        getLocationList();
        setAlarm(view);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        goToCurrentLocation();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
 /* ArrayList<String> lat = new ArrayList<>();
        ArrayList<String> lng = new ArrayList<>();
        ArrayList<String> audio = new ArrayList<>();

        for (LocationModel locationModel : locationModels) {
            // Add a circle of radius 50 meter
            lat.add(locationModel.getLat());
            lng.add(locationModel.getLon());
            audio.add((locationModel.getAudioFile()));
            Log.e("TAG", "setAlarm:1 " + locationModel.getLat());


        }*/
        /*serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.putStringArrayListExtra("LAT", lat);
        serviceIntent.putStringArrayListExtra("LON", lng);
        serviceIntent.putStringArrayListExtra("AUDIO", audio);
        serviceIntent.putExtra("RAD", (radius));*/
// startService(serviceIntent);
