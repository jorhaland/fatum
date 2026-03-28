package com.fatum.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatum.data.db.entities.*
import com.fatum.data.repository.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private fun today() = LocalDate.now().format(DATE_FMT)

// ─────────────────────────────────────────────────────────────────────────────
// HomeViewModel  –  Logging, heat-map  (RF-1.x, RF-6.1)
// Mood selector removed per user request. MoodRepository kept for analytics.
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val logRepo: LogRepository,
    private val habitRepo: HabitRepository
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _activeTag = MutableStateFlow<String?>(null)
    val activeTag: StateFlow<String?> = _activeTag.asStateFlow()

    val logs: StateFlow<List<LogEntity>> = _activeTag
        .flatMapLatest { tag ->
            if (tag == null) logRepo.observeAll() else logRepo.observeByTag(tag)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val timeCapsule: StateFlow<List<LogEntity>> = logRepo.observeOnThisDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _heatMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val heatMap: StateFlow<Map<String, Int>> = _heatMap.asStateFlow()

    init { loadHeatMap() }

    private fun loadHeatMap() = viewModelScope.launch {
        val logCounts   = logRepo.getLogCountsPerDay().associate { it.date_string to it.count }
        val habitCounts = habitRepo.getExecutionCountsPerDay().associate { it.date_string to it.count }
        _heatMap.value  = (logCounts.keys + habitCounts.keys).associateWith { d ->
            (logCounts[d] ?: 0) + (habitCounts[d] ?: 0)
        }
    }

    fun onInputChange(text: String) { _inputText.value = text }

    fun submitLog() = viewModelScope.launch {
        val text = _inputText.value.trim()
        if (text.isBlank()) return@launch
        val tags = Regex("#(\\w+)").findAll(text).map { it.groupValues[1].lowercase() }.toList()
        logRepo.add(text, tags)
        _inputText.value = ""
        loadHeatMap()
    }

    fun setTagFilter(tag: String?) { _activeTag.value = tag }
    fun deleteLog(log: LogEntity)  = viewModelScope.launch { logRepo.delete(log) }
    fun updateLog(log: LogEntity)  = viewModelScope.launch { logRepo.update(log) }
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

    private val _selectedDate = MutableStateFlow(today())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun selectDate(date: String) { _selectedDate.value = date }

    fun toggleHabit(habitId: Int) = viewModelScope.launch {
        repo.toggleExecution(habitId, _selectedDate.value)
    }

    fun addHabit(name: String, freqType: String, target: Int, priority: Int) =
        viewModelScope.launch { repo.addHabit(name, freqType, target, priority) }

    fun deleteHabit(id: Int) = viewModelScope.launch { repo.deleteHabit(id) }

    fun observeExecutions(habitId: Int) = repo.observeExecutions(habitId)
}

