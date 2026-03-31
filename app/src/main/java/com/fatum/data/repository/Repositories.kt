package com.fatum.data.repository

import com.fatum.data.db.dao.*
import com.fatum.data.db.entities.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val D = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private fun today() = LocalDate.now().format(D)

// ─────────────────────────────────────────────────────────────────────────────
// HabitRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class HabitRepository @Inject constructor(private val dao: HabitDao) {

    fun observeActive()                    = dao.observeActive()
    fun observeExecutions(id: Int)         = dao.observeExecutions(id)

    suspend fun addHabit(
        name: String, type: String, valueTarget: Int,
        freqType: String, freqTarget: Int
    ) = dao.insert(HabitEntity(
        name = name, habitType = type, valueTarget = valueTarget,
        frequencyType = freqType, frequencyTarget = freqTarget,
        createdAt = System.currentTimeMillis()
    ))

    suspend fun updateHabit(h: HabitEntity)         = dao.update(h)
    suspend fun deleteHabit(id: Int)                = dao.softDelete(id)

    /** Toggle BOOLEAN habit for a date. */
    suspend fun toggleBoolean(habitId: Int, date: String) {
        val ex = dao.getExecution(habitId, date)
        if (ex != null) dao.deleteExecutionByDate(habitId, date)
        else dao.upsertExecution(HabitExecutionEntity(
            habitId = habitId, dateCompleted = date,
            valueLogged = 1, timestampLogged = System.currentTimeMillis()
        ))
        recalcStreak(habitId)
    }

    /**
     * Log a value for a VALUE habit on a date.
     * Adds [delta] to the existing accumulated value.
     * If [delta] is null the execution is deleted (reset day).
     */
    suspend fun logValue(habitId: Int, date: String, delta: Int) {
        val habit = dao.getById(habitId) ?: return
        val ex    = dao.getExecution(habitId, date)
        val newVal = (ex?.valueLogged ?: 0) + delta
        if (newVal <= 0) {
            dao.deleteExecutionByDate(habitId, date)
        } else {
            dao.upsertExecution(HabitExecutionEntity(
                id            = ex?.id ?: 0,
                habitId       = habitId,
                dateCompleted = date,
                valueLogged   = newVal,
                timestampLogged = System.currentTimeMillis()
            ))
        }
        recalcStreak(habitId)
    }

    suspend fun getHeatmap(habitId: Int) = dao.getHeatmapForHabit(habitId)

    /** Recalculate current/max streak and persist. */
    suspend fun recalcStreak(habitId: Int) {
        val habit = dao.getById(habitId) ?: return
        val execs = dao.getExecutionsList(habitId)
            .filter { it.valueLogged >= (if (habit.habitType == "VALUE") habit.valueTarget else 1) }
            .map { LocalDate.parse(it.dateCompleted, D) }
            .sortedDescending()

        var cur = 0; var max = 0; var run = 0
        var prev: LocalDate? = null
        execs.sortedAscending().forEach { d ->
            run = if (prev == null || prev!!.plusDays(1) == d) run + 1 else 1
            if (run > max) max = run
            prev = d
        }
        // Current streak: consecutive from today backwards
        var expected = LocalDate.now()
        for (d in execs) {
            if (d == expected) { cur++; expected = expected.minusDays(1) }
            else if (d.isBefore(expected)) break
        }
        dao.updateStreaks(habitId, cur, maxOf(max, habit.maxStreak))
    }

    private fun List<LocalDate>.sortedAscending() = sortedWith(compareBy { it })

    suspend fun getTopStreak() = dao.getTopStreakDirect()
}

// ─────────────────────────────────────────────────────────────────────────────
// TaskRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class TaskRepository @Inject constructor(private val dao: TaskDao) {

    fun observePending()   = dao.observePending()
    fun observeCompleted() = dao.observeCompleted()

    suspend fun add(title: String, description: String?, priority: String, due: Long?) =
        dao.insert(TaskEntity(
            title = title, description = description,
            priority = priority, dueTimestamp = due,
            createdAt = System.currentTimeMillis()
        ))

    suspend fun toggle(task: TaskEntity) = dao.update(task.copy(isCompleted = !task.isCompleted))
    suspend fun update(task: TaskEntity) = dao.update(task)
    suspend fun delete(task: TaskEntity) = dao.delete(task)

    suspend fun getTopPending(limit: Int = 5) = dao.getTopPending(limit)
}

