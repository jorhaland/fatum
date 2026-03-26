package com.fatum.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatum.data.db.entities.*
import com.fatum.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private fun today() = LocalDate.now().format(DATE_FMT)

// ─────────────────────────────────────────────────────────────────────────────
// HomeViewModel  –  Micro-logging, mood, heat-map (RF-1.x, RF-6.1)
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val logRepo: LogRepository,
    private val moodRepo: MoodRepository,
    private val habitRepo: HabitRepository
) : ViewModel() {

    /** Raw input text in the quick-add field. */
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    /** Active tag filter (null = show all). */
    private val _activeTag = MutableStateFlow<String?>(null)
    val activeTag: StateFlow<String?> = _activeTag.asStateFlow()

    /** Full timeline or filtered by tag. */
    val logs: StateFlow<List<LogEntity>> = _activeTag
        .flatMapLatest { tag ->
            if (tag == null) logRepo.observeAll() else logRepo.observeByTag(tag)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayMood: StateFlow<DailyMoodEntity?> = moodRepo.observeToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Time-capsule entries (RF-1.5). */
    val timeCapsule: StateFlow<List<LogEntity>> = logRepo.observeOnThisDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Heat-map data: map of "YYYY-MM-DD" → intensity count (RF-6.1). */
    private val _heatMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val heatMap: StateFlow<Map<String, Int>> = _heatMap.asStateFlow()

    init { loadHeatMap() }

    private fun loadHeatMap() = viewModelScope.launch {
        val logCounts = logRepo.getLogCountsPerDay().associate { it.date_string to it.count }
        val habitCounts = habitRepo.getExecutionCountsPerDay().associate { it.date_string to it.count }
        val merged = (logCounts.keys + habitCounts.keys).associateWith { date ->
            (logCounts[date] ?: 0) + (habitCounts[date] ?: 0)
        }
        _heatMap.value = merged
    }

    fun onInputChange(text: String) { _inputText.value = text }

    /** Parses #hashtags from the content and saves the log. */
    fun submitLog() = viewModelScope.launch {
        val text = _inputText.value.trim()
        if (text.isBlank()) return@launch
        val tags = Regex("#(\\w+)").findAll(text).map { it.groupValues[1].lowercase() }.toList()
        logRepo.add(text, tags)
        _inputText.value = ""
        loadHeatMap()
    }

    fun setMood(score: Int) = viewModelScope.launch { moodRepo.setMood(score) }
    fun setTagFilter(tag: String?) { _activeTag.value = tag }
    fun deleteLog(log: LogEntity) = viewModelScope.launch { logRepo.delete(log) }
    fun updateLog(log: LogEntity) = viewModelScope.launch { logRepo.update(log) }
}

// ─────────────────────────────────────────────────────────────────────────────
// HabitsViewModel  –  RF-2.x
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repo: HabitRepository
) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> = repo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Selected calendar date for retroactive logging (RF-2.2). */
    private val _selectedDate = MutableStateFlow(today())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    /** IDs of habits completed on the selected date. */
    private val _completedIds = MutableStateFlow<Set<Int>>(emptySet())
    val completedIds: StateFlow<Set<Int>> = _completedIds.asStateFlow()

    init { refreshCompletedForDate() }

    fun selectDate(date: String) {
        _selectedDate.value = date
        refreshCompletedForDate()
    }

    private fun refreshCompletedForDate() = viewModelScope.launch {
        // Collect single snapshot of executions for the selected date
        repo.observeActive().first().forEach { habit ->
            // handled via observeExecutions below
        }
        _completedIds.value = emptySet() // reset; UI re-subscribes per-habit
    }

    fun toggleHabit(habitId: Int) = viewModelScope.launch {
        repo.toggleExecution(habitId, _selectedDate.value)
    }

    fun addHabit(name: String, freqType: String, target: Int, priority: Int) =
        viewModelScope.launch { repo.addHabit(name, freqType, target, priority) }

    fun deleteHabit(id: Int) = viewModelScope.launch { repo.deleteHabit(id) }

    fun observeExecutions(habitId: Int) = repo.observeExecutions(habitId)
}