// ─────────────────────────────────────────────────────────────────────────────
// PlannerViewModel  –  RF-3.x  (agenda + Google Calendar sync)
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val calendarRepo: CalendarRepository,
    private val goalRepo: GoalRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    enum class SyncState { IDLE, SYNCING, SUCCESS, ERROR }

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

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

    fun addEvent(title: String, startTs: Long, endTs: Long, rrule: String?, priority: Int, goalId: Int?) =
        viewModelScope.launch { calendarRepo.addLocalEvent(title, startTs, endTs, rrule, priority, goalId) }

    fun deleteEvent(event: CalendarEventEntity) = viewModelScope.launch { calendarRepo.deleteEvent(event) }
    fun updateEvent(event: CalendarEventEntity) = viewModelScope.launch { calendarRepo.updateEvent(event) }

    /** Full sync from Google Calendar → Room. Requires prior Google Sign-In. */
    fun syncCalendar() = viewModelScope.launch(Dispatchers.IO) {
        if (_syncState.value == SyncState.SYNCING) return@launch
        _syncState.value  = SyncState.SYNCING
        _syncError.value  = null
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: error("No hay cuenta de Google conectada. Ve a Ajustes para iniciar sesión.")

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf("https://www.googleapis.com/auth/calendar.readonly")
            ).apply { selectedAccount = account.account }

            val service = Calendar.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("FATUM").build()

            val now   = DateTime(System.currentTimeMillis())
            val limit = DateTime(System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000) // 60 days

            val items = service.events().list("primary")
                .setMaxResults(500)
                .setTimeMin(now)
                .setTimeMax(limit)
                .setSingleEvents(true)
                .setOrderBy("startTime")
                .execute()
                .items ?: emptyList()

            val entities = items.mapNotNull { ev ->
                val startMs = ev.start?.dateTime?.value ?: ev.start?.date?.value ?: return@mapNotNull null
                val endMs   = ev.end?.dateTime?.value   ?: ev.end?.date?.value   ?: return@mapNotNull null
                CalendarEventEntity(
                    gcalEventId    = ev.id,
                    title          = ev.summary ?: "(Sin título)",
                    startTimestamp = startMs,
                    endTimestamp   = endMs,
                    recurrenceRule = ev.recurrence?.firstOrNull(),
                    priorityStars  = 1,
                    isFromGcal     = true
                )
            }

            calendarRepo.replaceGcalEvents(entities)
            _syncState.value = SyncState.SUCCESS
            delay(3_000)
            _syncState.value = SyncState.IDLE
        } catch (e: Exception) {
            _syncError.value  = e.message ?: "Error desconocido"
            _syncState.value  = SyncState.ERROR
            delay(6_000)
            _syncState.value  = SyncState.IDLE
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GoalsViewModel  –  RF-4.x
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repo: GoalRepository
) : ViewModel() {

    val goals: StateFlow<List<GoalEntity>>         = repo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val orphanedTasks: StateFlow<List<TaskEntity>> = repo.observeOrphanedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeTasksForGoal(goalId: Int) = repo.observeTasksByGoal(goalId)

    fun addGoal(title: String, desc: String?, priority: Int, deadline: Long?) =
        viewModelScope.launch { repo.addGoal(title, desc, priority, deadline) }

    fun deleteGoal(goal: GoalEntity)  = viewModelScope.launch { repo.deleteGoal(goal) }
    fun updateGoal(goal: GoalEntity)  = viewModelScope.launch { repo.updateGoal(goal) }
    fun addTask(title: String, goalId: Int?, priority: Int, due: Long?) =
        viewModelScope.launch { repo.addTask(title, goalId, priority, due) }
    fun toggleTask(task: TaskEntity)  = viewModelScope.launch { repo.toggleTask(task) }
    fun deleteTask(task: TaskEntity)  = viewModelScope.launch { repo.deleteTask(task) }
}

