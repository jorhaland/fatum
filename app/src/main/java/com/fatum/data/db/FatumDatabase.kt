package com.fatum.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fatum.data.db.dao.*
import com.fatum.data.db.entities.*

@Database(
    entities = [
        HabitEntity::class,
        HabitExecutionEntity::class,
        TaskEntity::class,
        GoalEntity::class,
        MilestoneEntity::class,
        CalendarEventEntity::class,
        FocusSessionEntity::class
    ],
    version = 2,          // bumped from 1 — schema changed significantly
    exportSchema = false
)
abstract class FatumDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object { const val DATABASE_NAME = "fatum.db" }
}
