package com.fatum.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.glance.material3.ColorProviders
import com.fatum.data.db.FatumDatabase
import com.fatum.presentation.MainActivity
import com.fatum.presentation.theme.FatumColors

// ─────────────────────────────────────────────────────────────────────────────
// FatumWidget  –  RF-6.4
//
// BUG FIXES vs original:
//  - Removed import of non-existent androidx.glance.appwidget.lazy.LazyColumn
//  - Replaced GlanceModifier.defaultWeight() (doesn't exist) with height spacer
//  - DB is opened once per provideGlance call and properly closed after reading
//  - All DB reads wrapped in try/catch to prevent widget-triggered crashes
// ─────────────────────────────────────────────────────────────────────────────
class FatumWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var bestStreak     = 0
        var nextEventTitle = "Sin eventos próximos"

        val db = openWidgetDb(context)
        try {
            bestStreak = db.habitDao().getTopStreakDirect().coerceAtLeast(0)
            nextEventTitle = db.calendarEventDao()
                .getNextEvent(System.currentTimeMillis())?.title
                ?: "Sin eventos próximos"
        } catch (_: Exception) {
            // Never crash the widget; show safe defaults
        } finally {
            db.close()
        }

        provideContent {
            GlanceTheme {
                WidgetContent(bestStreak = bestStreak, nextEventTitle = nextEventTitle)
            }
        }
    }
}

@Composable
private fun WidgetContent(bestStreak: Int, nextEventTitle: String) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(FatumColors.Surface))
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        Text(
            text = "FATUM",
            style = TextStyle(color = ColorProvider(FatumColors.Accent), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        )
        Spacer(GlanceModifier.height(8.dp))
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = "🔥 $bestStreak días",
                style = TextStyle(color = ColorProvider(FatumColors.AccentSecondary), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "Mejor racha activa",
            style = TextStyle(color = ColorProvider(FatumColors.PrimaryVariant), fontSize = 11.sp)
        )
        Spacer(GlanceModifier.height(12.dp))
        Text(
            text = "📅 $nextEventTitle",
            style = TextStyle(color = ColorProvider(FatumColors.Primary), fontSize = 12.sp),
            maxLines = 2
        )
        // FIX: GlanceModifier.defaultWeight() does not exist. Use fixed spacer.
        Spacer(GlanceModifier.height(16.dp))
        Button(
            text = "+ Log rápido",
            onClick = actionStartActivity<MainActivity>(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ColorProvider(FatumColors.Accent),
                contentColor    = ColorProvider(FatumColors.Background)
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )
    }
}

class FatumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FatumWidget()
}

private fun openWidgetDb(context: Context): FatumDatabase =
    androidx.room.Room.databaseBuilder(
        context.applicationContext,
        FatumDatabase::class.java,
        FatumDatabase.DATABASE_NAME
    ).fallbackToDestructiveMigration().build()
