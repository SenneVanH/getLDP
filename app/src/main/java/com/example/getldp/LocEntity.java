package com.example.getldp;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;

@Entity
public class LocEntity {
    // If the field type is long or int (or its TypeConverter converts it to a long or int), Insert methods treat 0 as not-set while inserting the item.
    // If the field's type is Integer or Long (or its TypeConverter converts it to an Integer or a Long), Insert methods treat null as not-set while inserting the item.
    @PrimaryKey(autoGenerate = true)
    private long id;
    @Expose //@Expose marks it as to include in the Gson serialisation
    private long userId;
    @Expose
    private double longitude;
    @Expose
    private double latitude;
    @Expose
    private boolean exact; //if exact is true, this means no noise has been added to it.
    @Expose
    private long epoch = 0; //milliseconds since epoch. System.currentTimeMillis() should be used in client
    @Expose
    private double radius = 0; //measure of the accuracy of the perturbed location
    private boolean synced = false;

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

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    @Override
    public String toString() {
        return "{" +
                "longitude=" + longitude +
                ", latitude=" + latitude +
                ", exact=" + exact +
                ", timestamp=" + epoch +
                ", radius=" + radius +
                ", synced=" + synced +
                '}' + "\n\n";
    }
}