// ─────────────────────────────────────────────────────────────────────────────
// FocusViewModel  –  RF-5.x
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val repo: FocusRepository
) : ViewModel() {

    enum class TimerState { IDLE, RUNNING, FINISHED }

    private val _state         = MutableStateFlow(TimerState.IDLE)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _durationMinutes = MutableStateFlow(25)
    val durationMinutes: StateFlow<Int> = _durationMinutes.asStateFlow()

    private val _remainingMs = MutableStateFlow(25 * 60 * 1000L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private var sessionStartTs = 0L

    val recentSessions: StateFlow<List<FocusSessionEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDuration(minutes: Int) {
        _durationMinutes.value = minutes
        _remainingMs.value     = minutes * 60 * 1000L
    }

    fun startSession() {
        sessionStartTs     = System.currentTimeMillis()
        _remainingMs.value = _durationMinutes.value * 60 * 1000L
        _state.value       = TimerState.RUNNING
    }

    fun tick(deltaMs: Long) {
        val rem = (_remainingMs.value - deltaMs).coerceAtLeast(0L)
        _remainingMs.value = rem
        if (rem == 0L) finishSession(completed = true)
    }

    fun interruptSession() = viewModelScope.launch {
        _state.value  = TimerState.IDLE
        val endTs     = System.currentTimeMillis()
        val elapsed   = ((endTs - sessionStartTs) / 60_000).toInt().coerceAtLeast(1)
        repo.saveSession(sessionStartTs, endTs, elapsed, completed = false)
    }

    fun finishSession(completed: Boolean) = viewModelScope.launch {
        _state.value  = TimerState.FINISHED
        val endTs     = System.currentTimeMillis()
        repo.saveSession(sessionStartTs, endTs, _durationMinutes.value, completed)
    }

    fun reset() {
        _state.value       = TimerState.IDLE
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

    data class WeeklySummary(
        val weekRange: String,
        val avgMood: Float,
        val bestStreak: Pair<String, Int>,
        val deepWorkMinutes: Int,
        val goalProgress: List<Pair<String, Float>>
    )

    private val _weeklySummary = MutableStateFlow<WeeklySummary?>(null)
    val weeklySummary: StateFlow<WeeklySummary?> = _weeklySummary.asStateFlow()

    fun generateWeeklySummary() = viewModelScope.launch {
        val today  = LocalDate.now()
        val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val sunday = monday.plusDays(6)
        val from   = monday.format(DATE_FMT)
        val to     = sunday.format(DATE_FMT)
        val fromTs = monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toTs   = sunday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val avgMood   = moodRepo.averageInRange(from, to) ?: 0f
        val deepWork  = focusRepo.completedMinutesInRange(fromTs, toTs)
        val habits    = habitRepo.observeActive().first()
        val bestStreak = habits.maxByOrNull { it.currentStreak }?.let { it.name to it.currentStreak } ?: ("—" to 0)
        val goals     = goalRepo.observeAll().first()

        _weeklySummary.value = WeeklySummary(
            weekRange       = "$from → $to",
            avgMood         = avgMood,
            bestStreak      = bestStreak,
            deepWorkMinutes = deepWork,
            goalProgress    = goals.map { it.title to it.progressPercentage }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsViewModel  –  Google Sign-In + Drive backup
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class AccountState(
        val isSignedIn: Boolean,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?
    )

    private val _account = MutableStateFlow(readAccount())
    val account: StateFlow<AccountState> = _account.asStateFlow()

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    sealed class BackupState {
        object Idle    : BackupState()
        object Running : BackupState()
        data class Done(val message: String) : BackupState()
        data class Err(val message: String)  : BackupState()
    }

    private fun readAccount(): AccountState {
        val acct = GoogleSignIn.getLastSignedInAccount(context)
        return if (acct != null) AccountState(true, acct.displayName, acct.email, acct.photoUrl?.toString())
        else AccountState(false, null, null, null)
    }

    /** Called from the Activity after a successful Google Sign-In intent result. */
    fun onSignInSuccess(account: GoogleSignInAccount) {
        _account.value = AccountState(true, account.displayName, account.email, account.photoUrl?.toString())
    }

    fun signOut() {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        ).signOut().addOnCompleteListener {
            _account.value = AccountState(false, null, null, null)
        }
    }

    /** Manually trigger a Drive backup (also runs automatically at 03:00). */
    fun triggerBackup() = viewModelScope.launch(Dispatchers.IO) {
        _backupState.value = BackupState.Running
        try {
            val dbFile = context.getDatabasePath("fatum.db")
            if (!dbFile.exists()) error("Base de datos no encontrada")

            val backupDir = java.io.File(context.cacheDir, "backup").also { it.mkdirs() }
            val zipFile   = java.io.File(backupDir, "fatum_backup.zip")
            java.util.zip.ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("fatum.db"))
                java.io.FileInputStream(dbFile).copyTo(zip)
                zip.closeEntry()
            }
            com.fatum.workers.DriveUploader.uploadBackup(context, zipFile)
            _backupState.value = BackupState.Done("Backup subido a Google Drive ✓")
        } catch (e: Exception) {
            _backupState.value = BackupState.Err(e.message ?: "Error al hacer backup")
        } finally {
            delay(4_000)
            _backupState.value = BackupState.Idle
        }
    }
}
