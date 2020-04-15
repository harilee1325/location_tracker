package com.harilee.locationalarm;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.Maps;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.lang.reflect.Array;
import java.util.ArrayList;

import io.grpc.okhttp.internal.Util;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    final static int REQUEST_CODE = 1;
    Circle circle;
    LatLng location;
    public LocationManager lm;
    double alarm_location_latitude = 0;
    double alarm_location_longitutde = 0;
    double current_location_latitude = 0;
    double current_location_longitutde = 0;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private static final String CHANNEL_ID = "1";
    private FirebaseFirestore ref;
    private ArrayList<LocationModel> locationModels = new ArrayList<>();
    private Dialog dialog;


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
        Utility.showGifPopup(this, true, dialog);
        getLocationList();


    }

    private void getLocationList() {

        ref = FirebaseFirestore.getInstance();
        ref.collection("locations")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Utility.showGifPopup(MapsActivity.this, false, dialog);

                        if (task.isSuccessful() && task.getResult() != null) {
                            LocationModel locationModel;
                            for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                if (documentSnapshot.exists()) {
                                    locationModel = new LocationModel();
                                    Log.e("TAG", "onComplete: " + (documentSnapshot.getData().get("location_name")));
                                    locationModel.setCount(String.valueOf(documentSnapshot.getData().get("count")));
                                    locationModel.setLat(String.valueOf(documentSnapshot.getData().get("lat")));
                                    locationModel.setLon(String.valueOf(documentSnapshot.getData().get("lon")));
                                    locationModel.setLocationName(String.valueOf(documentSnapshot.getData().get("location_name")));
                                    locationModel.setAudioFile(String.valueOf(documentSnapshot.getData().get("audio")));
                                    locationModels.add(locationModel);
                                }
                            }
                        }
                    }
                });
    }

    // update the current location of user
    public void getMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        } else {
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            current_location_latitude = loc.getLatitude();
            current_location_longitutde = loc.getLongitude();
            Log.e("TAG", "getMyLocation: " + current_location_latitude);
            Log.e("TAG", "getMyLocation: " + current_location_longitutde);
            location = new LatLng(current_location_latitude, current_location_longitutde);
            goToCurrentLocation(location);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getMyLocation();

    }

    private void goToCurrentLocation(LatLng location) {
        mMap.addMarker(new MarkerOptions().position(location).title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(20.0f));  // zoom in
    }

    public void setAlarm(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
        } else {
            try {
                Toast.makeText(getApplicationContext(), "setting the alarm", Toast.LENGTH_SHORT).show();
                Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    current_location_latitude = loc.getLatitude();
                    current_location_longitutde = loc.getLongitude();
                    ArrayList<String> lat = new ArrayList<>();
                    ArrayList<String> lng = new ArrayList<>();
                    ArrayList<String> audio = new ArrayList<>();
                    for (LocationModel locationModel : locationModels) {
                        // Add a circle of radius 50 meter
                        lat.add(locationModel.getLat());
                        lng.add(locationModel.getLon());
                        audio.add((locationModel.getAudioFile()));
                        Log.e("TAG", "setAlarm:1 " + locationModel.getLat());


                        circle = mMap.addCircle(new CircleOptions()
                                .center(new LatLng(Double.parseDouble(locationModel.getLat())
                                        , Double.parseDouble(locationModel.getLon())))
                                .radius(50).fillColor(Color.GREEN));
                    }


                    Intent serviceIntent = new Intent(this, LocationService.class);
                    serviceIntent.putStringArrayListExtra("LAT", lat);
                    serviceIntent.putStringArrayListExtra("LON", lng);
                    serviceIntent.putStringArrayListExtra("AUDIO", audio);
                    serviceIntent.putExtra("RAD", ("50.0"));
                    startService(serviceIntent);


                }
            } catch (Exception e) {
                Log.e("TAG", "setAlarm: " + e.getLocalizedMessage());
            }
        }

    }

    public void cancelAlarm(View view) {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
//        alarmManager.cancel(pendingIntent);
        Toast.makeText(getApplicationContext(), "Alarm canceled", Toast.LENGTH_SHORT).show();
    }

    public void getMyLocation(View view) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        current_location_latitude = loc.getLatitude();
        current_location_longitutde = loc.getLongitude();
        location = new LatLng(current_location_latitude, current_location_longitutde);
        goToCurrentLocation(location);
    }
}
