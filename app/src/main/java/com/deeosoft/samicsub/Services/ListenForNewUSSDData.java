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
import com.deeosoft.samicsub.Model.DataModel;
import com.deeosoft.samicsub.Model.ResponseModel;
import com.deeosoft.samicsub.Model.TransactionIdModel;
import com.deeosoft.samicsub.R;
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

public class ListenForNewUSSDData extends Service {
    private static final String TAG = "ListenForNewUSSDData";
    TelephonyManager.UssdResponseCallback telephonyCallback;
    String status,message,sms_id,transaction_id,ussd_message;
    ListenForNewUSSDData.ConnectionInterface service;
    private volatile boolean destroy = false;
    PowerManager.WakeLock wakeLock;
    String network;
    String processType;
    ArrayList<DataModel> dataModels;
    String prevBalance;
    int transactionCount, balanceCheckCount;
//    ArrayList<DataModel> fTransaction = new ArrayList<>();
//    ArrayList<DataModel> sTransaction = new ArrayList<>();
//    JSONObject successfulTransactions = new JSONObject();
//    JSONObject imCompletedTransactions = new JSONObject();
//    JSONArray successfulArray = new JSONArray();
    DataModel currentTransaction;
    SharedPreferences appPref;
    boolean transactionExistHasSuccessful;
    String screen_message = "", transaction_type = "data";

    String testServer = "http://testsuper.samicsub.com/api/";
    String liveServer = "http://superadmin.samicsub.com/api/";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        transactionCount = 1;
        balanceCheckCount = 1;
        transactionExistHasSuccessful = false;
        OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(liveServer)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(ListenForNewUSSDData.ConnectionInterface.class);
        appPref = getApplicationContext().getSharedPreferences("samic sub", MODE_PRIVATE);
        telephonyCallback = new TelephonyManager.UssdResponseCallback() {
            @Override
            public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                super.onReceiveUssdResponse(telephonyManager, request, response);
                Pattern p = Pattern.compile("(([\\d]+[,][\\d]+[.][\\d]+)|([\\d]+[.][\\d]+))");
                Log.d("ussd response", response.toString());
                Matcher m = p.matcher(response);
                screen_message = response.toString();
                if(processType.equalsIgnoreCase("INITIAL BALANCE")){
                    if(m.find()) {
                        String balance = m.group();
                        balanceReceived(balance);
                    }else{
                        if(balanceCheckCount <= 5){
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "The app failed to get balance, Restarting the app in 5 seconds", Toast.LENGTH_LONG).show();
                                    DataModelProcess(dataModels);
                                }
                            }, 5000);
                        }else{
                            balanceCheckCount = 1;
                            Toast.makeText(getApplicationContext(), "You need to restart the application there were too many failed transaction", Toast.LENGTH_LONG).show();
                        }
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
                        processType = "USSD AIRTIME";
                        String balance = m.group();
                        balanceReceived(balance);
                    }
