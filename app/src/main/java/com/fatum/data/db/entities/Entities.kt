package com.fatum.data.db.entities

import androidx.room.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("name")             val name: String,
    @ColumnInfo("habit_type")       val habitType: String = "BOOLEAN",
    @ColumnInfo("value_target")     val valueTarget: Int = 0,
    @ColumnInfo("frequency_type")   val frequencyType: String = "DAILY",
    @ColumnInfo("frequency_target") val frequencyTarget: Int = 1,
    @ColumnInfo("current_streak")   val currentStreak: Int = 0,
    @ColumnInfo("max_streak")       val maxStreak: Int = 0,
    @ColumnInfo("created_at")       val createdAt: Long,
    @ColumnInfo("is_active")        val isActive: Boolean = true
)

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
        Index(value = ["habit_id", "date_completed"], unique = true)
    ]
)
data class HabitExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("habit_id")         val habitId: Int,
    @ColumnInfo("date_completed")   val dateCompleted: String,
    @ColumnInfo("value_logged")     val valueLogged: Int = 1,
    @ColumnInfo("timestamp_logged") val timestampLogged: Long
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("title")            val title: String,
    @ColumnInfo("description")      val description: String? = null,
    @ColumnInfo("priority")         val priority: String = "MEDIUM",
    @ColumnInfo("is_completed")     val isCompleted: Boolean = false,
    @ColumnInfo("due_timestamp")    val dueTimestamp: Long? = null,
    @ColumnInfo("created_at")       val createdAt: Long
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("title")             val title: String,
    @ColumnInfo("description")       val description: String? = null,
    @ColumnInfo("priority")          val priority: String = "MEDIUM",
    @ColumnInfo("deadline_timestamp")val deadlineTimestamp: Long? = null,
    @ColumnInfo("goal_type")         val goalType: String = "MILESTONE",
    @ColumnInfo("target_value")      val targetValue: Float = 0f,
    @ColumnInfo("current_value")     val currentValue: Float = 0f,
    @ColumnInfo("is_completed")      val isCompleted: Boolean = false,
    @ColumnInfo("created_at")        val createdAt: Long
)

@Entity(
    tableName   = "milestones",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices     = [Index(value = ["goal_id"])]
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("goal_id")           val goalId: Int,
    @ColumnInfo("title")             val title: String,
    @ColumnInfo("is_completed")      val isCompleted: Boolean = false,
    @ColumnInfo("order_index")       val orderIndex: Int = 0
)

@Entity(
    tableName = "calendar_events",
    indices   = [Index(value = ["start_timestamp"])]
)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("gcal_event_id")     val gcalEventId: String? = null,
    @ColumnInfo("title")             val title: String,
    @ColumnInfo("description")       val description: String? = null,
    @ColumnInfo("importance")        val importance: String = "MEDIUM",
    @ColumnInfo("start_timestamp")   val startTimestamp: Long,
    @ColumnInfo("end_timestamp")     val endTimestamp: Long,
    @ColumnInfo("recurrence_rule")   val recurrenceRule: String? = null,
    @ColumnInfo("is_from_gcal")      val isFromGcal: Boolean = false,
    @ColumnInfo("is_locally_modified") val isLocallyModified: Boolean = false
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo("start_timestamp")   val startTimestamp: Long,
    @ColumnInfo("end_timestamp")     val endTimestamp: Long,
    @ColumnInfo("duration_minutes")  val durationMinutes: Int,
    @ColumnInfo("status")            val status: String
)

fun CalendarEventEntity.occursOn(targetDate: LocalDate): Boolean {
    val startDate = Instant.ofEpochMilli(this.startTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()

    // Si no es recurrente, debe coincidir exactamente el día
    if (this.recurrenceRule.isNullOrBlank()) {
        return startDate == targetDate
    }

    // Si el evento empieza en el futuro, aún no ocurre
    if (startDate.isAfter(targetDate)) return false

    // Comprobar si ya superó la fecha límite (UNTIL)
    val untilStr = this.recurrenceRule.substringAfter("UNTIL=", "").substringBefore(";")
    if (untilStr.isNotEmpty()) {
        try {
            val untilDate = LocalDate.parse(untilStr.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"))
            if (targetDate.isAfter(untilDate)) return false
        } catch (e: Exception) {}
    }

    // Evaluar la regla de recurrencia
    return when {
        this.recurrenceRule.contains("FREQ=DAILY") -> true
        this.recurrenceRule.contains("FREQ=WEEKLY") -> startDate.dayOfWeek == targetDate.dayOfWeek
        this.recurrenceRule.contains("FREQ=MONTHLY") -> startDate.dayOfMonth == targetDate.dayOfMonth
        else -> false
    }
}