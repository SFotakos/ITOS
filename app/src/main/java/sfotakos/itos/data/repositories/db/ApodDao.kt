package sfotakos.itos.data.repositories.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import sfotakos.itos.data.entities.APOD

@Dao
interface ApodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertApod(posts: APOD)

    @Query("SELECT * FROM APOD ORDER BY DATE DESC")
    fun queryAllApodsDataSource(): DataSource.Factory<Int, APOD>

    @Query("SELECT * FROM APOD ORDER BY DATE DESC")
    fun queryAllApods(): List<APOD>

    @Query("SELECT * FROM APOD WHERE DATE = :selectedDate ORDER BY DATE DESC")
    fun queryByDate(selectedDate: String): APOD

    @Query("DELETE FROM APOD")
    fun deleteAll()
}