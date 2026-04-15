package com.example.myapplication.model;

import java.io.Serializable;

public class Attendance implements Serializable {

    private String id;
    private String workerId;
    private String workerName;
    private String date;         // dd/MM/yyyy
    private long checkInTime;    // System.currentTimeMillis()
    private boolean confirmed;

    public Attendance() {}

    public Attendance(String id, String workerId, String workerName,
                      String date, long checkInTime, boolean confirmed) {
        this.id = id;
        this.workerId = workerId;
        this.workerName = workerName;
        this.date = date;
        this.checkInTime = checkInTime;
        this.confirmed = confirmed;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getCheckInTime() { return checkInTime; }
    public void setCheckInTime(long checkInTime) { this.checkInTime = checkInTime; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
}
