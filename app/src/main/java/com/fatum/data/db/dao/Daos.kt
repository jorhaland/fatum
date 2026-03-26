package com.fatum.data.db.dao

import androidx.room.*
import com.fatum.data.db.entities.*
import kotlinx.coroutines.flow.Flow
import androidx.annotation.Keep
import androidx.room.ColumnInfo

@Keep
data class DateCount(
    @ColumnInfo(name = "date_string") val date_string: String,
    @ColumnInfo(name = "count") val count: Int
)

// ─────────────────────────────────────────────────────────────────────────────
// LogDao  –  RF-1.1, RF-1.4, RF-1.5, RF-1.6
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface LogDao {

    /** Insert a new log. Returns the generated row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntity): Long

    /** Update an existing log (RF-1.6). */
    @Update
    suspend fun update(log: LogEntity)

    /** Soft or hard delete (RF-1.6). */
    @Delete
    suspend fun delete(log: LogEntity)

    /** Full timeline, newest first (RF-1.4). */
    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<LogEntity>>

    /** Timeline for a single date, newest first. */
    @Query("SELECT * FROM logs WHERE date_string = :date ORDER BY timestamp DESC")
    fun observeByDate(date: String): Flow<List<LogEntity>>

    /**
     * Time-capsule query (RF-1.5): returns all logs whose date_string ends with
     * the same MM-DD suffix as :monthDay (format "-MM-DD") from a prior year.
     */
    @Query("SELECT * FROM logs WHERE date_string LIKE '%' || :monthDay AND date_string < :today ORDER BY date_string DESC")
    fun observeOnThisDay(monthDay: String, today: String): Flow<List<LogEntity>>

    /** Filter timeline by a single hashtag. */
    @Query("SELECT * FROM logs WHERE ',' || tags || ',' LIKE '%,' || :tag || ',%' ORDER BY timestamp DESC")
    fun observeByTag(tag: String): Flow<List<LogEntity>>

    /** All distinct tags for the chip filter bar. */
    @Query("SELECT DISTINCT tags FROM logs WHERE tags != '' ORDER BY timestamp DESC")
    suspend fun getAllTagStrings(): List<String>

    /** Heat-map aggregation: count of logs per date. */
    @Query("SELECT date_string, COUNT(*) as count FROM logs GROUP BY date_string")
    suspend fun getLogCountsPerDay(): List<DateCount>
}

// ─────────────────────────────────────────────────────────────────────────────
// MoodDao  –  RF-1.3
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface MoodDao {

    /** UPSERT – last write wins for the given date (RF-1.3). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mood: DailyMoodEntity)

    @Query("SELECT * FROM daily_mood WHERE date_string = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyMoodEntity?

    @Query("SELECT * FROM daily_mood WHERE date_string = :date LIMIT 1")
    fun observeByDate(date: String): Flow<DailyMoodEntity?>

    /** Returns all moods for correlation chart (RF-6.2). */
    @Query("SELECT * FROM daily_mood ORDER BY date_string ASC")
    fun observeAll(): Flow<List<DailyMoodEntity>>

    /** Average mood for a date range used in weekly summary (RF-6.3). */
    @Query("SELECT AVG(score) FROM daily_mood WHERE date_string BETWEEN :from AND :to")
    suspend fun averageScoreInRange(from: String, to: String): Float?
}

// ─────────────────────────────────────────────────────────────────────────────
// HabitDao  –  RF-2.1 – RF-2.4
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    /** Soft-delete by toggling is_active = false. */
    @Query("UPDATE habits SET is_active = 0 WHERE id = :id")
    suspend fun softDelete(id: Int)

    @Query("SELECT * FROM habits WHERE is_active = 1 ORDER BY priority_stars DESC, name ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): HabitEntity?

    // ── Execution helpers ──────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecution(exec: HabitExecutionEntity): Long

    @Query("DELETE FROM habit_executions WHERE habit_id = :habitId AND date_completed = :date")
    suspend fun deleteExecution(habitId: Int, date: String)

    @Query("SELECT * FROM habit_executions WHERE habit_id = :habitId ORDER BY date_completed DESC")
    fun observeExecutions(habitId: Int): Flow<List<HabitExecutionEntity>>

    /** All executions for a habit between two dates (streak calculation). */
    @Query("""
        SELECT * FROM habit_executions 
        WHERE habit_id = :habitId 
          AND date_completed BETWEEN :from AND :to 
        ORDER BY date_completed ASC
    """)
    suspend fun getExecutionsInRange(habitId: Int, from: String, to: String): List<HabitExecutionEntity>

    /** Single execution lookup to check whether today is already marked. */
    @Query("SELECT * FROM habit_executions WHERE habit_id = :habitId AND date_completed = :date LIMIT 1")
    suspend fun getExecution(habitId: Int, date: String): HabitExecutionEntity?

    /** Heat-map: count of habit completions per day. */
    @Query("SELECT date_completed as date_string, COUNT(*) as count FROM habit_executions GROUP BY date_completed")
    suspend fun getExecutionCountsPerDay(): List<DateCount>

    /** Update cached streak values without a full entity update. */
    @Query("UPDATE habits SET current_streak = :current, max_streak = :max WHERE id = :id")
    suspend fun updateStreaks(id: Int, current: Int, max: Int)
}

