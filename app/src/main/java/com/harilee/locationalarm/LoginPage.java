package com.harilee.locationalarm;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class LoginPage extends AppCompatActivity {

    private EditText userName;
    private EditText vehicleNum, aadharNum, mobileNum, fullName;
    private FirebaseFirestore ref;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        userName = findViewById(R.id.username);
        vehicleNum = findViewById(R.id.vehicle_num);
        aadharNum = findViewById(R.id.aadhar_num);
        mobileNum = findViewById(R.id.mobile_num);
        fullName = findViewById(R.id.full_name);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        vehicleNum.setVisibility(View.GONE);
        mobileNum.setVisibility(View.GONE);
        aadharNum.setVisibility(View.GONE);
        fullName.setVisibility(View.GONE);




    }

    public void addUser(View view) {

        final String username = userName.getText().toString().trim();

        if (!mobileNum.getText().toString().trim().isEmpty()){
            registerUser();
        }
        if (username.isEmpty()) {
            userName.setError("Enter  username");
        } else {
            ProgressDialog dialog = ProgressDialog.show(LoginPage.this, "",
                    "Logging in. Please wait...", true);
            ref = FirebaseFirestore.getInstance();
            ref.collection("users").whereEqualTo("name", username)
                    .get()
                    .addOnCompleteListener(task -> {
                        dialog.cancel();
                        if (task.isSuccessful()) {
                            if (task.getResult().size() > 0) {
                                Utility.setPreference(this, "USERNAME", username);
                                Toast.makeText(getApplicationContext(), "User logged in successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MapsActivity.class));
                            } else {

                                new AlertDialog.Builder(this)
                                        .setTitle("New User detected")
                                        .setMessage("Do you  want to register?")
                                        .setPositiveButton(android.R.string.yes, (dialog12, which) -> {
                                            registerUser();

                                        })
                                        .setNegativeButton(android.R.string.no, null)
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                            }
                        }
                    });
        }

    }

    private void registerUser() {
        final String username = userName.getText().toString().trim();
        String vehicle = vehicleNum.getText().toString().trim();
        String fullname = fullName.getText().toString().trim();
        String mob = mobileNum.getText().toString().trim();
        String aadharno = aadharNum.getText().toString().trim();
        vehicleNum.setVisibility(View.VISIBLE);
        mobileNum.setVisibility(View.VISIBLE);
        aadharNum.setVisibility(View.VISIBLE);
        fullName.setVisibility(View.VISIBLE);
        if (fullname.isEmpty()) {
            fullName.setError("Enter fullname");
        } else if (mob.isEmpty()) {
            mobileNum.setError("Enter mobile number");
        } else if (vehicle.isEmpty()) {
            vehicleNum.setError("Enter vehicle number");
        } else if (aadharno.isEmpty()) {
            aadharNum.setError("Enter aadhar number");
        } else {

            final Map<String, Object> users = new HashMap<>();
            users.put("name", username);
            users.put("vehicle_num", vehicle);
            users.put("mobile", mob);
            users.put("aadhar", aadharno);
            users.put("full_name", fullname);

            ProgressDialog dialog1 = ProgressDialog.show(LoginPage.this, "",
                    "Registering new user. Please wait...", true);
            ref.collection("users")
                    .add(users)
                    .addOnSuccessListener(documentReference -> {
                        dialog1.cancel();
                        Utility.setPreference(this, "USERNAME", username);
                        Toast.makeText(getApplicationContext(), "User added successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MapsActivity.class));
                    })
                    .addOnFailureListener(e -> {
                        dialog1.cancel();
                        Toast.makeText(getApplicationContext(), "Error adding document", Toast.LENGTH_SHORT).show();

                    });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}
