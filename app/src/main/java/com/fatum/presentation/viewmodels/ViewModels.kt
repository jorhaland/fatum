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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
private val D = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private fun today() = LocalDate.now().format(D)

// ─────────────────────────────────────────────────────────────────────────────
// DashboardViewModel  –  Home screen overview
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val habitRepo: HabitRepository,
    private val taskRepo: TaskRepository,
    private val goalRepo: GoalRepository,
    private val calRepo: CalendarRepository
) : ViewModel() {

    data class DashboardState(
        val todayEvents: List<CalendarEventEntity> = emptyList(),
        val habits: List<HabitEntity>              = emptyList(),
        val habitDoneIds: Set<Int>                 = emptySet(),
        val topTasks: List<TaskEntity>             = emptyList(),
        val topGoals: List<GoalEntity>             = emptyList()
    )

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val now    = LocalDate.now()
        val from   = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val to     = now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        val todayStr = today()

        val events  = calRepo.getInRange(from, to)
        val habits  = habitRepo.observeActive().first()
        val doneIds = habits.map { h ->
            h.id to (habitRepo.observeExecutions(h.id).first()
                .any { it.dateCompleted == todayStr && it.valueLogged >= (if (h.habitType == "VALUE") h.valueTarget else 1) })
        }.filter { it.second }.map { it.first }.toSet()

        _state.value = DashboardState(
            todayEvents  = events,
            habits       = habits,
            habitDoneIds = doneIds,
            topTasks     = taskRepo.getTopPending(5),
            topGoals     = goalRepo.getTopActive(3)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HabitsViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repo: HabitRepository
) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> = repo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedDate = MutableStateFlow(today())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun selectDate(d: String) { _selectedDate.value = d }

    fun observeExecutions(id: Int) = repo.observeExecutions(id)

    fun toggleBoolean(id: Int) = viewModelScope.launch(Dispatchers.IO) { repo.toggleBoolean(id, _selectedDate.value) }

    fun logValue(id: Int, delta: Int) = viewModelScope.launch(Dispatchers.IO) { repo.logValue(id, _selectedDate.value, delta) }

    fun addHabit(name: String, type: String, valueTarget: Int, freqType: String, freqTarget: Int) =
        viewModelScope.launch(Dispatchers.IO) { repo.addHabit(name, type, valueTarget, freqType, freqTarget) }

    fun updateHabit(h: HabitEntity) = viewModelScope.launch(Dispatchers.IO) { repo.updateHabit(h) }
    fun deleteHabit(id: Int)        = viewModelScope.launch(Dispatchers.IO) { repo.deleteHabit(id) }

    suspend fun getHeatmap(id: Int) = repo.getHeatmap(id)
}

// ─────────────────────────────────────────────────────────────────────────────
// TasksViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repo: TaskRepository
) : ViewModel() {

    val pending: StateFlow<List<TaskEntity>> = repo.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completed: StateFlow<List<TaskEntity>> = repo.observeCompleted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var showCompleted by androidx.compose.runtime.mutableStateOf(false)

    fun add(title: String, desc: String?, priority: String, due: Long?) =
        viewModelScope.launch(Dispatchers.IO) { repo.add(title, desc, priority, due) }

    fun toggle(task: TaskEntity)  = viewModelScope.launch(Dispatchers.IO) { repo.toggle(task) }
    fun update(task: TaskEntity)  = viewModelScope.launch(Dispatchers.IO) { repo.update(task) }
    fun delete(task: TaskEntity)  = viewModelScope.launch(Dispatchers.IO) { repo.delete(task) }
}