// ─────────────────────────────────────────────────────────────────────────────
// PlannerViewModel  –  RF-3.x, RF-4.x (agenda unified view)
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val calendarRepo: CalendarRepository,
    private val goalRepo: GoalRepository
) : ViewModel() {

    /** Currently displayed day in the agenda. */
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val eventsForDay: StateFlow<List<CalendarEventEntity>> = _selectedDate
        .flatMapLatest { date ->
            val from = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val to   = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            calendarRepo.observeInRange(from, to)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasksForDay: StateFlow<List<TaskEntity>> = _selectedDate
        .flatMapLatest { date ->
            val from = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val to   = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            goalRepo.observeTasksInRange(from, to)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDate(date: LocalDate) { _selectedDate.value = date }

    fun addEvent(
        title: String,
        startTs: Long,
        endTs: Long,
        rrule: String?,
        priority: Int,
        goalId: Int?
    ) = viewModelScope.launch {
        calendarRepo.addLocalEvent(title, startTs, endTs, rrule, priority, goalId)
    }

    fun deleteEvent(event: CalendarEventEntity) = viewModelScope.launch {
        calendarRepo.deleteEvent(event)
    }

    fun updateEvent(event: CalendarEventEntity) = viewModelScope.launch {
        calendarRepo.updateEvent(event)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GoalsViewModel  –  RF-4.x
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repo: GoalRepository
) : ViewModel() {

    val goals: StateFlow<List<GoalEntity>> = repo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val orphanedTasks: StateFlow<List<TaskEntity>> = repo.observeOrphanedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeTasksForGoal(goalId: Int) = repo.observeTasksByGoal(goalId)

    fun addGoal(title: String, desc: String?, priority: Int, deadline: Long?) =
        viewModelScope.launch { repo.addGoal(title, desc, priority, deadline) }

    fun deleteGoal(goal: GoalEntity) = viewModelScope.launch { repo.deleteGoal(goal) }

    fun addTask(title: String, goalId: Int?, priority: Int, due: Long?) =
        viewModelScope.launch { repo.addTask(title, goalId, priority, due) }

    fun toggleTask(task: TaskEntity) = viewModelScope.launch { repo.toggleTask(task) }
    fun deleteTask(task: TaskEntity) = viewModelScope.launch { repo.deleteTask(task) }
    fun updateGoal(goal: GoalEntity) = viewModelScope.launch { repo.updateGoal(goal) }
}

// ─────────────────────────────────────────────────────────────────────────────
// FocusViewModel  –  RF-5.x
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val repo: FocusRepository
) : ViewModel() {

    /** Timer state machine. */
    enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }

    private val _state = MutableStateFlow(TimerState.IDLE)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _durationMinutes = MutableStateFlow(25) // default Pomodoro
    val durationMinutes: StateFlow<Int> = _durationMinutes.asStateFlow()

    /** Remaining milliseconds. Updated every second by the service. */
    private val _remainingMs = MutableStateFlow(25 * 60 * 1000L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private var sessionStartTs = 0L

    val recentSessions: StateFlow<List<FocusSessionEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDuration(minutes: Int) {
        _durationMinutes.value = minutes
        _remainingMs.value = minutes * 60 * 1000L
    }

    fun startSession() {
        sessionStartTs = System.currentTimeMillis()
        _remainingMs.value = _durationMinutes.value * 60 * 1000L
        _state.value = TimerState.RUNNING
    }

    fun tick(deltaMs: Long) {
        val remaining = (_remainingMs.value - deltaMs).coerceAtLeast(0)
        _remainingMs.value = remaining
        if (remaining == 0L) finishSession(completed = true)
    }

    fun interruptSession() = viewModelScope.launch {
        _state.value = TimerState.IDLE
        val endTs = System.currentTimeMillis()
        val elapsed = ((endTs - sessionStartTs) / 60_000).toInt()
        repo.saveSession(sessionStartTs, endTs, elapsed, completed = false)
    }

    fun finishSession(completed: Boolean) = viewModelScope.launch {
        _state.value = TimerState.FINISHED
        val endTs = System.currentTimeMillis()
        repo.saveSession(sessionStartTs, endTs, _durationMinutes.value, completed)
    }

    fun reset() {
        _state.value = TimerState.IDLE
        _remainingMs.value = _durationMinutes.value * 60 * 1000L
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AnalyticsViewModel  –  RF-6.x
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val moodRepo: MoodRepository,
    private val habitRepo: HabitRepository,
    private val focusRepo: FocusRepository,
    private val goalRepo: GoalRepository
) : ViewModel() {

    val allMoods: StateFlow<List<DailyMoodEntity>> = moodRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeHabits: StateFlow<List<HabitEntity>> = habitRepo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allGoals: StateFlow<List<GoalEntity>> = goalRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentSessions: StateFlow<List<FocusSessionEntity>> = focusRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Selected habit for correlation chart (RF-6.2). */
    private val _correlationHabitId = MutableStateFlow<Int?>(null)
    val correlationHabitId: StateFlow<Int?> = _correlationHabitId.asStateFlow()

    fun selectCorrelationHabit(id: Int) { _correlationHabitId.value = id }

    /** Weekly summary report data holder (RF-6.3). */
    data class WeeklySummary(
        val weekRange: String,
        val avgMood: Float,
        val bestStreak: Pair<String, Int>, // habit name to streak
        val deepWorkMinutes: Int,
        val goalProgress: List<Pair<String, Float>>
    )

    private val _weeklySummary = MutableStateFlow<WeeklySummary?>(null)
    val weeklySummary: StateFlow<WeeklySummary?> = _weeklySummary.asStateFlow()

    fun generateWeeklySummary() = viewModelScope.launch {
        val today = LocalDate.now()
        val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val sunday = monday.plusDays(6)

        val from = monday.format(DATE_FMT)
        val to = sunday.format(DATE_FMT)
        val fromTs = monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toTs = sunday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val avgMood = moodRepo.averageInRange(from, to) ?: 0f
        val deepWork = focusRepo.completedMinutesInRange(fromTs, toTs)

        val habits = habitRepo.observeActive().first()
        val bestStreak = habits.maxByOrNull { it.currentStreak }
            ?.let { it.name to it.currentStreak } ?: ("—" to 0)

        val goals = goalRepo.observeAll().first()
        val goalProgress = goals.map { it.title to it.progressPercentage }

        _weeklySummary.value = WeeklySummary(
            weekRange = "$from → $to",
            avgMood = avgMood,
            bestStreak = bestStreak,
            deepWorkMinutes = deepWork,
            goalProgress = goalProgress
        )
    }
}
