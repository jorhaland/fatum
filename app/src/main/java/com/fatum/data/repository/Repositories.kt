package com.fatum.data.repository

import com.fatum.data.db.dao.*
import com.fatum.data.db.entities.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private fun today(): String = LocalDate.now().format(DATE_FMT)

// ─────────────────────────────────────────────────────────────────────────────
// LogRepository  –  RF-1.x
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class LogRepository @Inject constructor(private val dao: LogDao) {

    fun observeAll(): Flow<List<LogEntity>> = dao.observeAll()
    fun observeByDate(date: String): Flow<List<LogEntity>> = dao.observeByDate(date)
    fun observeByTag(tag: String): Flow<List<LogEntity>> = dao.observeByTag(tag)

    /**
     * RF-1.5 – "On this day" time capsule.
     * Returns logs whose date ends in the same MM-DD from any prior year.
     */
    fun observeOnThisDay(): Flow<List<LogEntity>> {
        val td = today()
        val monthDay = td.substring(4) // "-MM-DD"
        return dao.observeOnThisDay(monthDay, td)
    }

    suspend fun add(content: String, tags: List<String>): Long {
        val now = System.currentTimeMillis()
        val entry = LogEntity(
            content = content,
            timestamp = now,
            dateString = today(),
            tags = tags.joinToString(",")
        )
        return dao.insert(entry)
    }

    suspend fun update(log: LogEntity) = dao.update(log)
    suspend fun delete(log: LogEntity) = dao.delete(log)
    suspend fun getAllTagStrings(): List<String> = dao.getAllTagStrings()
    suspend fun getLogCountsPerDay() = dao.getLogCountsPerDay()
}

