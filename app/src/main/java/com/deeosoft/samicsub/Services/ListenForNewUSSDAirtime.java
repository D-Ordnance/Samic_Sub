package com.deeosoft.samicsub.Services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.deeosoft.samicsub.Home;
import com.deeosoft.samicsub.MainActivity;
import com.deeosoft.samicsub.Model.DataModel;
import com.deeosoft.samicsub.Model.ResponseModel;
import com.deeosoft.samicsub.Model.TransactionIdModel;
import com.deeosoft.samicsub.R;
import com.deeosoft.samicsub.tool.OnBalanceReceived;
import com.deeosoft.samicsub.tool.UnsafeOkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class ListenForNewUSSDAirtime extends Service {
    private static final String TAG = "ListenForNewUSSDAirtime";
    TelephonyManager.UssdResponseCallback telephonyCallback;
    String status,message,sms_id,transaction_id,ussd_message;
    ListenForNewUSSDAirtime.ConnectionInterface service;
    private volatile boolean destroy = false;
    PowerManager.WakeLock wakeLock;
    String network;
    String processType;
    ArrayList<DataModel> dataModels;
    String prevBalance;
    int transactionCount = 1;
    ArrayList<DataModel> failedTransactions = new ArrayList<>();
    JSONObject successfulTransactions = new JSONObject();
    JSONObject inCompletedTransactions = new JSONObject();
    JSONArray successfulArray = new JSONArray();
    int transactionCounter = 1;
    SharedPreferences appPref;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://superadmin.samicsub.com/api/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(ListenForNewUSSDAirtime.ConnectionInterface.class);
        appPref = getApplicationContext().getSharedPreferences("samic sub", MODE_PRIVATE);
        telephonyCallback = new TelephonyManager.UssdResponseCallback() {
            @Override
            public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                super.onReceiveUssdResponse(telephonyManager, request, response);
                Pattern p = Pattern.compile("(([\\d]+[,][\\d]+[.][\\d]+)|([\\d]+[.][\\d]+))");
                Matcher m = p.matcher(response);
                if(processType.equalsIgnoreCase("INITIAL BALANCE")){
                    if(m.find()) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Successfully got the balance", Toast.LENGTH_LONG).show();
                            }
                        });
                        String balance = m.group();
                        balanceReceived(balance);
                    }else{
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "The app failed to get balance, Restarting the app in 5 seconds", Toast.LENGTH_LONG).show();
                                DataModelProcess(dataModels);
                            }
                        }, 5000);
                    }
                }else if(processType.equalsIgnoreCase("USSD AIRTIME")){
                    processType = "CHECK BALANCE";
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "sending ussd", Toast.LENGTH_LONG).show();
                        }
                    });
                    sendUSSD(dataModels.get(0).getBalanceUSSD());
                }else{
                    if(m.find()) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Successfully got the balance", Toast.LENGTH_LONG).show();
                            }
                        });
                        processType = "USSD AIRTIME";
                        String balance = m.group();
                        balanceReceived(balance);
                    }else{
                        try {
                            inCompletedTransactions.put("transaction_id", dataModels.get(0).getTransaction_id());
                            successfulArray.put(inCompletedTransactions);
                            appPref.edit().putString("successful_transactions", successfulArray.toString()).apply();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "The app failed to get balance, Restarting the app in 5 seconds", Toast.LENGTH_LONG).show();
                                DataModelProcess(dataModels);
                            }
                        }, 5000);
                    }
                }
            }
            @Override
            public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
                if (processType.equalsIgnoreCase("CHECK BALANCE")) {
                    try {
                        inCompletedTransactions.put("transaction_id", dataModels.get(0).getTransaction_id());
                        successfulArray.put(inCompletedTransactions);
                        appPref.edit().putString("successful_transactions", successfulArray.toString()).apply();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "The app failed to get balance, Restarting the app in 5 seconds", Toast.LENGTH_LONG).show();
                            DataModelProcess(dataModels);
                        }
                    }, 5000);
                }else{
                    showStatus("SAMIC REQUEST failure Code " + failureCode, 0);
                    DataModelProcess(dataModels);
                }
            }
        };
        this.startForeground(1,CreateNotification());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null){
            network = intent.getStringExtra("NETWORK");
        }
        wakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock");

        wakeLock.acquire();
        getDataAPI();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        Log.d(TAG, "onDestroy: here");
        stopForeground(true);
        try {
            if (wakeLock.isHeld()){
                wakeLock.release();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        destroy = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getDataAPI(){
        processType = "INITIAL BALANCE";
        if(!network.contains("gifting")) {
            if (isOnline()) {
                if(!destroy) {
                    network = network.contains("mtn") ? "mtn" : network;
                    Call<ResponseModel> call = service.getAppData(network);
                    call.enqueue(new Callback<ResponseModel>() {
                        @Override
                        public void onResponse(Call<ResponseModel> call, Response<ResponseModel> response) {
                            //dismiss progress indicator
                            ResponseModel responseModel = response.body();
                            status = "-1";
                            if (response.body() != null) if (response.body().getStatus() != null)
                                status = response.body().getStatus();
                            if (status.equalsIgnoreCase("1")) {
                                dataModels = responseModel.getData();
                                DataModelProcess(dataModels);
                            } else {
                                getDataAPI();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseModel> call, Throwable t) {
                            //dismiss progress indicator
                            //show reason for failure
                            showStatus("Samic has lost network connection. trying to reconnect in 20 seconds. if it persist check your network connection."
                                    , 20000);
                        }
                    });
                }
            } else {
                showStatus("Samic has lost network connection. trying to reconnect in 20 seconds. if it persist check your network connection."
                        , 20000);
            }
        }else{
            showStatus("The network type is not supported for this transaction.",0);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void DataModelProcess(ArrayList<DataModel> dataObjects) {
        if(!dataObjects.isEmpty()) {
            showStatus("The number of transaction to be processed is " + dataObjects.size(), 0);
            if(processType.equalsIgnoreCase("INITIAL BALANCE")){
                showStatus("getting balance dialing: " + dataObjects.get(0).getBalanceUSSD(), 0);
                sendUSSD(dataObjects.get(0).getBalanceUSSD());
            }else {
                try {
                    String sTransaction = appPref.getString("successful_transactions", null);
                    JSONArray jsonArray = new JSONArray(sTransaction);
                    DataModel dataModel = dataObjects.get(0);
                    for(int i = 0; i < jsonArray.length(); i++){
                        if(!jsonArray.getJSONObject(i).getString("transaction_id").equalsIgnoreCase(dataModel.getTransaction_id())){
                            showStatus("SAMIC AIRTIME SERVICE now processing this: " + dataObjects.get(0).getUSSDString(), 0);
                            transaction_id = dataModel.getTransaction_id();
                            ussd_message = dataModel.getUSSDString();
                            sendUSSD(ussd_message);
                        }
                    }
                }catch(JSONException ex){
                    ex.printStackTrace();
                }
            }
        }else{
            showStatus("done processing all transactions, trying to fetch data from the web", 0);
            postDataAPI(transaction_id);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void sendUSSD(String ussd){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.sendUssdRequest(ussd,telephonyCallback,null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void balanceReceived(String balance) {
        if(!processType.equalsIgnoreCase("INITIAL BALANCE")) {
            showStatus("Your balance now is: " + balance, 0);
            if (prevBalance.equalsIgnoreCase(balance)) {
                if(transactionCount == 5) {
                    transactionCount = 1;
                    showStatus(dataModels.get(0).getUSSDString() + " failed after 5 trial", 0);
                    failedTransactions.add(dataModels.get(0));
                    dataModels.remove(0);
                    DataModelProcess(dataModels);
                }else{
                    transactionCount++;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Transaction failed, Will retry again in 10 seconds...", Toast.LENGTH_LONG).show();
                            DataModelProcess(dataModels);
                        }
                    }, 10000);
                }
            } else {
                showStatus(dataModels.get(0).getUSSDString() + " was successfully processed", 0);
                try {
                    successfulTransactions.put("ussd_string", dataModels.get(0).getUSSDString());
                    successfulTransactions.put("transaction_id", dataModels.get(0).getTransaction_id());
                    successfulArray.put(successfulTransactions);
                    appPref.edit().putString("successful_transactions", successfulArray.toString()).apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                dataModels.remove(0);
                DataModelProcess(dataModels);
            }
        }else{
            showStatus("Your initial balance is: " + balance, 0);
            processType = "USSD AIRTIME";
            prevBalance = balance;
            DataModelProcess(dataModels);
        }
    }

    private void postDataAPI(final String transaction_id){
        TransactionIdModel transactionIdModel = new TransactionIdModel(transaction_id);
        Call<ResponseModel> call = service.postDataAPI(transactionIdModel);
        call.enqueue(new Callback<ResponseModel>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<ResponseModel> call, Response<ResponseModel> response) {
                //check for the value of message and status to dictate the next move
                if(response.body() != null)
                    if(response.body().getStatus() != null){
                        getDataAPI();
                    }
            }

            @Override
            public void onFailure(Call<ResponseModel> call, Throwable t) {
                //dismiss progress indicator
                //show reason for failure
                postDataAPI(transaction_id);
            }
        });
    }

    public interface ConnectionInterface {
        @GET("airtime")
        Call<ResponseModel> getAppData(@Query("network") String network);

        @POST("airtime/done")
        @Headers("Accept: application/json")
        Call<ResponseModel> postDataAPI(@Body TransactionIdModel body);
    }


    public void showStatus(final String msg, long delay){
        Handler toastHandler = new Handler(Looper.getMainLooper());
        if(delay > 0) {
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            toastHandler.postDelayed(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    getDataAPI();
                }
            }, delay);
        }else{
            toastHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean isOnline(){
        ConnectivityManager networkManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifi = false;
        boolean isMobile = false;
        for (Network network : networkManager.getAllNetworks()) {
            NetworkInfo networkInfo = networkManager.getNetworkInfo(network);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return isWifi |= networkInfo.isConnected();
            }
            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return isMobile |= networkInfo.isConnected();
            }
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public Notification CreateNotification(){
        final String notificationChannelId = "SAMIC SUB SERVICE CHANNEL";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(notificationChannelId,
                    "Samic Sub Notification Channel",
                    android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Samic Sub Running");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent  = new Intent(this, Home.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            builder  =  new Notification.Builder(
                    this,
                    notificationChannelId);
        }
        else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("Samic Sub Service")
                .setContentText("Samic Sub Service is Running...")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Samic Sub")
                .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
                .build();
    }
}

