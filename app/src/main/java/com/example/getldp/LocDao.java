package com.example.getldp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocDao {

    @Query("SELECT * FROM locentity")
    LiveData<List<LocEntity>> loadAll();

    @Query("SELECT * FROM locentity WHERE synced=0")
    List<LocEntity> loadAllNotSynced();

    @Insert(onConflict = OnConflictStrategy.IGNORE) //will keep the existing record
    void insertAll(LocEntity... locEntities);

    @Update
    void updateToSynced(LocEntity... locEntities);

}
