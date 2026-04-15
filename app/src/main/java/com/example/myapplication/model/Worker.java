package com.example.myapplication.model;

import java.io.Serializable;

public class Worker implements Serializable {

    protected String id;
    protected String fName;
    protected String lName;
    protected String phone;
    protected String email;
    protected String pass;
    protected String jobTitle;
    protected boolean isAdmin;

    public Worker(String id, String fName, String lName, String phone, String email,
                  String pass, String jobTitle, boolean isAdmin) {
        this.id = id;
        this.fName = fName;
        this.lName = lName;
        this.phone = phone;
        this.email = email;
        this.pass = pass;
        this.jobTitle = jobTitle;
        this.isAdmin = isAdmin;
    }

    public Worker() {}

    public String getPass() { return pass; }
    public void setPass(String pass) { this.pass = pass; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getlName() { return lName; }
    public void setlName(String lName) { this.lName = lName; }

    public String getfName() { return fName; }
    public void setfName(String fName) { this.fName = fName; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public boolean isAdmin() { return isAdmin; }
    public boolean getIsAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
    public void setIsAdmin(boolean admin) { isAdmin = admin; }

    @Override
    public String toString() {
        return "Worker{id='" + id + "', fName='" + fName + "', lName='" + lName + "'}";
    }
}
