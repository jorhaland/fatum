package com.fatum.presentation.screens.profile

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
import com.fatum.presentation.viewmodels.ProfileViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val account  by vm.account.collectAsStateWithLifecycle()
    val opState  by vm.opState.collectAsStateWithLifecycle()
    val context  = LocalContext.current

    // ── Backup file picker (import) ──────────────────────────────────────────
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { vm.importBackup(it) }
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val acct = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                if (acct != null) vm.onSignInSuccess(acct)
            } catch (_: ApiException) { }
        }
    }

    fun launchSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/calendar.readonly"),
                Scope("https://www.googleapis.com/auth/drive.appdata")
            ).build()
        signInLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
    }

    Scaffold(
        containerColor = FatumColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Perfil", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = FatumColors.TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FatumColors.Background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Op status banner ──────────────────────────────────────────────
            if (opState != ProfileViewModel.OpState.Idle) {
                item {
                    val (bg, txt, msg) = when (opState) {
                        is ProfileViewModel.OpState.Ok      -> Triple(FatumColors.GreenSurface, FatumColors.Green, (opState as ProfileViewModel.OpState.Ok).msg)
                        is ProfileViewModel.OpState.Err     -> Triple(FatumColors.Error.copy(.1f), FatumColors.Error, (opState as ProfileViewModel.OpState.Err).msg)
                        ProfileViewModel.OpState.Loading    -> Triple(FatumColors.SurfaceVariant, FatumColors.TextMuted, "Procesando…")
                        else -> Triple(FatumColors.Background, FatumColors.TextMuted, "")
                    }
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg)
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (opState == ProfileViewModel.OpState.Loading)
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = FatumColors.Green)
                        else Icon(if (opState is ProfileViewModel.OpState.Ok) Icons.Default.CheckCircle else Icons.Default.Error, null, tint = txt, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = txt)
                    }
                }
            }

            // ── Google Account ────────────────────────────────────────────────
            item { SectionHeader("Cuenta de Google") }
            item {
                FatumCard(modifier = Modifier.fillMaxWidth()) {
                    if (account.isSignedIn) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(52.dp).clip(CircleShape).background(FatumColors.GreenSurface)
                                    .border(2.dp, FatumColors.GreenBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(account.name?.firstOrNull()?.uppercase() ?: "G",
                                     style = MaterialTheme.typography.titleLarge,
                                     color = FatumColors.Green, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(account.name ?: "Usuario", style = MaterialTheme.typography.titleMedium, color = FatumColors.TextPrimary)
                                Text(account.email ?: "", style = MaterialTheme.typography.bodySmall, color = FatumColors.TextMuted)
                            }
                            Icon(Icons.Default.CheckCircle, null, tint = FatumColors.Green)
                        }
                        Spacer(Modifier.height(14.dp))
                        FeatureRow(Icons.Outlined.CalendarMonth, "Google Calendar", "Sincroniza eventos automáticamente a las 2:30 AM", true)
                        Spacer(Modifier.height(6.dp))
                        FeatureRow(Icons.Outlined.CloudUpload, "Google Drive", "Backup nocturno automático", true)
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(onClick = { vm.signOut() },
                            modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FatumColors.Border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FatumColors.TextSecondary)) {
                            Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cerrar sesión", style = MaterialTheme.typography.titleSmall)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Box(Modifier.size(68.dp).clip(CircleShape).background(FatumColors.SurfaceVariant), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.AccountCircle, null, tint = FatumColors.TextMuted, modifier = Modifier.size(40.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Conecta tu cuenta de Google", style = MaterialTheme.typography.titleMedium, color = FatumColors.TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("Activa Calendar y el backup automático en Drive",
                                 style = MaterialTheme.typography.bodySmall, color = FatumColors.TextMuted,
                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            FeatureRow(Icons.Outlined.CalendarMonth, "Google Calendar", "Importar eventos automáticamente", false)
                            Spacer(Modifier.height(6.dp))
                            FeatureRow(Icons.Outlined.CloudUpload, "Google Drive", "Backup automático a las 2:30 AM", false)
                            Spacer(Modifier.height(16.dp))
                            FatumButton("Conectar con Google", onClick = { launchSignIn() },
                                modifier = Modifier.fillMaxWidth(),
                                icon = { Icon(Icons.Outlined.AccountCircle, null, tint = Color(0xFF0F1117), modifier = Modifier.size(18.dp)) })
                        }
                    }
                }
            }

            // ── Backup / Restore ──────────────────────────────────────────────
            item { SectionHeader("Datos") }
            item {
                FatumCard(modifier = Modifier.fillMaxWidth()) {
                    // Export
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Exportar backup", style = MaterialTheme.typography.titleSmall, color = FatumColors.TextPrimary)
                            Text(if (account.isSignedIn) "Sube a Google Drive ahora" else "Inicia sesión para subir a Drive",
                                 style = MaterialTheme.typography.bodySmall, color = FatumColors.TextMuted)
                        }
                        if (opState == ProfileViewModel.OpState.Loading) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = FatumColors.Green)
                        } else {
                            IconButton(onClick = { vm.exportBackup() },
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(FatumColors.GreenSurface).size(40.dp)) {
                                Icon(Icons.Default.CloudUpload, null, tint = FatumColors.Green, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = FatumColors.DividerLine, modifier = Modifier.padding(vertical = 12.dp))
                    // Import
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Importar backup", style = MaterialTheme.typography.titleSmall, color = FatumColors.TextPrimary)
                            Text("Restaura desde un archivo .zip o .db", style = MaterialTheme.typography.bodySmall, color = FatumColors.TextMuted)
                        }
                        IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(FatumColors.SurfaceVariant).size(40.dp)) {
                            Icon(Icons.Default.FileDownload, null, tint = FatumColors.TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // ── App info ──────────────────────────────────────────────────────
            item { SectionHeader("Información") }
            item {
                FatumCard(modifier = Modifier.fillMaxWidth()) {
                    InfoRow("Versión", "2.0")
                    HorizontalDivider(color = FatumColors.DividerLine, modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Sync Calendar", "Automático a las 2:30 AM")
                    HorizontalDivider(color = FatumColors.DividerLine, modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Backup Drive", "Automático a las 3:00 AM")
                    HorizontalDivider(color = FatumColors.DividerLine, modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow("Almacenamiento", "100% local (SQLite)")
                }
            }

            item {
                val context = LocalContext.current
                val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
                    uri?.let {
                        try {
                            val dbFile = context.getDatabasePath("fatum.db")
                            if (dbFile.exists()) {
                                context.contentResolver.openOutputStream(it)?.use { out -> dbFile.inputStream().use { input -> input.copyTo(out) } }
                                android.widget.Toast.makeText(context, "Exportado con éxito", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) { android.widget.Toast.makeText(context, "Error al exportar", android.widget.Toast.LENGTH_SHORT).show() }
                    }
                }
                val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    uri?.let {
                        try {
                            val dbFile = context.getDatabasePath("fatum.db")
                            context.contentResolver.openInputStream(it)?.use { input -> dbFile.outputStream().use { out -> input.copyTo(out) } }
                            context.getDatabasePath("fatum.db-wal").delete()
                            context.getDatabasePath("fatum.db-shm").delete()
                            android.widget.Toast.makeText(context, "Importado. Cierra la app de la multitarea y vuelve a abrirla.", android.widget.Toast.LENGTH_LONG).show()
                        } catch (e: Exception) { android.widget.Toast.makeText(context, "Error al importar", android.widget.Toast.LENGTH_SHORT).show() }
                    }
                }

                FatumCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Copias de Seguridad Locales", style = MaterialTheme.typography.titleMedium, color = FatumColors.TextPrimary)

                        FatumButton(
                            text = "Exportar Datos (Crear archivo)",
                            onClick = { exportLauncher.launch("fatum_backup.db") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Importar Datos (Leer archivo)", color = FatumColors.TextPrimary)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String, enabled: Boolean) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(if (enabled) FatumColors.GreenSurface else FatumColors.SurfaceVariant)
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (enabled) FatumColors.Green else FatumColors.TextMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = if (enabled) FatumColors.TextPrimary else FatumColors.TextSecondary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = FatumColors.TextMuted)
        }
        if (enabled) Text("✓", style = MaterialTheme.typography.labelLarge, color = FatumColors.Green)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = FatumColors.TextMuted)
    }
}
