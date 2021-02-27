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

import dagger.hilt.android.AndroidEntryPoint;
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

@AndroidEntryPoint
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
    String prevBalance = "0";
    int transactionCount, balanceCheckCount;
    int indexOfDataModel;
    int lengthOfDataModel;
//    ArrayList<DataModel> failedTransactions = new ArrayList<>();
//    JSONObject successfulTransactions = new JSONObject();
//    JSONObject inCompletedTransactions = new JSONObject();
//    JSONArray successfulArray = new JSONArray();
    String testServer = "http://testsuper.samicsub.com/api/";
    String liveServer = "http://superadmin.samicsub.com/api/";
    boolean transactionExistHasSuccessful;
    String screen_message = "", transaction_type = "airtime";
//    @Inject
    SharedPreferences appPref;
    boolean misformatted_balance_ussd = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {

        super.onCreate();
        transactionCount = 1;
        balanceCheckCount = 1;
        transactionExistHasSuccessful = false;
        OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(testServer)
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
                Log.d("ussd response", response.toString());
                Matcher m = p.matcher(response);
                screen_message = response.toString();
                showStatus(response.toString(),0);
                if(processType.equalsIgnoreCase("INITIAL BALANCE")){
                    if(m.find()) {
                        showStatus("Successfully got the balance\nbalance is " + m.group(),0);
                        String balance = m.group();
                        balanceReceived(balance);
                    }else{
                        transaction_id = dataModels.get(0).getTransaction_id();
                        postOrderForProcessing(screen_message, transaction_id,prevBalance,prevBalance,network,transaction_type,misformatted_balance_ussd = true);
//                        if(balanceCheckCount <= 5){
//                            balanceCheckCount++;
//                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Toast.makeText(getApplicationContext(), "The app failed to get balance, Restarting the app in 5 seconds", Toast.LENGTH_LONG).show();
//                                    DataModelProcess(dataModels);
//                                }
//                            }, 5000);
//
//                        }else{
//                            balanceCheckCount = 1;
//                            dataModels.remove(0);
//                            DataModelProcess(dataModels);
////                            showStatus("You need to restart the application there were too many failed transaction",0);
//                        }
                    }
                }else if(processType.equalsIgnoreCase("USSD AIRTIME")){
                    processType = "CHECK BALANCE";
//                    showStatus(response.toString(),0);
                    sendUSSD(dataModels.get(0).getBalanceUSSD());
                }else{
                    if(m.find()) {
                        showStatus("Successfully got the balance\nbalance is " + m.group(),0);
                        processType = "USSD AIRTIME";
                        String balance = m.group();
                        balanceReceived(balance);
                    }else{
                        transaction_id = dataModels.get(0).getTransaction_id();
                        postOrderForProcessing(screen_message, transaction_id,prevBalance,prevBalance,network,transaction_type,misformatted_balance_ussd = true);
//                        if(balanceCheckCount <= 5){
//                            balanceCheckCount++;
//                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Toast.makeText(getApplicationContext(), "The app failed to get balance, Restarting the app in 5 seconds", Toast.LENGTH_LONG).show();
//                                    DataModelProcess(dataModels);
//                                }
//                            }, 5000);
//
//                        }else{
//                            balanceCheckCount = 1;
//                            dataModels.remove(0);
//                            DataModelProcess(dataModels);
////                            showStatus("You need to restart the application there were too many failed transaction",0);
//                        }
                    }
//                    else{
//                        try {
//                            inCompletedTransactions.put("transaction_id", dataModels.get(0).getTransaction_id());
//                            successfulArray.put(inCompletedTransactions);
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
                if (processType.equalsIgnoreCase("CHECK BALANCE") || processType.equalsIgnoreCase("INITIAL BALANCE")) {
//                    try {
//                        inCompletedTransactions.put("transaction_id", dataModels.get(0).getTransaction_id());
//                        successfulArray.put(inCompletedTransactions);
//                        appPref.edit().putString("successful_transactions", successfulArray.toString()).apply();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                    transaction_id = dataModels.get(0).getTransaction_id();
                    postOrderForProcessing(screen_message, transaction_id,prevBalance," ",network,transaction_type,misformatted_balance_ussd = true);
//                    if(balanceCheckCount <=5 ){
//                        balanceCheckCount++;
//                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(getApplicationContext(), "The app failed to get balance, Restarting the app in 5 seconds (-1)", Toast.LENGTH_LONG).show();
//                                DataModelProcess(dataModels);
//                            }
//                        }, 5000);
//                    }else{
//                        balanceCheckCount = 1;
//                        dataModels.remove(0);
//                        DataModelProcess(dataModels);
////                        showStatus("You need to restart the application there were too many failed transaction (-1)",0);
//                    }
                }
                else{
                    transaction_id = dataModels.get(0).getTransaction_id();
                    postOrderForProcessing(screen_message, transaction_id,prevBalance,"0",network,transaction_type,misformatted_balance_ussd = true);
//                    if(transactionCount <= 5) {
//                        transactionCount++;
//                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(getApplicationContext(), "The app failed to process " + dataModels.get(0).getUSSDString() + " transaction, Restarting the app in 20 seconds (-1)", Toast.LENGTH_LONG).show();
//                                DataModelProcess(dataModels);
//                            }
//                        }, 20000);
//                    }else{
//                        transactionCount = 1;
//                        dataModels.remove(0);
//                        DataModelProcess(dataModels);
////                        showStatus("You need to restart the application there were too many failed transaction (-1)",0);
//                    }
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
                            System.out.println("Response List => " + responseModel.getData());
                            status = "-1";
                            try {
                                if (response.body() != null)
                                    if (response.body().getStatus() != null)
                                        status = response.body().getStatus();
                                if (status.equalsIgnoreCase("1")) {
                                    dataModels = responseModel.getData();
                                    lengthOfDataModel = dataModels.size();
                                    System.out.print("Lenght of Data Model is => "+ lengthOfDataModel);
                                    DataModelProcess(dataModels);
                                } else {
                                    getDataAPI();
                                }
                            }catch(Exception ex){
                                showStatus("Something went wrong will try again 20 seconds", 20000);
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseModel> call, Throwable t) {
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
        // CHECKING DATA TO BE TRANSACT
        System.out.println("Abdullah => I want to do Transaction Number of =>" + dataObjects.size() + " in Numbers");

        if(!dataObjects.isEmpty()) {

            showStatus("number of transactions to be processed: " + dataObjects.size(), 0);
            if(processType.equalsIgnoreCase("INITIAL BALANCE") || processType.equalsIgnoreCase("CHECK BALANCE")){
                Log.d("Balance", dataObjects.get(0).getBalanceUSSD());
                System.out.println("Abdullah => number of transactions to be processed: " + dataObjects.size() + " BUT I WANT TO Dial to get my balance ");
                sendUSSD(dataObjects.get(0).getBalanceUSSD());
                System.out.print("Abdullah => AM DONE CHECKING MY BALANCE");

            }else {
                System.out.println("Abdullah => My Active number of transaction is =>" + dataObjects.size() + " in Numbers");
//                showStatus("The number of transaction to be processed is " + dataObjects.size(), 0);
//                try {
//                    String sTransaction = appPref.getString("successful_transactions", null);
                    DataModel dataModel = dataObjects.get(0);
//                    if(sTransaction != null) {
//                        JSONArray jsonArray = new JSONArray(sTransaction);
//                        for (int i = 0; i < jsonArray.length(); i++) {
//                            if (jsonArray.getJSONObject(i).getString("transaction_id").equalsIgnoreCase(dataModel.getTransaction_id())) {
//                                transactionExistHasSuccessful = true;
//                                break;
//                            }
//                        }
//                        if(!transactionExistHasSuccessful){
//                            showStatus("SAMIC AIRTIME SERVICE now processing this: " + dataObjects.get(0).getUSSDString(), 0);
//                            transaction_id = dataModel.getTransaction_id();
//                            ussd_message = dataModel.getUSSDString();
//                            sendUSSD(ussd_message);
//                        }else{
//                            dataModels.remove(0);
//                            DataModelProcess(dataModels);
//                        }
//                    }else{
                        showStatus("SAMIC AIRTIME SERVICE now processing this: " + dataObjects.get(0).getUSSDString(), 0);
                        transaction_id = dataModel.getTransaction_id();
                        ussd_message = dataModel.getUSSDString();
                        sendUSSD(ussd_message);
//                    }
//                }catch(JSONException ex){
//                    ex.printStackTrace();
//                }
            }
        }
        else{
            System.out.println("Abdullah => It seems i don't have any transaction for NOW =>" + dataObjects.size() + "in Numbers");
//            showStatus("done processing all transactions, trying to fetch data from the web", 0);
//            postDataAPI(transaction_id);
            getDataAPI();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void sendUSSD(String ussd){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            System.out.println("******Abdullah: Telephony Callback => "+ telephonyCallback + "******");
            System.out.println("******Abdullah: USSD => "+ ussd + "******");

            telephonyManager.sendUssdRequest(ussd,telephonyCallback,null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void balanceReceived(String balance) {
        if(processType.equalsIgnoreCase("INITIAL BALANCE") || processType.equalsIgnoreCase("CHECK BALANCE")){
            prevBalance = balance;
            processType = "USSD AIRTIME";
            DataModelProcess(dataModels);
        }else if(processType.equalsIgnoreCase("USSD AIRTIME")) {
//            showStatus("Your balance now is: " + balance, 0);
//            if (prevBalance.equalsIgnoreCase(balance)) {
//                if(transactionCount == 5) {
//                    transactionCount = 1;
//                    showStatus(dataModels.get(0).getUSSDString() + " failed after 5 trial", 0);
//                    failedTransactions.add(dataModels.get(0));
                    DataModelProcess(dataModels);
                    postOrderForProcessing(screen_message, transaction_id, prevBalance, balance, network, transaction_type, misformatted_balance_ussd = false);
            System.out.println("******Abdullah: Am sending this back to server NOW => "+ screen_message + transaction_id +
                            prevBalance + balance + network + transaction_type + misformatted_balance_ussd +
                    "******");
                    prevBalance = balance;
//                }else{
//                    transactionCount++;
//                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(getApplicationContext(), "Transaction failed as balance remains the same, Will retry again in 10 seconds...", Toast.LENGTH_LONG).show();
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
        }
//        else{
//            processType = "USSD AIRTIME";
//            prevBalance = balance;
//            DataModelProcess(dataModels);
//        }
    }

    private void postOrderForProcessing(final String screen_message, final String transaction_id, final String balance_before, final String balance_after, final String network, final String transaction_type, final boolean misformatted_balance_ussd){
//        TransactionIdModel transactionIdModel = new TransactionIdModel(screen_message, transaction_id, balance_before, balance_after, network, transaction_type);
        Call<ResponseModel> call = service.processTransaction(screen_message, transaction_id, balance_before, balance_after, network, transaction_type, misformatted_balance_ussd);
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
                        showStatus("successfully posted " + dataModels.get(0).getUSSDString(), 0);
                        System.out.print("AM Quiting because i don't have any data to fetch.... BECAUSE Am => " + dataModels.isEmpty());
                            if(dataModels.isEmpty()){
                                System.out.print("DATA MODEL IS => " + dataModels.size());
                            showStatus("done processing all transactions, trying to fetch data from the web", 0);
                            getDataAPI();
                        }else{
                            dataModels.remove(0);
                            DataModelProcess(dataModels);
                        }
                    }
            }

            @Override
            public void onFailure(Call<ResponseModel> call, Throwable t) {
                //dismiss progress indicator
                //show reason for failure
//                Log.e("post_Retrofit_Error",t.getMessage());
                postOrderForProcessing(screen_message, transaction_id, balance_before, balance_after, network, transaction_type, misformatted_balance_ussd);
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

        @POST("process_transaction")
        @FormUrlEncoded
        Call<ResponseModel> processTransaction(@Field("screen_message") String screen_message,
                                               @Field("transaction_id") String transaction_id,
                                               @Field("balance_before") String balance_before,
                                               @Field("balance_after") String balance_after,
                                               @Field("network") String network,
                                               @Field("transaction_type") String transaction_type,
                                               @Field("misformatted_balance_ussd") boolean misformatted_balance_ussd);
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

