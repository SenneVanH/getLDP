package com.example.getldp;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {LocEntity.class}, version = 3)
public abstract class GetldpDatabase extends RoomDatabase{

    public abstract LocDao locDao();
}