// ─────────────────────────────────────────────────────────────────────────────
// GoalRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val milestoneDao: MilestoneDao
) {
    fun observeActive()            = goalDao.observeActive()
    fun observeAll()               = goalDao.observeAll()
    fun observeMilestones(gid: Int) = milestoneDao.observeByGoal(gid)

    suspend fun addGoal(
        title: String, description: String?, priority: String,
        deadline: Long?, goalType: String, targetValue: Float
    ) = goalDao.insert(GoalEntity(
        title = title, description = description, priority = priority,
        deadlineTimestamp = deadline, goalType = goalType, targetValue = targetValue,
        createdAt = System.currentTimeMillis()
    ))

    suspend fun updateGoal(g: GoalEntity) = goalDao.update(g)
    suspend fun deleteGoal(g: GoalEntity) = goalDao.delete(g)
    suspend fun markCompleted(g: GoalEntity) = goalDao.update(g.copy(isCompleted = true))

    // ── Value goals ──────────────────────────────────────────────────────────
    suspend fun adjustValue(goal: GoalEntity, delta: Float) {
        val newVal = (goal.currentValue + delta).coerceAtLeast(0f)
        goalDao.update(goal.copy(
            currentValue = newVal,
            isCompleted  = goal.targetValue > 0 && newVal >= goal.targetValue
        ))
    }

    suspend fun setTargetValue(goal: GoalEntity, target: Float) =
        goalDao.update(goal.copy(targetValue = target))

    // ── Milestone goals ──────────────────────────────────────────────────────
    suspend fun addMilestone(goalId: Int, title: String) {
        val order = milestoneDao.getByGoal(goalId).size
        milestoneDao.insert(MilestoneEntity(goalId = goalId, title = title, orderIndex = order))
        refreshGoalProgress(goalId)
    }

    suspend fun toggleMilestone(m: MilestoneEntity) {
        milestoneDao.update(m.copy(isCompleted = !m.isCompleted))
        refreshGoalProgress(m.goalId)
    }

    suspend fun deleteMilestone(m: MilestoneEntity) {
        milestoneDao.delete(m)
        refreshGoalProgress(m.goalId)
    }

    private suspend fun refreshGoalProgress(goalId: Int) {
        val total     = milestoneDao.countTotal(goalId)
        val completed = milestoneDao.countCompleted(goalId)
        val goal      = goalDao.getById(goalId) ?: return
        val progress  = if (total == 0) 0f else completed.toFloat() / total
        goalDao.update(goal.copy(isCompleted = total > 0 && completed == total))
    }

    suspend fun getTopActive(limit: Int = 3) = goalDao.getTopActive(limit)
}

// ─────────────────────────────────────────────────────────────────────────────
// CalendarRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class CalendarRepository @Inject constructor(private val dao: CalendarEventDao) {

    fun observeInRange(from: Long, to: Long): Flow<List<CalendarEventEntity>> =
        dao.observeInRange(from, to)

    suspend fun getInRange(from: Long, to: Long) = dao.getInRange(from, to)

    suspend fun addEvent(
        title: String, description: String?, importance: String,
        start: Long, end: Long, rrule: String?
    ) = dao.insert(CalendarEventEntity(
        title = title, description = description, importance = importance,
        startTimestamp = start, endTimestamp = end, recurrenceRule = rrule
    ))

    suspend fun update(event: CalendarEventEntity) =
        dao.update(event.copy(isLocallyModified = true))

    suspend fun delete(event: CalendarEventEntity) = dao.delete(event)

    /**
     * Full GCal sync: deletes only unmodified GCal events and inserts fresh batch.
     * Locally-modified GCal events are preserved.
     */
    suspend fun syncFromGcal(events: List<CalendarEventEntity>) {
        dao.deleteUnmodifiedGcalEvents()
        dao.insertAll(events.map { it.copy(isFromGcal = true, isLocallyModified = false) })
    }

    suspend fun getNextEvent(): CalendarEventEntity? =
        dao.getNextEvent(System.currentTimeMillis())
}

// ─────────────────────────────────────────────────────────────────────────────
// FocusRepository
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class FocusRepository @Inject constructor(private val dao: FocusSessionDao) {

    fun observeAll() = dao.observeAll()

    suspend fun save(start: Long, end: Long, minutes: Int, completed: Boolean) =
        dao.insert(FocusSessionEntity(
            startTimestamp  = start, endTimestamp = end,
            durationMinutes = minutes,
            status          = if (completed) "COMPLETED" else "FAILED_INTERRUPTED"
        ))

    suspend fun completedMinutesInRange(from: Long, to: Long) =
        dao.completedMinutesInRange(from, to) ?: 0
}
