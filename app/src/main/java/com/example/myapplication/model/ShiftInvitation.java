package com.example.myapplication.model;

import java.io.Serializable;
import java.util.List;

public class ShiftInvitation implements Serializable {

    private String id;
    private String date;       // dd/MM/yyyy
    private String shiftType;  // "בוקר" / "ערב"
    private long   createdAt;
    private boolean open;      // true = פתוחה לבקשות
    private List<String> targetWorkerIds; // null = כל העובדים

    public ShiftInvitation() {}

    public ShiftInvitation(String date, String shiftType) {
        this.date      = date;
        this.shiftType = shiftType;
        this.createdAt = System.currentTimeMillis();
        this.open      = true;
    }

    public String getId()           { return id; }
    public void   setId(String id)  { this.id = id; }

    public String getDate()             { return date; }
    public void   setDate(String date)  { this.date = date; }

    public String getShiftType()                { return shiftType; }
    public void   setShiftType(String t)        { this.shiftType = t; }

    public long getCreatedAt()          { return createdAt; }
    public void setCreatedAt(long t)    { this.createdAt = t; }

    public boolean isOpen()             { return open; }
    public void    setOpen(boolean b)   { this.open = b; }

    public List<String> getTargetWorkerIds()                     { return targetWorkerIds; }
    public void         setTargetWorkerIds(List<String> ids)     { this.targetWorkerIds = ids; }
}
