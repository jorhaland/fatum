package com.fatum.di

import android.content.Context
import androidx.room.Room
import com.fatum.data.db.FatumDatabase
import com.fatum.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): FatumDatabase =
        Room.databaseBuilder(ctx, FatumDatabase::class.java, FatumDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()   // dev build: wipe on schema change
            .build()

    @Provides fun provideHabitDao(db: FatumDatabase): HabitDao             = db.habitDao()
    @Provides fun provideTaskDao(db: FatumDatabase): TaskDao               = db.taskDao()
    @Provides fun provideGoalDao(db: FatumDatabase): GoalDao               = db.goalDao()
    @Provides fun provideMilestoneDao(db: FatumDatabase): MilestoneDao     = db.milestoneDao()
    @Provides fun provideCalendarEventDao(db: FatumDatabase): CalendarEventDao = db.calendarEventDao()
    @Provides fun provideFocusSessionDao(db: FatumDatabase): FocusSessionDao  = db.focusSessionDao()
}
