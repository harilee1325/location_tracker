package com.harilee.locationalarm;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetReport extends AppCompatActivity {

    private ReportAdapter adapter;
    private ArrayList<LocationModel> locationModelLocationList = new ArrayList<>();
    private String name = "";
    private String user = "";
    private String date = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_layout);

        RecyclerView reportList = findViewById(R.id.report_list);
        adapter = new ReportAdapter(locationModelLocationList);
        reportList.setAdapter(adapter);
        getData();

    }

    private void getData() {
        ProgressDialog dialog = ProgressDialog.show(this, "",
                "Generating Report. Please wait...", true);
        FirebaseFirestore ref = FirebaseFirestore.getInstance();
        ref.collection("locations")
                .get()
                .addOnCompleteListener(task -> {
                    dialog.cancel();
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
                                locationModelLocationList.add(locationModel);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Reports could not be generated", Toast.LENGTH_SHORT).show();
                    }
                }).
                addOnFailureListener(e -> {
                    dialog.cancel();
                    Toast.makeText(this, "Reports could not be generated", Toast.LENGTH_SHORT).show();

                });


    }


    private class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
        private ArrayList<LocationModel> locationModelLocationList;

        public ReportAdapter(ArrayList<LocationModel> locationModelLocationList) {

            this.locationModelLocationList = locationModelLocationList;
        }

        @NonNull
        @Override
        public ReportAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.report_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportAdapter.ViewHolder holder, int position) {

            holder.count.setText(locationModelLocationList.get(position).getCount());
            holder.locationName.setText(locationModelLocationList.get(position).getLocationName());
            holder.locationLat.setText("Latitude : " + locationModelLocationList.get(position).getLat());
            holder.locationLon.setText("Longitude : " + locationModelLocationList.get(position).getLon());
            holder.cardReport.setOnClickListener(v -> {
                getReport(locationModelLocationList.get(position));
            });
        }

        @Override
        public int getItemCount() {
            return locationModelLocationList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView locationName, locationLat, locationLon, count;
            public CardView cardReport;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardReport = itemView.findViewById(R.id.card_report);
                locationLat = itemView.findViewById(R.id.location_lat);
                locationLon = itemView.findViewById(R.id.location_lon);
                locationName = itemView.findViewById(R.id.location_name);
                count = itemView.findViewById(R.id.counter);
            }
        }
    }

    private void getReport(LocationModel locationModel) {

        TextView locationName, locationLat, locationLon, count, userName, userCount, dateStr;
        CardView cardReport;
        LinearLayout userLayout;
        ProgressBar userProgress;
        name = "";
        user = "";
        date = "";
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.SheetDialog);
        dialog.setContentView(R.layout.report_data);
        cardReport = dialog.findViewById(R.id.card_report);
        locationLat = dialog.findViewById(R.id.location_lat);
        locationLon = dialog.findViewById(R.id.location_lon);
        locationName = dialog.findViewById(R.id.location_name);
        count = dialog.findViewById(R.id.counter);
        userName = dialog.findViewById(R.id.user_name);
        userLayout = dialog.findViewById(R.id.user_layout);
        userProgress = dialog.findViewById(R.id.user_count_progress);
        userCount = dialog.findViewById(R.id.user_count);
        dateStr = dialog.findViewById(R.id.date);

        dialog.show();

        locationName.setText(locationModel.getLocationName());
        locationLat.setText(locationModel.getLat());
        locationLon.setText(locationModel.getLon());
        count.setText(locationModel.getCount());

        FirebaseFirestore ref = FirebaseFirestore.getInstance();
        DocumentReference codesRef = ref.collection("counter").document(locationModel.getLat() + locationModel.getLon());

        codesRef.get()
                .addOnCompleteListener(task -> {
                    userProgress.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, Object> map = task.getResult().getData();
                        List<CounterModel> counterModelList = new ArrayList<>();
                        CounterModel counterModel;
                        userLayout.setVisibility(View.VISIBLE);

                        if (map != null) {
                            for (Map.Entry<String, Object> entry : map.entrySet()) {
                                Log.d("TAG", entry.getKey());
                                counterModel = new CounterModel();
                                counterModel.setName(entry.getKey());
                                counterModel.setCount(String.valueOf(entry.getValue()));
                                counterModelList.add(counterModel);
                            }
                            for (CounterModel counterModel1 : counterModelList) {
                                user = user + counterModel1.getCount() + "\n";

                            }
                            userCount.setText(user);
                        }
                    }

                }).addOnFailureListener(e -> {
            userProgress.setVisibility(View.GONE);
            Toast.makeText(this, "No data to show", Toast.LENGTH_SHORT).show();
        });

        ref.collection("date").document(locationModel.getLat() + locationModel.getLon())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, Object> map = task.getResult().getData();
                        CounterModel counterModel;
                        List<CounterModel> counterModelList = new ArrayList<>();
                        if (map != null) {
                            for (Map.Entry<String, Object> entry : map.entrySet()) {
                                counterModel = new CounterModel();
                                counterModel.setName(entry.getKey());
                                counterModel.setDate(String.valueOf(entry.getValue()));
                                counterModelList.add(counterModel);

                            }
                            for (CounterModel counterModel1 : counterModelList) {
                                name = name + counterModel1.getName() + "\n";
                                date = date + counterModel1.getDate() + "\n";
                            }
                            userName.setText(name);
                            dateStr.setText(date);
                        }
                    }
                }).addOnFailureListener(e -> {

        });
        dialog.setCancelable(true);
    }
}