// ─────────────────────────────────────────────────────────────────────────────
// GoalDao  –  RF-4.1 – RF-4.2
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE is_completed = 0 ORDER BY priority_stars DESC, deadline_timestamp ASC")
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY priority_stars DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): GoalEntity?

    /** Recalculate and persist progress after a task is toggled (RF-4.2). */
    @Query("""
        UPDATE goals
        SET progress_percentage = (
            SELECT CAST(SUM(CASE WHEN is_completed = 1 THEN 1 ELSE 0 END) AS FLOAT) 
                   / (CASE WHEN COUNT(*) = 0 THEN 1 ELSE COUNT(*) END) * 100
            FROM tasks 
            WHERE goal_id = :goalId
        )
        WHERE id = :goalId
    """)
    suspend fun refreshProgress(goalId: Int)
}

// ─────────────────────────────────────────────────────────────────────────────
// TaskDao  –  RF-4.1 – RF-4.4
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    /** All tasks for a specific goal, pending first. */
    @Query("SELECT * FROM tasks WHERE goal_id = :goalId ORDER BY is_completed ASC, priority_stars DESC")
    fun observeByGoal(goalId: Int): Flow<List<TaskEntity>>

    /** Orphaned tasks (RF-4.3). */
    @Query("SELECT * FROM tasks WHERE goal_id IS NULL AND is_completed = 0 ORDER BY priority_stars DESC")
    fun observeOrphaned(): Flow<List<TaskEntity>>

    /** Tasks with a due date – used in unified agenda view (RF-3.1). */
    @Query("SELECT * FROM tasks WHERE due_timestamp BETWEEN :from AND :to ORDER BY due_timestamp ASC")
    fun observeInDateRange(from: Long, to: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): TaskEntity?

    /** Bulk completion count for goal progress recalc (RF-4.2). */
    @Query("SELECT COUNT(*) FROM tasks WHERE goal_id = :goalId AND is_completed = 1")
    suspend fun completedCountForGoal(goalId: Int): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE goal_id = :goalId")
    suspend fun totalCountForGoal(goalId: Int): Int
}

// ─────────────────────────────────────────────────────────────────────────────
// CalendarEventDao  –  RF-3.1 – RF-3.4
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface CalendarEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CalendarEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CalendarEventEntity>)

    @Update
    suspend fun update(event: CalendarEventEntity)

    @Delete
    suspend fun delete(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE gcal_event_id = :gcalId")
    suspend fun deleteByGcalId(gcalId: String)

    /** Agenda view: events in a time window, ordered by start (RF-3.1). */
    @Query("SELECT * FROM calendar_events WHERE start_timestamp BETWEEN :from AND :to ORDER BY start_timestamp ASC")
    fun observeInRange(from: Long, to: Long): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE gcal_event_id = :gcalId LIMIT 1")
    suspend fun getByGcalId(gcalId: String): CalendarEventEntity?

    /** Upcoming event for widget (RF-6.4). */
    @Query("SELECT * FROM calendar_events WHERE start_timestamp > :now ORDER BY start_timestamp ASC LIMIT 1")
    suspend fun getNextEvent(now: Long): CalendarEventEntity?

    /** Wipe all GCal-sourced events before a fresh sync. */
    @Query("DELETE FROM calendar_events WHERE is_from_gcal = 1")
    suspend fun deleteAllGcalEvents()
}

// ─────────────────────────────────────────────────────────────────────────────
// FocusSessionDao  –  RF-5.1, RF-5.4, RF-6.3
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface FocusSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FocusSessionEntity): Long

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY start_timestamp DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    /** Weekly summary helper (RF-6.3). */
    @Query("""
        SELECT SUM(duration_minutes) 
        FROM focus_sessions 
        WHERE status = 'COMPLETED' 
          AND start_timestamp BETWEEN :from AND :to
    """)
    suspend fun completedMinutesInRange(from: Long, to: Long): Int?

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED' AND start_timestamp BETWEEN :from AND :to")
    suspend fun completedCountInRange(from: Long, to: Long): Int
}
