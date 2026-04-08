package org.mrlem.composesample.db

import androidx.room.Database
import androidx.room.RoomDatabase
import org.mrlem.composesample.coaching.CoachingMessageDao
import org.mrlem.composesample.coaching.CoachingMessageEntity
import org.mrlem.composesample.inbox.InboxDao
import org.mrlem.composesample.inbox.InboxEntryEntity
import org.mrlem.composesample.step.StepDao
import org.mrlem.composesample.step.StepEntity
import org.mrlem.composesample.theme.ThemeDao
import org.mrlem.composesample.theme.ThemeEntity
import org.mrlem.composesample.today.ScheduledStepDao
import org.mrlem.composesample.today.ScheduledStepEntity

@Database(
    entities = [
        InboxEntryEntity::class,
        ThemeEntity::class,
        StepEntity::class,
        ScheduledStepEntity::class,
        CoachingMessageEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class IdeaToActionDatabase : RoomDatabase() {
    abstract fun inboxDao(): InboxDao
    abstract fun themeDao(): ThemeDao
    abstract fun stepDao(): StepDao
    abstract fun scheduledStepDao(): ScheduledStepDao
    abstract fun coachingMessageDao(): CoachingMessageDao
}