//                    else{
//                        try {
//                            imCompletedTransactions.put("transaction_id", dataModels.get(0).getTransaction_id());
//                            successfulArray.put(imCompletedTransactions);
//                            appPref.edit().putString("successful_transactions", successfulArray.toString()).apply();
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(getApplicationContext(), "The app failed to get balance, Restarting the app in 5 seconds", Toast.LENGTH_LONG).show();
//                                DataModelProcess(dataModels);
//                            }
//                        }, 5000);
//                    }
                }
            }
            @Override
            public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
                if (processType.equalsIgnoreCase("CHECK BALANCE")) {
//                    try {
//                        imCompletedTransactions.put("transaction_id", dataModels.get(0).getTransaction_id());
//                        successfulArray.put(imCompletedTransactions);
//                        appPref.edit().putString("successful_transactions", successfulArray.toString()).apply();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                    if(balanceCheckCount <= 5){
                        balanceCheckCount++;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "The app failed to get balance, Restarting the app in 5 seconds", Toast.LENGTH_LONG).show();
                                DataModelProcess(dataModels);
                            }
                        }, 5000);
                    }else{
                        balanceCheckCount = 1;
                        Toast.makeText(getApplicationContext(), "You need to restart the application there were too many failed transaction", Toast.LENGTH_LONG).show();
                    }
                }else{
                    showStatus("SAMIC REQUEST failure Code " + failureCode, 0);
                    if(transactionCount <= 5) {
                        transactionCount++;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "The app failed to process " + dataModels.get(0).getUSSDString() + " transaction, Restarting the app in 20 seconds", Toast.LENGTH_LONG).show();
                                DataModelProcess(dataModels);
                            }
                        }, 20000);
                    }else{
                        transactionCount = 1;
                        Toast.makeText(getApplicationContext(), "You need to restart the application there were too many failed transaction", Toast.LENGTH_LONG).show();
                    }
                }
            }
        };
        this.startForeground(1,CreateNotification());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getDataAPI(){
        processType = "INITIAL BALANCE";
        if(network.contains("mtn")) {
            if (isOnline()) {
                if (!destroy) {
                    String type = (network.contains("sme")) ? "sme" : "gifting";
                    Call<ResponseModel> call = service.getAppData("mtn",type);
                    call.enqueue(new Callback<ResponseModel>() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onResponse(Call<ResponseModel> call, Response<ResponseModel> response) {
                            //dismiss progress indicator
                            ResponseModel responseModel = response.body();
                            status = "-1";
                            if (response.body() != null) if (response.body().getStatus() != null)
                                status = response.body().getStatus();
                            if (status.equalsIgnoreCase("1")) {
                                dataModels = responseModel.getData();
                                DataModelProcess(responseModel.getData());
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
            if (isOnline()) {
                if (!destroy) {
                    Call<ResponseModel> call = service.getAppData(network);
                    call.enqueue(new Callback<ResponseModel>() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onResponse(Call<ResponseModel> call, Response<ResponseModel> response) {
                            //dismiss progress indicator
                            ResponseModel responseModel = response.body();
                            status = "-1";
                            if (response.body() != null) if (response.body().getStatus() != null)
                                status = response.body().getStatus();
                            if (status.equalsIgnoreCase("1")) {
                                DataModelProcess(responseModel.getData());
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
        }
    }

    private void DataModelProcess(ArrayList<DataModel> dataObjects) {
        if(!dataObjects.isEmpty()) {
            if(processType.equalsIgnoreCase("INITIAL BALANCE") || processType.equalsIgnoreCase("CHECK BALANCE")){
                sendUSSD(dataObjects.get(0).getBalanceUSSD());
                //screen_message
                //transaction_id
                //balance_after
                //balance_before
                //network
            }else {
//                showStatus("The number of transaction to be processed is " + dataObjects.size(), 0);
//                try {
//                    String sTransaction = appPref.getString("successful_transactions", null);
                    DataModel dataModel = dataObjects.get(0);
                    Log.d(TAG, "DataModelProcess: here1");
//                    if(sTransaction != null) {
//                        JSONArray jsonArray = new JSONArray(sTransaction);
//                        for (int i = 0; i < jsonArray.length(); i++) {
//                            if (jsonArray.getJSONObject(i).getString("transaction_id").equalsIgnoreCase(dataModel.getTransaction_id())) {
//                                transactionExistHasSuccessful = true;
//                                break;
//                            }
//                        }
//                        if(!transactionExistHasSuccessful){
//                            showStatus("SAMIC DATA SERVICE now processing this: " + dataObjects.get(0).getUSSDString(), 0);
//                            transaction_id = dataModel.getTransaction_id();
//                            ussd_message = dataModel.getUSSDString();
//                            sendUSSD(ussd_message);
//                        }else{
//                            dataModels.remove(0);
//                            DataModelProcess(dataModels);
//                        }
//                    }else{
                        Log.d(TAG, "DataModelProcess: here");
                        showStatus("SAMIC DATA SERVICE now processing this: " + dataObjects.get(0).getUSSDString(), 0);
                        transaction_id = dataModel.getTransaction_id();
                        ussd_message = dataModel.getUSSDString();
                        sendUSSD(ussd_message);
//                    }
//                }catch(JSONException ex){
//                    ex.printStackTrace();
//                }
            }
        }
//        else{
//            showStatus("done processing all transactions, trying to fetch data from the web", 0);
//            postDataAPI(transaction_id);
//        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void sendUSSD(String ussd){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
//            Log.v(TAG, "Permission is granted");
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.sendUssdRequest(ussd,telephonyCallback,null);
        }
//        else{
//            Log.d(TAG, "sendUSSD: ");
//        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void balanceReceived(String balance) {
//        Log.d(TAG, "balanceReceived: three");
        if(processType.equalsIgnoreCase("INITIAL BALANCE")){
            prevBalance = balance;
            processType = "USSD AIRTIME";
            DataModelProcess(dataModels);
        }else if(processType.equalsIgnoreCase("USSD AIRTIME")) {
//            showStatus("Your balance now is: " + balance, 0);
//            if (prevBalance.equalsIgnoreCase(balance)) {
//                if(transactionCount == 5) {
//                    transactionCount = 1;
//                    showStatus(dataModels.get(0).getUSSDString() + " failed after 5 trial", 0);
//                    fTransaction.add(dataModels.get(0));
                    dataModels.remove(0);
                    transaction_id = dataModels.get(0).getTransaction_id();
                    postOrderForProcessing(screen_message, transaction_id, prevBalance, balance, network, transaction_type);
                    prevBalance = balance;
//                    processType = "CHECK BALANCE";
//                    DataModelProcess(dataModels);
//                }else{
//                    transactionCount++;
//                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(), "Transaction failed, Will retry again in 10 seconds...", Toast.LENGTH_LONG).show();
//                            DataModelProcess(dataModels);
//                        }
//                    }, 10000);
//                }
//            } else {
//                showStatus(dataModels.get(0).getUSSDString() + " was successfully processed", 0);
//                try {
//                    successfulTransactions.put("ussd_string", dataModels.get(0).getUSSDString());
//                    successfulTransactions.put("transaction_id", dataModels.get(0).getTransaction_id());
//                    successfulArray.put(successfulTransactions);
//                    appPref.edit().putString("successful_transactions", successfulArray.toString()).apply();
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                dataModels.remove(0);
//                DataModelProcess(dataModels);
//            }
        }else{
//            showStatus("Your initial balance is: " + balance, 0);
            processType = "USSD AIRTIME";
            prevBalance = balance;
            DataModelProcess(dataModels);
        }
    }

    private void postOrderForProcessing(final String screen_message, final String transaction_id, final String balance_before, final String balance_after, final String network, final String transaction_type){
//        TransactionIdModel transactionIdModel = new TransactionIdModel(screen_message, transaction_id, balance_before, balance_after, network, transaction_type);
        Call<ResponseModel> call = service.processTransaction(screen_message, transaction_id, balance_before, balance_after, network, transaction_type);
        call.enqueue(new Callback<ResponseModel>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<ResponseModel> call, Response<ResponseModel> response) {
                //check for the value of message and status to dictate the next move
//                Log.d("POST_RESPONSE", response.raw().message());
                if(response.body() != null)
                    if(response.body().getStatus() != null) {
//                        getDataAPI();
//                        Log.d("POST_STATUS", response.body().getStatus());
                        if(dataModels.isEmpty()){
                            getDataAPI();
                        }else{
//                            dataModels.remove(0);
                            DataModelProcess(dataModels);
                        }
                    }
            }

            @Override
            public void onFailure(Call<ResponseModel> call, Throwable t) {
                //dismiss progress indicator
                //show reason for failure
//                Log.e("post_Retrofit_Error",t.getMessage());
                postOrderForProcessing(screen_message, transaction_id, balance_before, balance_after, network, transaction_type);
            }
        });
    }

    private void postDataAPI(final String transaction_id){
        TransactionIdModel transactionIdModel = new TransactionIdModel(transaction_id);
        Call<ResponseModel> call = service.postDataAPI(transactionIdModel);
        call.enqueue(new Callback<ResponseModel>() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onResponse(Call<ResponseModel> call, Response<ResponseModel> response) {
                //check for the value of message and status to dictate the next move
//                Log.d("POST_RESPONSE", response.raw().message());
                if(response.body() != null)
                    if(response.body().getStatus() != null) {
                        getDataAPI();
//                        Log.d("POST_STATUS", response.body().getStatus());
                }
            }

            @Override
            public void onFailure(Call<ResponseModel> call, Throwable t) {
                //dismiss progress indicator
                //show reason for failure
//                Log.e("post_Retrofit_Error",t.getMessage());
                postDataAPI(transaction_id);
            }
        });
    }

    public interface ConnectionInterface {
        @GET("data_bundle")
        Call<ResponseModel> getAppData(@Query("network") String network);

        @GET("data_bundle")
        Call<ResponseModel> getAppData(@Query("network") String network, @Query("type") String type);

        @POST("data_bundle/done")
        @Headers("Accept: application/json")
        Call<ResponseModel> postDataAPI(@Body TransactionIdModel body);

        @POST("process_transaction")
        @FormUrlEncoded
        Call<ResponseModel> processTransaction(@Field("screen_message") String screen_message,
                                               @Field("transaction_id") String transaction_id,
                                               @Field("balance_before") String balance_before,
                                               @Field("balance_after") String balance_after,
                                               @Field("network") String network,
                                               @Field("transaction_type") String transaction_type);
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
                    "Samic Sub ussd data Notification Channel",
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
