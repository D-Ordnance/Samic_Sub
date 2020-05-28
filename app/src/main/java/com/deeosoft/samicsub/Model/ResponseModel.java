package com.deeosoft.samicsub.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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
    private DataModel[] data;

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

    public DataModel[] getData(){
        return this.data;
    }

    public String DataModelToString(DataModel[] models){
        return Arrays.toString(models);
    }

    public void setData(DataModel[] data) {
        this.data = data;
    }
}
