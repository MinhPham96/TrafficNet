package com.example.minh.trafficnet;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Minh on 20/11/2017.
 */



public class StreetPoint {

    private String name;        //name of the street
    private int status;         //status of the street
    //a format to display the modified day time
    static SimpleDateFormat dateFormat = new SimpleDateFormat("E dd.MM.yy 'at' HH:mm:ss");
    private Date timeModified;  //the modified day time
    private double latitude;    //the latitude of the street
    private double longitude;   //the longitude of the street
    private String editedBy;    //the user who edited the street status

    //constructors
    public StreetPoint() {
    }

    public StreetPoint(String name, double latitude, double longitude, int status, String editedBy, Date timeModified) {
        this.name = name;
        this.status = status;
        this.editedBy = editedBy;
        this.timeModified = timeModified;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    //name, latitude and longitude cannot be changed so these instances only have get method
    //other than that the other have both get and set method
    //some print method is used to get the preferred string format to display on the app

    public String getName() {
        return name;
    }

    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String user) { this.editedBy = user; }

    public  String printEditedBy() { return "Edited By: " + editedBy; }

    public String printStatus() {
        String status_str = "DEFAULT";
        switch(this.status) {
            default:
                status_str = "NORMAL";
                break;
            case 0:
                status_str = "LIGHT";
                break;
            case 2:
                status_str = "BUSY";
                break;
        }
        return status_str;
    }

    public int getStatus() {return status;}

    public void setStatus(int newStatus) {
        this.status = newStatus;
    }

    public String printTimeModified() {
        return "Last Modified: " + dateFormat.format(timeModified);
    }

    public Date getTimeModified() {
        if(timeModified != null) return timeModified;
        else return  null;
    }

    public void setTimeModified(Date newTime) {
        timeModified = newTime;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

}
