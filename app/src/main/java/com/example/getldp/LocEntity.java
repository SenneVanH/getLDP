package com.example.getldp;


import androidx.annotation.Nullable;

public class LocEntity {
    private long userId;
    private double longitude;
    private double latitude;
    private boolean exact; //if exact is true, this means no noise has been added to it.
    @Nullable
    private Long epoch; //milliseconds since epoch. System.currentTimeMillis() should be used in client
    private double radius = 0; //measure of the accuracy of the perturbed location

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public long getEpoch() {
        return epoch;
    }

    public void setEpoch(long epoch) {
        this.epoch = epoch;
    }

    public boolean isExact() {
        return exact;
    }

    public void setExact(boolean exact) {
        this.exact = exact;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

}