// ─────────────────────────────────────────────────────────────────────────────
// GoalsViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repo: GoalRepository
) : ViewModel() {

    val goals: StateFlow<List<GoalEntity>> = repo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeMilestones(gid: Int) = repo.observeMilestones(gid)

    fun addGoal(title: String, desc: String?, priority: String, deadline: Long?,
                goalType: String, targetValue: Float) =
        viewModelScope.launch(Dispatchers.IO) { repo.addGoal(title, desc, priority, deadline, goalType, targetValue) }

    fun deleteGoal(g: GoalEntity)      = viewModelScope.launch(Dispatchers.IO) { repo.deleteGoal(g) }
    fun updateGoal(g: GoalEntity)      = viewModelScope.launch(Dispatchers.IO) { repo.updateGoal(g) }
    fun adjustValue(g: GoalEntity, d: Float) = viewModelScope.launch(Dispatchers.IO) { repo.adjustValue(g, d) }
    fun setTarget(g: GoalEntity, t: Float)   = viewModelScope.launch(Dispatchers.IO) { repo.setTargetValue(g, t) }
    fun addMilestone(gid: Int, title: String) = viewModelScope.launch(Dispatchers.IO) { repo.addMilestone(gid, title) }
    fun toggleMilestone(m: MilestoneEntity)   = viewModelScope.launch(Dispatchers.IO) { repo.toggleMilestone(m) }
    fun deleteMilestone(m: MilestoneEntity)   = viewModelScope.launch(Dispatchers.IO) { repo.deleteMilestone(m) }
}

