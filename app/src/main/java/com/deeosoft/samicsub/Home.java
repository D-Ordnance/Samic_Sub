package com.deeosoft.samicsub;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class Home extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "Home";
    Button btnSms,btnUSSD,btnData;

    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    public void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_home);
        btnSms = findViewById(R.id.btnSms);
        btnUSSD = findViewById(R.id.btnUSSD);
        btnData = findViewById(R.id.btnData);
        btnSms.setEnabled(false);
        btnUSSD.setEnabled(false);
        btnData.setEnabled(false);
        btnSms.setOnClickListener(this);
        btnUSSD.setOnClickListener(this);
        btnData.setOnClickListener(this);
        checkPermission();
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
            requestPermission();
        }else{
            btnSms.setEnabled(true);
            btnUSSD.setEnabled(true);
            btnData.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    btnSms.setEnabled(true);
                    btnUSSD.setEnabled(true);
                    btnData.setEnabled(true);
                }else{
                    Snackbar.make(btnData,"Allow permissions requested for SamicSub to function properly", Snackbar.LENGTH_INDEFINITE)
                            .setAction("OK",new View.OnClickListener(){
                                @Override
                                public void onClick(View v) {
                                    requestPermission();
                                }
                            }).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void requestPermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_NETWORK_STATE},
                MY_PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnSms:
                goToSMSPage();
                break;
            case R.id.btnUSSD:
                goToUSSDPage();
                break;
            case R.id.btnData:
                goToDataPage();
                break;
        }
    }

    public void goToSMSPage(){
        startActivity(new Intent(this,MainActivity.class));
    }

    public void goToUSSDPage(){
        startActivity(new Intent(this,USSD.class));
    }

    public void goToDataPage(){
        startActivity(new Intent(this,USSDData.class));
    }
}
