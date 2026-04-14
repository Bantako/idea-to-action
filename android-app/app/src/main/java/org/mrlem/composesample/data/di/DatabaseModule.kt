package org.mrlem.composesample.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.mrlem.composesample.data.db.AppDatabase
import org.mrlem.composesample.data.db.EdgeDao
import org.mrlem.composesample.data.db.NodeDao
import org.mrlem.composesample.data.db.ThemeDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "idea-to-action.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideNodeDao(db: AppDatabase): NodeDao = db.nodeDao()

    @Provides
    fun provideEdgeDao(db: AppDatabase): EdgeDao = db.edgeDao()

    @Provides
    fun provideThemeDao(db: AppDatabase): ThemeDao = db.themeDao()
}