// ─────────────────────────────────────────────────────────────────────────────
// PlannerViewModel  –  Calendar + GCal sync
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val repo: CalendarRepository,
    @ApplicationContext private val ctx: Context
) : ViewModel() {

    enum class SyncState { IDLE, SYNCING, SUCCESS, ERROR }

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    private val _syncMsg = MutableStateFlow<String?>(null)
    val syncMsg: StateFlow<String?> = _syncMsg.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val eventsForDay: StateFlow<List<CalendarEventEntity>> = _selectedDate
        .flatMapLatest { d ->
            val from = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val to   = d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            repo.observeInRange(from, to)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDate(d: LocalDate) { _selectedDate.value = d }

    fun addEvent(title: String, desc: String?, importance: String,
                 start: Long, end: Long, rrule: String?) =
        viewModelScope.launch(Dispatchers.IO) { repo.addEvent(title, desc, importance, start, end, rrule) }

    fun updateEvent(event: CalendarEventEntity) = viewModelScope.launch(Dispatchers.IO) { repo.update(event) }
    fun deleteEvent(event: CalendarEventEntity) = viewModelScope.launch(Dispatchers.IO) { repo.delete(event) }

    fun syncCalendar() = viewModelScope.launch(Dispatchers.IO) {
        if (_syncState.value == SyncState.SYNCING) return@launch
        _syncState.value = SyncState.SYNCING; _syncMsg.value = null
        try {
            val account = GoogleSignIn.getLastSignedInAccount(ctx)
                ?: error("Inicia sesión con Google en Perfil primero.")

            val cred = GoogleAccountCredential.usingOAuth2(
                ctx, listOf("https://www.googleapis.com/auth/calendar.readonly")
            ).apply { selectedAccount = account.account }

            val svc = Calendar.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), cred)
                .setApplicationName("FATUM").build()

            val now   = DateTime(System.currentTimeMillis())
            val limit = DateTime(System.currentTimeMillis() + 90L * 86_400_000)

            val items = svc.events().list("primary")
                .setMaxResults(500).setTimeMin(now).setTimeMax(limit)
                .setSingleEvents(true).setOrderBy("startTime").execute().items ?: emptyList()

            val entities = items.mapNotNull { ev ->
                val s = ev.start?.dateTime?.value ?: ev.start?.date?.value ?: return@mapNotNull null
                val e = ev.end?.dateTime?.value   ?: ev.end?.date?.value   ?: return@mapNotNull null
                CalendarEventEntity(
                    gcalEventId = ev.id,
                    title       = ev.summary ?: "(Sin título)",
                    description = ev.description,
                    importance  = "MEDIUM",
                    startTimestamp = s, endTimestamp = e,
                    recurrenceRule = ev.recurrence?.firstOrNull(),
                    isFromGcal     = true
                )
            }
            repo.syncFromGcal(entities)
            _syncState.value = SyncState.SUCCESS
            _syncMsg.value   = "${entities.size} eventos importados"
            delay(3_000); _syncState.value = SyncState.IDLE; _syncMsg.value = null
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
            _syncMsg.value   = e.message
            delay(6_000); _syncState.value = SyncState.IDLE; _syncMsg.value = null
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FocusViewModel  –  Deep Work timer
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val repo: FocusRepository
) : ViewModel() {

    enum class TimerState { IDLE, RUNNING, FINISHED }

    private val _state        = MutableStateFlow(TimerState.IDLE)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _dur          = MutableStateFlow(25)
    val durationMinutes: StateFlow<Int> = _dur.asStateFlow()

    private val _rem          = MutableStateFlow(25 * 60_000L)
    val remainingMs: StateFlow<Long> = _rem.asStateFlow()

    private var startTs = 0L

    val sessions = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDuration(m: Int) { _dur.value = m; _rem.value = m * 60_000L }

    fun startSession() { startTs = System.currentTimeMillis(); _rem.value = _dur.value * 60_000L; _state.value = TimerState.RUNNING }

    fun tick(d: Long) { val r = (_rem.value - d).coerceAtLeast(0L); _rem.value = r; if (r == 0L) finish(true) }

    fun interrupt() = viewModelScope.launch(Dispatchers.IO) {
        _state.value = TimerState.IDLE
        val end = System.currentTimeMillis()
        repo.save(startTs, end, ((end - startTs) / 60_000).toInt().coerceAtLeast(1), false)
    }

    fun finish(completed: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        _state.value = TimerState.FINISHED
        repo.save(startTs, System.currentTimeMillis(), _dur.value, completed)
    }

    fun reset() { _state.value = TimerState.IDLE; _rem.value = _dur.value * 60_000L }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProfileViewModel  –  Google Sign-In + backup
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context
) : ViewModel() {

    data class AccountState(val isSignedIn: Boolean, val name: String?, val email: String?)

    private val _account = MutableStateFlow(readAccount())
    val account: StateFlow<AccountState> = _account.asStateFlow()

    private val _opState = MutableStateFlow<OpState>(OpState.Idle)
    val opState: StateFlow<OpState> = _opState.asStateFlow()

    sealed class OpState {
        object Idle                      : OpState()
        object Loading                   : OpState()
        data class Ok(val msg: String)   : OpState()
        data class Err(val msg: String)  : OpState()
    }

    private fun readAccount(): AccountState {
        val a = GoogleSignIn.getLastSignedInAccount(ctx)
        return if (a != null) AccountState(true, a.displayName, a.email)
        else AccountState(false, null, null)
    }

    fun onSignInSuccess(a: GoogleSignInAccount) {
        _account.value = AccountState(true, a.displayName, a.email)
    }

    fun signOut() {
        GoogleSignIn.getClient(ctx,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        ).signOut().addOnCompleteListener { _account.value = AccountState(false, null, null) }
    }

    fun exportBackup() = viewModelScope.launch(Dispatchers.IO) {
        _opState.value = OpState.Loading
        try {
            val dbFile = ctx.getDatabasePath("fatum.db")
            if (!dbFile.exists()) error("Base de datos no encontrada")

            val dir   = java.io.File(ctx.cacheDir, "backup").also { it.mkdirs() }
            val zip   = java.io.File(dir, "fatum_backup.zip")
            java.util.zip.ZipOutputStream(zip.outputStream().buffered()).use { z ->
                z.putNextEntry(java.util.zip.ZipEntry("fatum.db"))
                java.io.FileInputStream(dbFile).copyTo(z); z.closeEntry()
            }
            com.fatum.workers.DriveUploader.uploadBackup(ctx, zip)
            _opState.value = OpState.Ok("Backup exportado a Google Drive ✓")
        } catch (e: Exception) {
            _opState.value = OpState.Err(e.message ?: "Error al exportar")
        } finally {
            delay(4_000); _opState.value = OpState.Idle
        }
    }

    fun importBackup(uri: android.net.Uri) = viewModelScope.launch(Dispatchers.IO) {
        _opState.value = OpState.Loading
        try {
            val dbPath = ctx.getDatabasePath("fatum.db")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                // If it's a zip, extract; if raw db, copy directly
                if (uri.toString().endsWith(".zip")) {
                    java.util.zip.ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (entry.name == "fatum.db") { dbPath.outputStream().use { zip.copyTo(it) }; break }
                            entry = zip.nextEntry
                        }
                    }
                } else {
                    dbPath.outputStream().use { input.copyTo(it) }
                }
            }
            _opState.value = OpState.Ok("Backup importado. Reinicia la app.")
        } catch (e: Exception) {
            _opState.value = OpState.Err(e.message ?: "Error al importar")
        } finally {
            delay(4_000); _opState.value = OpState.Idle
        }
    }
}
