package com.fatum.data.db.dao

import androidx.room.*
import com.fatum.data.db.entities.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// HabitDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(h: HabitEntity): Long
    @Update                                          suspend fun update(h: HabitEntity)
    @Query("UPDATE habits SET is_active=0 WHERE id=:id") suspend fun softDelete(id: Int)

    @Query("SELECT * FROM habits WHERE is_active=1 ORDER BY name ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id=:id LIMIT 1")
    suspend fun getById(id: Int): HabitEntity?

    // ── Executions ────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExecution(e: HabitExecutionEntity): Long

    @Delete suspend fun deleteExecution(e: HabitExecutionEntity)

    @Query("DELETE FROM habit_executions WHERE habit_id=:habitId AND date_completed=:date")
    suspend fun deleteExecutionByDate(habitId: Int, date: String)

    @Query("SELECT * FROM habit_executions WHERE habit_id=:habitId AND date_completed=:date LIMIT 1")
    suspend fun getExecution(habitId: Int, date: String): HabitExecutionEntity?

    @Query("SELECT * FROM habit_executions WHERE habit_id=:habitId ORDER BY date_completed DESC")
    fun observeExecutions(habitId: Int): Flow<List<HabitExecutionEntity>>

    @Query("SELECT * FROM habit_executions WHERE habit_id=:habitId ORDER BY date_completed DESC")
    suspend fun getExecutionsList(habitId: Int): List<HabitExecutionEntity>

    @Query("SELECT * FROM habit_executions WHERE habit_id=:habitId AND date_completed BETWEEN :from AND :to ORDER BY date_completed ASC")
    suspend fun getExecutionsInRange(habitId: Int, from: String, to: String): List<HabitExecutionEntity>

    @Query("SELECT date_completed AS date_string, SUM(value_logged) AS count FROM habit_executions WHERE habit_id=:habitId GROUP BY date_completed")
    suspend fun getHeatmapForHabit(habitId: Int): List<DateCount>

    @Query("UPDATE habits SET current_streak=:current, max_streak=:max WHERE id=:id")
    suspend fun updateStreaks(id: Int, current: Int, max: Int)

    @Query("SELECT COALESCE(MAX(current_streak),0) FROM habits WHERE is_active=1")
    suspend fun getTopStreakDirect(): Int
}

data class DateCount(val date_string: String, val count: Int)

// ─────────────────────────────────────────────────────────────────────────────
// TaskDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(t: TaskEntity): Long
    @Update                                          suspend fun update(t: TaskEntity)
    @Delete                                          suspend fun delete(t: TaskEntity)

    @Query("""
        SELECT * FROM tasks WHERE is_completed=0
        ORDER BY CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, created_at ASC
    """)
    fun observePending(): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks WHERE is_completed=1
        ORDER BY CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, created_at DESC
    """)
    fun observeCompleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id=:id LIMIT 1")
    suspend fun getById(id: Int): TaskEntity?

    /** Tasks due today for dashboard */
    @Query("""
        SELECT * FROM tasks WHERE is_completed=0 AND due_timestamp BETWEEN :from AND :to
        ORDER BY CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END
    """)
    suspend fun getTasksForDay(from: Long, to: Long): List<TaskEntity>

    /** Top N pending tasks ordered by priority for dashboard */
    @Query("""
        SELECT * FROM tasks WHERE is_completed=0
        ORDER BY CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END
        LIMIT :limit
    """)
    suspend fun getTopPending(limit: Int): List<TaskEntity>
}

// ─────────────────────────────────────────────────────────────────────────────
// GoalDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(g: GoalEntity): Long
    @Update                                          suspend fun update(g: GoalEntity)
    @Delete                                          suspend fun delete(g: GoalEntity)

    @Query("""
        SELECT * FROM goals WHERE is_completed=0
        ORDER BY CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, created_at ASC
    """)
    fun observeActive(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY created_at DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id=:id LIMIT 1")
    suspend fun getById(id: Int): GoalEntity?

    @Query("""
        SELECT * FROM goals WHERE is_completed=0
        ORDER BY CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END
        LIMIT :limit
    """)
    suspend fun getTopActive(limit: Int): List<GoalEntity>
}

// ─────────────────────────────────────────────────────────────────────────────
// MilestoneDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface MilestoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(m: MilestoneEntity): Long
    @Update                                          suspend fun update(m: MilestoneEntity)
    @Delete                                          suspend fun delete(m: MilestoneEntity)

    @Query("SELECT * FROM milestones WHERE goal_id=:goalId ORDER BY order_index ASC")
    fun observeByGoal(goalId: Int): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestones WHERE goal_id=:goalId ORDER BY order_index ASC")
    suspend fun getByGoal(goalId: Int): List<MilestoneEntity>

    @Query("SELECT COUNT(*) FROM milestones WHERE goal_id=:goalId AND is_completed=1")
    suspend fun countCompleted(goalId: Int): Int

    @Query("SELECT COUNT(*) FROM milestones WHERE goal_id=:goalId")
    suspend fun countTotal(goalId: Int): Int
}

// ─────────────────────────────────────────────────────────────────────────────
// CalendarEventDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface CalendarEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(e: CalendarEventEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(events: List<CalendarEventEntity>)
    @Update                                          suspend fun update(e: CalendarEventEntity)
    @Delete                                          suspend fun delete(e: CalendarEventEntity)

    @Query("SELECT * FROM calendar_events WHERE start_timestamp BETWEEN :from AND :to ORDER BY start_timestamp ASC")
    fun observeInRange(from: Long, to: Long): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE start_timestamp BETWEEN :from AND :to ORDER BY start_timestamp ASC")
    suspend fun getInRange(from: Long, to: Long): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE id=:id LIMIT 1")
    suspend fun getById(id: Int): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE gcal_event_id=:gcalId LIMIT 1")
    suspend fun getByGcalId(gcalId: String): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE start_timestamp > :now ORDER BY start_timestamp ASC LIMIT 1")
    suspend fun getNextEvent(now: Long): CalendarEventEntity?

    /** Delete only non-locally-modified GCal events (preserve local edits) */
    @Query("DELETE FROM calendar_events WHERE is_from_gcal=1 AND is_locally_modified=0")
    suspend fun deleteUnmodifiedGcalEvents()
}

// ─────────────────────────────────────────────────────────────────────────────
// FocusSessionDao
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(s: FocusSessionEntity): Long
    @Update                                          suspend fun update(s: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY start_timestamp DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT SUM(duration_minutes) FROM focus_sessions WHERE status='COMPLETED' AND start_timestamp BETWEEN :from AND :to")
    suspend fun completedMinutesInRange(from: Long, to: Long): Int?
}
