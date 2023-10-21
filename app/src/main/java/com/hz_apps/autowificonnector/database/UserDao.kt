package com.hz_apps.autowificonnector.database

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT * FROM wifi_configurations")
    fun getAll() : LiveData<List<WifiConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(wifiConfig : WifiConfig)

    @Query("DELETE FROM wifi_configurations WHERE id=:id")
    fun deleteItem(id : Int)
}