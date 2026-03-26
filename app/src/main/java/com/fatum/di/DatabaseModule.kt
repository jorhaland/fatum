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

/**
 * Hilt module that provides database and DAO singletons.
 * Repositories are @Singleton and inject their own DAOs via constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FatumDatabase =
        Room.databaseBuilder(context, FatumDatabase::class.java, FatumDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration() // Replace with proper Migration in production
            .build()

    @Provides fun provideLogDao(db: FatumDatabase): LogDao = db.logDao()
    @Provides fun provideMoodDao(db: FatumDatabase): MoodDao = db.moodDao()
    @Provides fun provideHabitDao(db: FatumDatabase): HabitDao = db.habitDao()
    @Provides fun provideGoalDao(db: FatumDatabase): GoalDao = db.goalDao()
    @Provides fun provideTaskDao(db: FatumDatabase): TaskDao = db.taskDao()
    @Provides fun provideCalendarEventDao(db: FatumDatabase): CalendarEventDao = db.calendarEventDao()
    @Provides fun provideFocusSessionDao(db: FatumDatabase): FocusSessionDao = db.focusSessionDao()
}
