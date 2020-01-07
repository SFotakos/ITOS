package sfotakos.itos.data.repositories.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import sfotakos.itos.data.entities.APOD

@Database(
    entities = [APOD::class],
    version = 1,
    exportSchema = false
)
abstract class ContinuityDb : RoomDatabase() {
    companion object {
        fun create(context: Context): ContinuityDb {
            val databaseBuilder =
                Room.databaseBuilder(context, ContinuityDb::class.java, "continuity.db")
            return databaseBuilder.build()
        }
    }

    abstract fun apodDao(): ApodDao
}
