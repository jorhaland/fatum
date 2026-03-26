package com.fatum.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension to get the DataStore instance from Context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fatum_prefs")

/**
 * Manages lightweight user preferences using Jetpack DataStore.
 * Handles settings like: Google account name, notification preferences,
 * default focus duration, onboarding completion flag, etc.
 */
@Singleton
class FatumPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_GOOGLE_ACCOUNT  = stringPreferencesKey("google_account")
        val KEY_FOCUS_DURATION  = intPreferencesKey("focus_duration_minutes")
        val KEY_STREAK_NOTIF    = booleanPreferencesKey("streak_notifications")
        val KEY_ONBOARDED       = booleanPreferencesKey("onboarding_done")
        val KEY_DARK_MODE       = booleanPreferencesKey("dark_mode")
    }

    // ── Readers ──────────────────────────────────────────────────────────────

    val googleAccount: Flow<String?> = context.dataStore.data.map { it[KEY_GOOGLE_ACCOUNT] }

    val focusDuration: Flow<Int> = context.dataStore.data.map { it[KEY_FOCUS_DURATION] ?: 25 }

    val streakNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_STREAK_NOTIF] ?: true
    }

    val isOnboarded: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDED] ?: false }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_MODE] ?: true }

    // ── Writers ──────────────────────────────────────────────────────────────

    suspend fun setGoogleAccount(account: String?) {
        context.dataStore.edit { prefs ->
            if (account != null) prefs[KEY_GOOGLE_ACCOUNT] = account
            else prefs.remove(KEY_GOOGLE_ACCOUNT)
        }
    }

    suspend fun setFocusDuration(minutes: Int) {
        context.dataStore.edit { it[KEY_FOCUS_DURATION] = minutes }
    }

    suspend fun setStreakNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_STREAK_NOTIF] = enabled }
    }

    suspend fun setOnboarded(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDED] = done }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = dark }
    }
}
