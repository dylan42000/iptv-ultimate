package com.dylandos.iptv.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dylandos.iptv.data.dao.AccountDao
import com.dylandos.iptv.data.dao.CategoryDao
import com.dylandos.iptv.data.dao.ChannelDao
import com.dylandos.iptv.data.dao.ProgramDao
import com.dylandos.iptv.data.dao.RecordingDao
import com.dylandos.iptv.data.entity.AccountEntity
import com.dylandos.iptv.data.entity.CategoryEntity
import com.dylandos.iptv.data.entity.ChannelEntity
import com.dylandos.iptv.data.entity.ProgramEntity
import com.dylandos.iptv.data.entity.RecordingEntity

/** Converters for types Room cannot natively persist. */
class Converters {
    @TypeConverter
    fun fromLongList(value: List<Long>?): String = value?.joinToString(",") ?: ""

    @TypeConverter
    fun toLongList(value: String): List<Long> =
        if (value.isBlank()) emptyList() else value.split(",").mapNotNull { it.toLongOrNull() }
}

@Database(
    entities = [
        AccountEntity::class,
        ChannelEntity::class,
        CategoryEntity::class,
        ProgramEntity::class,
        RecordingEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun channelDao(): ChannelDao
    abstract fun categoryDao(): CategoryDao
    abstract fun programDao(): ProgramDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        const val DB_NAME = "dylandos.db"

        /** Migration from schema version 0 -> 1 (fresh install bootstrap). */
        private val MIGRATION_0_1 = object : Migration(0, 1) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fresh schema is created automatically by Room; nothing to do.
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_0_1)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
