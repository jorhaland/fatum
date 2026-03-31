package com.fatum.data.db.entities

import androidx.room.*

// ─────────────────────────────────────────────────────────────────────────────
// HabitEntity  –  RF-2.x
// Two habit types: BOOLEAN (done/not-done) and VALUE (e.g. "read 60 min").
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("name")             val name: String,
    /** BOOLEAN | VALUE */
    @ColumnInfo("habit_type")       val habitType: String = "BOOLEAN",
    /** For VALUE habits: the daily target (e.g. 60 minutes). */
    @ColumnInfo("value_target")     val valueTarget: Int = 0,
    /** DAILY | WEEKLY_X | WEEKLY | MONTHLY_X | MONTHLY */
    @ColumnInfo("frequency_type")   val frequencyType: String = "DAILY",
    @ColumnInfo("frequency_target") val frequencyTarget: Int = 1,
    @ColumnInfo("current_streak")   val currentStreak: Int = 0,
    @ColumnInfo("max_streak")       val maxStreak: Int = 0,
    @ColumnInfo("created_at")       val createdAt: Long,
    @ColumnInfo("is_active")        val isActive: Boolean = true
)

// ─────────────────────────────────────────────────────────────────────────────
// HabitExecutionEntity
// One row per day per habit. For VALUE habits, value_logged accumulates
// across multiple entries in a day. We store the aggregate per day.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "habit_executions",
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habit_id"], onDelete = ForeignKey.CASCADE)],
    indices    = [Index("habit_id"), Index(["habit_id","date_completed"], unique = true)]
)
data class HabitExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("habit_id")         val habitId: Int,
    /** YYYY-MM-DD */
    @ColumnInfo("date_completed")   val dateCompleted: String,
    /** For VALUE habits: accumulated value for this day. For BOOLEAN: always 1. */
    @ColumnInfo("value_logged")     val valueLogged: Int = 1,
    @ColumnInfo("timestamp_logged") val timestampLogged: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// TaskEntity  –  Standalone tasks with HIGH/MEDIUM/LOW priority
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("title")            val title: String,
    @ColumnInfo("description")      val description: String? = null,
    /** HIGH | MEDIUM | LOW */
    @ColumnInfo("priority")         val priority: String = "MEDIUM",
    @ColumnInfo("is_completed")     val isCompleted: Boolean = false,
    @ColumnInfo("due_timestamp")    val dueTimestamp: Long? = null,
    @ColumnInfo("created_at")       val createdAt: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// GoalEntity  –  Long-term goals with MILESTONE or VALUE tracking
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("title")             val title: String,
    @ColumnInfo("description")       val description: String? = null,
    /** HIGH | MEDIUM | LOW */
    @ColumnInfo("priority")          val priority: String = "MEDIUM",
    @ColumnInfo("deadline_timestamp")val deadlineTimestamp: Long? = null,
    /** MILESTONE | VALUE */
    @ColumnInfo("goal_type")         val goalType: String = "MILESTONE",
    /** For VALUE goals: target to reach (e.g. 10000 €). */
    @ColumnInfo("target_value")      val targetValue: Float = 0f,
    /** For VALUE goals: current accumulated value. */
    @ColumnInfo("current_value")     val currentValue: Float = 0f,
    @ColumnInfo("is_completed")      val isCompleted: Boolean = false,
    @ColumnInfo("created_at")        val createdAt: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// MilestoneEntity  –  Child checkboxes for MILESTONE-type goals
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName   = "milestones",
    foreignKeys = [ForeignKey(GoalEntity::class, ["id"], ["goal_id"], onDelete = ForeignKey.CASCADE)],
    indices     = [Index("goal_id")]
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("goal_id")           val goalId: Int,
    @ColumnInfo("title")             val title: String,
    @ColumnInfo("is_completed")      val isCompleted: Boolean = false,
    @ColumnInfo("order_index")       val orderIndex: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// CalendarEventEntity  –  RF-3.x  (local cache + GCal imports)
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "calendar_events",
    indices   = [Index("start_timestamp")]
)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Non-null for events imported from GCal. */
    @ColumnInfo("gcal_event_id")     val gcalEventId: String? = null,
    @ColumnInfo("title")             val title: String,
    @ColumnInfo("description")       val description: String? = null,
    /** HIGH | MEDIUM | LOW */
    @ColumnInfo("importance")        val importance: String = "MEDIUM",
    @ColumnInfo("start_timestamp")   val startTimestamp: Long,
    @ColumnInfo("end_timestamp")     val endTimestamp: Long,
    /** RFC 5545 RRULE string, e.g. "FREQ=WEEKLY;BYDAY=MO,WE,FR" */
    @ColumnInfo("recurrence_rule")   val recurrenceRule: String? = null,
    /** True if originally imported from GCal (edits stay local). */
    @ColumnInfo("is_from_gcal")      val isFromGcal: Boolean = false,
    /** True if this event was locally edited after GCal import. */
    @ColumnInfo("is_locally_modified") val isLocallyModified: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// FocusSessionEntity  –  Deep Work sessions (timer)
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("start_timestamp")   val startTimestamp: Long,
    @ColumnInfo("end_timestamp")     val endTimestamp: Long,
    @ColumnInfo("duration_minutes")  val durationMinutes: Int,
    /** COMPLETED | FAILED_INTERRUPTED */
    @ColumnInfo("status")            val status: String
)
