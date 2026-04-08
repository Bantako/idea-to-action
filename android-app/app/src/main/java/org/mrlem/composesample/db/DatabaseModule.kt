package org.mrlem.composesample.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.mrlem.composesample.coaching.CoachingMessageDao
import org.mrlem.composesample.inbox.InboxDao
import org.mrlem.composesample.step.StepDao
import org.mrlem.composesample.theme.ThemeDao
import org.mrlem.composesample.today.ScheduledStepDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IdeaToActionDatabase =
        Room.databaseBuilder(context, IdeaToActionDatabase::class.java, "idea_to_action.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideInboxDao(db: IdeaToActionDatabase): InboxDao = db.inboxDao()

    @Provides
    fun provideThemeDao(db: IdeaToActionDatabase): ThemeDao = db.themeDao()

    @Provides
    fun provideStepDao(db: IdeaToActionDatabase): StepDao = db.stepDao()

    @Provides
    fun provideScheduledStepDao(db: IdeaToActionDatabase): ScheduledStepDao = db.scheduledStepDao()

    @Provides
    fun provideCoachingMessageDao(db: IdeaToActionDatabase): CoachingMessageDao = db.coachingMessageDao()
}
