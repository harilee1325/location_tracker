package com.harilee.locationalarm;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;


public class GetReport extends AppCompatActivity {

    private ReportAdapter adapter;
    private ArrayList<LocationModel> locationModelLocationList = new ArrayList<>();
    private String name = "";
    private String user = "";
    private String date = "";
    private ArrayList<CounterModel> counterModelList = new ArrayList<>();
    private ArrayList<CounterModel> dateModelList = new ArrayList<>();
    private ImageView sendMail;
    private FirebaseFirestore ref;
    private ArrayList<String> userNameStr = new ArrayList<>();
    private String col1 = "", col2 = "", col3 = "";
    private ArrayList<Integer> visitingCount = new ArrayList<>();
    private ArrayList<ArrayList<UserModel>> userModelListList = new ArrayList<>();
    private ArrayList<ArrayList<UserModel>> userModelListListDate = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_layout);
        System.setProperty("org.apache.poi.javax.xml.stream.XMLOutputFactory", "com.fasterxml.aalto.stax.OutputFactoryImpl");
        System.setProperty("org.apache.poi.javax.xml.stream.XMLEventFactory", "com.fasterxml.aalto.stax.EventFactoryImpl");
        RecyclerView reportList = findViewById(R.id.report_list);
        sendMail = findViewById(R.id.send_excel_to_mail);
        sendMail.setVisibility(View.GONE);
        sendMail.setOnClickListener(v -> {

            sendExcelToMail();
        });
        adapter = new ReportAdapter(locationModelLocationList);
        reportList.setAdapter(adapter);
        getData();
        sendMailMethod();


    }

    private void sendMailMethod() {

        ref = FirebaseFirestore.getInstance();
        ref.collection("user_location_count").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        userModelListList.clear();
                        for (QueryDocumentSnapshot snapshot : task.getResult()) {
                            Map<String, Object> map = snapshot.getData();
                            UserModel userModel;
                            userNameStr.add(snapshot.getId());
                            ArrayList<UserModel> userModelArrayList = new ArrayList<>();
                            for (Map.Entry<String, Object> mapObj : map.entrySet()) {
                                userModel = new UserModel();
                                userModel.setName(mapObj.getKey());
                                userModel.setCounter(String.valueOf(mapObj.getValue()));
                                userModelArrayList.add(userModel);
                            }
                            Log.e("TAG", "sendMailMethod:11   " + userModelArrayList.size());
                            userModelListList.add(userModelArrayList);

                        }

                        if (userModelListList.size() > 0)
                            sendMail.setVisibility(View.VISIBLE);


                    }
                }).addOnFailureListener(e -> {
            Log.e("TAG", "sendMailMethod: " + e.getLocalizedMessage());
        });
        ref.collection("user_location_date").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        userModelListListDate.clear();
                        for (QueryDocumentSnapshot snapshot : task.getResult()) {
                            Map<String, Object> map = snapshot.getData();
                            UserModel userModel;
                            ArrayList<UserModel> userModelListDate = new ArrayList<>();
                            for (Map.Entry<String, Object> mapObj : map.entrySet()) {
                                userModel = new UserModel();
                                userModel.setDate(String.valueOf(mapObj.getValue()));
                                userModelListDate.add(userModel);
                            }
                            userModelListListDate.add(userModelListDate);

                        }
                        if (userModelListListDate.size() > 0)
                            sendMail.setVisibility(View.VISIBLE);

                    }
                }).addOnFailureListener(e -> {
            Log.e("TAG", "sendMailMethod: " + e.getLocalizedMessage());

        });

    }

    private void getData() {
        ProgressDialog dialog = ProgressDialog.show(this, "",
                "Generating Report. Please wait...", true);
        ref = FirebaseFirestore.getInstance();
        ref.collection("locations")
                .get()
                .addOnCompleteListener(task -> {
                    dialog.cancel();
                    if (task.isSuccessful() && task.getResult() != null) {
                        LocationModel locationModel;
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            if (documentSnapshot.exists()) {
                                locationModel = new LocationModel();
                                //    Log.e("TAG", "onComplete: " + (documentSnapshot.getData().get("location_name")));
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

    public void logOut(View view) {

        startActivity(new Intent(this, ProfilePage.class));
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
            if (!locationModelLocationList.get(position).getCount().equalsIgnoreCase("null"))
                holder.count.setText(locationModelLocationList.get(position).getCount());
            else
                holder.count.setText("0");


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
        if (!locationModel.getCount().equalsIgnoreCase("null"))
            count.setText(locationModel.getCount());
        else
            count.setText("0");


        FirebaseFirestore ref = FirebaseFirestore.getInstance();
        DocumentReference codesRef = ref.collection("counter").document(locationModel.getLat() + locationModel.getLon());
        codesRef.get()
                .addOnCompleteListener(task -> {
                    userProgress.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, Object> map = task.getResult().getData();
                        counterModelList = new ArrayList<>();
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
                        dateModelList = new ArrayList<>();
                        if (map != null) {
                            for (Map.Entry<String, Object> entry : map.entrySet()) {
                                counterModel = new CounterModel();
                                counterModel.setName(entry.getKey());
                                counterModel.setDate(String.valueOf(entry.getValue()));
                                dateModelList.add(counterModel);

                            }
                            for (CounterModel counterModel1 : dateModelList) {
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

    private void sendExcelToMail() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

            return;
        }

        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Users"); //Creating a sheet

        Row rowHeader = sheet.createRow(0);
        rowHeader.createCell(0).setCellValue("ID");
        rowHeader.createCell(1).setCellValue("Fence area ");
        rowHeader.createCell(2).setCellValue("No of times played ");
        rowHeader.createCell(3).setCellValue("Date/time");

        int k = 0;
        for (int i = 0; i < userModelListList.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(userNameStr.get(i));
            col1 = col2 = col3 = "";
            Log.e("TAG", "sendExcelToMail: 11" + userModelListList.get(i).size());
            for (int j = 0; j < userModelListList.get(i).size(); j++) {
                col1 = col1 + userModelListList.get(i).get(j).getName() + "\n";
                col2 = col2 + userModelListList.get(i).get(j).getCounter() + "\n";
                col3 = col3 + userModelListListDate.get(i).get(j).getDate() + "\n";
            }
            row.createCell(1).setCellValue(col1);
            row.createCell(2).setCellValue(col2);
            row.createCell(3).setCellValue(col3);

            //  row.createCell(1).setCellValue(VALUE_YOU_WANT_TO_KEEP_ON_2ND_COLUMN);
        }

        String fileName = "sheet.xlsx"; //Name of the file

        String extStorageDirectory = this.getFilesDir().toString();
        File folder = new File(extStorageDirectory, "files");// Name of the folder you want to keep your file in the local storage.
        folder.mkdir(); //creating the folder
        File file = new File(folder, fileName);
        Log.e("TAG", "sendExcelToMail: " + file.getAbsolutePath());
        try {
            file.createNewFile(); // creating the file inside the folder
        } catch (IOException e1) {
            e1.printStackTrace();
            Log.e("TAG", "sendExcelToMail: " + e1.getLocalizedMessage());
        }

        try {
            FileOutputStream fileOut = new FileOutputStream(file); //Opening the file
            workbook.write(fileOut); //Writing all your row column inside the file
            fileOut.close(); //closing the file and done
            Log.e("TAG", "sendExcelToMail: " + "success");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TAG", "sendExcelToMail: " + e.getLocalizedMessage());

        }
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"leehari007@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Location Data");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "....");
        File root = this.getFilesDir();
        String pathToMyAttachedFile = "files/sheet.xlsv";
        File fileRead = file.getAbsoluteFile();
        if (!fileRead.exists() || !fileRead.canRead()) {
            Log.e("TAG", "sendExcelToMail: no file");
            return;
        }
        Uri apkURI = FileProvider.getUriForFile(
                this,
                this.getApplicationContext()
                        .getPackageName() + ".provider", fileRead);
        Uri uri = Uri.fromFile(fileRead);
        emailIntent.putExtra(Intent.EXTRA_STREAM, apkURI);
        startActivity(Intent.createChooser(emailIntent, "Pick an Email provider"));
    }
}
