package com.deeosoft.samicsub.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.deeosoft.samicsub.Home;
import com.deeosoft.samicsub.MainActivity;
import com.deeosoft.samicsub.Model.DataModel;
import com.deeosoft.samicsub.Model.ResponseModel;
import com.deeosoft.samicsub.Model.SMSModel;
import com.deeosoft.samicsub.R;
import com.deeosoft.samicsub.tool.UnsafeOkHttpClient;

import java.util.ArrayList;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class ListenForNewSMSData extends Service {
    private static final String TAG = "ListenForNewSMSData";

    String status,message;
    ConnectionInterface service;
    private volatile boolean destroy = false;
    PowerManager.WakeLock wakeLock;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public void onCreate() {
        super.onCreate();
        OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://superadmin.mtncug.com/api/sms/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(ConnectionInterface.class);

        this.startForeground(1,CreateNotification());
    }

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * {@link IBinder} is usually for a complex interface
     * that has been <a href="{@docRoot}guide/components/aidl.html">described using
     * aidl</a>.
     *
     * <p><em>Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process</em>.  More information about the main thread can be found in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
     * Threads</a>.</p>
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        wakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock");

        wakeLock.acquire();

        getDataAPI();
        return super.onStartCommand(intent, flags, startId);
    }

    private void getDataAPI(){
        if(!destroy) {
            Log.d("here", "here");
            Call<ResponseModel> call = service.getAppData();
            call.enqueue(new Callback<ResponseModel>() {
                @Override
                public void onResponse(Call<ResponseModel> call, Response<ResponseModel> response) {
                    //dismiss progress indicator
                    ResponseModel responseModel = response.body();
                    status = response.body().getStatus();
                    Log.d("status", status);
                    if (status.equalsIgnoreCase("1")) {
                        Log.d(TAG, "onResponse: status is one");
                        DataModelProcess(responseModel.getData());
                    } else {
                        Log.d(TAG, "onResponse: else");
                        getDataAPI();
                    }
                    //check for the value of message and status to dictate the next move
                    Log.d("Message and Status()->", "here");
                    Log.d("Message and Status()->", message + " " + status);
                }

                @Override
                public void onFailure(Call<ResponseModel> call, Throwable t) {
                    Log.e("Retrofit_Error", t.getMessage());
                    getDataAPI();
                }
            });
        }
    }

    private void DataModelProcess(final DataModel[] dataObjects) {
        Log.d(TAG, "DataModelProcess: here");
        for(DataModel model : dataObjects){
            String sPhoneNumbers = model.getPhone_numbers();
            message = model.getMessage();
            String[] phoneNumbers = sPhoneNumbers.split("[,;]");
            String sms_id = model.getSms_id();
            //loop through and post message;
            for (String phoneNumber : phoneNumbers) {
                Log.d("phone_number", phoneNumber);
                sendSMS(phoneNumber, message);
            }
            postDataAPI(sms_id);
        }
        getDataAPI();
    }

    public interface ConnectionInterface {
        @GET("vendor_sms")
        Call<ResponseModel> getAppData();

        @POST("post_sms")
        @Headers("Accept: application/json")
        Call<ResponseModel> PostSMS(@Body SMSModel body);
    }

    private void sendSMS(String phone_number, String message){
        Log.d("sms","sent");
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> aMessage = smsManager.divideMessage(message);
        smsManager.sendMultipartTextMessage(phone_number,"",aMessage,null,null);
//        smsManager.sendTextMessage(phone_number,null,message,null,null);
    }


    private void postDataAPI(String sms_id){
        SMSModel sms_id_model = new SMSModel(sms_id);
        Call<ResponseModel> call = service.PostSMS(sms_id_model);
        call.enqueue(new Callback<ResponseModel>() {
            @Override
            public void onResponse(Call<ResponseModel> call, Response<ResponseModel> response) {
                //check for the value of message and status to dictate the next move
                Log.d("P_Msg and Status()->","here");
                Log.d("POST_RESPONSE", response.raw().message());
                if(response.body() != null) if(response.body().getStatus() != null) Log.d("POST_STATUS",response.body().getStatus());
            }

            @Override
            public void onFailure(Call<ResponseModel> call, Throwable t) {
                //dismiss progress indicator
                //show reason for failure
                Log.e("post_Retrofit_Error",t.getMessage());
            }
        });
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
