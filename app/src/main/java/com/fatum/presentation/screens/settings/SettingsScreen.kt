package com.fatum.presentation.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatum.presentation.components.FatumButton
import com.fatum.presentation.components.FatumCard
import com.fatum.presentation.components.SectionHeader
import com.fatum.presentation.theme.FatumColors
import com.fatum.presentation.viewmodels.SettingsViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val account     by vm.account.collectAsStateWithLifecycle()
    val backupState by vm.backupState.collectAsStateWithLifecycle()
    val context     = LocalContext.current

    // ── Google Sign-In launcher ──────────────────────────────────────────────
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task  = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val acct  = task.getResult(ApiException::class.java)
                if (acct != null) vm.onSignInSuccess(acct)
            } catch (_: ApiException) { /* show error if needed */ }
        }
    }

    fun launchSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/calendar.readonly"),
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.appdata")
            )
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        signInLauncher.launch(client.signInIntent)
    }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Google Account ────────────────────────────────────────────────
            item { SectionHeader("Cuenta de Google") }
            item {
                FatumCard(modifier = Modifier.fillMaxWidth()) {
                    if (account.isSignedIn) {
                        // Signed-in state
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar circle
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(FatumColors.GreenSurface)
                                    .border(2.dp, FatumColors.GreenBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = account.displayName?.firstOrNull()?.uppercase() ?: "G",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = FatumColors.Green,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    account.displayName ?: "Usuario",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FatumColors.TextPrimary
                                )
                                Text(
                                    account.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FatumColors.TextMuted
                                )
                            }
                            // Green tick
                            Icon(Icons.Default.CheckCircle, null, tint = FatumColors.Green, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        // Features enabled
                        FeatureRow(Icons.Outlined.CalendarMonth, "Google Calendar", "Sincroniza tus eventos bidirecccionalmente", true)
                        Spacer(Modifier.height(6.dp))
                        FeatureRow(Icons.Outlined.CloudUpload, "Google Drive Backup", "Backup automático cada noche a las 03:00", true)
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { vm.signOut() },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FatumColors.Border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FatumColors.TextSecondary)
                        ) {
                            Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cerrar sesión", style = MaterialTheme.typography.titleSmall)
                        }
                    } else {
                        // Not signed in
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(FatumColors.SurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.AccountCircle, null, tint = FatumColors.TextMuted, modifier = Modifier.size(36.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Conecta tu cuenta de Google",
                                style = MaterialTheme.typography.titleMedium,
                                color = FatumColors.TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Activa la sincronización con Calendar y el backup automático en Drive",
                                style = MaterialTheme.typography.bodySmall,
                                color = FatumColors.TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            // Features preview
                            FeatureRow(Icons.Outlined.CalendarMonth, "Google Calendar", "Sincroniza eventos", false)
                            Spacer(Modifier.height(6.dp))
                            FeatureRow(Icons.Outlined.CloudUpload, "Google Drive", "Backup automático nightly", false)
                            Spacer(Modifier.height(16.dp))
                            FatumButton(
                                text = "Conectar con Google",
                                onClick = { launchSignIn() },
                                modifier = Modifier.fillMaxWidth(),
                                icon = {
                                    Icon(Icons.Outlined.AccountCircle, null, tint = Color(0xFF0F1117), modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }
            }

            // ── Backup manual ─────────────────────────────────────────────────
            if (account.isSignedIn) {
                item { SectionHeader("Datos") }
                item {
                    FatumCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Backup manual", style = MaterialTheme.typography.titleSmall, color = FatumColors.TextPrimary)
                                Text(
                                    when (backupState) {
                                        is SettingsViewModel.BackupState.Idle    -> "Todos tus datos guardados en Drive"
                                        is SettingsViewModel.BackupState.Running -> "Subiendo…"
                                        is SettingsViewModel.BackupState.Done    -> (backupState as SettingsViewModel.BackupState.Done).message
                                        is SettingsViewModel.BackupState.Err     -> (backupState as SettingsViewModel.BackupState.Err).message
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (backupState) {
                                        is SettingsViewModel.BackupState.Done -> FatumColors.Green
                                        is SettingsViewModel.BackupState.Err  -> FatumColors.Error
                                        else                                  -> FatumColors.TextMuted
                                    }
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            if (backupState is SettingsViewModel.BackupState.Running) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = FatumColors.Green
                                )
                            } else {
                                IconButton(
                                    onClick = { vm.triggerBackup() },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(FatumColors.GreenSurface)
                                        .size(40.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, null, tint = FatumColors.Green, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── App info ─────────────────────────────────────────────────────
            item { SectionHeader("Información") }
            item {
                FatumCard(modifier = Modifier.fillMaxWidth()) {
                    InfoRow("Versión", "1.0")
                    HorizontalDivider(color = FatumColors.DividerLine, modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Base de datos", "Room (SQLite local)")
                    HorizontalDivider(color = FatumColors.DividerLine, modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Backup automático", "Cada noche a las 03:00")
                    HorizontalDivider(color = FatumColors.DividerLine, modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Notificaciones racha", "Cada día a las 22:00")
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String, enabled: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) FatumColors.GreenSurface else FatumColors.SurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, null, tint = if (enabled) FatumColors.Green else FatumColors.TextMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = if (enabled) FatumColors.TextPrimary else FatumColors.TextSecondary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
        }
        if (enabled) {
            Text("✓", style = MaterialTheme.typography.labelLarge, color = FatumColors.Green)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextMuted)
    }
}
