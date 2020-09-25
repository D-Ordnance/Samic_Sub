package com.deeosoft.samicsub;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class Home extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "Home";
    Button btnSms,btnUSSD,btnData;
    String networkValue;

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
        findViewById(R.id.sendUssd).setOnClickListener(this);

        // Spinner element
        final Spinner spinner = findViewById(R.id.networkSpinner);
        spinner.setOnItemSelectedListener(this);
//        networkValue = (String)spinner.getSelectedItem();

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
                goToAirtimePage();
                break;
            case R.id.btnData:
                goToDataPage();
                break;
            case R.id.sendUssd:
                CallUssd();
                break;
        }
    }

    private void CallUssd() {
        try {
                String UssdCodeNew = "*140" + Uri.encode("#");
                if (ActivityCompat.checkSelfPermission(Home.this, Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Home.this,
                            new String[]{Manifest.permission.CALL_PHONE}, 1);
                } else {
                    startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + UssdCodeNew)));
                }
        } catch (Exception eExcept) {
            eExcept.printStackTrace();
            Log.d(TAG, "CallUSSD: " + eExcept.getMessage());
        }
    }

    public void goToSMSPage(){
        startActivity(new Intent(this, MainActivity.class));
    }

    public void goToAirtimePage(){
        Log.d(TAG, "goToUSSDPage: " + networkValue);
        if(networkValue.equals("Select network to process")) {
            Toast.makeText(this,"Select network to process", Toast.LENGTH_LONG).show();
        }else if(networkValue.contains("gifting")){
            Toast.makeText(this,"The network type is not supported for this transaction.", Toast.LENGTH_LONG).show();
        }else {
            startActivity(new Intent(this,USSD.class).putExtra("NETWORK",networkValue));
        }
    }

    public void goToDataPage(){
        Log.d(TAG, "goToUSSDPage: " + networkValue);
        if(networkValue.equals("Select network to process")) {
            Toast.makeText(this,"Select network to process", Toast.LENGTH_LONG).show();
        }else {
            startActivity(new Intent(this,USSDData.class).putExtra("NETWORK",networkValue));
        }
    }

    /**
     * <p>Callback method to be invoked when an item in this view has been
     * selected. This callback is invoked only when the newly selected
     * position is different from the previously selected position or if
     * there was no selected item.</p>
     * <p>
     * Implementers can call getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param parent   The AdapterView where the selection happened
     * @param view     The view within the AdapterView that was clicked
     * @param position The position of the view in the adapter
     * @param id       The row id of the item that is selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        networkValue = (String)parent.getSelectedItem();
    }

    /**
     * Callback method to be invoked when the selection disappears from this
     * view. The selection can disappear for instance when touch is activated
     * or when the adapter becomes empty.
     *
     * @param parent The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
