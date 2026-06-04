package com.mc.mateamhf.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ConcertStateEntity::class, FriendPickEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun concertStateDao(): ConcertStateDao
    abstract fun friendPickDao(): FriendPickDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hellfest.db",
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS friend_pick (
                        friendName TEXT NOT NULL,
                        artistKey TEXT NOT NULL,
                        artistDisplay TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(friendName, artistKey)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
