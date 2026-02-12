package com.example.myapplication.model;

import androidx.annotation.NonNull;

public class Shift {
    private String date;
    private String type;
    private String notes;

    // קונסטרקטור ריק עבור Firebase
    public Shift() {}

    // קונסטרקטור מלא
    public Shift(String date, String type, String notes) {
        this.date = date;
        this.type = type;
        this.notes = notes;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    // מתודה תואמת לשגיאה שקיבלת כדי שהקומפילציה תעבור
    public void setShiftTime(String date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @NonNull
    @Override
    public String toString() {
        return "Shift{" +
                "date='" + date + '\'' +
                ", type='" + type + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}