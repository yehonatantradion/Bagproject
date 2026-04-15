package com.example.myapplication.services;

import com.example.myapplication.model.Worker;

import java.util.Date;
import java.util.List;

public class Shift {
    protected String id;
    protected String dayOfWeek0;
    protected Date date;
    protected Worker shiftMan;
    protected List <Worker> workerList;
    protected String status;
    protected String shiftType;

    @Override
    public String toString() {
        return "Shift{" +
                "id='" + id + '\'' +
                ", dayOfWeek0='" + dayOfWeek0 + '\'' +
                ", date=" + date +
                ", shiftMan=" + shiftMan +
                ", workerList=" + workerList +
                ", status='" + status + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDayOfWeek0() {
        return dayOfWeek0;
    }

    public void setDayOfWeek0(String dayOfWeek0) {
        this.dayOfWeek0 = dayOfWeek0;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Worker getShiftMan() {
        return shiftMan;
    }

    public void setShiftMan(Worker shiftMan) {
        this.shiftMan = shiftMan;
    }

    public List<Worker> getWorkerList() {
        return workerList;
    }

    public void setWorkerList(List<Worker> workerList) {
        this.workerList = workerList;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public Shift() {
    }

    public Shift(String id, String dayOfWeek0, Date date, Worker shiftMan, List<Worker> workerList, String status) {
        this.id = id;
        this.dayOfWeek0 = dayOfWeek0;
        this.date = date;
        this.shiftMan = shiftMan;
        this.workerList = workerList;
        this.status = status;
    }
}