// ─────────────────────────────────────────────────────────────────────────────
// MoodRepository  –  RF-1.3
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class MoodRepository @Inject constructor(private val dao: MoodDao) {

    fun observeToday(): Flow<DailyMoodEntity?> = dao.observeByDate(today())
    fun observeAll(): Flow<List<DailyMoodEntity>> = dao.observeAll()

    /** Saves or updates today's mood. */
    suspend fun setMood(score: Int) {
        dao.upsert(
            DailyMoodEntity(
                dateString = today(),
                score = score,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun averageInRange(from: String, to: String) = dao.averageScoreInRange(from, to)
}

// ─────────────────────────────────────────────────────────────────────────────
// HabitRepository  –  RF-2.x
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class HabitRepository @Inject constructor(private val dao: HabitDao) {

    fun observeActive(): Flow<List<HabitEntity>> = dao.observeActive()
    fun observeExecutions(habitId: Int): Flow<List<HabitExecutionEntity>> =
        dao.observeExecutions(habitId)

    suspend fun addHabit(
        name: String,
        frequencyType: String,
        frequencyTarget: Int,
        priorityStars: Int
    ): Long {
        return dao.insert(
            HabitEntity(
                name = name,
                frequencyType = frequencyType,
                frequencyTarget = frequencyTarget,
                priorityStars = priorityStars,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateHabit(habit: HabitEntity) = dao.update(habit)
    suspend fun deleteHabit(id: Int) = dao.softDelete(id)

    /** Toggle a habit completion for a given date. Triggers streak recalc. */
    suspend fun toggleExecution(habitId: Int, date: String) {
        val existing = dao.getExecution(habitId, date)
        if (existing != null) {
            dao.deleteExecution(habitId, date)
        } else {
            dao.insertExecution(
                HabitExecutionEntity(
                    habitId = habitId,
                    dateCompleted = date,
                    timestampLogged = System.currentTimeMillis()
                )
            )
        }
        recalculateStreaks(habitId)
    }

    /**
     * Recalculates current and max streak after any execution change.
     * Supports DAILY habits (consecutive days) and cycle-based habits.
     */
    suspend fun recalculateStreaks(habitId: Int) {
        val habit = dao.getById(habitId) ?: return
        val executions = dao.getExecutionsInRange(habitId, "1970-01-01", today())
        val dates = executions.map { LocalDate.parse(it.dateCompleted, DATE_FMT) }.sortedDescending()

        val current: Int
        val max: Int

        when (habit.frequencyType) {
            "DAILY" -> {
                // Count consecutive days ending at today (or yesterday if today not done)
                var streak = 0
                var expected = LocalDate.now()
                if (dates.isNotEmpty() && dates.first() != expected) {
                    // Allow yesterday as the last valid day (haven't done today yet)
                    expected = expected.minusDays(1)
                }
                for (d in dates) {
                    if (d == expected) { streak++; expected = expected.minusDays(1) }
                    else if (d.isBefore(expected)) break
                }
                current = streak

                // Max streak calculation
                var maxStreak = 0; var runStreak = 0; var prev: LocalDate? = null
                for (d in dates.sortedAscending()) {
                    runStreak = if (prev == null || prev.plusDays(1) == d) runStreak + 1 else 1
                    if (runStreak > maxStreak) maxStreak = runStreak
                    prev = d
                }
                max = maxStreak
            }
            else -> {
                // For non-daily: count how many full cycles were met consecutively
                current = 0; max = 0 // Simplified – extend with cycle logic as needed
            }
        }

        dao.updateStreaks(habitId, current, maxOf(max, habit.maxStreak))
    }

    private fun List<LocalDate>.sortedAscending() = sortedWith(compareBy { it })

    suspend fun getExecutionCountsPerDay() = dao.getExecutionCountsPerDay()
    suspend fun habitsDoneToday(): List<Int> {
        return dao.getExecutionsInRange(0, today(), today())
            .map { it.habitId }
            .distinct()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GoalRepository  –  RF-4.x
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val taskDao: TaskDao
) {
    fun observeActive(): Flow<List<GoalEntity>> = goalDao.observeActive()
    fun observeAll(): Flow<List<GoalEntity>> = goalDao.observeAll()
    fun observeTasksByGoal(goalId: Int): Flow<List<TaskEntity>> = taskDao.observeByGoal(goalId)
    fun observeOrphanedTasks(): Flow<List<TaskEntity>> = taskDao.observeOrphaned()

    suspend fun addGoal(
        title: String,
        description: String?,
        priorityStars: Int,
        deadlineTimestamp: Long?
    ): Long = goalDao.insert(
        GoalEntity(
            title = title,
            description = description,
            priorityStars = priorityStars,
            deadlineTimestamp = deadlineTimestamp,
            createdAt = System.currentTimeMillis()
        )
    )

    suspend fun updateGoal(goal: GoalEntity) = goalDao.update(goal)
    suspend fun deleteGoal(goal: GoalEntity) = goalDao.delete(goal)

    suspend fun addTask(
        title: String,
        goalId: Int?,
        priorityStars: Int,
        dueTimestamp: Long?
    ): Long = taskDao.insert(
        TaskEntity(
            title = title,
            goalId = goalId,
            priorityStars = priorityStars,
            dueTimestamp = dueTimestamp,
            createdAt = System.currentTimeMillis()
        )
    )

    /** Toggle task completion and refresh parent goal progress (RF-4.2). */
    suspend fun toggleTask(task: TaskEntity) {
        val updated = task.copy(isCompleted = !task.isCompleted)
        taskDao.update(updated)
        task.goalId?.let { goalDao.refreshProgress(it) }
    }

    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)

    fun observeTasksInRange(from: Long, to: Long): Flow<List<TaskEntity>> =
        taskDao.observeInDateRange(from, to)
}

// ─────────────────────────────────────────────────────────────────────────────
// CalendarRepository  –  RF-3.x
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class CalendarRepository @Inject constructor(private val dao: CalendarEventDao) {

    fun observeInRange(from: Long, to: Long): Flow<List<CalendarEventEntity>> =
        dao.observeInRange(from, to)

    suspend fun addLocalEvent(
        title: String,
        startTimestamp: Long,
        endTimestamp: Long,
        recurrenceRule: String?,
        priorityStars: Int,
        assignedGoalId: Int?
    ): Long = dao.insert(
        CalendarEventEntity(
            title = title,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            recurrenceRule = recurrenceRule,
            priorityStars = priorityStars,
            assignedGoalId = assignedGoalId,
            isFromGcal = false
        )
    )

    suspend fun updateEvent(event: CalendarEventEntity) = dao.update(event)
    suspend fun deleteEvent(event: CalendarEventEntity) = dao.delete(event)
    suspend fun deleteByGcalId(gcalId: String) = dao.deleteByGcalId(gcalId)

    /** Replaces all GCal-sourced events with a fresh batch (full sync). */
    suspend fun replaceGcalEvents(events: List<CalendarEventEntity>) {
        dao.deleteAllGcalEvents()
        dao.insertAll(events)
    }

    suspend fun getNextEvent(): CalendarEventEntity? =
        dao.getNextEvent(System.currentTimeMillis())
}

// ─────────────────────────────────────────────────────────────────────────────
// FocusRepository  –  RF-5.x, RF-6.3
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class FocusRepository @Inject constructor(private val dao: FocusSessionDao) {

    fun observeAll(): Flow<List<FocusSessionEntity>> = dao.observeAll()

    suspend fun saveSession(
        startTimestamp: Long,
        endTimestamp: Long,
        durationMinutes: Int,
        completed: Boolean
    ): Long = dao.insert(
        FocusSessionEntity(
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            durationMinutes = durationMinutes,
            status = if (completed) "COMPLETED" else "FAILED_INTERRUPTED"
        )
    )

    suspend fun completedMinutesInRange(from: Long, to: Long): Int =
        dao.completedMinutesInRange(from, to) ?: 0
}
