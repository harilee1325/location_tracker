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
    private boolean isPermissionGranted;
    private boolean mBounded;
    private LocationService locationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        dialog = new Dialog(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
      /*  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        }*/
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        geofencingClient = LocationServices.getGeofencingClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_CODE);
            Log.e(TAG, "onCreate: permission denied");
            isPermissionGranted = false;

        } else {
            Log.e(TAG, "onCreate: permission granded");
            isPermissionGranted = true;
            getLocationList();

        }
        Utility.showGifPopup(this, true, dialog);


    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isPermissionGranted = true;
                    getMyLocation();


                } else {
                    isPermissionGranted = false;
                    Toast.makeText(this, "please grant permission to continue", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
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
                                                            (float) Double.parseDouble(String.valueOf(documentSnapshot.getData().get("radius")))
                                                    )
                                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                                                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                                    .setNotificationResponsiveness(1000)
                                                    .build());

                                            circle = mMap.addCircle(new CircleOptions()
                                                    .center(new LatLng(Double.parseDouble(locationModel.getLat())
                                                            , Double.parseDouble(locationModel.getLon())))
                                                    .radius(Double.parseDouble(String.valueOf(documentSnapshot.getData().get("radius"))))
                                                    .strokeColor(getResources().getColor(R.color.lignt_primary))
                                                    .fillColor(getResources().getColor(R.color.lignt_primary)));
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
        mMap = googleMap;
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getMyLocation();
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
        Toast.makeText(getApplicationContext(), "starting broadcast", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MyBroadCastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, location -> {
                    if (moveCamer) {
                        moveCamer = false;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude())
                                , 18.0f));
                    }
                    if (currentLocationMarker != null) {
                        currentLocationMarker.remove();
                    }

                    geofencingClient = LocationServices.getGeofencingClient(this);

                    markerOptions = new MarkerOptions();
                    markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
                    markerOptions.title("Current Location");
                    currentLocationMarker = mMap.addMarker(markerOptions);
                    Log.d(TAG, "Location Change Lat Lng " + location.getLatitude() + " " + location.getLongitude());
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
        Intent mIntent = new Intent(this, LocationService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        /*Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);*/
        if (googleApiClient != null)
            googleApiClient.connect();
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

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getApplicationContext(), "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            locationService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(getApplicationContext(), "Service is connected", Toast.LENGTH_SHORT).show();
            mBounded = true;
            LocationService.LocalBinder mLocalBinder = (LocationService.LocalBinder) service;
            MapsActivity.this.locationService = mLocalBinder.getService();
        }
    };

    public void cancelAlarm(View view) {
        if (mBounded)
            locationService.stopPlaying();
        Intent intent = new Intent(this, LocationService.class);
        stopService(intent);
        removeGeofence();

    }

    public void getMyLocation() {

        //mMap.setBuildingsEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        } else {
            moveCamer = true;
            mMap.setMyLocationEnabled(true);
            if (isPermissionGranted)
                getLocationList();

            goToCurrentLocation();

        }
    }

    public void getReport(View view) {


        if (Utility.getPreference(this, "USERNAME").equalsIgnoreCase("joy") ||
                Utility.getPreference(this, "USERNAME").equalsIgnoreCase("hari"))
            startActivity(new Intent(this, GetReport.class));
        else
            Toast.makeText(this, "Sorry you dont have access to view report", Toast.LENGTH_SHORT).show();
    }

    public void logOut(View view) {

        startActivity(new Intent(this, ProfilePage.class));
    }

    public void refreshData(View view) {
        Utility.showGifPopup(MapsActivity.this, true, dialog);
        getLocationList();
        setAlarm(view);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (isPermissionGranted)
            goToCurrentLocation();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
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
