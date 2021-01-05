package com.deeosoft.samicsub.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;

public class ResponseModel {
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("data")
    @Expose
    private ArrayList<DataModel> data;

    @SerializedName("error")
    @Expose
    private String error;

    public String getMessage(){
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus(){
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ArrayList<DataModel> getData(){
        return this.data;
    }

    public String DataModelToString(DataModel[] models){
        return Arrays.toString(models);
    }

    public void setData(ArrayList<DataModel> data) {
        this.data = data;
    }

    public String getError() {
        return this.error;
    }
    public void setError(String error) {
        this.error = error;
    }
}
