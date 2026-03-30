package com.fatum.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fatum.data.db.dao.*
import com.fatum.data.db.entities.*

/**
 * Central Room database for FATUM.
 *
 * • exportSchema = true  → generates a JSON schema file under app/schemas/
 *   which is useful for migration testing and version diffs.
 * • All entities are listed here.  Bump [version] and provide a [Migration]
 *   whenever the schema changes in a production build.
 */
@Database(
    entities = [
        LogEntity::class,
        DailyMoodEntity::class,
        HabitEntity::class,
        HabitExecutionEntity::class,
        GoalEntity::class,
        TaskEntity::class,
        CalendarEventEntity::class,
        FocusSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FatumDatabase : RoomDatabase() {

    abstract fun logDao(): LogDao
    abstract fun moodDao(): MoodDao
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun taskDao(): TaskDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        const val DATABASE_NAME = "fatum.db"
    }
}
