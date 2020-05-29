package com.deeosoft.samicsub;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.deeosoft.samicsub.Services.ListenForNewUSSDData;
import com.google.android.material.snackbar.Snackbar;

public class USSDData extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "USSDData";

    ImageView img_chip;
    Button start_listen_service, stop_listen_service;
    Animation animation_first;
    Handler timerTask;
    SharedPreferences appPref;

    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    @TargetApi(Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ((TextView)findViewById(R.id.toolBarTitle)).setText("SAMIC DATA USSD SERVICE");

        img_chip = findViewById(R.id.img_chip);
        start_listen_service = findViewById(R.id.startListenService);
        stop_listen_service = findViewById(R.id.stopListenService);
        img_chip.setOnClickListener(this);
        start_listen_service.setOnClickListener(this);
        stop_listen_service.setOnClickListener(this);

        checkPermission();
        stop_listen_service.setEnabled(false);

        timerTask = new Handler();

        appPref = getSharedPreferences("SAMIC_SUB",MODE_PRIVATE);
        if(appPref.getBoolean("DATA_SERVICE_RUNNING",false)){
            stop_listen_service.setEnabled(true);
            start_listen_service.setEnabled(false);
            animateImage();
            Log.d(TAG, "onCreate: DATA_SERVICE_RUNNING true");
        }else{
            Log.d(TAG, "onCreate: DATA_SERVICE_RUNNING");
            stop_listen_service.setEnabled(false);
        }
    }

    public void checkPermission(){
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_NETWORK_STATE)
                        != PackageManager.PERMISSION_GRANTED)){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE,
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.ACCESS_NETWORK_STATE},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }

    public void animateImage(){
        animation_first = AnimationUtils.loadAnimation(this,R.anim.hyperspace_jump);
        img_chip.startAnimation(animation_first);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.startListenService:
                if(isNetworkAvailable())StartListenService();else Snackbar.make(img_chip, "No Network", Snackbar.LENGTH_INDEFINITE)
                        .setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //nothing...
                            }
                        }).show();
                break;
            case R.id.stopListenService:
                StopListenService();
                break;
        }
    }

    private void StartListenService(){
        stop_listen_service.setEnabled(true);
        start_listen_service.setEnabled(false);
        animation_first = AnimationUtils.loadAnimation(this,R.anim.hyperspace_jump);
        img_chip.startAnimation(animation_first);
        appPref.edit().putBoolean("DATA_SERVICE_RUNNING",true).apply();
        Intent startIntent = new Intent(this, ListenForNewUSSDData.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForegroundService(startIntent);
            return;
        }
        startService(startIntent);
    }

    private void StopListenService(){
        if(stop_listen_service.isEnabled())stop_listen_service.setEnabled(false);
        start_listen_service.setEnabled(true);
        img_chip.clearAnimation();
        animation_first.cancel();
        animation_first.reset();
        appPref.edit().putBoolean("DATA_SERVICE_RUNNING",false).apply();
        Intent stopIntent = new Intent(this, ListenForNewUSSDData.class);
        stopService(stopIntent);
    }

    public boolean isNetworkAvailable() {
        boolean isConnected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }catch (Exception e){
            Log.e("NetworkE", String.valueOf(e));
        }
        return isConnected;
    }
}
