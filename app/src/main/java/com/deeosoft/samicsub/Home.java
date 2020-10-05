package com.deeosoft.samicsub;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.deeosoft.samicsub.Model.DataModel;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Home extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private static final String TAG = "Home";
    Button btnSms,btnUSSD,btnData;
    String networkValue;

    TelephonyManager.UssdResponseCallback telephonyCallback;

    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    JSONArray dummyArray = null;
    JSONObject currentTransaction = null;
    int transactionCount = 1;

    @RequiresApi(api = Build.VERSION_CODES.O)
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
        String dummyData = "[{\"ussd_string\":\"*123#\"},{\"ussd_string\":\"*123#\"},{\"ussd_string\":\"*123#\"},{\"ussd_string\":\"*123#\"},{\"ussd_string\":\"*123#\"}]";
        try {
            dummyArray = new JSONArray(dummyData);
        }catch(JSONException e){
            e.printStackTrace();
        }

        telephonyCallback = new TelephonyManager.UssdResponseCallback() {
            @Override
            public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                super.onReceiveUssdResponse(telephonyManager, request, response);
                String test = "Your data balance:\nSME Data Sponsor: 52777.27 expires 25/12/2019";
                Log.d(TAG, "ussd response: "+ response.toString());
                Pattern p = Pattern.compile("(([\\d]+[,][\\d]+[.][\\d]+)|([\\d]+[.][\\d]+))");
                Matcher m = p.matcher(response);
                Matcher n = p.matcher(test);
//                m.find();
//                n.find();
                try{
                    if(m.find()){
                        String balanceOne = m.group();
                        Log.d(TAG, "onReceiveUssdResponse: " + balanceOne);
                        balanceReceived(balanceOne);
                    }else{
                        Log.d(TAG, "onReceiveUssdResponse: here");
                    }
                }catch (IllegalStateException ex){
                    ex.printStackTrace();
                }
//                String balance = m.group();
            }
            @Override
            public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
                Log.d(TAG, "onReceiveUssdResponseFailed: " + failureCode);
            }
        };

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

    @TargetApi(Build.VERSION_CODES.O)
    private void CallUssd() {
//        try {
//                String UssdCodeNew = "*140" + Uri.encode("#");
//                if (ActivityCompat.checkSelfPermission(Home.this, Manifest.permission.CALL_PHONE)
//                        != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(Home.this,
//                            new String[]{Manifest.permission.CALL_PHONE}, 1);
//                } else {
//                    startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + UssdCodeNew)));
//                }
//        } catch (Exception eExcept) {
//            eExcept.printStackTrace();
//            Log.d(TAG, "CallUSSD: " + eExcept.getMessage());
//        }

//        Log.d(TAG, "CallUSSD: here");
//            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
//                Log.d(TAG, "CallUssd: here2");
//                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//                telephonyManager.sendUssdRequest("*140#",telephonyCallback,null);
//            }

        Log.d(TAG, "CallUssd: " + dummyArray.length());
        DataModelProcess(dummyArray);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void DataModelProcess(JSONArray dataObjects) {
        try {
            if(dataObjects.length() > 0) {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        Toast.makeText(Home.this,"Processing the " + formatPosition(transactionCount), Toast.LENGTH_LONG).show();
                    }
                });
                currentTransaction = dataObjects.getJSONObject(0);
                Log.d(TAG, "DataModelProcess: one");
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "CallUssd: here2");
                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    telephonyManager.sendUssdRequest(currentTransaction.getString("ussd_string"),telephonyCallback,null);
                }
            }else{
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        Toast.makeText(Home.this,"done processing all transactions", Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void balanceReceived(final String balance) {
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                Toast.makeText(Home.this,"Your balance is: " + balance, Toast.LENGTH_LONG).show();
            }
        });
        try {
//            String value = dummyArray.getJSONObject(0).getString("ussd_string");
            dummyArray.remove(0);
            transactionCount++;
            DataModelProcess(dummyArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String formatPosition(int pos){
        String posQualifier = "th";
        String result = "";
        if(pos == 1){
            result = "1st transaction";
        }else if(pos == 2){
            result = "2nd transaction";
        }else if(pos == 3){
            result = "3rd transaction";
        }else{
            result = pos + posQualifier + " transaction";
        }
        return result;
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
