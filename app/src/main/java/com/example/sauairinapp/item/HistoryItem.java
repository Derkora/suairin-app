package com.example.sauairinapp.item;

public class HistoryItem {
    private final String dbValue;
    private final String date;
    private final String time;

    public HistoryItem(String dbValue, String date, String time) {
        this.dbValue = dbValue;
        this.date = date;
        this.time = time;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }
}
