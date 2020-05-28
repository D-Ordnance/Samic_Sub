package com.deeosoft.samicsub.Services;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.deeosoft.samicsub.Model.DataModel;
import com.deeosoft.samicsub.Model.ResponseModel;
import com.deeosoft.samicsub.Model.TransactionIdModel;
import com.deeosoft.samicsub.tool.UnsafeOkHttpClient;

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

public class ListenForNewUSSDData extends Service {
    private static final String TAG = "ListenForNewUSSDData";
    TelephonyManager.UssdResponseCallback telephonyCallback;
    String status,message,sms_id,transaction_id,ussd_message;
    ListenForNewUSSDData.ConnectionInterface service;
    private volatile boolean destroy = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://superadmin.mtncug.com/api/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(ListenForNewUSSDData.ConnectionInterface.class);

        telephonyCallback = new TelephonyManager.UssdResponseCallback() {
            /**
             * Called when a USSD request has succeeded.  The {@code response} contains the USSD
             * response received from the network.  The calling app can choose to either display the
             * response to the user or perform some operation based on the response.
             * <p>
             * USSD responses are unstructured text and their content is determined by the mobile network
             * operator.
             *
             * @param telephonyManager the TelephonyManager the callback is registered to.
             * @param request          the USSD request sent to the mobile network.
             * @param response         the response to the USSD request provided by the mobile network.
             **/
            @Override
            public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                super.onReceiveUssdResponse(telephonyManager, request, response);
                Log.d(TAG, response.toString());
                postDataAPI(transaction_id);
                getDataAPI();
            }
        };
    }

    private void getDataAPI(){
        if(!destroy) {
            Call<ResponseModel> call = service.getAppData();
            call.enqueue(new Callback<ResponseModel>() {
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
                    Log.e("get_Retrofit_Error", t.getMessage());
                }
            });
        }
    }

    private void DataModelProcess(final DataModel[] dataObjects) {
        for(DataModel dataModel: dataObjects){
            transaction_id = dataModel.getTransaction_id();
            ussd_message = dataModel.getUSSDString();
            Log.d("USSD=>",ussd_message);
            sendUSSD(ussd_message);
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

    private void postDataAPI(String transaction_id){
        TransactionIdModel transactionIdModel = new TransactionIdModel(transaction_id);
        Call<ResponseModel> call = service.PostAirTime(transactionIdModel);
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

    public interface ConnectionInterface {
        @GET("data_bundle")
        Call<ResponseModel> getAppData();

        @POST("data_bundle/done")
        @Headers("Accept: application/json")
        Call<ResponseModel> PostAirTime(@Body TransactionIdModel body);
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
        getDataAPI();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroy = true;
    }
}
