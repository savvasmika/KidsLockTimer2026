package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.PairedChildDevice
import com.example.model.TempUnlockCode
import com.example.model.UnlockRequest

@Database(
    entities = [
        PairedChildDevice::class,
        UnlockRequest::class,
        TempUnlockCode::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun unlockRequestDao(): UnlockRequestDao
    abstract fun tempUnlockCodeDao(): TempUnlockCodeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kidlock_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
