package com.deeosoft.samicsub.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class DataModel {
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("sms_id")
    @Expose
    private String sms_id;
    @SerializedName("user_id")
    @Expose
    private String user_id;
    @SerializedName("vendor_id")
    @Expose
    private String vendor_id;
    @SerializedName("sender_id")
    @Expose
    private String sender_id;
    @SerializedName("phone_numbers")
    @Expose
    private String phone_numbers;
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("page_num")
    @Expose
    private String page_num;
    @SerializedName("amount_deducted")
    @Expose
    private String amount_deducted;
    @SerializedName("status")
    @Expose
    private int status;
    @SerializedName("new_balance")
    @Expose
    private String new_balance;
    @SerializedName("when_sent")
    @Expose
    private String when_sent;
    @SerializedName("wallet_balance")
    @Expose
    private String wallet_balance;
    @SerializedName("user_plan")
    @Expose
    private String user_plan;
    @SerializedName("vendor_name")
    @Expose
    private String vendor_name;
    @SerializedName("phone_number")
    @Expose
    private String phone_number;
    @SerializedName("email_address")
    @Expose
    private String email_address;
    @SerializedName("deleted")
    @Expose
    private String deleted;
    @SerializedName("password")
    @Expose
    private String password;
    @SerializedName("rave_public_key")
    @Expose
    private String rave_public_key;
    @SerializedName("rave_private_key")
    @Expose
    private String rave_private_key;
    @SerializedName("bank_details")
    @Expose
    private String bank_details;
    @SerializedName("slug")
    @Expose
    private String slug;
    @SerializedName("when_created")
    @Expose
    private String when_created;
    @SerializedName("full_name")
    @Expose
    private String full_name;
    @SerializedName("transaction_id")
    @Expose
    private String transaction_id;
    @SerializedName("string")
    @Expose
    private String string;
    @SerializedName("ussd_string")
    @Expose
    private String ussd_string;
    @SerializedName("balance_ussd")
    @Expose
    private String balance_ussd;

    public String getId(){
        return this.id;
    }

    public void setId(String Id) {
        this.id = Id;
    }

    public String getSms_id() {
        return sms_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getVendor_id() {
        return vendor_id;
    }

    public void setVendor_id(String vendor_id) {
        this.vendor_id = vendor_id;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

    public String getPhone_numbers() {
        return phone_numbers;
    }

    public void setPhone_numbers(String phone_numbers) {
        this.phone_numbers = phone_numbers;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPage_num() {
        return page_num;
    }

    public void setPage_num(String page_num) {
        this.page_num = page_num;
    }

    public String getAmount_deducted() {
        return amount_deducted;
    }

    public void setAmount_deducted(String amount_deducted) {
        this.amount_deducted = amount_deducted;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getNew_balance() {
        return new_balance;
    }

    public void setNew_balance(String new_balance) {
        this.new_balance = new_balance;
    }

    public String getWhen_sent() {
        return when_sent;
    }

    public void setWhen_sent(String when_sent) {
        this.when_sent = when_sent;
    }

    public String getWallet_balance() {
        return wallet_balance;
    }

    public void setWallet_balance(String wallet_balance) {
        this.wallet_balance = wallet_balance;
    }

    public String getUser_plan() {
        return user_plan;
    }

    public void setUser_plan(String user_plan) {
        this.user_plan = user_plan;
    }

    public String getVendor_name() {
        return vendor_name;
    }

    public void setVendor_name(String vendor_name) {
        this.vendor_name = vendor_name;
    }

    public void setSms_id(String sms_id) {
        this.sms_id = sms_id;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getEmail_address() {
        return email_address;
    }

    public void setEmail_address(String email_address) {
        this.email_address = email_address;
    }

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRave_public_key() {
        return rave_public_key;
    }

    public void setRave_public_key(String rave_public_key) {
        this.rave_public_key = rave_public_key;
    }

    public String getRave_private_key() {
        return rave_private_key;
    }

    public void setRave_private_key(String rave_private_key) {
        this.rave_private_key = rave_private_key;
    }

    public String getBank_details() {
        return bank_details;
    }

    public void setBank_details(String bank_details) {
        this.bank_details = bank_details;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getWhen_created() {
        return when_created;
    }

    public void setWhen_created(String when_created) {
        this.when_created = when_created;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getTransaction_id() {
        return transaction_id;
    }

    public void setTransaction_id(String transaction_id) {
        this.transaction_id = transaction_id;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public String getUSSDString() {
        return ussd_string;
    }

    public void setUSSDString(String ussd_string) {
        this.ussd_string = ussd_string;
    }

    public String getBalanceUSSD() {
        return balance_ussd;
    }

    public void setBalanceUSSD(String balance_ussd) {
        this.balance_ussd = balance_ussd;
    }
}
