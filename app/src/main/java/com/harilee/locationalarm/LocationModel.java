package com.harilee.locationalarm;

import java.util.ArrayList;

public class LocationModel {

    private String locationName;
    private String lat;
    private String lon;
    private String audioFile;
    private String count;


    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(String audioFile) {
        this.audioFile = audioFile;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
