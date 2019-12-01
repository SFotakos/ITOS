package sfotakos.itos.data.repositories.db

import androidx.paging.DataSource
import androidx.room.*
import sfotakos.itos.data.entities.APOD

@Dao
interface ApodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertApod(posts: APOD)

    @Query("SELECT * FROM APOD ORDER BY DATE DESC")
    fun queryAllApods(): DataSource.Factory<Int, APOD>

    @Query("SELECT * FROM APOD WHERE DATE = :selectedDate ORDER BY DATE DESC")
    fun queryByDate(selectedDate: String): APOD

    @Query("DELETE FROM APOD")
    fun deleteAll()
}