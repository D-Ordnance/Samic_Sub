package com.deeosoft.samicsub;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import com.deeosoft.samicsub.Services.ListenForNewSMSData;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    ImageView img_chip;
    Button start_listen_service, stop_listen_service;
    Animation animation_first;
    boolean continueBatchTask = true;
    SharedPreferences appPref;

    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        img_chip = findViewById(R.id.img_chip);
        start_listen_service = findViewById(R.id.startListenService);
        stop_listen_service = findViewById(R.id.stopListenService);
        img_chip.setOnClickListener(this);
        start_listen_service.setOnClickListener(this);
        stop_listen_service.setOnClickListener(this);

        checkPermission();
        appPref = getSharedPreferences("SAMIC_SUB",MODE_PRIVATE);
        if(appPref.getBoolean("SMS_SERVICE_RUNNING",false)){
            stop_listen_service.setEnabled(true);
            start_listen_service.setEnabled(false);
            animateImage();
            Log.d(TAG, "onCreate: SMS_SERVICE_RUNNING true");
        }else{
            Log.d(TAG, "onCreate: SMS_SERVICE_RUNNING");
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
        animateImage();
        Log.d("TIMER_TASK)()->", Calendar.getInstance().getTime().toString());
        appPref.edit().putBoolean("SMS_SERVICE_RUNNING",true).apply();
        Intent intent = new Intent(this,ListenForNewSMSData.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForegroundService(intent);
            return;
        }
        startService(intent);
    }

    public void animateImage(){
        Log.d(TAG, "animateImage: here");
        animation_first = AnimationUtils.loadAnimation(this,R.anim.hyperspace_jump);
        img_chip.startAnimation(animation_first);
    }

    private void StopListenService(){
        if(stop_listen_service.isEnabled())stop_listen_service.setEnabled(false);
        start_listen_service.setEnabled(true);
        img_chip.clearAnimation();
        animation_first.cancel();
        animation_first.reset();
        appPref.edit().putBoolean("SMS_SERVICE_RUNNING",false).apply();
        Intent intent = new Intent(this, ListenForNewSMSData.class);
        stopService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    public String ReadDummy(){
        StringBuffer sb = new StringBuffer();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(
                    "response.txt")));
            String temp;
            while ((temp = br.readLine()) != null)
                sb.append(temp);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close(); // stop reading
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}