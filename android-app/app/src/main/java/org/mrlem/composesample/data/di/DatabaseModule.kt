package org.mrlem.composesample.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.mrlem.composesample.data.db.AppDatabase
import org.mrlem.composesample.data.db.DailyLogDao
import org.mrlem.composesample.data.db.MemoDao
import org.mrlem.composesample.data.db.ProjectDao
import org.mrlem.composesample.data.db.StepDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "idea-to-action.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
            )
            .build()

    @Provides
    fun provideMemoDao(db: AppDatabase): MemoDao = db.memoDao()

    @Provides
    fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()

    @Provides
    fun provideStepDao(db: AppDatabase): StepDao = db.stepDao()

    @Provides
    fun provideDailyLogDao(db: AppDatabase): DailyLogDao = db.dailyLogDao()
}
