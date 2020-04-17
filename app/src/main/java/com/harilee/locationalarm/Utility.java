package com.harilee.locationalarm;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;

import java.util.Objects;

import me.ibrahimsn.lib.CirclesLoadingView;

public class Utility {



    public static void setPreference(Context context, String key, String value) {

        SharedPreferences.Editor editor = context.getSharedPreferences("location", Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();

    }

    public static String getPreference(Context context, String key) {

        SharedPreferences prefs = context.getSharedPreferences("location", Context.MODE_PRIVATE);
        String result = prefs.getString(key, "");
        //String a = prefs.getString(key,"");
        return result;
    }

    public static void showGifPopup(final Context mContext, boolean show, Dialog dialog) {

        dialog.setContentView(R.layout.progress_bar);
        dialog.setCancelable(false);
        CirclesLoadingView imageView = dialog.findViewById(R.id.loader);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        if (mContext != null) {
            if (!((Activity) mContext).isFinishing()) {
                try {
                    if (show) {
                        if (!dialog.isShowing())
                            dialog.show();
                    } else {
                        if (dialog.isShowing())
                            dialog.dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }


}
