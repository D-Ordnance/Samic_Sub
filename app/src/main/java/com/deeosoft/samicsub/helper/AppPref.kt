package com.deeosoft.samicsub.helper

import android.content.Context
import android.content.SharedPreferences

class AppPref(context: Context) {
    val appPref = context.getSharedPreferences("tru", Context.MODE_PRIVATE)
    fun writeAppAirtimeState(airtime_state:String){
        appPref.edit().putString("airtime state", airtime_state).apply()
    }

    fun readAppAirtimeState():String{
        return appPref.getString("airtime state", null)!!
    }

    fun writeAppDataState(data_state:String){
        appPref.edit().putString("data state", data_state).apply()
    }

    fun readAppDataState():String{
        return appPref.getString("data state", null)!!
    }
}