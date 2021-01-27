package com.deeosoft.samicsub.Model;

public class TransactionIdModel {
    private String transaction_id;
    public String balance_before;
    public String balance_after;
    public String network;
    public String screen_message;
    public String transaction_type;

    public TransactionIdModel(String transaction_id){
        this.transaction_id = transaction_id;
    }

    public TransactionIdModel(String transaction_id, String balance_before, String balance_after, String network, String screen_message, String transaction_type){
        this.transaction_id = transaction_id;
        this.balance_before = balance_before;
        this.balance_after = balance_after;
        this.network = network;
        this.screen_message = screen_message;
        this.transaction_type = transaction_type;
    }
}
