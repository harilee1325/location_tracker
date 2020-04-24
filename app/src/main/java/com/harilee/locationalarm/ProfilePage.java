package com.harilee.locationalarm;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ProfilePage extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        TextView userName, fullName, vehicleNum, mobileNum, aadharNum;
        userName = findViewById(R.id.user_name);
        fullName = findViewById(R.id.full_name);
        vehicleNum = findViewById(R.id.vehicle_num);
        mobileNum = findViewById(R.id.mobile_num);
        aadharNum = findViewById(R.id.aadhar_num);
        /*getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);*/
        userName.setText(Utility.getPreference(this, "USERNAME"));
        Dialog dialog = new Dialog(this);
        Utility.showGifPopup(this, true, dialog);


        FirebaseFirestore ref = FirebaseFirestore.getInstance();
        ref.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    Utility.showGifPopup(this, false, dialog);

                    if (task.isSuccessful()) {
                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                if (documentSnapshot != null)
                                    if (String.valueOf(documentSnapshot.getData().get("name")).equalsIgnoreCase(Utility.getPreference(this, "USERNAME"))) {
                                        fullName.setText(String.valueOf(documentSnapshot.getData().get("full_name")));
                                        vehicleNum.setText(String.valueOf(documentSnapshot.getData().get("vehicle_num")));
                                        mobileNum.setText(String.valueOf(documentSnapshot.getData().get("mobile")));
                                        aadharNum.setText(String.valueOf(documentSnapshot.getData().get("aadhar")));

                                    }
                            }
                        }
                    }
                });

    }

    public void logOut(View view) {
        Utility.setPreference(this, "IS_LOGIN", "No");
        startActivity(new Intent(this, LoginPage.class));
    }

    public void goBack(View view) {
        startActivity(new Intent(this, MapsActivity.class));
    }
}
