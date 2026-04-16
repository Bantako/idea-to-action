package org.mrlem.composesample.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MemoEntity::class,
        ProjectEntity::class,
        StepEntity::class,
        DailyLogEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun memoDao(): MemoDao
    abstract fun projectDao(): ProjectDao
    abstract fun stepDao(): StepDao
    abstract fun dailyLogDao(): DailyLogDao

    class Converters {

        @TypeConverter
        fun fromProjectStatus(value: ProjectStatus): String = value.name

        @TypeConverter
        fun toProjectStatus(value: String): ProjectStatus = ProjectStatus.valueOf(value)

        @TypeConverter
        fun fromStepStatus(value: StepStatus): String = value.name

        @TypeConverter
        fun toStepStatus(value: String): StepStatus = StepStatus.valueOf(value)
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS themes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "color TEXT NOT NULL DEFAULT '#607D8B', " +
                        "createdAt INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL("ALTER TABLE nodes ADD COLUMN themeId INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS memos (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "text TEXT NOT NULL, " +
                        "projectId INTEGER, " +
                        "createdAt INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS projects (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "goal TEXT, " +
                        "status TEXT NOT NULL DEFAULT 'ACTIVE', " +
                        "focusedAt INTEGER, " +
                        "createdAt INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS steps (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "projectId INTEGER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "sortOrder INTEGER NOT NULL DEFAULT 0, " +
                        "status TEXT NOT NULL DEFAULT 'PENDING', " +
                        "doneAt INTEGER, " +
                        "createdAt INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS daily_logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "date TEXT NOT NULL, " +
                        "stepId INTEGER, " +
                        "what TEXT NOT NULL, " +
                        "note TEXT, " +
                        "createdAt INTEGER NOT NULL" +
                        ")"
                )
                db.execSQL(
                    "INSERT INTO memos (id, text, createdAt) " +
                        "SELECT id, title, createdAt FROM nodes " +
                        "WHERE status IN ('IDEA', 'DEFERRED', 'READY')"
                )
                db.execSQL(
                    "INSERT INTO projects (id, title, createdAt) " +
                        "SELECT id, name, createdAt FROM themes"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS nodes")
                db.execSQL("DROP TABLE IF EXISTS edges")
                db.execSQL("DROP TABLE IF EXISTS themes")
            }
        }
    }
}
