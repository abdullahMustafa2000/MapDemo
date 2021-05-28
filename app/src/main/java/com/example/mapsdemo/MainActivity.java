package com.example.mapsdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final int ERROR_DIALOG_REQUEST = 9001;

    Button mapBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isServiceOk(this)) {
            initView(this);
        }
    }

    public void initView(MainActivity activity) {
        mapBtn = findViewById(R.id.btn_map);
        mapBtn.setOnClickListener(v -> {
            startActivity(new Intent(activity, MapsActivity.class));
        });
    }

    private boolean isServiceOk(MainActivity context) {
        Log.d(TAG, "isServiceOk: checking google play services version");
        GoogleApiAvailability googleInstance = GoogleApiAvailability.getInstance();
        int available = googleInstance.isGooglePlayServicesAvailable(context);

        if (available == ConnectionResult.SUCCESS) {
            Log.d(TAG, "isServiceOk: every is fine and ready to get locations");
            return true;
        } else if (googleInstance.isUserResolvableError(available)) {
            Log.d(TAG, "isServiceOk: there is an issue but it could be solved");
            Dialog dialog = googleInstance.getErrorDialog(context, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else
            Toast.makeText(context, "you can't access map and locations", Toast.LENGTH_SHORT).show();
        return false;
    }
}