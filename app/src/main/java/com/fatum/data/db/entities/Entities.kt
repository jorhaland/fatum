package com.fatum.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────────────────────────────────────
// LogEntity  –  RF-1.1 / RF-1.2 / RF-1.4
// Represents a single micro-log entry in the diary.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Raw text content written by the user. May contain #hashtags. */
    @ColumnInfo(name = "content") val content: String,
    /** Unix epoch millis – used for precise ordering in the timeline. */
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    /** ISO-8601 date "YYYY-MM-DD" – used for fast date-group queries. */
    @ColumnInfo(name = "date_string") val dateString: String,
    /**
     * Comma-separated hashtag list, e.g. "gym,work,ideas".
     * Stored as plain text; split in-memory when needed.
     */
    @ColumnInfo(name = "tags") val tags: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// DailyMoodEntity  –  RF-1.3
// Only one mood entry per day (date_string is the PK).
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "daily_mood")
data class DailyMoodEntity(
    /** "YYYY-MM-DD" – acting as PK ensures one row per calendar day. */
    @PrimaryKey @ColumnInfo(name = "date_string") val dateString: String,
    /** Mood score 1–10.  Last write wins (UPSERT). */
    @ColumnInfo(name = "score") val score: Int,
    /** Epoch millis of the last update for display purposes. */
    @ColumnInfo(name = "timestamp") val timestamp: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// HabitEntity  –  RF-2.1 / RF-2.3
// Defines a habit's metadata and cached streak values.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    /**
     * Frequency strategy – one of:
     * DAILY | WEEKLY_X_TIMES | WEEKLY | MONTHLY_X_TIMES | MONTHLY
     */
    @ColumnInfo(name = "frequency_type") val frequencyType: String,
    /** Target count per cycle (e.g. 3 for "3 times a week"). */
    @ColumnInfo(name = "frequency_target") val frequencyTarget: Int = 1,
    /** Visual priority 1–3 stars. */
    @ColumnInfo(name = "priority_stars") val priorityStars: Int = 1,
    /** Current active streak – recalculated after each retroactive edit. */
    @ColumnInfo(name = "current_streak") val currentStreak: Int = 0,
    /** All-time best streak. */
    @ColumnInfo(name = "max_streak") val maxStreak: Int = 0,
    /** Creation timestamp in epoch millis. */
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** Whether the habit is still active (soft-delete). */
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

// ─────────────────────────────────────────────────────────────────────────────
// HabitExecutionEntity  –  RF-2.2
// One row per completion event. Supports retroactive logging.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "habit_executions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habit_id"]),
        Index(value = ["habit_id", "date_completed"], unique = true) // one entry per day per habit
    ]
)
data class HabitExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "habit_id") val habitId: Int,
    /** The logical date this completion counts for (YYYY-MM-DD). */
    @ColumnInfo(name = "date_completed") val dateCompleted: String,
    /** Actual wall-clock time the row was written. */
    @ColumnInfo(name = "timestamp_logged") val timestampLogged: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// GoalEntity  –  RF-4.1 / RF-4.2
// Long-term goal container.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "priority_stars") val priorityStars: Int = 1,
    @ColumnInfo(name = "deadline_timestamp") val deadlineTimestamp: Long? = null,
    /** 0.0–100.0 – updated automatically when child tasks are completed. */
    @ColumnInfo(name = "progress_percentage") val progressPercentage: Float = 0f,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// TaskEntity  –  RF-4.1 / RF-4.3 / RF-4.4
// Short-term task; optionally linked to a parent Goal.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goal_id"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Null means the task is "orphaned" (plain To-Do). */
    @ColumnInfo(name = "goal_id") val goalId: Int? = null,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "priority_stars") val priorityStars: Int = 1,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "due_timestamp") val dueTimestamp: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// CalendarEventEntity  –  RF-3.1 / RF-3.2 / RF-3.3 / RF-3.4
// Local cache of Google Calendar events + native events.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "calendar_events",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["assigned_goal_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["assigned_goal_id"]), Index(value = ["start_timestamp"])]
)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** ID returned by Google Calendar API (null for local-only events). */
    @ColumnInfo(name = "gcal_event_id") val gcalEventId: String? = null,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "start_timestamp") val startTimestamp: Long,
    @ColumnInfo(name = "end_timestamp") val endTimestamp: Long,
    /** RFC 5545 RRULE string for recurring events, e.g. "FREQ=WEEKLY;BYDAY=MO,WE". */
    @ColumnInfo(name = "recurrence_rule") val recurrenceRule: String? = null,
    @ColumnInfo(name = "priority_stars") val priorityStars: Int = 1,
    /** Set when this event is a time-block for a specific Goal. */
    @ColumnInfo(name = "assigned_goal_id") val assignedGoalId: Int? = null,
    /** Tracks whether the event originates from GCal (true) or is local (false). */
    @ColumnInfo(name = "is_from_gcal") val isFromGcal: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// FocusSessionEntity  –  RF-5.1 / RF-5.4
// Records each Deep Work timer session and its outcome.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "start_timestamp") val startTimestamp: Long,
    @ColumnInfo(name = "end_timestamp") val endTimestamp: Long,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    /**
     * Session outcome:
     *  - COMPLETED      – timer ran to zero without interruption
     *  - FAILED_INTERRUPTED – user tapped "Interrupt session"
     */
    @ColumnInfo(name = "status") val status: String // "COMPLETED" | "FAILED_INTERRUPTED"
)
