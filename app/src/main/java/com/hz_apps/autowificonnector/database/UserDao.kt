package com.hz_apps.autowificonnector.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Query("SELECT * FROM wifi_configurations ORDER BY `order` ASC")
    fun getAll(): LiveData<List<WifiConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(wifiConfig: WifiConfig)

    @Query("DELETE FROM wifi_configurations WHERE id=:id")
    fun deleteItem(id: Int)

    @Update
    fun update(wifiConfig: WifiConfig)

    @Query("UPDATE wifi_configurations SET `order` = :order WHERE id = :id")
    fun updateOrder(id: Int, order: Int)

    @Query("SELECT MAX(`order`) FROM wifi_configurations")
    fun getMaxOrder(): Int

}