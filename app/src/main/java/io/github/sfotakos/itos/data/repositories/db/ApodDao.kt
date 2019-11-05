package io.github.sfotakos.itos.data.repositories.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.sfotakos.itos.data.entities.APOD

@Dao
interface ApodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertApod(posts : APOD)

    @Query("SELECT * FROM APOD")
    fun queryAllApods() : DataSource.Factory<Int, APOD>
}