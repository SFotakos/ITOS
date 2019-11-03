package io.github.sfotakos.itos.data.repositories.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.sfotakos.itos.data.entities.APOD


@Database(
    entities = [APOD::class],
    version = 1,
    exportSchema = false
)
abstract class ApodDb : RoomDatabase() {
    companion object {
        fun create(context: Context): ApodDb {
            val databaseBuilder = Room.databaseBuilder(context, ApodDb::class.java, "ITOS.db")
            return databaseBuilder.build()
        }
    }

    abstract fun apodDao(): ApodDao
}
