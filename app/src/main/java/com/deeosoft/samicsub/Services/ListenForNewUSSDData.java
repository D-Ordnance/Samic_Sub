package com.deeosoft.samicsub.Services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import retrofit2.http.Part;
import retrofit2.http.Query;

public class ListenForNewUSSDData extends Service implements OnBalanceReceived {
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
    OnBalanceReceived listener;
    ArrayList<String> temp = new ArrayList<>();

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

        service = retrofit.create(ListenForNewUSSDData.ConnectionInterface.class);

        telephonyCallback = new TelephonyManager.UssdResponseCallback() {
            @Override
            public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                super.onReceiveUssdResponse(telephonyManager, request, response);
                String test = "Your data balance:\nSME Data Sponsor: 52777.27 expires 25/12/2019";
                Pattern p = Pattern.compile("[\\d]+[,][\\d]+[.][\\d]+|[\\d]+[.][\\d]+");
                Matcher m = p.matcher(response);
                String balance = m.group();
                if(processType.equalsIgnoreCase("INITIAL BALANCE")){
                    Log.d(TAG, "onReceiveUssdResponse: two");
                    Log.d(TAG, "onReceiveUssdResponse: " + response.toString());
                    listener.balanceReceived(balance);
                }else if(processType.equalsIgnoreCase("USSD AIRTIME")){
                    processType = "CHECK BALANCE";
                    Log.d(TAG, "onReceiveUssdResponse: four");
                    sendUSSD(dataModels.get(0).getBalanceUSSD());
                }else{
                    processType = "USSD AIRTIME";
                    Log.d(TAG, "onReceiveUssdResponse: " + processType);
                    listener.balanceReceived(balance);
                }
            }

            @Override
            public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                showStatus("SAMIC REQUEST failure Code " + failureCode,0);
                DataModelProcess(dataModels);
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
                                Log.d(TAG, "onResponse: else");
                                getDataAPI();
                            }
                            Log.d("GET_API Msg_Status()->", message + " " + status);
                        }

                        @Override
                        public void onFailure(Call<ResponseModel> call, Throwable t) {
                            //dismiss progress indicator
                            //show reason for failure
                            Log.d(TAG, "GetAPI onFailure: " + t.getMessage());
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
                                Log.d(TAG, "onResponse: else");
                                getDataAPI();
                            }
                            Log.d("GET_API Msg_Status()->", message + " " + status);
                        }

                        @Override
                        public void onFailure(Call<ResponseModel> call, Throwable t) {
                            //dismiss progress indicator
                            //show reason for failure
                            Log.d(TAG, "GetAPI onFailure: " + t.getMessage());
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
            if(processType.equalsIgnoreCase("INITIAL BALANCE")){
                Log.d(TAG, "DataModelProcess: one");
                sendUSSD(dataObjects.get(0).getBalanceUSSD());
            }else {
                showStatus("SAMIC AIRTIME SERVICE now processing this: " + dataObjects.get(0).getUSSDString(), 0);
                DataModel dataModel = dataObjects.get(0);
                transaction_id = dataModel.getTransaction_id();
                ussd_message = dataModel.getUSSDString();
                Log.d("USSD=>", ussd_message);
                sendUSSD(ussd_message);
            }
        }else{
            postDataAPI(transaction_id);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void sendUSSD(String ussd){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission is granted");
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            telephonyManager.sendUssdRequest(ussd,telephonyCallback,null);
        }else{
            Log.d(TAG, "sendUSSD: ");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void balanceReceived(String balance) {
        Log.d(TAG, "balanceReceived: three");
        if(!processType.equalsIgnoreCase("INITIAL BALANCE")) {
            if (prevBalance.equalsIgnoreCase(balance)) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        DataModelProcess(dataModels);
                    }
                }, 21000);
            } else {
                dataModels.remove(0);
                DataModelProcess(dataModels);
            }
        }else{
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
                Log.d("POST_RESPONSE", response.raw().message());
                if(response.body() != null)
                    if(response.body().getStatus() != null) {
                        getDataAPI();
                        Log.d("POST_STATUS", response.body().getStatus());
                }
            }

            @Override
            public void onFailure(Call<ResponseModel> call, Throwable t) {
                //dismiss progress indicator
                //show reason for failure
                Log.e("post_Retrofit_Error",t.getMessage());
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
