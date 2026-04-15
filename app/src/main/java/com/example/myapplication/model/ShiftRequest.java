package com.example.myapplication.model;

import java.io.Serializable;

public class ShiftRequest implements Serializable {

    private String requestId;   // מזהה ב-Firebase
    private String workerId;
    private String workerName;
    private String date;        // dd/MM/yyyy
    private String startTime;   // HH:mm
    private String endTime;     // HH:mm
    private String notes;
    private String status;      // "pending" / "approved" / "rejected"
    private String shiftType;  // "בוקר" / "ערב"
    private boolean isApproved; // שמירת תאימות אחורית

    public ShiftRequest() {}

    // קונסטרקטור מלא (חדש)
    public ShiftRequest(String workerId, String workerName, String date,
                        String startTime, String endTime, String notes) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.notes = notes;
        this.status = "pending";
        this.isApproved = false;
    }

    // קונסטרקטור ישן (תאימות אחורית עם WorkerRequestShift)
    public ShiftRequest(String workerId, String workerName, String date) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.date = date;
        this.status = "pending";
        this.isApproved = false;
    }

    // --- Getters / Setters ---

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    // getId() / setId() – alias לנוחות
    public String getId() { return requestId; }
    public void setId(String id) { this.requestId = id; }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.isApproved = "approved".equals(status);
    }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) {
        this.isApproved = approved;
        if (approved) this.status = "approved";
    }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }
}
