package com.example.myapplication.model;

import java.util.Date;
import java.util.List;

public class Shift {
    protected String id;
    protected int dayInWeek;
    protected Date date;
    protected Worker mngr;
    protected String shiftTime;
    protected int workerNeeded;
    protected List<Worker> workerList;
    protected String status;

    public Shift(String id, int dayInWeek, Date date, Worker mngr, String shiftTime, int workerNeeded, List<Worker> workerList, String status) {
        this.id = id;
        this.dayInWeek = dayInWeek;
        this.date = date;
        this.mngr = mngr;
        this.shiftTime = shiftTime;
        this.workerNeeded = workerNeeded;
        this.workerList = workerList;
        this.status = status;
    }

    public Shift() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getDayInWeek() { return dayInWeek; }
    public void setDayInWeek(int dayInWeek) { this.dayInWeek = dayInWeek; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public Worker getMngr() { return mngr; }
    public void setMngr(Worker mngr) { this.mngr = mngr; }

    public List<Worker> getWorkerList() { return workerList; }




    public void setWorkerList(List<Worker> workerList) { this.workerList = workerList; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getShiftTime() { return shiftTime; }
    public void setShiftTime(String shiftTime) { this.shiftTime = shiftTime; }

    public int getWorkerNeeded() { return workerNeeded; }
    public void setWorkerNeeded(int workerNeeded) { this.workerNeeded = workerNeeded; }

    @Override
    public String toString() {
        return "Shift{" +
                "id='" + id + '\'' +
                ", dayInWeek=" + dayInWeek +
                ", date=" + date +
                ", mngr=" + mngr +
                ", shiftTime='" + shiftTime + '\'' +
                ", workerNeeded=" + workerNeeded +
                ", workerList=" + workerList +
                ", status='" + status + '\'' +
                '}';
    }
}
